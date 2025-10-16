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
  fun `detectAnimal returns empty list on empty or null body`() = runTest {
    // Mock API response
    val jsonResponse = ""
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))
    val result1 = repository.detectAnimal(context, testUri)
    assertTrue(result1.isEmpty())
  }

  @Test
  fun `detectAnimal returns empty list on field 'annotations' missing`() = runTest {
    val jsonResponse = "{}"
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))
    val result = repository.detectAnimal(context, testUri)
    assertTrue(result.isEmpty())
  }

  @Test
  fun `detectAnimal returns empty on missing fields`() = runTest {
    val missingFieldCases =
        listOf(
            // Missing label
            """{"annotations":[{"score":0.95,"bbox":[0.1,0.2,0.3,0.4],"taxonomy":{"id":"id","class":"mammalia","order":"carnivora","family":"canidae","genus":"","species":""}}]}""",
            // Missing score
            """{"annotations":[{"label":"canine family","bbox":[0.1,0.2,0.3,0.4],"taxonomy":{"id":"id","class":"mammalia","order":"carnivora","family":"canidae","genus":"","species":""}}]}""",
            // Missing bbox
            """{"annotations":[{"label":"canine family","score":0.95,"taxonomy":{"id":"id","class":"mammalia","order":"carnivora","family":"canidae","genus":"","species":""}}]}""",
            // Missing taxonomy
            """{"annotations":[{"label":"canine family","score":0.95,"bbox":[0.1,0.2,0.3,0.4]}]}""",
            // Multiple fields missing (label and score)
            """{"annotations":[{"bbox":[0.1,0.2,0.3,0.4],"taxonomy":{"id":"id","class":"mammalia","order":"carnivora","family":"canidae","genus":"","species":""}}]}""",
        )
    for (json in missingFieldCases) {
      mockWebServer.enqueue(MockResponse().setBody(json).setResponseCode(200))
      val result = repository.detectAnimal(context, testUri)
      assertTrue(result.isEmpty())
    }
  }

  @Test
  fun `detectAnimal returns empty on missing taxonomy fields`() = runTest {
    val missingTaxonomyCases =
        listOf(
            // Missing id
            """{"annotations":[{"label":"canine family","score":0.95,"bbox":[0.1,0.2,0.3,0.4],"taxonomy":{"class":"mammalia","order":"carnivora","family":"canidae","genus":"","species":""}}]}""",
            // Missing class
            """{"annotations":[{"label":"canine family","score":0.95,"bbox":[0.1,0.2,0.3,0.4],"taxonomy":{"id":"id","order":"carnivora","family":"canidae","genus":"","species":""}}]}""",
            // Missing order
            """{"annotations":[{"label":"canine family","score":0.95,"bbox":[0.1,0.2,0.3,0.4],"taxonomy":{"id":"id","class":"mammalia","family":"canidae","genus":"","species":""}}]}""",
            // Missing family
            """{"annotations":[{"label":"canine family","score":0.95,"bbox":[0.1,0.2,0.3,0.4],"taxonomy":{"id":"id","class":"mammalia","order":"carnivora","genus":"","species":""}}]}""",
            // Missing genus
            """{"annotations":[{"label":"canine family","score":0.95,"bbox":[0.1,0.2,0.3,0.4],"taxonomy":{"id":"id","class":"mammalia","order":"carnivora","family":"canidae","species":""}}]}""",
            // Missing species
            """{"annotations":[{"label":"canine family","score":0.95,"bbox":[0.1,0.2,0.3,0.4],"taxonomy":{"id":"id","class":"mammalia","order":"carnivora","family":"canidae","genus":""}}]}""",
        )
    for (json in missingTaxonomyCases) {
      mockWebServer.enqueue(MockResponse().setBody(json).setResponseCode(200))
      val result = repository.detectAnimal(context, testUri)
      assertTrue(result.isEmpty())
    }
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

  @Test
  fun `detectAnimal returns empty for bbox size not equal to 4 or missing`() = runTest {
    val invalidCases =
        listOf(
            """{"annotations":[{"bbox":[0.1,0.2,0.3],"score":0.99,"label":"canine family","taxonomy":{}}]}""",
            """{"annotations":[{"bbox":[0.1,0.2,0.3,0.4,0.5],"score":0.99,"label":"canine family","taxonomy":{}}]}""",
            """{"annotations":[{"score":0.99,"label":"canine family","taxonomy":{}}]}""",
        )
    for (json in invalidCases) {
      mockWebServer.enqueue(MockResponse().setBody(json).setResponseCode(200))
      val result = repository.detectAnimal(context, testUri)
      assertTrue(result.isEmpty())
    }
  }
}
