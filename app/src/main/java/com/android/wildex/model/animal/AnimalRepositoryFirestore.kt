package com.android.wildex.model.animal

import android.util.Log
import com.android.wildex.model.utils.Id
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val ANIMAL_COLLECTION_PATH = "animals"

class AnimalRepositoryFirestore(private val db: FirebaseFirestore) : AnimalRepository {

  override suspend fun getAnimal(animalId: Id): Animal {
    val doc = db.collection(ANIMAL_COLLECTION_PATH).document(animalId).get().await()
    require(doc.exists()) { "Animal with given Id $animalId not found" }
    return convertToAnimal(doc)
        ?: throw IllegalArgumentException("Animal with given Id $animalId not found")
  }

  override suspend fun getAllAnimals(): List<Animal> {
    val collection = db.collection(ANIMAL_COLLECTION_PATH).get().await()
    val docs = collection.documents
    if (docs.isEmpty()) {
      Log.w("AnimalRepositoryFirestore", "No animals found in the collection.")
      return emptyList()
    }
    return docs.mapNotNull { convertToAnimal(it) }
  }

  override suspend fun addAnimal(animal: Animal) {
    if (animalExists(animal.name)) return

    val documentRef = db.collection(ANIMAL_COLLECTION_PATH).document(animal.animalId)
    val documentSnapshot = documentRef.get().await()

    require(!documentSnapshot.exists()) {
      "AnimalRepositoryFirestore: An animal with ID '${animal.animalId}' already exists."
    }

    documentRef.set(animal).await()
  }

  private fun convertToAnimal(doc: DocumentSnapshot): Animal? {
    return try {
      val animalId = doc.id
      val pictureURL = doc.getString("pictureURL") ?: throwMissingFieldException("pictureURL")
      val name = doc.getString("name") ?: throwMissingFieldException("name")
      val species = doc.getString("species") ?: throwMissingFieldException("species")
      val description = doc.getString("description") ?: throwMissingFieldException("description")

      Animal(
          animalId = animalId,
          pictureURL = pictureURL,
          name = name,
          species = species,
          description = description)
    } catch (e: Exception) {
      Log.e("AnimalRepositoryFirestore", "Error converting document to Animal", e)
      null
    }
  }

  private fun throwMissingFieldException(field: String): Nothing {
    throw IllegalArgumentException("Missing required field in AnimalRepository: $field")
  }

  private suspend fun animalExists(name: String): Boolean {
    return try {
      val snapshot = db.collection(ANIMAL_COLLECTION_PATH).whereEqualTo("name", name).get().await()
      !snapshot.isEmpty
    } catch (e: Exception) {
      Log.e("AnimalRepositoryFirestore", "Error checking if animal exists", e)
      false
    }
  }
}
