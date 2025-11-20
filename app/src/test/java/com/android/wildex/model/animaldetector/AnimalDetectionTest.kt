package com.android.wildex.model.animaldetector

import android.net.Uri
import io.mockk.*
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AnimalInfoRepositoryHttpTest : AnimalInfoRepositoryTest() {

  private lateinit var mockUri: Uri

  override val urlPropName: String = "speciesNetUrl"

  @Before
  override fun setup() {
    super.setup()
    mockUri = mockk()
  }

  // ==================== detectAnimal Tests ====================

  @Test
  fun `detectAnimal should successfully detect lion with high confidence`() = runTest {
    val imageBytes = "fake_image_data".toByteArray()
    val eventId = "test-event-123"

    setupImageInputStream(imageBytes)

    mockWebServer.enqueue(
        MockResponse().setResponseCode(200).setBody("""{"event_id": "$eventId"}"""))

    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(lionDetectionResponse))

    val result = repository.detectAnimal(context, mockUri)

    assertEquals(1, result.size)
    assertEquals("lion", result[0].animalType)
    assertTrue(result[0].confidence > 0.9f)
    assertEquals("mammalia", result[0].taxonomy.animalClass)
    assertEquals("carnivora", result[0].taxonomy.order)
    assertEquals("felidae", result[0].taxonomy.family)
    assertEquals("panthera", result[0].taxonomy.genus)
    assertEquals("leo", result[0].taxonomy.species)

    assertEquals(2, mockWebServer.requestCount)
  }

  @Test
  fun `detectAnimal should filter out low confidence predictions`() = runTest {
    val imageBytes = "fake_image_data".toByteArray()
    val eventId = "test-event-123"

    setupImageInputStream(imageBytes)

    mockWebServer.enqueue(
        MockResponse().setResponseCode(200).setBody("""{"event_id":"$eventId"}"""))

    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mixedConfidenceResponse))

    val result = repository.detectAnimal(context, mockUri)

    assertEquals(2, result.size)
    assertTrue(result.all { it.confidence >= 0.5f })
    assertEquals("lion", result[0].animalType)
    assertEquals("prairie dog", result[1].animalType)
  }

  @Test
  fun `detectAnimal should handle multiple animals detected`() = runTest {
    val imageBytes = "fake_image_data".toByteArray()
    val eventId = "test-event-123"

    setupImageInputStream(imageBytes)

    mockWebServer.enqueue(
        MockResponse().setResponseCode(200).setBody("""{"event_id": "$eventId"}"""))

    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(multipleAnimalsResponse))

    val result = repository.detectAnimal(context, mockUri)

    assertEquals(3, result.size)
    result.forEach { detection ->
      assertNotNull(detection.animalType)
      assertTrue(detection.confidence >= 0.5f)
    }
    assertEquals("lion", result[0].animalType)
    assertEquals("wolf", result[1].animalType)
    assertEquals("elephant", result[2].animalType)
  }

  @Test(expected = IOException::class)
  fun `detectAnimal should throw IOException when image URI cannot be opened`() = runTest {
    every { contentResolver.openInputStream(mockUri) } returns null
    every { contentResolver.getType(mockUri) } returns "image/jpeg"

    repository.detectAnimal(context, mockUri)
  }

  @Test(expected = IOException::class)
  fun `detectAnimal should throw IOException when POST request fails`() = runTest {
    val imageBytes = "fake_image_data".toByteArray()
    setupImageInputStream(imageBytes)

    mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

    repository.detectAnimal(context, mockUri)
  }

  @Test(expected = IOException::class)
  fun `detectAnimal should throw IOException when event_id is missing`() = runTest {
    val imageBytes = "fake_image_data".toByteArray()
    setupImageInputStream(imageBytes)

    mockWebServer.enqueue(
        MockResponse().setResponseCode(200).setBody("""{"status": "success"}""") // No event_id
        )

    repository.detectAnimal(context, mockUri)
  }

  @Test(expected = IOException::class)
  fun `detectAnimal should throw IOException when GET response fails`() = runTest {
    val imageBytes = "fake_image_data".toByteArray()
    val eventId = "test-event-123"

    setupImageInputStream(imageBytes)

    mockWebServer.enqueue(
        MockResponse().setResponseCode(200).setBody("""{"event_id": "$eventId"}"""))

    mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))

    repository.detectAnimal(context, mockUri)
  }

  @Test(expected = IOException::class)
  fun `detectAnimal should throw IOException when GET response is empty`() = runTest {
    val imageBytes = "fake_image_data".toByteArray()
    val eventId = "test-event-123"

    setupImageInputStream(imageBytes)

    mockWebServer.enqueue(
        MockResponse().setResponseCode(200).setBody("""{"event_id": "$eventId"}"""))

    mockWebServer.enqueue(
        MockResponse().setResponseCode(200).setBody("") // Empty response
        )

    repository.detectAnimal(context, mockUri)
  }

  @Test
  fun `detectAnimal should send correct authorization header`() = runTest {
    val imageBytes = "fake_image_data".toByteArray()
    val eventId = "test-event-123"

    setupImageInputStream(imageBytes)

    mockWebServer.enqueue(
        MockResponse().setResponseCode(200).setBody("""{"event_id": "$eventId"}"""))

    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(lionDetectionResponse))

    repository.detectAnimal(context, mockUri)

    val postRequest = mockWebServer.takeRequest()
    assertTrue(postRequest.headers["Authorization"]?.startsWith("Bearer ") == true)

    val getRequest = mockWebServer.takeRequest()
    assertTrue(getRequest.headers["Authorization"]?.startsWith("Bearer ") == true)
  }

  @Test
  fun `detectAnimal should send image as base64 in request body`() = runTest {
    val imageBytes = "test_image".toByteArray()
    val eventId = "test-event-123"

    setupImageInputStream(imageBytes)

    mockWebServer.enqueue(
        MockResponse().setResponseCode(200).setBody("""{"event_id": "$eventId"}"""))

    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(lionDetectionResponse))

    repository.detectAnimal(context, mockUri)

    val request = mockWebServer.takeRequest()
    val requestBody = request.body.readUtf8()
    assertTrue(requestBody.contains("data:"))
    assertTrue(requestBody.contains("base64"))
  }

  @Test
  fun `detectAnimal should use correct mime type from content resolver`() = runTest {
    val imageBytes = "fake_image_data".toByteArray()
    val eventId = "test-event-123"

    every { contentResolver.openInputStream(mockUri) } returns ByteArrayInputStream(imageBytes)
    every { contentResolver.getType(mockUri) } returns "image/png"

    mockWebServer.enqueue(
        MockResponse().setResponseCode(200).setBody("""{"event_id": "$eventId"}"""))

    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(lionDetectionResponse))

    repository.detectAnimal(context, mockUri)

    val request = mockWebServer.takeRequest()
    val requestBody = request.body.readUtf8()
    assertTrue(requestBody.contains("image/png"))
  }

  @Test
  fun `detectAnimal should default to image jpeg when mime type is null`() = runTest {
    val imageBytes = "fake_image_data".toByteArray()
    val eventId = "test-event-123"

    every { contentResolver.openInputStream(mockUri) } returns ByteArrayInputStream(imageBytes)
    every { contentResolver.getType(mockUri) } returns null

    mockWebServer.enqueue(
        MockResponse().setResponseCode(200).setBody("""{"event_id": "$eventId"}"""))
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(lionDetectionResponse))

    repository.detectAnimal(context, mockUri)

    val request = mockWebServer.takeRequest()
    val requestBody = request.body.readUtf8()
    assertTrue(requestBody.contains("image/jpeg"))
  }

  @Test
  fun `detectAnimal should parse taxonomy correctly`() = runTest {
    val imageBytes = "fake_image_data".toByteArray()
    val eventId = "test-event-123"

    setupImageInputStream(imageBytes)

    mockWebServer.enqueue(
        MockResponse().setResponseCode(200).setBody("""{"event_id": "$eventId"}"""))

    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(lionDetectionResponse))

    val result = repository.detectAnimal(context, mockUri)

    val taxonomy = result[0].taxonomy
    assertEquals("ddf59264-185a-4d35-b647-2785792bdf54", taxonomy.id)
    assertEquals("mammalia", taxonomy.animalClass)
    assertEquals("carnivora", taxonomy.order)
    assertEquals("felidae", taxonomy.family)
    assertEquals("panthera", taxonomy.genus)
    assertEquals("leo", taxonomy.species)
  }

  // ==================== Helper Methods ====================

  private fun setupImageInputStream(imageBytes: ByteArray) {
    every { contentResolver.openInputStream(mockUri) } returns ByteArrayInputStream(imageBytes)
    every { contentResolver.getType(mockUri) } returns "image/jpeg"
  }

  // ==================== Test Data Helpers ====================

  private val lionDetectionResponse: String =
      """
        event: complete
        data: [
          {
            "predictions": [
              {
                "classifications": {
                  "classes": [
                    "ddf59264-185a-4d35-b647-2785792bdf54;mammalia;carnivora;felidae;panthera;leo;lion"
                  ],
                  "scores": [0.987]
                }
              }
            ]
          }
        ]
      """

  private val mixedConfidenceResponse: String =
      """
        event: complete
        data: [
          {
            "predictions": [
              {
                "classifications": {
                  "classes": [
                    "id1;mammalia;carnivora;felidae;panthera;leo;lion",
                    "id2;mammalia;rodentia;sciuridae;cynomys;ludovicianus;prairie dog",
                    "id3;aves;;;;;bird"
                  ],
                  "scores": [0.95, 0.6, 0.3]
                }
              }
            ]
          }
        ]
      """

  private val multipleAnimalsResponse: String =
      """
        event: complete
        data: [
          {
            "predictions": [
              {
                "classifications": {
                  "classes": [
                    "id1;mammalia;carnivora;felidae;panthera;leo;lion",
                    "id2;mammalia;carnivora;canidae;canis;lupus;wolf",
                    "id3;mammalia;proboscidea;elephantidae;loxodonta;africana;elephant"
                  ],
                  "scores": [0.85, 0.75, 0.65]
                }
              }
            ]
          }
        ]
      """
}
