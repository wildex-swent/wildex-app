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
  fun reverseGeocode_success_returnsPlaceName_andBuildsCorrectUrl() = runBlocking {
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
  fun forwardGeocode_success_returnsLocation_andBuildsCorrectUrl() = runBlocking {
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
  fun searchSuggestions_success_returnsListOfLocations_andBuildsCorrectUrl() = runBlocking {
    val json =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": { "type": "Point", "coordinates": [ -7.589843, 33.57311 ] },
              "properties": {
                "place_formatted": "Casablanca, Morocco"
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

  @Test
  fun reverseGeocode_nameFallbacks_workCorrectly() = runBlocking {
    val jsonPlaceFormatted =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": { "type": "Point", "coordinates": [ -7.5, 33.5 ] },
              "properties": {
                "full_address": "",
                "place_formatted": "Somewhere, World",
                "name": "IgnoredName"
              }
            }
          ]
        }
        """
            .trimIndent()

    val client1 = RecordingOkHttpClient(responseCode = 200, responseBody = jsonPlaceFormatted)
    val repo1 =
        MapboxGeocodingRepository(
            okHttpClient = client1,
            accessToken = "test-token",
        )
    val result1 = repo1.reverseGeocode(33.5, -7.5)
    assertEquals("Somewhere, World", result1)

    val jsonNameOnly =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": { "type": "Point", "coordinates": [ 1.0, 2.0 ] },
              "properties": {
                "full_address": "",
                "place_formatted": "",
                "name": "OnlyName"
              }
            }
          ]
        }
        """
            .trimIndent()

    val client2 = RecordingOkHttpClient(responseCode = 200, responseBody = jsonNameOnly)
    val repo2 =
        MapboxGeocodingRepository(
            okHttpClient = client2,
            accessToken = "test-token",
        )
    val result2 = repo2.reverseGeocode(2.0, 1.0)
    assertEquals("OnlyName", result2)
  }

  @Test
  fun searchSuggestions_handlesMissingNamesAndInvalidGeometry() = runBlocking {
    val jsonUnknownName =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": { "type": "Point", "coordinates": [ 6.0, 46.0 ] },
              "properties": {
                "full_address": "",
                "place_formatted": "",
                "name": ""
              }
            }
          ]
        }
        """
            .trimIndent()

    val clientUnknown = RecordingOkHttpClient(responseCode = 200, responseBody = jsonUnknownName)
    val repoUnknown =
        MapboxGeocodingRepository(
            okHttpClient = clientUnknown,
            accessToken = "test-token",
        )

    val resultsUnknown = repoUnknown.searchSuggestions("whatever", limit = 5)
    assertEquals(1, resultsUnknown.size)
    val loc = resultsUnknown[0]
    assertEquals("Unknown location", loc.name)
    assertEquals(46.0, loc.latitude, 1e-6)
    assertEquals(6.0, loc.longitude, 1e-6)

    val jsonMissingGeometry =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "properties": {
                "full_address": "No geometry here"
              }
            }
          ]
        }
        """
            .trimIndent()

    val clientMissingGeom =
        RecordingOkHttpClient(responseCode = 200, responseBody = jsonMissingGeometry)
    val repoMissingGeom =
        MapboxGeocodingRepository(
            okHttpClient = clientMissingGeom,
            accessToken = "test-token",
        )

    val resultsMissingGeom = repoMissingGeom.searchSuggestions("something", limit = 5)
    assertTrue(resultsMissingGeom.isEmpty())

    val jsonShortCoords =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": { "type": "Point", "coordinates": [ 6.0 ] },
              "properties": {
                "full_address": "Bad coords"
              }
            }
          ]
        }
        """
            .trimIndent()

    val clientShortCoords =
        RecordingOkHttpClient(responseCode = 200, responseBody = jsonShortCoords)
    val repoShortCoords =
        MapboxGeocodingRepository(
            okHttpClient = clientShortCoords,
            accessToken = "test-token",
        )

    val resultsShortCoords = repoShortCoords.searchSuggestions("something", limit = 5)
    assertTrue(resultsShortCoords.isEmpty())

    val jsonNaNCoords =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": { "type": "Point", "coordinates": [ "NaN", 33.0 ] },
              "properties": {
                "full_address": "Weird coords"
              }
            }
          ]
        }
        """
            .trimIndent()

    val clientNaN = RecordingOkHttpClient(responseCode = 200, responseBody = jsonNaNCoords)
    val repoNaN =
        MapboxGeocodingRepository(
            okHttpClient = clientNaN,
            accessToken = "test-token",
        )

    val resultsNaN = repoNaN.searchSuggestions("something", limit = 5)
    assertTrue(resultsNaN.isEmpty())
  }

  @Test
  fun noFeatures_inResponse_returnsNullOrEmptyList() = runBlocking {
    val jsonNoFeatures =
        """
        {
          "type": "FeatureCollection",
          "features": []
        }
        """
            .trimIndent()

    val client = RecordingOkHttpClient(responseCode = 200, responseBody = jsonNoFeatures)
    val repo =
        MapboxGeocodingRepository(
            okHttpClient = client,
            accessToken = "test-token",
        )

    val forwardResult = repo.forwardGeocode("something")
    assertNull(forwardResult)

    val suggestResult = repo.searchSuggestions("anything", limit = 5)
    assertTrue(suggestResult.isEmpty())
  }
}
