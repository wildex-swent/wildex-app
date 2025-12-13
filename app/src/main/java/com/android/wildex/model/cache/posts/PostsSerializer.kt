package com.android.wildex.model.cache.posts

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.android.wildex.datastore.LocationProto
import com.android.wildex.datastore.PostCacheStorage
import com.android.wildex.datastore.PostProto
import com.android.wildex.model.social.Post
import com.android.wildex.model.utils.Location
import com.google.firebase.Timestamp
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object PostCacheSerializer : Serializer<PostCacheStorage> {
  override val defaultValue: PostCacheStorage = PostCacheStorage.getDefaultInstance()

  override suspend fun readFrom(input: InputStream): PostCacheStorage {
    try {
      return PostCacheStorage.parseFrom(input)
    } catch (e: InvalidProtocolBufferException) {
      throw CorruptionException("Cannot read proto (post)", e)
    }
  }

  override suspend fun writeTo(t: PostCacheStorage, output: OutputStream) {
    t.writeTo(output)
  }
}

val Context.postDataStore: DataStore<PostCacheStorage> by
    dataStore(fileName = "post_cache.pb", serializer = PostCacheSerializer)

fun Post.toProto(): PostProto {
  val postProto =
      PostProto.newBuilder()
          .setPostId(this.postId)
          .setAuthorId(this.authorId)
          .setPictureUrl(this.pictureURL)
          .setDescription(this.description)
          .setDate(this.date.seconds)
          .setAnimalId(this.animalId)
          .setLastUpdated(System.currentTimeMillis())
  this.location?.let { postProto.setLocation(it.toProto()) }
  return postProto.build()
}

fun PostProto.toPost(): Post {
  return Post(
      postId = this.postId,
      authorId = this.authorId,
      pictureURL = this.pictureUrl,
      location =
          if (this.hasLocation()) {
            Location(
                latitude = this.location.latitude,
                longitude = this.location.longitude,
                name = this.location.name,
                specificName = this.location.specificName,
                generalName = this.location.generalName,
            )
          } else {
            null
          },
      description = this.description,
      date = Timestamp(this.date, 0),
      animalId = this.animalId,
  )
}

fun Location.toProto(): LocationProto {
  return LocationProto.newBuilder()
      .setLatitude(this.latitude)
      .setLongitude(this.longitude)
      .setName(this.name)
      .setSpecificName(this.specificName)
      .setGeneralName(this.generalName)
      .build()
}
