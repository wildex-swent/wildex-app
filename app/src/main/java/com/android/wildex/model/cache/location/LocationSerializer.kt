package com.android.wildex.model.cache.location

import com.android.wildex.datastore.LocationProto
import com.android.wildex.model.utils.Location

fun Location.toProto(): LocationProto {
  return LocationProto.newBuilder()
      .setLatitude(this.latitude)
      .setLongitude(this.longitude)
      .setName(this.name)
      .setSpecificName(this.specificName)
      .setGeneralName(this.generalName)
      .build()
}

fun LocationProto.toLocation(): Location {
  return Location(
      latitude = this.latitude,
      longitude = this.longitude,
      name = this.name,
      specificName = this.specificName,
      generalName = this.generalName)
}
