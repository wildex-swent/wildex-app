package com.android.wildex.model.user

import com.android.wildex.model.animal.Animal
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.cache.user.IUserAnimalsCache
import com.android.wildex.model.utils.Id
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val USER_ANIMALS_COLLECTION_PATH = "userAnimals"

class UserAnimalsRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val cache: IUserAnimalsCache,
    private val animalRepository: AnimalRepository,
) : UserAnimalsRepository {

  private val collection = db.collection(USER_ANIMALS_COLLECTION_PATH)

  /**
   * Initializes UserAnimals for a new User with empty list and zero count.
   *
   * @param userId The ID of the user whose animals are to be initialized
   */
  override suspend fun initializeUserAnimals(userId: Id) {
    val docRef = collection.document(userId)
    ensureDocumentDoesNotExist(docRef, userId)
    val userAnimals = UserAnimals(userId = userId)
    docRef.set(userAnimals).await()
    cache.saveUserAnimals(userAnimals)
  }

  /**
   * Retrieves all UserAnimals associated with a specific User.
   *
   * @param userId The id of the user whose animals are to be retrieved
   * @return The list of all the animals of a specific user
   */
  override suspend fun getAllAnimalsByUser(userId: Id): List<Animal> {
    cache.getUserAnimals(userId)?.animalsId?.let {
      return it.map { id -> animalRepository.getAnimal(id) }
    }

    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    val userAnimals = docRef.get().await().toObject(UserAnimals::class.java)
    requireNotNull(userAnimals)
    val animals = userAnimals.animalsId.map { animalRepository.getAnimal(it) }

    cache.saveUserAnimals(userAnimals)
    return animals
  }

  /**
   * Get UserAnimals count of a specific User.
   *
   * @param userId The id of the user whose animalsCount is to be retrieved
   * @return The animalsCount of a specific user
   */
  override suspend fun getAnimalsCountOfUser(userId: Id): Int {
    cache.getAnimalsCountOfUser(userId)?.let {
      return it
    }

    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    val userAnimals = docRef.get().await().toObject(UserAnimals::class.java)
    requireNotNull(userAnimals)
    val count = userAnimals.animalsCount

    cache.saveUserAnimals(userAnimals)
    return count
  }

  /**
   * Add an Animal to the UserAnimals of a specific User.
   *
   * @param userId The Id of the user whose animalsId is to be updated
   * @param animalId the Id of the animal to add to a specific user's animalsId
   */
  override suspend fun addAnimalToUserAnimals(userId: Id, animalId: Id) {
    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    val userAnimals = docRef.get().await().toObject(UserAnimals::class.java)
    val animalsId = userAnimals?.animalsId?.toMutableList() ?: mutableListOf()
    var animalsCount = userAnimals?.animalsCount ?: 0
    if (!animalsId.contains(animalId)) {
      animalsId.add(animalId)
      animalsCount = animalsCount.inc()
    }

    val newUserAnimals =
        UserAnimals(userId = userId, animalsId = animalsId, animalsCount = animalsCount)
    docRef.set(newUserAnimals).await()
    cache.saveUserAnimals(newUserAnimals)
  }

  /**
   * Delete an Animal to the UserAnimals of a specific User.
   *
   * @param userId The Id of the user whose animalsId is to be updated
   * @param animalId the Id of the animal to delete from a specific user's animalsId
   */
  override suspend fun deleteAnimalToUserAnimals(userId: Id, animalId: Id) {
    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    val userAnimals = docRef.get().await().toObject(UserAnimals::class.java)
    val animalsId = userAnimals?.animalsId?.toMutableList() ?: mutableListOf()
    var animalsCount = userAnimals?.animalsCount ?: 0
    if (animalsId.contains(animalId)) {
      animalsId.remove(animalId)
      animalsCount = animalsCount.dec()
    }

    val newUserAnimals =
        UserAnimals(userId = userId, animalsId = animalsId, animalsCount = animalsCount)
    docRef.set(newUserAnimals).await()
    cache.saveUserAnimals(newUserAnimals)
  }

  override suspend fun deleteUserAnimals(userId: Id) {
    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)
    docRef.delete().await()
    cache.deleteUserAnimals(userId)
  }

  override suspend fun refreshCache() {
    cache.clearAll()
  }

  /**
   * Ensures no userAnimals item in the document reference has a specific userId.
   *
   * @param docRef The document reference containing all userAnimals.
   * @param userId The ID that no userAnimals should have.
   */
  private suspend fun ensureDocumentDoesNotExist(docRef: DocumentReference, userId: Id) {
    val doc = docRef.get().await()
    require(!doc.exists()) { "A userAnimal with userId '${userId}' already exists." }
  }

  /**
   * Ensures one userAnimals item in the document reference has a specific userId.
   *
   * @param docRef The document reference containing all userAnimals.
   * @param commentId The ID that one userAnimals should have.
   */
  private suspend fun ensureDocumentExists(docRef: DocumentReference, userId: String) {
    val doc = docRef.get().await()
    require(doc.exists()) { "A userAnimal with userId '${userId}' does not exist." }
  }
}
