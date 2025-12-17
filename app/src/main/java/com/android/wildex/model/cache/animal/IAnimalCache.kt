package com.android.wildex.model.cache.animal

import com.android.wildex.model.animal.Animal
import com.android.wildex.model.utils.Id

/** Cache interface for animal data. */
interface IAnimalCache {
  /**
   * Retrieves an animal by its ID from the cache.
   *
   * @param animalId The ID of the animal to retrieve.
   * @return The Animal object if found, or null if not found.
   */
  suspend fun getAnimal(animalId: Id): Animal?

  /**
   * Retrieves all animals from the cache.
   *
   * @return A list of Animal objects.
   */
  suspend fun getAllAnimals(): List<Animal>?

  /**
   * Saves an animal to the cache.
   *
   * @param animal The Animal object to add.
   */
  suspend fun saveAnimal(animal: Animal)

  /**
   * Saves multiple animals to the cache.
   *
   * @param animals A list of Animal objects to save.
   */
  suspend fun saveAnimals(animals: List<Animal>)

  /** Clears all animal data from the cache. */
  suspend fun clearAll()
}
