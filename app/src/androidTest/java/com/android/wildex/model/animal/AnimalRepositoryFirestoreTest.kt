package com.android.wildex.model.animal

import android.util.Log
import com.android.wildex.utils.FirestoreTest
import com.android.wildex.utils.offline.FakeAnimalCache
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

const val ANIMAL_COLLECTION_PATH = "animals"

class AnimalRepositoryFirestoreTest : FirestoreTest(ANIMAL_COLLECTION_PATH) {
  private val animalCache = FakeAnimalCache()
  private var repository = AnimalRepositoryFirestore(Firebase.firestore, animalCache)

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
  fun getAnimalUsesCacheAfterFirstFetch() {
    runTest {
      repository.addAnimal(animal1)
      assertEquals(animal1, repository.getAnimal(animal1.animalId))

      Firebase.firestore
          .collection(ANIMAL_COLLECTION_PATH)
          .document(animal1.animalId)
          .update("name", "tampered")
          .await()

      assertEquals(animal1.name, repository.getAnimal(animal1.animalId).name)
    }
  }

  @Test
  fun getAllAnimalsUsesCacheWhenAvailable() {
    runTest {
      repository.addAnimal(animal1)
      repository.addAnimal(animal2)
      val first = repository.getAllAnimals()
      assertEquals(2, first.size)

      Firebase.firestore
          .collection(ANIMAL_COLLECTION_PATH)
          .document(animal1.animalId)
          .update("name", "tampered")
          .await()
      val second = repository.getAllAnimals()
      assertEquals(first, second)
    }
  }

  @Test
  fun refreshCacheClearsAnimalCache() {
    runTest {
      repository.addAnimal(animal1)
      assertEquals(animal1, repository.getAnimal(animal1.animalId))
      repository.refreshCache()
      assertNull(animalCache.getAnimal(animal1.animalId))
      assertEquals(animal1, repository.getAnimal(animal1.animalId))
    }
  }

  @Test
  fun addAnimalWithExistingNameIsIgnored() {
    runTest {
      repository.addAnimal(animal1)
      repository.addAnimal(animal1.copy(animalId = "differentId", pictureURL = "differentURL"))
      val animals = repository.getAllAnimals()
      assertEquals(1, animals.size)
      assertEquals(animal1, animals.first())
    }
  }

  @Test
  fun addAnimalWhenAnimalExistsQueryFailsDoesNotCrash() {
    runTest {
      val badRepo =
          AnimalRepositoryFirestore(Firebase.firestore.collection("abc").firestore, animalCache)
      badRepo.addAnimal(animal1)
      assertTrue(badRepo.getAllAnimals().isNotEmpty())
    }
  }

  @Test
  fun convertToAnimalWhenDocumentThrowsExceptionReturnsNull() {
    runTest {
      Firebase.firestore
          .collection(ANIMAL_COLLECTION_PATH)
          .document("badAnimal")
          .set(mapOf("name" to 123))
          .await()
      assertTrue(repository.getAllAnimals().isEmpty())
    }
  }

  @Test
  fun getAnimalWithMissingRequiredFieldReturnsNotFound() {
    runTest {
      Firebase.firestore
          .collection(ANIMAL_COLLECTION_PATH)
          .document("missingFields")
          .set(mapOf("name" to "n", "pictureURL" to "url"))
          .await()
      val exception = runCatching { repository.getAnimal("missingFields") }.exceptionOrNull()
      assertTrue(exception is IllegalArgumentException)
    }
  }
}
