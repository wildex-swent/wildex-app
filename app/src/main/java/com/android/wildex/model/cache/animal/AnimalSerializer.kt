package com.android.wildex.model.cache.animal

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.android.wildex.datastore.AnimalCacheStorage
import com.android.wildex.datastore.AnimalProto
import com.android.wildex.model.animal.Animal
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object AnimalCacheSerializer : Serializer<AnimalCacheStorage> {
  override val defaultValue: AnimalCacheStorage = AnimalCacheStorage.getDefaultInstance()

  override suspend fun readFrom(input: InputStream): AnimalCacheStorage {
    try {
      return AnimalCacheStorage.parseFrom(input)
    } catch (e: InvalidProtocolBufferException) {
      throw CorruptionException("Cannot read proto (animal)", e)
    }
  }

  override suspend fun writeTo(t: AnimalCacheStorage, output: OutputStream) {
    t.writeTo(output)
  }
}

val Context.animalDataStore: DataStore<AnimalCacheStorage> by
    dataStore(fileName = "animal_cache.pb", serializer = AnimalCacheSerializer)

fun Animal.toProto(): AnimalProto {
  return AnimalProto.newBuilder()
      .setAnimalId(this.animalId)
      .setPictureUrl(this.pictureURL)
      .setName(this.name)
      .setSpecies(this.species)
      .setDescription(this.description)
      .setLastUpdated(System.currentTimeMillis())
      .build()
}

fun AnimalProto.toAnimal(): Animal {
  return Animal(
      animalId = this.animalId,
      pictureURL = this.pictureUrl,
      name = this.name,
      species = this.species,
      description = this.description)
}
