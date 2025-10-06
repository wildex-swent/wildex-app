package com.android.wildex.model.animaldetector

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.ByteArrayInputStream
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class AnimalDetectRepositoryTest {

  private lateinit var mockWebServer: MockWebServer
  private lateinit var client: OkHttpClient
  private lateinit var repository: AnimalDetectRepository
  private lateinit var context: Context
  private lateinit var contentResolver: ContentResolver
  private lateinit var testUri: Uri

  @Before
  fun setUp() {
    mockWebServer = MockWebServer()
    mockWebServer.start()

    client = OkHttpClient()
    repository = AnimalDetectRepository(client)

    // Mock context and its content resolver
    context = Mockito.mock(Context::class.java)
    contentResolver = Mockito.mock(ContentResolver::class.java)
    Mockito.`when`(context.contentResolver).thenReturn(contentResolver)

    testUri = Mockito.mock(Uri::class.java)
    val fakeImage = ByteArrayInputStream(ByteArray(10))
    Mockito.`when`(contentResolver.getType(testUri)).thenReturn("image/jpeg")
    Mockito.`when`(contentResolver.openInputStream(testUri)).thenReturn(fakeImage)

    // Override client
    val field = repository.javaClass.getDeclaredField("client")
    field.isAccessible = true
    field.set(repository, client)

    // Override baseUrl. Path matching is optional in this case.
    val mockUrl = mockWebServer.url("/api/v1/detect").toString()
    val urlField = repository.javaClass.getDeclaredField("baseUrl")
    urlField.isAccessible = true
    urlField.set(repository, mockUrl)
  }

  @After
  fun tearDown() {
    mockWebServer.shutdown()
  }

  @Test
  fun `detectAnimal returns correct response on success`() {
    // Mock API response
    val jsonResponse =
        """
            {
              "annotations": [
                { "label": "Dog", "score": 0.95 }
              ]
            }
        """
            .trimIndent()
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))
    val result = repository.detectAnimal(context, testUri)

    assertNotNull(result)
    assertEquals("Dog", result?.animalType)
    assertEquals(0.95f, result?.confidence)
  }

  @Test
  fun `detectAnimal returns null on empty body`() {
    // Mock API response
    val jsonResponse = ""
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))
    val result = repository.detectAnimal(context, testUri)

    assertNull(result)
  }

  @Test
  fun `detectAnimal returns null on field 'annotations' missing`() {
    val jsonResponse =
        """
            {
              "predictions": [
                { "label": "Dog", "score": 0.95 }
              ]
            }
        """
            .trimIndent()
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))
    val result = repository.detectAnimal(context, testUri)

    assertNull(result)
  }

  @Test
  fun `detectAnimal returns null on field 'label' missing`() {
    val jsonResponse =
        """
            {
              "annotations": [
                { "type": "Dog", "score": 0.95 }
              ]
            }
        """
            .trimIndent()
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))
    val result = repository.detectAnimal(context, testUri)

    assertNull(result)
  }

  @Test
  fun `detectAnimal returns null on incorrect field 'label' value`() {
    val jsonResponse =
        """
            {
              "annotations": [
                { "label": 37, "score": 0.95 }
              ]
            }
        """
            .trimIndent()
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))
    val result = repository.detectAnimal(context, testUri)

    assertNull(result)
  }

  @Test
  fun `detectAnimal returns null on field 'score' missing`() {
    val jsonResponse =
        """
            {
              "annotations": [
                { "label": "Dog", "confidence": 0.95 }
              ]  
            }
        """
            .trimIndent()
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))
    val result = repository.detectAnimal(context, testUri)

    assertNull(result)
  }

  @Test
  fun `detectAnimal returns null on incorrect field 'score' value`() {
    val jsonResponse =
        """
            {
              "predictions": [
                { "label": "Dog", "score": "invalid" }
              ]
            }
        """
            .trimIndent()
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))
    val result = repository.detectAnimal(context, testUri)

    assertNull(result)
  }

  @Test
  fun `detectAnimal returns null on invalid URI`() {
    val invalidUri = Mockito.mock(Uri::class.java)
    Mockito.`when`(contentResolver.openInputStream(invalidUri)).thenReturn(null)

    val result = repository.detectAnimal(context, invalidUri)

    assertNull(result)
  }

  @Test
  fun `detectAnimal returns null on HTTP error`() {
    mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))
    val result = repository.detectAnimal(context, testUri)

    assertNull(result)
  }
}
