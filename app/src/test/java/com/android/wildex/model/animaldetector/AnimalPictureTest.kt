package com.android.wildex.model.animaldetector

import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AnimalPictureTest : AnimalInfoRepositoryTest() {

  override val urlPropName: String = "pexelsUrl"

  @Test
  fun `getAnimalPicture returns URL when response is successful`() = runBlocking {
    val jsonResponse =
        """
        {
          "photos": [
            {
              "src": {
                "medium": "https://example.com/animal.jpg"
              }
            }
          ]
        }
        """
            .trimIndent()

    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

    val result = repository.getAnimalPicture("lion")
    assertEquals("https://example.com/animal.jpg", result)
  }

  @Test
  fun `getAnimalPicture throws IOException on unsuccessful response`() = runBlocking {
    mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))

    val exception =
        assertThrows(IOException::class.java) {
          runBlocking { repository.getAnimalPicture("lion") }
        }

    assert(exception.message!!.contains("Unexpected code"))
  }

  @Test
  fun `getAnimalPicture throws IOException when medium is missing`() = runBlocking {
    val jsonResponse =
        """
        {
          "photos": [
            {
              "src": {}
            }
          ]
        }
        """
            .trimIndent()

    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

    val exception =
        assertThrows(IOException::class.java) {
          runBlocking { repository.getAnimalPicture("lion") }
        }

    assert(exception.message!!.contains("No content found"))
  }

  @Test
  fun `getAnimalPicture throws IOException when photos key is missing`() = runBlocking {
    val jsonResponse = """{}"""
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

    val exception =
        assertThrows(IOException::class.java) {
          runBlocking { repository.getAnimalPicture("lion") }
        }
    assert(exception.message!!.contains("No content found"))
  }

  @Test
  fun `getAnimalPicture throws IOException when first photo is null`() = runBlocking {
    val jsonResponse = """{"photos": []}"""
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

    val exception =
        assertThrows(IOException::class.java) {
          runBlocking { repository.getAnimalPicture("lion") }
        }
    assert(exception.message!!.contains("No content found"))
  }

  @Test
  fun `getAnimalPicture throws IOException when src key is missing`() = runBlocking {
    val jsonResponse =
        """
        {
          "photos": [
            {}
          ]
        }
    """
            .trimIndent()
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

    val exception =
        assertThrows(IOException::class.java) {
          runBlocking { repository.getAnimalPicture("lion") }
        }
    assert(exception.message!!.contains("No content found"))
  }

  @Test
  fun `getAnimalPicture throws IOException when medium key is missing`() = runBlocking {
    val jsonResponse =
        """
        {
          "photos": [
            { "src": {} }
          ]
        }
    """
            .trimIndent()
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

    val exception =
        assertThrows(IOException::class.java) {
          runBlocking { repository.getAnimalPicture("lion") }
        }
    assert(exception.message!!.contains("No content found"))
  }

  @Test
  fun `getAnimalPicture throws IOException when medium is not string`() = runBlocking {
    val jsonResponse =
        """
        {
          "photos": [
            { "src": { "medium": 123 } }
          ]
        }
    """
            .trimIndent()
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

    val exception =
        assertThrows(IOException::class.java) {
          runBlocking { repository.getAnimalPicture("lion") }
        }
    assert(exception.message!!.contains("No content found"))
  }
}
