package com.android.wildex.model.animaldetector

import com.android.wildex.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimalDescriptionTest : AnimalDetectRepositoryTest() {

    override val urlPropName: String = "hf_baseUrl"

    @Test
    fun `getAnimalDescription forms correct request`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        repository.getAnimalDescription("animalName")

        // Assert request sent to mock server
        val recorded = mockWebServer.takeRequest()
        assertEquals("POST", recorded.method)

        val contentType = recorded.getHeader("Content-Type")
        assertNotNull(contentType)
        assertTrue(contentType!!.contains("application/json"))
        assertEquals(
            "Bearer ${BuildConfig.HUGGINGFACE_API_KEY}",
            recorded.getHeader("Authorization")
        )

        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"model\":"))
        val json = Json.parseToJsonElement(body).jsonObject
        assertNotNull(json["model"]?.jsonPrimitive?.contentOrNull)

        // Check messages
        val messages = json["messages"]?.jsonArray
        assertNotNull(messages)
        assertTrue(messages!!.isNotEmpty())

        // Check user role message
        val userMsg = messages[1].jsonObject
        assertEquals("user", userMsg["role"]?.jsonPrimitive?.contentOrNull)
        assertNotNull(userMsg["content"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `getAnimalDescription returns expected content`() {
        val modelResponse =
            """
            {
              "choices": [
                {
                  "message": {
                    "content": "The lion is a large carnivorous feline, known as the king of the jungle."
                  }
                }
              ]
            }
        """
                .trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(modelResponse))

        val animalName = "lion"

        val description = repository.getAnimalDescription(animalName)

        assertNotNull(description)
        assertEquals(
            "The lion is a large carnivorous feline, known as the king of the jungle.",
            description,
        )
    }

    @Test
    fun `getAnimalDescription returns null on HTTP error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val description = repository.getAnimalDescription("elephant")

        assertNull(description)
    }

    @Test
    fun `returns null if response body is null`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        assertNull(repository.getAnimalDescription("lion"))
    }

    @Test
    fun `returns null if choices field missing`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{}"""))
        assertNull(repository.getAnimalDescription("lion"))
    }

    @Test
    fun `returns null if choices not an array`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"choices":{}}"""))
        assertNull(repository.getAnimalDescription("lion"))
    }

    @Test
    fun `returns null if choices array empty`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"choices":[]}"""))
        assertNull(repository.getAnimalDescription("lion"))
    }

    @Test
    fun `returns null if first choice not object`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"choices":[123]}"""))
        assertNull(repository.getAnimalDescription("lion"))
    }

    @Test
    fun `returns null if message field missing`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"choices":[{}]}"""))
        assertNull(repository.getAnimalDescription("lion"))
    }

    @Test
    fun `returns null if message not an object`() {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"choices":[{"message":123}]}""")
        )
        assertNull(repository.getAnimalDescription("lion"))
    }

    @Test
    fun `returns null if content field missing`() {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"choices":[{"message":{}}]}""")
        )
        assertNull(repository.getAnimalDescription("lion"))
    }

    @Test
    fun `returns null if content is not a string`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"choices":[{"message":{"content":123}}]}""")
        )
        assertNull(repository.getAnimalDescription("lion"))
    }

    @Test
    fun `returns cleaned content on valid response with think tags`() {
        val body =
            """
            {
              "choices":[
                {"message":{"content":"<think>ignore</think>The lion is majestic."}}
              ]
            }
        """
                .trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val description = repository.getAnimalDescription("lion")
        assertEquals("The lion is majestic.", description)
    }
}
