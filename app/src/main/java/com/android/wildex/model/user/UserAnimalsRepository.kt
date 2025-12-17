package com.android.wildex.model.user

import com.android.wildex.model.animal.Animal
import com.android.wildex.model.utils.Id

/** Represents a repository that manages UserAnimal items. */
interface UserAnimalsRepository {

  /** Initializes UserAnimals for a new User with empty list and zero count. */
  suspend fun initializeUserAnimals(userId: Id)

  /** Retrieves UserAnimals associated with a specific User. */
  suspend fun getAllAnimalsByUser(userId: Id): List<Animal>

  /** Get UserAnimals count of a specific User. */
  suspend fun getAnimalsCountOfUser(userId: Id): Int

  /** Add an Animal to the UserAnimals of a specific User. */
  suspend fun addAnimalToUserAnimals(userId: Id, animalId: Id)

  /** Delete an Animal to the UserAnimals of a specific User. */
  suspend fun deleteAnimalToUserAnimals(userId: Id, animalId: Id)

  /** Delete the UserAnimals linked to the given user */
  suspend fun deleteUserAnimals(userId: Id)

  /** Clears the user animals cache. */
  suspend fun refreshCache()
}
