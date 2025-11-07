package com.android.wildex.model.user

import com.android.wildex.model.utils.Id
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val USER_SETTINGS_COLLECTION_PATH = "userSettings"

class UserSettingsRepositoryFirestore(private val db: FirebaseFirestore) : UserSettingsRepository {

  private val collection = db.collection(USER_SETTINGS_COLLECTION_PATH)

  override suspend fun initializeUserSettings(userId: String) {
    val docRef = collection.document(userId)
    ensureDocumentDoesNotExist(docRef, userId)
    docRef.set(UserSettings(userId = userId)).await()
  }

  override suspend fun getEnableNotification(userId: String): Boolean {
    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    val userSettings = docRef.get().await().toObject(UserSettings::class.java)
    return userSettings?.enableNotifications ?: throw IllegalStateException("UserSettings for userId '$userId' is null.")
  }

  override suspend fun setEnableNotification(userId: String, enable: Boolean) {
    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    docRef.update("enableNotifications", enable).await()
  }

  override suspend fun getAppearanceMode(userId: String): AppearanceMode {
    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    val userSettings = docRef.get().await().toObject(UserSettings::class.java)
    return userSettings?.appearanceMode ?: throw IllegalStateException("UserSettings for userId '$userId' is null.")
  }

  override suspend fun setAppearanceMode(
    userId: String,
    mode: AppearanceMode
  ) {
    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    docRef.update("appearanceMode", mode).await()
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