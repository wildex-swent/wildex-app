package com.android.wildex.model.user

import com.android.wildex.model.animal.Animal

/** Represents a repository that manages UserAnimal items. */
interface UserAnimalsRepository {

  /** Initializes UserAnimals for a new User with empty list and zero count. */
  suspend fun initializeUserAnimals(userId: String)

  /** Retrieves UserAnimals associated with a specific User. */
  suspend fun getAllAnimalsByUser(userId: String): List<Animal>

  /** Get UserAnimals count of a specific User. */
  suspend fun getAnimalsCountOfUser(userId: String): Int

  /** Add an Animal to the UserAnimals of a specific User. */
  suspend fun addUserAnimals(userId: String, animalId: String)

  /** Delete an Animal to the UserAnimals of a specific User. */
  suspend fun deleteUserAnimals(userId: String, animalId: String)
}
