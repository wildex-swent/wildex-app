package com.android.wildex.model.animal

import android.util.Log
import com.android.wildex.utils.FirestoreTest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

const val ANIMAL_COLLECTION_PATH = "animal"

class AnimalRepositoryFirestoreTest : FirestoreTest(ANIMAL_COLLECTION_PATH) {
  private var repository = AnimalRepositoryFirestore(Firebase.firestore)

  @Before
  override fun setUp() {
    super.setUp()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun canAddAnimalToRepository() = runTest {
    repository.addAnimal(animal1)
    val animals = repository.getAllAnimals()
    Log.e("AnimalRepositoryFirestoreTest", "Animals size: ${animals.size}")
    assertEquals(1, animals.size)
    val storedAnimal = animals.first()
    Log.e("AnimalRepositoryFirestoreTest", "Expected animal: $animal1")
    Log.e("AnimalRepositoryFirestoreTest", "Stored animal: $storedAnimal")
    assertEquals(animal1, storedAnimal)
  }

  @Test
  fun addAnimalWithSameNameDoesNotAddDouble() = runTest {
    repository.addAnimal(animal1)
    repository.addAnimal(animal1.copy(animalId = "Different Id"))
    val animals = repository.getAllAnimals()
    assertEquals(1, animals.size)
    val storedAnimal = animals.first()
    assertEquals(animal1, storedAnimal)
  }

  @Test
  fun addAnimalWithExistingIdThrowsException() = runTest {
    repository.addAnimal(animal1)
    var exceptionThrown = false
    try {
      repository.addAnimal(animal1.copy(name = "Different Name"))
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals(
          "AnimalRepositoryFirestore: An animal with ID '${animal1.animalId}' already exists.",
          e.message,
      )
    }
    assert(exceptionThrown) { "Expected IllegalArgumentException was not thrown." }
  }

  @Test
  fun canGetAnimalById() = runTest {
    repository.addAnimal(animal1)
    val animal = repository.getAnimal(animal1.animalId)
    assertEquals(animal1, animal)
  }

  @Test
  fun getAllAnimalsReturnsEmptyListWhenNoAnimalsAdded() = runTest {
    val animals = repository.getAllAnimals()
    assertTrue(animals.isEmpty())
  }

  @Test
  fun canAddMultipleAnimalsAndRetrieveAll() = runTest {
    repository.addAnimal(animal1)
    repository.addAnimal(animal2)
    val animals = repository.getAllAnimals()
    assertEquals(2, animals.size)
    assertTrue(animals.contains(animal1))
    assertTrue(animals.contains(animal2))
  }

  @Test
  fun getAnimalWithNonExistentIdThrowsException() = runTest {
    var exceptionThrown = false
    try {
      repository.getAnimal("nonExistentId")
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals("Animal with given Id nonExistentId not found", e.message)
    }
    assert(exceptionThrown) { "Expected IllegalArgumentException was not thrown." }
  }

  @Test
  fun addAnimalWithMissingFieldsReturnsEmptyList() = runTest {
    val invalidData =
        mapOf("pictureURL" to "someUrl", "name" to "someName", "description" to "someDescription")
    Firebase.firestore
        .collection(ANIMAL_COLLECTION_PATH)
        .document("invalidAnimalId")
        .set(invalidData)
        .await()

    val animals = repository.getAllAnimals()
    assertTrue(animals.isEmpty())
  }

  @Test
  fun canDeleteAnimalById() = runTest {
    repository.addAnimal(animal1)
    repository.deleteAnimal(animal1.animalId)
    val animals = repository.getAllAnimals()
    assertTrue(animals.isEmpty())
  }
}
