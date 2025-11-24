package com.android.wildex.model.location

/** Abstraction for geocoding operations. */
interface GeocodingRepository {

  /**
   * Reverse geocode coordinates into a place name.
   *
   * @return place name or null if not found.
   */
  suspend fun reverseGeocode(lat: Double, lon: Double): String?

  /**
   * Forward geocode text into a single result.
   *
   * @return a feature or null.
   */
  suspend fun forwardGeocode(query: String): GeocodingFeature?

  /**
   * Search for location suggestions based on a query.
   *
   * @return A list of geocoding features matching the query.
   */
  suspend fun searchSuggestions(query: String, limit: Int = 5): List<GeocodingFeature>
}

/**
 * Simple data class representing a Mapbox geocoding feature.
 *
 * @param placeName Human-readable place name.
 * @param lon Longitude in degrees.
 * @param lat Latitude in degrees.
 */
data class GeocodingFeature(
    val placeName: String?,
    val lon: Double,
    val lat: Double,
)
