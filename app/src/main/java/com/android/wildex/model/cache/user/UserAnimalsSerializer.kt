package com.android.wildex.model.cache.user

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.android.wildex.datastore.UserAnimalsCacheStorage
import com.android.wildex.datastore.UserAnimalsProto
import com.android.wildex.model.user.UserAnimals
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object UserAnimalsCacheSerializer : Serializer<UserAnimalsCacheStorage> {
  override val defaultValue: UserAnimalsCacheStorage = UserAnimalsCacheStorage.getDefaultInstance()

  override suspend fun readFrom(input: InputStream): UserAnimalsCacheStorage {
    try {
      return UserAnimalsCacheStorage.parseFrom(input)
    } catch (e: InvalidProtocolBufferException) {
      throw CorruptionException("Cannot read proto (user animal)")
    }
  }

  override suspend fun writeTo(t: UserAnimalsCacheStorage, output: OutputStream) {
    t.writeTo(output)
  }
}

val Context.userAnimalsDataStore: DataStore<UserAnimalsCacheStorage> by
    dataStore(fileName = "user_animal_cache.pb", serializer = UserAnimalsCacheSerializer)

fun UserAnimals.toProto(): UserAnimalsProto {
  return UserAnimalsProto.newBuilder()
      .setUserId(this.userId)
      .addAllAnimalsId(this.animalsId)
      .setAnimalsCount(this.animalsCount)
      .setLastUpdated(System.currentTimeMillis())
      .build()
}

fun UserAnimalsProto.toUserAnimals(): UserAnimals {
  return UserAnimals(
      userId = this.userId, animalsId = this.animalsIdList, animalsCount = this.animalsCount)
}
