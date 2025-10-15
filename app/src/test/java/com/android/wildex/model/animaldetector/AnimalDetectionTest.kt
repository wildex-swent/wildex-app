package com.android.wildex.model.animaldetector

import android.net.Uri
import com.android.wildex.BuildConfig
import java.io.ByteArrayInputStream
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class AnimalDetectionTest : AnimalDetectRepositoryTest() {
  private lateinit var testUri: Uri
  override val urlPropName: String = "adBaseurl"

  @Before
  override fun setUp() {
    super.setUp()
    testUri = Mockito.mock(Uri::class.java)
    val fakeImage = ByteArrayInputStream(ByteArray(10))
    Mockito.`when`(contentResolver.getType(testUri)).thenReturn("image/jpeg")
    Mockito.`when`(contentResolver.openInputStream(testUri)).thenReturn(fakeImage)
  }

  @Test
  fun `detectAnimal forms correct multipart request`() = runTest {
    repository.detectAnimal(context, testUri)

    val recorded = mockWebServer.takeRequest()
    assertEquals("POST", recorded.method)

    val contentType = recorded.getHeader("Content-Type")
    assertNotNull(contentType)
    assertTrue(contentType!!.startsWith("multipart/form-data"))
    val apiKey = recorded.getHeader("Authorization")
    assertNotNull(apiKey)
    assertEquals("Bearer ${BuildConfig.ANIMALDETECT_API_KEY}", apiKey)

    val body = recorded.body.readUtf8()
    assertTrue(body.contains("name=\"image\""))
    assertTrue(body.contains("name=\"country\""))
  }

  @Test
  fun `detectAnimal returns correct response on success`() = runTest {
    // Mock API response with full annotation
    val jsonResponse =
        """
        {
          "annotations": [
            {
              "label": "canine family",
              "score": 0.9975,
              "bbox": [0.4127, 0.8261, 0.2027, 0.1727],
              "taxonomy": {
                "id": "3184697f-51ad-4608-9a28-9edb5500159c",
                "class": "mammalia",
                "order": "carnivora",
                "family": "canidae",
                "genus": "",
                "species": ""
              }
            }
          ]
        }
        """
            .trimIndent()
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))
    val result = repository.detectAnimal(context, testUri)

    assertNotNull(result)
    assertEquals(1, result.size)
    val annotation = result[0]
    assertEquals("canine family", annotation.animalType)
    assertEquals(0.9975f, annotation.confidence)
    assertEquals(0.4127f, annotation.boundingBox.x)
    assertEquals(0.8261f, annotation.boundingBox.y)
    assertEquals(0.2027f, annotation.boundingBox.width)
    assertEquals(0.1727f, annotation.boundingBox.height)
    assertEquals("3184697f-51ad-4608-9a28-9edb5500159c", annotation.taxonomy.id)
    assertEquals("mammalia", annotation.taxonomy.animalClass)
    assertEquals("carnivora", annotation.taxonomy.order)
    assertEquals("canidae", annotation.taxonomy.family)
    assertEquals("", annotation.taxonomy.genus)
    assertEquals("", annotation.taxonomy.species)
  }

  @Test
  fun `detectAnimal parses multiple annotations correctly`() = runTest {
    val jsonResponse =
        """
        {
          "annotations": [
            {
              "label": "canine family",
              "score": 0.9975,
              "bbox": [0.4127, 0.8261, 0.2027, 0.1727],
              "taxonomy": {
                "id": "3184697f-51ad-4608-9a28-9edb5500159c",
                "class": "mammalia",
                "order": "carnivora",
                "family": "canidae",
                "genus": "",
                "species": ""
              }
            },
            {
              "label": "canine family",
              "score": 0.9298,
              "bbox": [0.3797, 0.4149, 0.1906, 0.1285],
              "taxonomy": {
                "id": "3184697f-51ad-4608-9a28-9edb5500159c",
                "class": "mammalia",
                "order": "carnivora",
                "family": "canidae",
                "genus": "",
                "species": ""
              }
            }
          ]
        }
        """
            .trimIndent()
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))
    val result = repository.detectAnimal(context, testUri)

    assertNotNull(result)
    assertEquals(2, result.size)
    val first = result[0]
    val second = result[1]
    assertEquals(0.9975f, first.confidence)
    assertEquals(0.9298f, second.confidence)
    assertEquals(0.4127f, first.boundingBox.x)
    assertEquals(0.3797f, second.boundingBox.x)
    assertEquals("canine family", first.animalType)
    assertEquals("canine family", second.animalType)
  }

  @Test
  fun `detectAnimal returns empty list on empty body`() = runTest {
    // Mock API response
    val jsonResponse = ""
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))
    val result = repository.detectAnimal(context, testUri)
    assertTrue(result.isEmpty())
  }

  @Test
  fun `detectAnimal returns empty list on field 'annotations' missing`() = runTest {
    val jsonResponse = "{}"
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))
    val result = repository.detectAnimal(context, testUri)
    assertTrue(result.isEmpty())
  }

  @Test
  fun `detectAnimal returns null on field 'label' missing`() = runTest {
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

    assertTrue(result.isEmpty())
  }

  @Test
  fun `detectAnimal returns null on incorrect field 'label' value`() = runTest {
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

    assertTrue(result.isEmpty())
  }

  @Test
  fun `detectAnimal returns null on field 'score' missing`() = runTest {
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

    assertTrue(result.isEmpty())
  }

  @Test
  fun `detectAnimal returns null on incorrect field 'score' value`() = runTest {
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

    assertTrue(result.isEmpty())
  }

  @Test
  fun `detectAnimal returns null on invalid URI`() = runTest {
    val invalidUri = Mockito.mock(Uri::class.java)
    Mockito.`when`(contentResolver.openInputStream(invalidUri)).thenReturn(null)

    val result = repository.detectAnimal(context, invalidUri)

    assertTrue(result.isEmpty())
  }

  @Test
  fun `detectAnimal returns null on HTTP error`() = runTest {
    mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))
    val result = repository.detectAnimal(context, testUri)

    assertTrue(result.isEmpty())
  }
}
