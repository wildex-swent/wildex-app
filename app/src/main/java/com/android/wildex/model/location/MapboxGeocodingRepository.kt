package com.android.wildex.model.location

import com.android.wildex.BuildConfig
import com.android.wildex.model.utils.Location
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Geocoding repository using Mapbox.
 *
 * @param okHttpClient The OkHttpClient to use for network requests.
 * @param accessToken The Mapbox access token.
 * @param dispatcher The CoroutineDispatcher to use for network requests.
 * @param baseURL The base URL for the Mapbox API.
 */
class MapboxGeocodingRepository(
    private val okHttpClient: OkHttpClient,
    private val accessToken: String = BuildConfig.MAPBOX_ACCESS_TOKEN,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val baseURL: String = "https://api.mapbox.com",
) : GeocodingRepository {

  override suspend fun reverseGeocode(latitude: Double, longitude: Double): Location? =
      withContext(dispatcher) {
        val base = baseURL.toHttpUrl()
        val url =
            base
                .newBuilder()
                .addPathSegments("search/geocode/v6/reverse")
                .addQueryParameter("longitude", longitude.toString())
                .addQueryParameter("latitude", latitude.toString())
                .addQueryParameter("access_token", accessToken)
                .addQueryParameter("language", "en")
                .addQueryParameter("limit", "1")
                .build()

        val request = Request.Builder().url(url).get().build()
        runCatching {
              okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                val name = parseFirstPlaceName(body) ?: return@use null
                val addressParts = name.trim().split(',')
                if (addressParts.size > 1) {
                  val specificAddress = addressParts.take(addressParts.size - 1).joinToString(", ")
                  val generalAddress = addressParts.takeLast(1).first()
                  Location(latitude, longitude, name, specificAddress, generalAddress)
                } else {
                  Location(latitude, longitude, name)
                }
              }
            }
            .getOrNull()
      }

  override suspend fun forwardGeocode(query: String): Location? =
      withContext(dispatcher) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext null
        val url = forwardURL(query = trimmed, limit = 1)

        val request = Request.Builder().url(url).get().build()
        runCatching {
              okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                parseFirstFeature(body)
              }
            }
            .getOrNull()
      }

  override suspend fun searchSuggestions(query: String, limit: Int): List<Location> =
      withContext(dispatcher) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        val url = forwardURL(query = trimmed, limit = limit)

        val request = Request.Builder().url(url).get().build()
        runCatching {
              okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val body = response.body?.string() ?: return@use emptyList<Location>()
                parseFeaturesList(body)
              }
            }
            .getOrDefault(emptyList())
      }

  /**
   * Builds the URL for forward geocoding requests.
   *
   * @param query The search query.
   * @param limit The maximum number of results to return.
   * @return The HttpUrl for the forward geocoding request.
   */
  private fun forwardURL(query: String, limit: Int): HttpUrl {
    val base = baseURL.toHttpUrl()
    return base
        .newBuilder()
        .addPathSegments("search/geocode/v6/forward")
        .addQueryParameter("q", query)
        .addQueryParameter("access_token", accessToken)
        .addQueryParameter("language", "en")
        .addQueryParameter("limit", limit.toString())
        .addQueryParameter("types", "place,locality,region,address,street")
        .addQueryParameter("autocomplete", "true")
        .build()
  }

  /**
   * Parses a JSON string representing a geocoding response and extracts the name of the first
   * place.
   *
   * @param json The JSON string to parse.
   * @return The name of the first place extracted from the JSON, or null if none found.
   */
  private fun parseFirstPlaceName(json: String): String? {
    val root = JSONObject(json)
    val features = root.optJSONArray("features") ?: return null
    if (features.length() == 0) return null
    val first = features.optJSONObject(0) ?: return null
    val properties = first.optJSONObject("properties") ?: return null
    return properties.getBestName()
  }

  /**
   * Parses a JSON string representing a geocoding response and extracts the first Location.
   *
   * @param json The JSON string to parse.
   * @return The first Location extracted from the JSON, or null if none found.
   */
  private fun parseFirstFeature(json: String): Location? {
    val root = JSONObject(json)
    val features = root.optJSONArray("features") ?: return null
    if (features.length() == 0) return null
    val first = features.optJSONObject(0) ?: return null

    return first.getLocation()
  }

  /**
   * Parses a JSON string representing a list of features and extracts a list of Location.
   *
   * @param json The JSON string to parse.
   * @return A list of Location extracted from the JSON.
   */
  private fun parseFeaturesList(json: String): List<Location> {
    val root = JSONObject(json)
    val features = root.optJSONArray("features") ?: return emptyList()

    val out = mutableListOf<Location>()
    for (i in 0 until features.length()) {
      val feature = features.optJSONObject(i) ?: continue
      val location = feature.getLocation() ?: continue
      out += location
    }
    return out
  }

  /**
   * Gets the best available name from the properties.
   *
   * @return The best name available, or null if none found.
   */
  private fun JSONObject.getBestName(): String? {
    return this.optString("full_address", "").takeIf { it.isNotBlank() }
        ?: this.optString("place_formatted", "").takeIf { it.isNotBlank() }
        ?: this.optString("name", "").takeIf { it.isNotBlank() }
  }

  /**
   * Extracts a Location from a JSONObject representing a feature.
   *
   * @return The Location, or null if extraction fails.
   */
  private fun JSONObject.getLocation(): Location? {
    val properties = this.optJSONObject("properties") ?: return null
    val name = properties.getBestName() ?: "Unknown location"

    val geometry = this.optJSONObject("geometry") ?: return null
    val coordinates = geometry.optJSONArray("coordinates") ?: return null
    if (coordinates.length() < 2) return null

    val longitude = coordinates.optDouble(0)
    val latitude = coordinates.optDouble(1)
    if (longitude.isNaN() || latitude.isNaN()) return null

    val addressParts = name.trim().split(",")
    if (addressParts.isEmpty()) return Location(latitude, longitude, name)

    val generalAddress = addressParts.takeLast(1).first()
    val specificAddress = addressParts.take(addressParts.size - 1).joinToString(", ")

    return Location(
        latitude = latitude,
        longitude = longitude,
        name = name,
        specificName = specificAddress,
        generalName = generalAddress,
    )
  }
}
