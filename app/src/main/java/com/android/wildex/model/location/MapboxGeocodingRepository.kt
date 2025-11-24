package com.android.wildex.model.location

import com.android.wildex.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/** Mapbox-backed implementation of GeocodingRepository using OkHttp directly. */
class MapboxGeocodingRepository(
    private val okHttpClient: OkHttpClient,
    private val accessToken: String = BuildConfig.MAPBOX_ACCESS_TOKEN,
) : GeocodingRepository {

  override suspend fun reverseGeocode(lat: Double, lon: Double): String? =
      withContext(Dispatchers.IO) {
        val url =
            HttpUrl.Builder()
                .scheme("https")
                .host("api.mapbox.com")
                .addPathSegments("geocoding/v5/mapbox.places")
                .addPathSegment("$lon,$lat.json")
                .addQueryParameter("access_token", accessToken)
                .addQueryParameter("language", "en")
                .addQueryParameter("limit", "1")
                .build()

        val request = Request.Builder().url(url).get().build()

        runCatching {
              okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                parseFirstPlaceName(body)
              }
            }
            .getOrNull()
      }

  override suspend fun forwardGeocode(query: String): GeocodingFeature? =
      withContext(Dispatchers.IO) {
        val url =
            HttpUrl.Builder()
                .scheme("https")
                .host("api.mapbox.com")
                .addPathSegments("geocoding/v5/mapbox.places")
                .addPathSegment("${query.trim()}.json")
                .addQueryParameter("access_token", accessToken)
                .addQueryParameter("language", "en")
                .addQueryParameter("limit", "1")
                .addQueryParameter("autocomplete", "true")
                .build()

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

  private fun parseFirstPlaceName(json: String): String? {
    val root = JSONObject(json)
    val features = root.optJSONArray("features") ?: return null
    if (features.length() == 0) return null
    val first = features.optJSONObject(0) ?: return null
    return first.optString("place_name", "").takeIf { it.isNotBlank() }
  }

  private fun parseFirstFeature(json: String): GeocodingFeature? {
    val root = JSONObject(json)
    val features = root.optJSONArray("features") ?: return null
    if (features.length() == 0) return null
    val first = features.optJSONObject(0) ?: return null

    val name = first.optString("place_name", "").takeIf { it.isNotBlank() }
    val centerArr = first.optJSONArray("center") ?: return null
    if (centerArr.length() < 2) return null

    val lon = centerArr.optDouble(0)
    val lat = centerArr.optDouble(1)

    return GeocodingFeature(
        placeName = name,
        lon = lon,
        lat = lat,
    )
  }
}
