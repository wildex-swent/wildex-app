package com.android.wildex.model.animaldetector

import com.android.wildex.BuildConfig
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimalDescriptionTest : AnimalInfoRepositoryTest() {

  override val urlPropName: String = "deepseekUrl"

  @Test
  fun `getAnimalDescription forms correct request`() = runTest {
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

    repository.getAnimalDescription("animalName")

    // Assert request sent to mock server
    val recorded = mockWebServer.takeRequest()
    assertEquals("POST", recorded.method)

    val contentType = recorded.getHeader("Content-Type")
    assertNotNull(contentType)
    assertTrue(contentType!!.contains("application/json"))
    assertEquals("Bearer ${BuildConfig.HUGGINGFACE_API_KEY}", recorded.getHeader("Authorization"))

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
  fun `getAnimalDescription returns expected content`() = runTest {
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

  @Test(expected = IOException::class)
  fun `getAnimalDescription throws IOException on HTTP error`() = runTest {
    mockWebServer.enqueue(MockResponse().setResponseCode(500))

    repository.getAnimalDescription("elephant")
  }

  @Test(expected = IOException::class)
  fun `throws IOException if response body is null`() = runTest {
    mockWebServer.enqueue(MockResponse().setResponseCode(200))
    repository.getAnimalDescription("lion")
  }

  @Test(expected = IOException::class)
  fun `throws IOException if choices field missing`() = runTest {
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{}"""))
    repository.getAnimalDescription("lion")
  }

  @Test(expected = IOException::class)
  fun `throws IOException if choices not an array`() = runTest {
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"choices":{}}"""))
    repository.getAnimalDescription("lion")
  }

  @Test(expected = IOException::class)
  fun `throws IOException if choices array empty`() = runTest {
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"choices":[]}"""))
    repository.getAnimalDescription("lion")
  }

  @Test(expected = IOException::class)
  fun `throws IOException if first choice not object`() = runTest {
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"choices":[123]}"""))
    repository.getAnimalDescription("lion")
  }

  @Test(expected = IOException::class)
  fun `throws IOException if message field missing`() = runTest {
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"choices":[{}]}"""))
    repository.getAnimalDescription("lion")
  }

  @Test(expected = IOException::class)
  fun `throws IOException if message not an object`() = runTest {
    mockWebServer.enqueue(
        MockResponse().setResponseCode(200).setBody("""{"choices":[{"message":123}]}"""))
    repository.getAnimalDescription("lion")
  }

  @Test(expected = IOException::class)
  fun `throws IOException if content field missing`() = runTest {
    mockWebServer.enqueue(
        MockResponse().setResponseCode(200).setBody("""{"choices":[{"message":{}}]}"""))
    repository.getAnimalDescription("lion")
  }

  @Test(expected = IOException::class)
  fun `throws IOException if content is not a string`() = runTest {
    mockWebServer.enqueue(
        MockResponse()
            .setResponseCode(200)
            .setBody("""{"choices":[{"message":{"content":123}}]}"""))
    repository.getAnimalDescription("lion")
  }

  @Test
  fun `returns cleaned content on valid response with think tags`() = runTest {
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
