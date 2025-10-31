package com.android.wildex.model.animal

import com.android.wildex.model.utils.Id

class FakeAnimalRepository : AnimalRepository {
  val animals = mutableListOf<Animal>()

  override suspend fun getAnimal(animalId: Id): Animal {
    return animals.find { it.animalId == animalId }!!
  }

  override suspend fun getAllAnimals(): List<Animal> {
    return animals
  }

  override suspend fun addAnimal(animal: Animal) {
    animals.add(animal)
  }
}