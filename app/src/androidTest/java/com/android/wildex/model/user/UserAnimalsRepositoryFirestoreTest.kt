package com.android.wildex.model.user

import com.android.wildex.model.animal.AnimalRepositoryFirestore
import com.android.wildex.utils.FirebaseEmulator
import com.android.wildex.utils.FirestoreTest
import com.android.wildex.utils.offline.FakeAnimalCache
import com.android.wildex.utils.offline.FakeUserAnimalsCache
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test

private const val USER_ANIMALS_COLLECTION_PATH = "userAnimals"

class UserAnimalsRepositoryFirestoreTest : FirestoreTest(USER_ANIMALS_COLLECTION_PATH) {
  private val animalCache = FakeAnimalCache()
  private val userAnimalsCache = FakeUserAnimalsCache()
  private var animalRepository = AnimalRepositoryFirestore(FirebaseEmulator.firestore, animalCache)
  private var repository =
      UserAnimalsRepositoryFirestore(FirebaseEmulator.firestore, userAnimalsCache, animalRepository)

  private suspend fun getUsersCount(): Int = super.getCount()

  @Test
  fun initializeUserAnimalsWhenNoUserExists() = runTest {
    var exceptionThrown = false

    try {
      repository.initializeUserAnimals(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assertFalse(exceptionThrown)
    assertEquals(1, getUsersCount())

    val animalsId = repository.getAllAnimalsByUser(user1.userId)
    val animalsCount = repository.getAnimalsCountOfUser(user1.userId)

    assertTrue(animalsId.isEmpty())
    assertEquals(0, animalsCount)
  }

  @Test
  fun initializeUserAnimalsWhenUserAlreadyExists() = runTest {
    repository.initializeUserAnimals(user1.userId)

    var exceptionThrown = false

    try {
      repository.initializeUserAnimals(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userAnimal with userId '${user1.userId}' already exists.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun getAllAnimalsByUserWhenNoUserExists() = runTest {
    var exceptionThrown = false

    try {
      repository.getAllAnimalsByUser(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userAnimal with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun getAnimalsCountOfUserWhenNoUserExists() = runTest {
    var exceptionThrown = false

    try {
      repository.getAnimalsCountOfUser(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userAnimal with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun addAnimalToUserAnimalsWhenNoUserExists() = runTest {
    var exceptionThrown = false

    try {
      repository.addAnimalToUserAnimals(user1.userId, animal1.animalId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userAnimal with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun addAnimalToUserAnimalsWhenUserAlreadyExists() = runTest {
    animalRepository.addAnimal(animal1)
    repository.initializeUserAnimals(user1.userId)

    var exceptionThrown = false

    try {
      repository.addAnimalToUserAnimals(user1.userId, animal1.animalId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assertFalse(exceptionThrown)

    val animalsId = repository.getAllAnimalsByUser(user1.userId)
    val animalsCount = repository.getAnimalsCountOfUser(user1.userId)

    assertEquals(1, animalsCount)
    assertEquals(1, animalsId.size)
    assertTrue(animalsId.contains(animal1))
  }

  @Test
  fun addAnimalToUserAnimalsTwiceSameAnimal() = runTest {
    animalRepository.addAnimal(animal1)
    animalRepository.addAnimal(animal2)
    repository.initializeUserAnimals(user1.userId)

    var exceptionThrown = false

    try {
      repository.addAnimalToUserAnimals(user1.userId, animal1.animalId)
      repository.addAnimalToUserAnimals(user1.userId, animal2.animalId)
      repository.addAnimalToUserAnimals(user1.userId, animal1.animalId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assertFalse(exceptionThrown)

    val animalsId = repository.getAllAnimalsByUser(user1.userId)
    val animalsCount = repository.getAnimalsCountOfUser(user1.userId)

    assertEquals(2, animalsCount)
    assertEquals(2, animalsId.size)
    assertTrue(animalsId.contains(animal1))
    assertTrue(animalsId.contains(animal2))
  }

  @Test
  fun deleteAnimalToUserAnimalsWhenNoUserExists() = runTest {
    var exceptionThrown = false

    try {
      repository.deleteAnimalToUserAnimals(user1.userId, animal1.animalId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userAnimal with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun deleteAnimalToUserAnimalsWhenUserAlreadyExists() = runTest {
    animalRepository.addAnimal(animal1)
    repository.initializeUserAnimals(user1.userId)
    repository.addAnimalToUserAnimals(user1.userId, animal1.animalId)

    var exceptionThrown = false

    try {
      repository.deleteAnimalToUserAnimals(user1.userId, animal1.animalId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assertFalse(exceptionThrown)

    val animalsId = repository.getAllAnimalsByUser(user1.userId)
    val animalsCount = repository.getAnimalsCountOfUser(user1.userId)

    assertTrue(animalsId.isEmpty())
    assertEquals(0, animalsCount)
  }

  @Test
  fun deleteAnimalToUserAnimalsTwiceSameAnimal() = runTest {
    animalRepository.addAnimal(animal1)
    animalRepository.addAnimal(animal2)
    repository.initializeUserAnimals(user1.userId)
    repository.addAnimalToUserAnimals(user1.userId, animal1.animalId)
    repository.addAnimalToUserAnimals(user1.userId, animal2.animalId)

    var exceptionThrown = false

    try {
      repository.deleteAnimalToUserAnimals(user1.userId, animal1.animalId)
      repository.deleteAnimalToUserAnimals(user1.userId, animal1.animalId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assertFalse(exceptionThrown)

    val animalsId = repository.getAllAnimalsByUser(user1.userId)
    val animalsCount = repository.getAnimalsCountOfUser(user1.userId)

    assertTrue(animalsId.contains(animal2))
    assertEquals(1, animalsId.size)
    assertEquals(1, animalsCount)
  }

  @Test
  fun deleteUserAnimalsWhenNoUserExists() = runTest {
    var exceptionThrown = false

    try {
      repository.deleteUserAnimals(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userAnimal with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun deleteUserAnimalsWhenUserExists() = runTest {
    var exceptionThrown = false
    repository.initializeUserAnimals(user1.userId)

    repository.deleteUserAnimals(user1.userId)
    try {
      repository.getAllAnimalsByUser(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals("A userAnimal with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun getAllAnimalsByUserUsesCacheWhenPresent() {
    runTest {
      animalRepository.addAnimal(animal1)
      animalRepository.addAnimal(animal2)
      val cached =
          UserAnimals(
              userId = user1.userId,
              animalsId = listOf(animal1.animalId, animal2.animalId),
              animalsCount = 2)
      userAnimalsCache.saveUserAnimals(cached)

      val animals = repository.getAllAnimalsByUser(user1.userId)
      assertEquals(2, animals.size)
      assertTrue(animals.contains(animal1))
      assertTrue(animals.contains(animal2))
    }
  }

  @Test
  fun getAnimalsCountOfUserUsesCacheWhenPresent() {
    runTest {
      val cached = UserAnimals(userId = user1.userId, animalsId = listOf(), animalsCount = 0)
      userAnimalsCache.saveUserAnimals(cached)
      assertEquals(0, repository.getAnimalsCountOfUser(user1.userId))
    }
  }

  @Test
  fun addAnimalToUserAnimalsDoesNotDuplicateAnimal() {
    runTest {
      animalRepository.addAnimal(animal1)
      repository.initializeUserAnimals(user1.userId)
      repository.addAnimalToUserAnimals(user1.userId, animal1.animalId)
      repository.addAnimalToUserAnimals(user1.userId, animal1.animalId)

      assertEquals(1, repository.getAllAnimalsByUser(user1.userId).size)
      assertEquals(1, repository.getAnimalsCountOfUser(user1.userId))
    }
  }

  @Test
  fun deleteAnimalToUserAnimalsDoesNothingIfAnimalMissing() {
    runTest {
      animalRepository.addAnimal(animal1)
      animalRepository.addAnimal(animal2)
      repository.initializeUserAnimals(user1.userId)
      repository.addAnimalToUserAnimals(user1.userId, animal1.animalId)
      repository.addAnimalToUserAnimals(user1.userId, animal2.animalId)
      repository.deleteAnimalToUserAnimals(user1.userId, animal1.animalId)
      repository.deleteAnimalToUserAnimals(user1.userId, animal1.animalId)

      assertEquals(1, repository.getAllAnimalsByUser(user1.userId).size)
      assertEquals(1, repository.getAnimalsCountOfUser(user1.userId))
    }
  }

  @Test
  fun getAllAnimalByUserSavesResultToCache() {
    runTest {
      animalRepository.addAnimal(animal1)
      repository.initializeUserAnimals(user1.userId)
      repository.addAnimalToUserAnimals(user1.userId, animal1.animalId)
      userAnimalsCache.clearAll()

      repository.getAllAnimalsByUser(user1.userId)
      val cached = userAnimalsCache.getUserAnimals(user1.userId)
      assertEquals(1, cached?.animalsCount)
      assertTrue(cached?.animalsId?.contains(animal1.animalId) == true)
    }
  }

  @Test
  fun refreshCacheClearsUSerAnimalsCache() {
    runTest {
      val cached =
          UserAnimals(userId = user1.userId, animalsId = listOf(animal1.animalId), animalsCount = 1)
      userAnimalsCache.saveUserAnimals(cached)
      repository.refreshCache()
      assertNull(userAnimalsCache.getUserAnimals(user1.userId))
    }
  }
}
