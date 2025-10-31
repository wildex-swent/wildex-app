package com.android.wildex.model.user

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.utils.Id
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val USER_ANIMALS_COLLECTION_PATH = "userAnimals"

class UserAnimalsRepositoryFirestore(private val db: FirebaseFirestore) : UserAnimalsRepository {

  private val collection = db.collection(USER_ANIMALS_COLLECTION_PATH)

  /**
   * Initializes UserAnimals for a new User with empty list and zero count.
   *
   * @param userId The ID of the user whose animals are to be initialized
   */
  override suspend fun initializeUserAnimals(userId: Id) {
    val docRef = collection.document(userId)
    ensureDocumentDoesNotExist(docRef, userId)
    docRef.set(UserAnimals(userId = userId)).await()
  }

  /**
   * Retrieves all UserAnimals associated with a specific User.
   *
   * @param userId The id of the user whose animals are to be retrieved
   * @return The list of all the animals of a specific user
   */
  override suspend fun getAllAnimalsByUser(userId: Id): List<Animal> {
    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    val userAnimals = docRef.get().await().toObject(UserAnimals::class.java)
    val animals =
        userAnimals?.animalsId?.map { RepositoryProvider.animalRepository.getAnimal(it) }
            ?: emptyList()

    return animals
  }

  /**
   * Get UserAnimals count of a specific User.
   *
   * @param userId The id of the user whose animalsCount is to be retrieved
   * @return The animalsCount of a specific user
   */
  override suspend fun getAnimalsCountOfUser(userId: Id): Int {
    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    val userAnimals = docRef.get().await().toObject(UserAnimals::class.java)
    return userAnimals?.animalsCount ?: 0
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

    docRef
        .set(UserAnimals(userId = userId, animalsId = animalsId, animalsCount = animalsCount))
        .await()
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

    docRef
        .set(UserAnimals(userId = userId, animalsId = animalsId, animalsCount = animalsCount))
        .await()
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
