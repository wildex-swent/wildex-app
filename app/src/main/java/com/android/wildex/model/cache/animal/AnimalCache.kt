package com.android.wildex.model.cache.animal

import androidx.datastore.core.DataStore
import com.android.wildex.datastore.AnimalCacheStorage
import com.android.wildex.model.ConnectivityObserver
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.utils.Id
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private const val STALE_DURATION_MS = 10 * 60 * 1000L // 10 minutes

class AnimalCache(
    private val animalDataStore: DataStore<AnimalCacheStorage>,
    private val connectivityObserver: ConnectivityObserver,
) : IAnimalCache {

  private fun isStale(lastUpdated: Long): Boolean {
    val isOnline = connectivityObserver.isOnline.value
    val currentTime = System.currentTimeMillis()
    return isOnline && (currentTime - lastUpdated) > STALE_DURATION_MS
  }

  override suspend fun getAnimal(animalId: Id): Animal? {
    return animalDataStore.data
        .map {
          val cached = it.animalsMap[animalId]
          if (cached != null && !isStale(cached.lastUpdated)) {
            cached.toAnimal()
          } else {
            null
          }
        }
        .firstOrNull()
  }

  override suspend fun getAllAnimals(): List<Animal>? {
    val animals =
        animalDataStore.data
            .map { cacheStorage ->
              val animals = cacheStorage.animalsMap.values
              if (animals.isNotEmpty() && animals.all { !isStale(it.lastUpdated) }) {
                animals.map { it.toAnimal() }
              } else {
                null
              }
            }
            .firstOrNull()
    return if (animals == null && !connectivityObserver.isOnline.value) {
      emptyList()
    } else {
      animals
    }
  }

  override suspend fun saveAnimal(animal: Animal) {
    animalDataStore.updateData {
      it.toBuilder().putAnimals(animal.animalId, animal.toProto()).build()
    }
  }

  override suspend fun saveAnimals(animals: List<Animal>) {
    animals.forEach { saveAnimal(it) }
  }

  override suspend fun clearAll() {
    animalDataStore.updateData { it.toBuilder().clearAnimals().build() }
  }
}
