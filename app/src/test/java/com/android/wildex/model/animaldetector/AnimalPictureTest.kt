package com.android.wildex.model.animaldetector

import android.content.Context
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnimalPictureTest : AnimalInfoRepositoryTest() {

  override val urlPropName: String = "pexelsUrl"

  private lateinit var mockContext: Context
  private lateinit var cacheDir: File

  @Before
  fun setupContext() {
    cacheDir = createTempDirectory("test_cache").toFile()
    mockContext = mock<Context> { on { cacheDir } doReturn cacheDir }
  }

  @Test
  fun `getAnimalPicture returns Uri when response is successful`() = runBlocking {
    val jsonResponse =
        """
        {
          "photos": [
            {
              "src": {
                "large": "${mockWebServer.url("/test-image.jpg")}"
              }
            }
          ]
        }
        """
            .trimIndent()

    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

    // Mock the image download
    val fakeImageData = ByteArray(100) { it.toByte() }
    mockWebServer.enqueue(
        MockResponse().setResponseCode(200).setBody(okio.Buffer().write(fakeImageData)))

    val result = repository.getAnimalPicture(mockContext, "lion")

    assertTrue(result.scheme == "file")
    assertTrue(result.path?.contains("temp_upload_") == true)
    assertTrue(result.path?.endsWith(".jpg") == true)
  }

  @Test
  fun `getAnimalPicture throws IOException on unsuccessful response`() = runBlocking {
    mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))

    val exception =
        assertThrows(IOException::class.java) {
          runBlocking { repository.getAnimalPicture(mockContext, "lion") }
        }

    assert(exception.message!!.contains("Unexpected code"))
  }

  @Test
  fun `getAnimalPicture throws IOException when photos key is missing`() = runBlocking {
    val jsonResponse = """{}"""
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

    val exception =
        assertThrows(IOException::class.java) {
          runBlocking { repository.getAnimalPicture(mockContext, "lion") }
        }
    assert(exception.message!!.contains("No content found"))
  }

  @Test
  fun `getAnimalPicture throws IOException when first photo is null`() = runBlocking {
    val jsonResponse = """{"photos": []}"""
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

    val exception =
        assertThrows(IOException::class.java) {
          runBlocking { repository.getAnimalPicture(mockContext, "lion") }
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
          runBlocking { repository.getAnimalPicture(mockContext, "lion") }
        }
    assert(exception.message!!.contains("No content found"))
  }

  @Test
  fun `getAnimalPicture throws IOException when large key is missing`() = runBlocking {
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
          runBlocking { repository.getAnimalPicture(mockContext, "lion") }
        }
    assert(exception.message!!.contains("No content found"))
  }

  @Test
  fun `getAnimalPicture throws IOException when large is not string`() = runBlocking {
    val jsonResponse =
        """
        {
          "photos": [
            { "src": { "large": 123 } }
          ]
        }
        """
            .trimIndent()
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

    val exception =
        assertThrows(IOException::class.java) {
          runBlocking { repository.getAnimalPicture(mockContext, "lion") }
        }
    assert(exception.message!!.contains("No content found"))
  }
}
