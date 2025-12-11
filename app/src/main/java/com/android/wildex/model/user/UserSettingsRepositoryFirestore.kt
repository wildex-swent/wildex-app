package com.android.wildex.model.user

import com.android.wildex.model.cache.usersettings.IUserSettingsCache
import com.android.wildex.model.utils.Id
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val USER_SETTINGS_COLLECTION_PATH = "userSettings"

class UserSettingsRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val cache: IUserSettingsCache
) : UserSettingsRepository {

  private val collection = db.collection(USER_SETTINGS_COLLECTION_PATH)

  override suspend fun initializeUserSettings(userId: String) {
    val docRef = collection.document(userId)
    ensureDocumentDoesNotExist(docRef, userId)
    docRef.set(UserSettings(userId = userId)).await()
    cache.initializeUserSettings()
  }

  override suspend fun getEnableNotification(userId: String): Boolean {
    cache.getEnableNotification(userId)?.let {
      return it
    }

    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    val userSettings = docRef.get().await().toObject(UserSettings::class.java)
    val enable = userSettings?.enableNotifications ?: true

    cache.setEnableNotification(userId, enable)
    return enable
  }

  override suspend fun setEnableNotification(userId: String, enable: Boolean) {
    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    docRef.update("enableNotifications", enable).await()
    cache.setEnableNotification(userId, enable)
  }

  override suspend fun getAppearanceMode(userId: String): AppearanceMode {
    cache.getAppearanceMode(userId)?.let {
      return it
    }

    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    val userSettings = docRef.get().await().toObject(UserSettings::class.java)
    val mode = userSettings?.appearanceMode ?: AppearanceMode.AUTOMATIC

    cache.setAppearanceMode(userId, mode)
    return mode
  }

  override suspend fun setAppearanceMode(userId: String, mode: AppearanceMode) {
    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    docRef.update("appearanceMode", mode).await()
    cache.setAppearanceMode(userId, mode)
  }

  override suspend fun deleteUserSettings(userId: Id) {
    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    docRef.delete().await()
    cache.clear(userId)
  }

  /**
   * Ensures no UserSettings item in the document reference has a specific userId.
   *
   * @param docRef The document reference containing all userSettings.
   * @param userId The ID that no userSettings should have.
   */
  private suspend fun ensureDocumentDoesNotExist(docRef: DocumentReference, userId: Id) {
    val doc = docRef.get().await()
    require(!doc.exists()) { "A userSettings with userId '${userId}' already exists." }
  }

  /**
   * Ensures one UserSettings item in the document reference has a specific userId.
   *
   * @param docRef The document reference containing all userSettings.
   * @param userId The ID of the user whose userSettings should exist.
   */
  private suspend fun ensureDocumentExists(docRef: DocumentReference, userId: String) {
    val doc = docRef.get().await()
    require(doc.exists()) { "A userSettings with userId '${userId}' does not exist." }
  }
}
