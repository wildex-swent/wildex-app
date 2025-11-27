package com.android.wildex.model.location

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.model.utils.Location
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Timeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapboxGeocodingRepositoryAndroidTest {

  /** Simple fake OkHttpClient that always returns the same response. */
  private class RecordingOkHttpClient(
      private val responseCode: Int = 200,
      private val responseBody: String = """{ "features": [] }"""
  ) : OkHttpClient() {

    var lastRequest: Request? = null

    override fun newCall(request: Request): Call {
      lastRequest = request
      return object : Call {
        override fun request(): Request = request

        override fun execute(): Response {
          return Response.Builder()
              .request(request)
              .protocol(Protocol.HTTP_1_1)
              .code(responseCode)
              .message("OK")
              .body(responseBody.toResponseBody("application/json".toMediaType()))
              .build()
        }

        override fun enqueue(responseCallback: okhttp3.Callback) {
          error("enqueue not used in tests")
        }

        override fun cancel() {}

        override fun isExecuted(): Boolean = true

        override fun isCanceled(): Boolean = false

        override fun clone(): Call = this

        override fun timeout(): Timeout = Timeout.NONE
      }
    }
  }

  @Test
  fun reverseGeocode_success_returnsPlaceName() = runBlocking {
    val json =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": { "type": "Point", "coordinates": [ -7.589843, 33.57311 ] },
              "properties": {
                "full_address": "Casablanca, Morocco",
                "place_name": "Casablanca, Morocco",
                "name": "Casablanca"
              }
            }
          ]
        }
        """
            .trimIndent()

    val client = RecordingOkHttpClient(responseCode = 200, responseBody = json)
    val repo =
        MapboxGeocodingRepository(
            okHttpClient = client,
            accessToken = "test-token",
        )

    val result = repo.reverseGeocode(latitude = 33.57311, longitude = -7.589843)
    assertEquals("Casablanca, Morocco", result)

    val request = client.lastRequest!!
    assertEquals("https", request.url.scheme)
    assertEquals("api.mapbox.com", request.url.host)
    assertEquals("/search/geocode/v6/reverse", request.url.encodedPath)
    assertEquals("-7.589843", request.url.queryParameter("longitude"))
    assertEquals("33.57311", request.url.queryParameter("latitude"))
    assertEquals("test-token", request.url.queryParameter("access_token"))
    assertEquals("1", request.url.queryParameter("limit"))
  }

  @Test
  fun reverseGeocode_httpError_returnsNull() = runBlocking {
    val client = RecordingOkHttpClient(responseCode = 500, responseBody = "{}")
    val repo =
        MapboxGeocodingRepository(
            okHttpClient = client,
            accessToken = "test-token",
        )

    val result = repo.reverseGeocode(33.0, -7.0)
    assertNull(result)
  }

  @Test
  fun forwardGeocode_success_returnsLocation() = runBlocking {
    val json =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": { "type": "Point", "coordinates": [ -7.589843, 33.57311 ] },
              "properties": {
                "full_address": "Casablanca, Morocco",
                "place_name": "Casablanca, Morocco",
                "name": "Casablanca"
              }
            }
          ]
        }
        """
            .trimIndent()

    val client = RecordingOkHttpClient(responseCode = 200, responseBody = json)
    val repo =
        MapboxGeocodingRepository(
            okHttpClient = client,
            accessToken = "test-token",
        )

    val result: Location? = repo.forwardGeocode("  Casablanca  ")
    requireNotNull(result)

    assertEquals("Casablanca, Morocco", result.name)
    assertEquals(33.57311, result.latitude, 1e-6)
    assertEquals(-7.589843, result.longitude, 1e-6)

    val request = client.lastRequest!!
    assertEquals("/search/geocode/v6/forward", request.url.encodedPath)
    assertEquals("Casablanca", request.url.queryParameter("q"))
    assertEquals("1", request.url.queryParameter("limit"))
    assertEquals("place,locality,region,address,street", request.url.queryParameter("types"))
    assertEquals("true", request.url.queryParameter("autocomplete"))
  }

  @Test
  fun forwardGeocode_emptyQuery_returnsNullWithoutRequest() = runBlocking {
    val client = RecordingOkHttpClient(responseCode = 200, responseBody = "{}")
    val repo =
        MapboxGeocodingRepository(
            okHttpClient = client,
            accessToken = "test-token",
        )

    val result = repo.forwardGeocode("   ")
    assertNull(result)
    assertNull(client.lastRequest)
  }

  @Test
  fun searchSuggestions_success_returnsListOfLocations() = runBlocking {
    val json =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": { "type": "Point", "coordinates": [ -7.589843, 33.57311 ] },
              "properties": {
                "full_address": "Casablanca, Morocco"
              }
            },
            {
              "type": "Feature",
              "geometry": { "type": "Point", "coordinates": [ 6.6323, 46.5197 ] },
              "properties": {
                "name": "Lausanne, Switzerland"
              }
            }
          ]
        }
        """
            .trimIndent()

    val client = RecordingOkHttpClient(responseCode = 200, responseBody = json)
    val repo =
        MapboxGeocodingRepository(
            okHttpClient = client,
            accessToken = "test-token",
        )

    val results = repo.searchSuggestions("cas", limit = 5)
    assertEquals(2, results.size)

    val casablanca = results[0]
    assertEquals("Casablanca, Morocco", casablanca.name)
    assertEquals(33.57311, casablanca.latitude, 1e-6)
    assertEquals(-7.589843, casablanca.longitude, 1e-6)

    val lausanne = results[1]
    assertEquals("Lausanne, Switzerland", lausanne.name)
    assertEquals(46.5197, lausanne.latitude, 1e-6)
    assertEquals(6.6323, lausanne.longitude, 1e-6)

    val request = client.lastRequest!!
    assertEquals("/search/geocode/v6/forward", request.url.encodedPath)
    assertEquals("cas", request.url.queryParameter("q"))
    assertEquals("5", request.url.queryParameter("limit"))
  }

  @Test
  fun searchSuggestions_httpError_returnsEmptyList() = runBlocking {
    val client = RecordingOkHttpClient(responseCode = 500, responseBody = "{}")
    val repo =
        MapboxGeocodingRepository(
            okHttpClient = client,
            accessToken = "test-token",
        )
    val results = repo.searchSuggestions("cas", limit = 5)
    assertTrue(results.isEmpty())
  }

  @Test
  fun searchSuggestions_emptyQuery_returnsEmptyListWithoutRequest() = runBlocking {
    val client = RecordingOkHttpClient(responseCode = 200, responseBody = "{}")
    val repo =
        MapboxGeocodingRepository(
            okHttpClient = client,
            accessToken = "test-token",
        )
    val results = repo.searchSuggestions("  ", limit = 5)
    assertTrue(results.isEmpty())
    assertNull(client.lastRequest)
  }
}
