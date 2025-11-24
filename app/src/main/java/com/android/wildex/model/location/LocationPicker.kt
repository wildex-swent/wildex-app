package com.android.wildex.model.location

import java.io.Serializable

/**
 * Location selected from the location picker.
 *
 * @param name place name.
 * @param latitude Latitude in degrees.
 * @param longitude Longitude in degrees.
 */
data class PickedLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
) : Serializable
