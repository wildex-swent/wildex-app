package com.android.wildex.model.user

import com.android.wildex.model.animal.Animal
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.utils.Id

class FakeUserAnimalsRepository(private val animalRepository: AnimalRepository) : UserAnimalsRepository {
  val mapUserToAnimals = mutableMapOf<Id, MutableList<Animal>>()

  override suspend fun initializeUserAnimals(userId: Id) {
    mapUserToAnimals.put(userId, mutableListOf())
  }

  override suspend fun getAllAnimalsByUser(userId: Id): List<Animal> {
    return mapUserToAnimals[userId] ?: throw Exception("User not found")
  }

  override suspend fun getAnimalsCountOfUser(userId: Id): Int {
    return getAllAnimalsByUser(userId).size
  }

  override suspend fun addAnimalToUserAnimals(
    userId: Id,
    animalId: Id
  ) {
    val oldList = mapUserToAnimals.getValue(userId)
    oldList.add(animalRepository.getAnimal(animalId))
    mapUserToAnimals.put(userId, oldList)
  }

  override suspend fun deleteAnimalToUserAnimals(
    userId: Id,
    animalId: Id
  ) {
    val oldList = mapUserToAnimals.getValue(userId)
    oldList.removeIf { it.animalId == animalId }
    mapUserToAnimals.put(userId, oldList)
  }
}