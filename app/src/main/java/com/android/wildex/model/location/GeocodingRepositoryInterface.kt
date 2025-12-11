package com.android.wildex.model.location

import com.android.wildex.model.utils.Location

/** Interface for geocoding operations. */
interface GeocodingRepository {

  /**
   * Reverse geocode coordinates into a place name.
   *
   * @param latitude latitude
   * @param longitude longitude
   * @return the full Location object if found or null if not found
   */
  suspend fun reverseGeocode(latitude: Double, longitude: Double): Location?

  /**
   * Forward geocode text into a single result.
   *
   * @param query search query
   * @return a feature or null.
   */
  suspend fun forwardGeocode(query: String): Location?

  /**
   * Search for location suggestions based on a query.
   *
   * @param query search query
   * @param limit maximum number of results to return
   * @return A list of geocoding features matching the query.
   */
  suspend fun searchSuggestions(query: String, limit: Int): List<Location>
}
