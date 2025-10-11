package com.android.wildex.model.animaldetector

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.wildex.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Repository class for interacting with the AnimalDetect API.
 *
 * This class provides a method to detect animals in an image by uploading it to the AnimalDetect
 * API and parsing the response.
 *
 * @property client The OkHttpClient instance used for making HTTP requests.
 */
class AnimalDetectRepository(val client: OkHttpClient) {
    private val ad_apiKey = BuildConfig.ANIMALDETECT_API_KEY

    private val hf_apiKey = BuildConfig.HUGGINGFACE_API_KEY

    private val ad_baseUrl = "https://www.animaldetect.com/api/v1/detect"

    private val hf_baseUrl = "https://router.huggingface.co/v1/chat/completions"

  /**
   * Detects an animal in the provided image URI.
   *
   * Steps performed:
   * 1. Creates a temporary file in the app's cache directory from the given [imageUri].
   * 2. Determines the MIME type of the image and maps it to a suitable file extension.
   * 3. Builds a multipart/form-data HTTP POST request containing:
   *     - The image file
   *     - Country ("USA")
   *     - Detection threshold ("0.2")
   * 4. Executes the request synchronously using [OkHttpClient].
   * 5. Parses the JSON response and extracts the first annotation's label and confidence score.
   * 6. Cleans up the temporary file after the request.
   *
   * **Important:** This function performs network I/O and should be called from a background
   * thread, such as a coroutine running on [kotlinx.coroutines.Dispatchers.IO].
   *
   * @param context The Android context, used to access the [android.content.ContentResolver].
   * @param imageUri The URI of the image to detect.
   * @return [AnimalDetectResponse] containing the detected label and confidence score, or `null` if
   *   the request fails or the response is invalid.
   */
  fun detectAnimal(context: Context, imageUri: Uri): AnimalDetectResponse? {
    var tempFile: File? = null
    return try {
      // Detect MIME type and map to file extension
      val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"

      // Create temporary file
      tempFile = File.createTempFile("upload", ".jpg", context.cacheDir)
      context.contentResolver.openInputStream(imageUri)?.use { input ->
        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
      } ?: throw IOException("Failed to open image URI")

      // Build multipart/form-data request
      val requestBody =
          MultipartBody.Builder()
              .setType(MultipartBody.FORM)
              .addFormDataPart(
                  "image",
                  tempFile.name,
                  tempFile.asRequestBody(mimeType.toMediaTypeOrNull()),
              )
              .addFormDataPart("country", "KEN")
              .addFormDataPart("threshold", "0.2")
              .build()

      val request =
          Request.Builder()
              .url(ad_baseUrl)
              .addHeader("Authorization", "Bearer $ad_apiKey")
              .post(requestBody)
              .build()

      // Execute request
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IOException("Unexpected code $response")
        }

        val body = response.body?.string()
        if (body.isNullOrEmpty()) {
            throw IOException("Empty response body")
        }

        // Parse JSON response
        val jsonElem = Json.parseToJsonElement(body)
        val result = jsonElem.jsonObject["annotations"]?.jsonArray?.firstOrNull()
        if (result == null) {
            throw IOException("No annotations found")
        }

        val label = result.jsonObject["label"]?.jsonPrimitive?.takeIf { it.isString }?.content
        val confidence = result.jsonObject["score"]?.jsonPrimitive?.float

        if (label == null || confidence == null) {
            throw IOException("Invalid response body structure")
        }

        return AnimalDetectResponse(label, confidence)
      }
    } catch (e: Exception) {
      Log.e("AnimalDetectRepository", "Error detecting animal: ${e.message}", e)
      null
    } finally {
      // Clean up temp file
        tempFile?.delete()?.let {
            if (!it) {
                Log.e("AnimalDetectRepository", "Error deleting temp file")
            }
        }
    }
  }

    fun getAnimalDescription(animalName: String): String? =
        try {
            val payload =
                """
            {
              "model": "deepseek-ai/DeepSeek-R1:novita",
              "messages": [
                { "role": "system", "content" : "You are a wildlife guide, describe the given animal species in a short paragraph, no line breaks or anything. Do not give me the thinking process." },
                { "role": "user", "content": "Describe this animal in a few sentences: $animalName" }
              ],
              "max_tokens": 100,
              "stream": false
            }
        """
                    .trimIndent()

            val requestBody = payload.toRequestBody("application/json".toMediaType())
            val request =
                Request.Builder()
                    .url(hf_baseUrl)
                    .addHeader("Authorization", "Bearer $hf_apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

            val client =
                client
                    .newBuilder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val body = response.body?.string() ?: throw IOException("Empty response body")
                val json = Json.parseToJsonElement(body)
                json.jsonObject["choices"]
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject
                    ?.get("message")
                    ?.jsonObject
                    ?.get("content")
                    ?.jsonPrimitive
                    ?.content
                    ?.replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "")
                    ?.replace(Regex("^\\s*Description\\s*:\\s*", RegexOption.IGNORE_CASE), "")
                    ?.trim() ?: throw IOException("No content found")
            }
        } catch (e: Exception) {
            Log.e("AnimalDetectRepository", "Error getting animal description: ${e.message}", e)
            null
        }
}
