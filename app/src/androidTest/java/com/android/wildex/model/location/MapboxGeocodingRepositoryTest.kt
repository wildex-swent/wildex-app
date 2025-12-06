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
                    "place_formatted": "ignored",
                    "name": "ignored"
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

    val result = repo.reverseGeocode(latitude = 33.57311, longitude = -7.589843, true)
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
    val result = repo.reverseGeocode(33.0, -7.0, true)
    assertNull(result)
  }

  @Test
  fun reverseGeocode_parsingEdgeCases_returnNull() = runBlocking {
    val jsonNoFeatures =
        """
            { "type": "FeatureCollection" }
            """
            .trimIndent()
    val repoNoFeatures =
        MapboxGeocodingRepository(
            okHttpClient = RecordingOkHttpClient(responseBody = jsonNoFeatures),
            accessToken = "test-token",
        )
    assertNull(repoNoFeatures.reverseGeocode(0.0, 0.0, true))

    val jsonEmptyFeatures =
        """
            {
              "type": "FeatureCollection",
              "features": []
            }
            """
            .trimIndent()
    val repoEmpty =
        MapboxGeocodingRepository(
            okHttpClient = RecordingOkHttpClient(responseBody = jsonEmptyFeatures),
            accessToken = "test-token",
        )
    assertNull(repoEmpty.reverseGeocode(0.0, 0.0, true))

    val jsonNoProperties =
        """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": { "type": "Point", "coordinates": [0.0, 0.0] }
                }
              ]
            }
            """
            .trimIndent()
    val repoNoProps =
        MapboxGeocodingRepository(
            okHttpClient = RecordingOkHttpClient(responseBody = jsonNoProperties),
            accessToken = "test-token",
        )
    assertNull(repoNoProps.reverseGeocode(0.0, 0.0, true))

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
    val repoPlaceFormatted =
        MapboxGeocodingRepository(
            okHttpClient = RecordingOkHttpClient(responseBody = jsonPlaceFormatted),
            accessToken = "test-token",
        )
    assertEquals("Somewhere, World", repoPlaceFormatted.reverseGeocode(33.5, -7.5, true))

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
    val repoNameOnly =
        MapboxGeocodingRepository(
            okHttpClient = RecordingOkHttpClient(responseBody = jsonNameOnly),
            accessToken = "test-token",
        )
    assertEquals("OnlyName", repoNameOnly.reverseGeocode(2.0, 1.0, true))
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
                    "full_address": "Casablanca, Morocco"
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
  fun forwardGeocode_parsingEdgeCases_returnNull() = runBlocking {
    val jsonNoFeatures =
        """
            { "type": "FeatureCollection" }
            """
            .trimIndent()
    val repoNoFeatures =
        MapboxGeocodingRepository(
            okHttpClient = RecordingOkHttpClient(responseBody = jsonNoFeatures),
            accessToken = "test-token",
        )
    assertNull(repoNoFeatures.forwardGeocode("x"))

    val jsonNonObjectFeature =
        """
            {
              "type": "FeatureCollection",
              "features": [
                "not-an-object"
              ]
            }
            """
            .trimIndent()
    val repoNonObject =
        MapboxGeocodingRepository(
            okHttpClient = RecordingOkHttpClient(responseBody = jsonNonObjectFeature),
            accessToken = "test-token",
        )
    assertNull(repoNonObject.forwardGeocode("y"))

    val jsonEmpty =
        """
            {
              "type": "FeatureCollection",
              "features": []
            }
            """
            .trimIndent()
    val repoEmpty =
        MapboxGeocodingRepository(
            okHttpClient = RecordingOkHttpClient(responseBody = jsonEmpty),
            accessToken = "test-token",
        )
    assertNull(repoEmpty.forwardGeocode("z"))
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
  fun searchSuggestions_parsingEdgeCases_handlesWeirdFeatures() = runBlocking {
    val jsonNoFeatures =
        """
            { "type": "FeatureCollection" }
            """
            .trimIndent()
    val repoNoFeatures =
        MapboxGeocodingRepository(
            okHttpClient = RecordingOkHttpClient(responseBody = jsonNoFeatures),
            accessToken = "test-token",
        )
    assertTrue(repoNoFeatures.searchSuggestions("x", 5).isEmpty())

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
    val repoUnknownName =
        MapboxGeocodingRepository(
            okHttpClient = RecordingOkHttpClient(responseBody = jsonUnknownName),
            accessToken = "test-token",
        )
    val unknown = repoUnknownName.searchSuggestions("x", 5)
    assertEquals(1, unknown.size)
    assertEquals("Unknown location", unknown[0].name)

    val jsonNoGeometry =
        """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": { "full_address": "No geometry here" }
                }
              ]
            }
            """
            .trimIndent()
    val repoNoGeom =
        MapboxGeocodingRepository(
            okHttpClient = RecordingOkHttpClient(responseBody = jsonNoGeometry),
            accessToken = "test-token",
        )
    assertTrue(repoNoGeom.searchSuggestions("y", 5).isEmpty())

    val jsonMissingCoordsArray =
        """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": { "type": "Point" },
                  "properties": { "full_address": "No coordinates here" }
                }
              ]
            }
            """
            .trimIndent()
    val repoMissingCoordsArray =
        MapboxGeocodingRepository(
            okHttpClient = RecordingOkHttpClient(responseBody = jsonMissingCoordsArray),
            accessToken = "test-token",
        )
    assertTrue(repoMissingCoordsArray.searchSuggestions("y", 5).isEmpty())

    val jsonShortCoords =
        """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": { "type": "Point", "coordinates": [ 6.0 ] },
                  "properties": { "full_address": "Bad coords" }
                }
              ]
            }
            """
            .trimIndent()
    val repoShortCoords =
        MapboxGeocodingRepository(
            okHttpClient = RecordingOkHttpClient(responseBody = jsonShortCoords),
            accessToken = "test-token",
        )
    assertTrue(repoShortCoords.searchSuggestions("z", 5).isEmpty())

    val jsonNaNCoords =
        """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": { "type": "Point", "coordinates": [ "NaN", 33.0 ] },
                  "properties": { "full_address": "Weird coords" }
                }
              ]
            }
            """
            .trimIndent()
    val repoNaN =
        MapboxGeocodingRepository(
            okHttpClient = RecordingOkHttpClient(responseBody = jsonNaNCoords),
            accessToken = "test-token",
        )
    assertTrue(repoNaN.searchSuggestions("w", 5).isEmpty())

    val jsonMixed =
        """
            {
              "type": "FeatureCollection",
              "features": [
                "not-an-object",
                {
                  "type": "Feature",
                  "geometry": { "type": "Point", "coordinates": [ 6.0, 46.0 ] },
                  "properties": { "name": "Valid place" }
                }
              ]
            }
            """
            .trimIndent()
    val repoMixed =
        MapboxGeocodingRepository(
            okHttpClient = RecordingOkHttpClient(responseBody = jsonMixed),
            accessToken = "test-token",
        )
    val mixed = repoMixed.searchSuggestions("mix", 5)
    assertEquals(1, mixed.size)
    assertEquals("Valid place", mixed[0].name)
    assertEquals(46.0, mixed[0].latitude, 1e-6)
    assertEquals(6.0, mixed[0].longitude, 1e-6)
  }
}
