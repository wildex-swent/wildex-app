package com.android.wildex.model.animaldetector

import android.net.Uri
import com.android.wildex.BuildConfig
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.ByteArrayInputStream

class AnimalDetectionTest : AnimalDetectRepositoryTest() {
    private lateinit var testUri: Uri
    override val urlPropName: String = "ad_baseUrl"

    @Before
    override fun setUp() {
        super.setUp()
        testUri = Mockito.mock(Uri::class.java)
        val fakeImage = ByteArrayInputStream(ByteArray(10))
        Mockito.`when`(contentResolver.getType(testUri)).thenReturn("image/jpeg")
        Mockito.`when`(contentResolver.openInputStream(testUri)).thenReturn(fakeImage)
    }

    @Test
    fun `detectAnimal forms correct multipart request`() {
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
