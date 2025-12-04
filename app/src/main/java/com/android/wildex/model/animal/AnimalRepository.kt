package com.android.wildex.model.animal

import com.android.wildex.model.utils.Id

/** Represents a repository that manages Animals. */
interface AnimalRepository {

  /** Retrieves a specific Animal item by its unique identifier. */
  suspend fun getAnimal(animalId: Id): Animal

  /** Retrieves a list of all Animal items in the repository. */
  suspend fun getAllAnimals(): List<Animal>

  /** Adds a new Animal item to the repository. */
  suspend fun addAnimal(animal: Animal)

  /** Deletes an Animal item from the repository. */
  suspend fun deleteAnimal(animalId: Id)
}
