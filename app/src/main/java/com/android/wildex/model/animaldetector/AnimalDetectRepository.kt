package com.android.wildex.model.animaldetector

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.wildex.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

/**
 * Repository class for interacting with the AnimalDetect API.
 *
 * This class provides a method to detect animals in an image by uploading it to the AnimalDetect
 * API and parsing the response.
 *
 * @property client The OkHttpClient instance used for making HTTP requests.
 */
class AnimalDetectRepository(val client: OkHttpClient) {
  private val adApikey = BuildConfig.ANIMALDETECT_API_KEY

  private val hfApikey = BuildConfig.HUGGINGFACE_API_KEY

  private val adBaseurl = "https://www.animaldetect.com/api/v1/detect"

  private val hfBaseurl = "https://router.huggingface.co/v1/chat/completions"

  /**
   * Detects animals in the provided image URI. **Important:** This function performs network I/O
   * and should be called from a background thread, such as a coroutine running on
   * [kotlinx.coroutines.Dispatchers.IO].
   *
   * @param context The Android context, used to access the [android.content.ContentResolver].
   * @param imageUri The URI of the image to detect.
   * @return List<AnimalDetectResponse> containing detected labels and confidence scores, or empty list if
   *   the request fails or the response is invalid.
   */
  suspend fun detectAnimal(
      context: Context,
      imageUri: Uri,
      countryCode: String = "KEN",
  ): List<AnimalDetectResponse> =
      withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
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
                  .addFormDataPart("country", countryCode)
                  .build()

          val request =
              Request.Builder()
                  .url(adBaseurl)
                  .addHeader("Authorization", "Bearer $adApikey")
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
              val annotations = jsonElem.jsonObject["annotations"]?.jsonArray ?: return@use emptyList()

              annotations.mapNotNull { result ->
                val obj = result.jsonObject
                val label = obj["label"]?.jsonPrimitive?.takeIf { it.isString }?.content ?: return@mapNotNull null
                val confidence = obj["score"]?.jsonPrimitive?.float ?: return@mapNotNull null
                val bboxArr = obj["bbox"]?.jsonArray ?: return@mapNotNull null
                if (bboxArr.size != 4) return@mapNotNull null
                val boundingBox = BoundingBox(
                  bboxArr[0].jsonPrimitive.float,
                  bboxArr[1].jsonPrimitive.float,
                  bboxArr[2].jsonPrimitive.float,
                  bboxArr[3].jsonPrimitive.float
                )
                val taxonomyObj = obj["taxonomy"]?.jsonObject ?: return@mapNotNull null
                val taxonomy = Taxonomy(
                  taxonomyObj["id"]?.jsonPrimitive?.content ?: "",
                  taxonomyObj["class"]?.jsonPrimitive?.content ?: "",
                  taxonomyObj["order"]?.jsonPrimitive?.content ?: "",
                  taxonomyObj["family"]?.jsonPrimitive?.content ?: "",
                  taxonomyObj["genus"]?.jsonPrimitive?.content ?: "",
                  taxonomyObj["species"]?.jsonPrimitive?.content ?: ""
                )
                AnimalDetectResponse(label, confidence, boundingBox, taxonomy)
              }
            }
        } catch (e: Exception) {
          Log.e(this.javaClass.name, "Error detecting animal: ${e.message}", e)
          emptyList()
        } finally {
          // Clean up temp file
          tempFile?.delete()?.let {
            if (!it) {
              Log.e(this.javaClass.name, "Error deleting temp file")
            }
          }
        }
      }

  /**
   * Requests a short descriptive paragraph for a given animal from a HuggingFace LLM.
   *
   * This function sends a POST request to the LLM API with a system prompt instructing the model to
   * behave as a wildlife guide and a user prompt containing the animal name. The response is parsed
   * to extract the response content.
   *
   * @param animalName The name of the animal to describe.
   * @return A cleaned description string if the request succeeds and a valid response is received;
   *   null if an error occurs, the response body is empty, or the expected content is missing.
   * @throws IOException Internally caught; any network or parsing errors result in null.
   */
  suspend fun getAnimalDescription(animalName: String): String? =
      withContext(Dispatchers.IO) {
        try {
          val payload =
              """
            {
              "model": "deepseek-ai/DeepSeek-R1:novita",
              "messages": [
                { "role": "system", "content" : "You are a wildlife guide, describe the given animal species in a very short paragraph, no line breaks or anything. Do not give me the thinking process." },
                { "role": "user", "content": "Describe this animal in a few sentences: $animalName" }
              ],
              "stream": false
            }
        """
                  .trimIndent()

          val requestBody = payload.toRequestBody("application/json".toMediaType())
          val request =
              Request.Builder()
                  .url(hfBaseurl)
                  .addHeader("Authorization", "Bearer $hfApikey")
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
                ?.takeIf { it.isString }
                ?.content
                ?.replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "")
                ?.trim() ?: throw IOException("No content found")
          }
        } catch (e: Exception) {
          Log.e(this.javaClass.name, "Error getting animal description: ${e.message}", e)
          null
        }
      }
}
