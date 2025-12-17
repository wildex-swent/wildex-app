package com.android.wildex.utils.offline

import com.android.wildex.model.animal.Animal
import com.android.wildex.model.cache.animal.IAnimalCache
import com.android.wildex.model.utils.Id

class FakeAnimalCache : IAnimalCache {
  val cache = mutableMapOf<String, Animal>()

  init {
    cache.clear()
  }

  override suspend fun getAnimal(animalId: Id): Animal? {
    return cache[animalId]
  }

  override suspend fun getAllAnimals(): List<Animal>? {
    return cache.values.toList()
  }

  override suspend fun saveAnimal(animal: Animal) {
    cache.put(animal.animalId, animal)
  }

  override suspend fun saveAnimals(animals: List<Animal>) {
    animals.forEach { saveAnimal(it) }
  }

  override suspend fun clearAll() {
    cache.clear()
  }
}
