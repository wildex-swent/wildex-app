package com.android.wildex.model.utils

import java.io.Serializable

/**
 * Represents a geographical location with latitude and longitude.
 *
 * @property latitude The latitude of the location.
 * @property longitude The longitude of the location.
 * @property name The name of the location.
 */
data class Location(val latitude: Double, val longitude: Double, val name: String = "") :
    Serializable
