package com.android.wildex.model.animaldetector

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.wildex.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray

/**
 * Repository class for interacting with the AnimalDetect API.
 *
 * This class provides a method to detect animals in an image by uploading it to the AnimalDetect
 * API and parsing the response.
 *
 * @property client The OkHttpClient instance used for making HTTP requests.
 */
class AnimalInfoRepositoryHttp(val client: OkHttpClient) : AnimalInfoRepository {

  private val hfApikey = BuildConfig.HUGGINGFACE_API_KEY

  private val speciesNetUrl = "https://youssef-9511-speciesnetapi.hf.space/gradio_api/call/predict"

  private val deepseekUrl = "https://router.huggingface.co/v1/chat/completions"

  /**
   * Detects animals in the provided image URI. **Important:** This function performs network I/O
   * and should be called from a background thread, such as a coroutine running on
   * [kotlinx.coroutines.Dispatchers.IO].
   *
   * @param context The Android context, used to access the [android.content.ContentResolver].
   * @param imageUri The URI of the image to detect.
   * @return List<AnimalDetectResponse> containing detected labels and confidence scores, or throws
   *   exception if failed.
   */
  @OptIn(ExperimentalEncodingApi::class)
  override suspend fun detectAnimal(context: Context, imageUri: Uri): List<AnimalDetectResponse> =
      withContext(Dispatchers.IO) {
        val bytes =
            context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                ?: throw IOException("Failed to open image URI")

        val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
        val base64Image = Base64.encode(bytes)

        val dataUri = "data:$mimeType;base64,$base64Image"
        val jsonBody = """{"data": ["$dataUri"]}"""
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
        val eventIdRequest =
            Request.Builder()
                .url(speciesNetUrl)
                .addHeader("Authorization", "Bearer $hfApikey")
                .post(requestBody)
                .build()

        val eventId =
            client.newCall(eventIdRequest).execute().use { response ->
              if (!response.isSuccessful) throw IOException("Unexpected POST response: $response")

              val body = response.body?.string() ?: throw IOException("Empty POST response")

              val json = Json.parseToJsonElement(body).jsonObject
              json["event_id"]?.jsonPrimitive?.content ?: throw IOException("No event ID returned")
            }

        val request =
            Request.Builder()
                .url("https://youssef-9511-speciesnetapi.hf.space/gradio_api/call/predict/$eventId")
                .addHeader("Authorization", "Bearer $hfApikey")
                .get()
                .build()

        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) throw IOException("Unexpected GET response: $response")

          val responseBody = response.body!!.string()
          if (responseBody.isEmpty()) throw IOException("Empty GET response")

          parseAnimalDetectResponse(responseBody)
        }
      }

  private fun parseAnimalDetectResponse(raw: String): List<AnimalDetectResponse> {
    // isolate the JSON array after "data: "
    val jsonArrayText = raw.substringAfter("data: ").trim()
    val dataArray = JSONArray(jsonArrayText)

    val dataObj = dataArray.getJSONObject(0)
    val predictions = dataObj.getJSONArray("predictions")

    return (0 until predictions.length()).map { i ->
      val predictionObject = predictions.getJSONObject(i)
      val prediction = predictionObject.getString("prediction")
      val score = predictionObject.getDouble("prediction_score").toFloat()

      val box = predictionObject.getJSONArray("detections").getJSONObject(0).getJSONArray("bbox")
      val boundingBox =
          BoundingBox(
              x = box.getDouble(0).toFloat(),
              y = box.getDouble(1).toFloat(),
              width = box.getDouble(2).toFloat(),
              height = box.getDouble(3).toFloat(),
          )

      val parts = prediction.split(";")
      val taxonomy =
          Taxonomy(
              id = parts.getOrNull(0) ?: "",
              animalClass = parts.getOrNull(1) ?: "",
              order = parts.getOrNull(2) ?: "",
              family = parts.getOrNull(3) ?: "",
              genus = parts.getOrNull(4) ?: "",
              species = parts.getOrNull(5) ?: "",
          )
      val animalType = parts.getOrNull(6) ?: ""

      AnimalDetectResponse(
          animalType = animalType,
          confidence = score,
          boundingBox = boundingBox,
          taxonomy = taxonomy,
      ).also {
        Log.d("AnimalInfoRepository", "Parsed response: $it")
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
   * @throws IOException if the request fails or the response is invalid.
   */
  override suspend fun getAnimalDescription(animalName: String): String =
      withContext(Dispatchers.IO) {
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
                .url(deepseekUrl)
                .addHeader("Authorization", "Bearer $hfApikey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

        val longTimeoutClient =
            client
                .newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        longTimeoutClient.newCall(request).execute().use { response ->
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
      }
}
