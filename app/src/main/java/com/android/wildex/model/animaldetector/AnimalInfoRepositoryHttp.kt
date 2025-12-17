package com.android.wildex.model.animaldetector

import android.content.Context
import android.net.Uri
import com.android.wildex.BuildConfig
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Repository class for interacting with the AnimalDetect API.
 *
 * This class provides a method to detect animals in an image by uploading it to the AnimalDetect
 * API and parsing the response.
 *
 * @property client The OkHttpClient instance used for making HTTP requests.
 */
class AnimalInfoRepositoryHttp(client: OkHttpClient) : AnimalInfoRepository {
  companion object {
    private const val CONTENT_TYPE = "application/json"
  }

  private val client =
      client
          .newBuilder()
          .connectTimeout(60, TimeUnit.SECONDS)
          .readTimeout(60, TimeUnit.SECONDS)
          .writeTimeout(60, TimeUnit.SECONDS)
          .build()
  private val hfApikey = BuildConfig.HUGGINGFACE_API_KEY
  private val pxApiKey = BuildConfig.PEXELS_API_KEY

  private val speciesNetUrl = "https://youssef-9511-speciesnetapi.hf.space/gradio_api/call/predict"

  private val deepseekUrl = "https://router.huggingface.co/v1/chat/completions"
  private val pexelsUrl = "https://api.pexels.com/v1/"

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
  override suspend fun detectAnimal(
      context: Context,
      imageUri: Uri,
      coroutineContext: CoroutineContext,
  ): List<AnimalDetectResponse> =
      withContext(coroutineContext) {
        val bytes =
            context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                ?: throw IOException("Failed to open image URI")

        val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
        val base64Image = Base64.encode(bytes)

        val dataUri = "data:$mimeType;base64,$base64Image"
        val jsonBody =
            """
                {
                    "data": [
                        "$dataUri",
                        null
                    ]
                }
            """
                .trimIndent()
        val requestBody = jsonBody.toRequestBody(CONTENT_TYPE.toMediaType())
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
                .url("$speciesNetUrl/$eventId")
                .addHeader("Authorization", "Bearer $hfApikey")
                .get()
                .build()

        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) throw IOException("Unexpected GET response: $response")

          val responseBody = response.body!!.string()
          if (responseBody.isEmpty()) throw IOException("Empty GET response")

          try {
            parseAnimalDetectResponse(responseBody)
          } catch (_: Exception) {
            throw IOException("Invalid JSON response")
          }
        }
      }

  /* Example raw:
     event: complete
     data: [
             {
               "predictions": [
                 {
                   "filepath": "/tmp/tmp7mg1vv12.jpg",
                   "classifications": {
                     "classes": [
                       "ddf59264-185a-4d35-b647-2785792bdf54;mammalia;carnivora;felidae;panthera;leo;lion",
                       "fdc27cfb-3756-4794-992d-1d512f7c5474;mammalia;rodentia;sciuridae;cynomys;ludovicianus;arizona black-tailed prairie dog",
                       "b1352069-a39c-4a84-a949-60044271c0c1;aves;;;;;bird",
                       "6ffe2064-cabd-4fcb-8c1b-f168bf381aab;mammalia;cetartiodactyla;bovidae;tragelaphus;oryx;common eland",
                       "3d80f1d6-b1df-4966-9ff4-94053c7a902a;mammalia;carnivora;canidae;canis;familiaris;domestic dog"
                     ],
                     "scores": [
                       0.9870538115501404,
                       0.006546854041516781,
                       0.003047803184017539,
                       0.0005521145649254322,
                       0.00034705971484072506
                     ]
                   },
                   "detections": [
                     {
                       "category": "1",
                       "label": "animal",
                       "conf": 0.8112816214561462,
                       "bbox": [
                         0.1919642984867096,
                         0.0,
                         0.7633928656578064,
                         0.9822221994400024
                       ]
                     }
                   ],
                   "prediction": "ddf59264-185a-4d35-b647-2785792bdf54;mammalia;carnivora;felidae;panthera;leo;lion",
                   "prediction_score": 0.9870538115501404,
                   "prediction_source": "classifier",
                   "model_version": "4.0.1a"
                 }
               ]
             }
           ]
  */
  private fun parseAnimalDetectResponse(raw: String): List<AnimalDetectResponse> {
    val jsonArrayText = raw.substringAfter("data: ").trim()
    val dataArray = Json.parseToJsonElement(jsonArrayText)

    val classificationsObject =
        dataArray.jsonArray
            .firstOrNull()
            ?.jsonObject
            ?.get("predictions")
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("classifications")
            ?.jsonObject ?: throw IOException("No classifications found")

    val classesArray = classificationsObject["classes"]?.jsonArray
    val scoresArray = classificationsObject["scores"]?.jsonArray
    if (classesArray == null || scoresArray == null) throw IOException("No classes or scores found")
    val length = minOf(classesArray.size, scoresArray.size)

    return (0 until length).mapNotNull { i ->
      val className = classesArray[i].jsonPrimitive.content
      val score = scoresArray[i].jsonPrimitive.float
      if (score < 0.7) null
      else {
        val (taxonomy, animalType) = parseClassification(className)
        if (animalType == "blank" || animalType == "vehicle" || animalType == "human")
            return@mapNotNull null
        AnimalDetectResponse(
            animalType = animalType,
            confidence = score,
            taxonomy = taxonomy,
        )
      }
    }
  }

  private fun parseClassification(classification: String): Pair<Taxonomy, String> {
    val parts = classification.split(";")
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
    return Pair(taxonomy, animalType)
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
  override suspend fun getAnimalDescription(
      animalName: String,
      coroutineContext: CoroutineContext,
  ): String =
      withContext(coroutineContext) {
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

        val requestBody = payload.toRequestBody(CONTENT_TYPE.toMediaType())
        val request =
            Request.Builder()
                .url(deepseekUrl)
                .addHeader("Authorization", "Bearer $hfApikey")
                .addHeader("Content-Type", CONTENT_TYPE)
                .post(requestBody)
                .build()

        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) throw IOException("Unexpected code $response")

          val body = response.body?.string() ?: throw IOException("Empty response body")
          try {
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
          } catch (_: IllegalArgumentException) {
            throw IOException("Invalid JSON response")
          }
        }
      }

  override suspend fun getAnimalPicture(
      context: Context,
      animalName: String,
      coroutineContext: CoroutineContext,
  ): Uri =
      withContext(coroutineContext) {
        val search = "search?query=$animalName&size=small&page=1&per_page=1"
        val request =
            Request.Builder()
                .url("$pexelsUrl$search")
                .addHeader("Authorization", pxApiKey)
                .get()
                .build()

        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) throw IOException("Unexpected code $response")

          val body = response.body?.string() ?: throw IOException("No response body")
          val json = Json.parseToJsonElement(body)
          val url =
              json.jsonObject["photos"]
                  ?.jsonArray
                  ?.firstOrNull()
                  ?.jsonObject
                  ?.get("src")
                  ?.jsonObject
                  ?.get("large")
                  ?.jsonPrimitive
                  ?.takeIf { it.isString }
                  ?.content ?: throw IOException("No content found")
          val inputStream = URL(url).openConnection().getInputStream()
          val cacheFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
          cacheFile.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
          Uri.fromFile(cacheFile)
        }
      }
}
