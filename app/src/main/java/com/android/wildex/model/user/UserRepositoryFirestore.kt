package com.android.wildex.model.user

import android.util.Log
import com.android.wildex.model.cache.user.IUserCache
import com.android.wildex.model.utils.Id
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val USERS_COLLECTION_PATH = "users"
private const val TAG = "UserRepositoryFirestore"

class UserRepositoryFirestore(private val db: FirebaseFirestore, private val cache: IUserCache) :
    UserRepository {

  override suspend fun getUser(userId: Id): User {
    cache.getUser(userId)?.let {
      return it
    }

    val document = db.collection(USERS_COLLECTION_PATH).document(userId).get().await()
    require(document.exists()) { "UserRepositoryFirestore: User $userId not found" }

    val user =
        documentToUser(document)
            ?: throw Exception("UserRepositoryFirestore: User $userId not found")

    cache.saveUser(user)
    return user
  }

  override suspend fun getAllUsers(): List<User> {
    cache.getAllUsers()?.let {
      return it
    }
    val collection = db.collection(USERS_COLLECTION_PATH).get().await()
    val users = mutableListOf<User>()
    for (document in collection.documents) {
      val user = documentToUser(document)
      if (user != null) {
        users.add(user)
      } else {
        Log.e(TAG, "UserRepositoryFirestore: error converting document ${document.id} to User")
      }
    }
    cache.saveUsers(users)
    return users
  }

  override suspend fun getSimpleUser(userId: Id): SimpleUser {
    cache.getUser(userId)?.let {
      return SimpleUser(
          userId = it.userId,
          username = it.username,
          profilePictureURL = it.profilePictureURL,
          userType = it.userType)
    }

    val document = db.collection(USERS_COLLECTION_PATH).document(userId).get().await()
    require(document.exists()) { "UserRepositoryFirestore: User $userId not found" }
    return documentToSimpleUser(document)
        ?: throw Exception("UserRepositoryFirestore: User $userId not found")
  }

  override suspend fun addUser(user: User) {
    val documentId = db.collection(USERS_COLLECTION_PATH).document(user.userId)
    val document = documentId.get().await()
    require(!document.exists()) {
      "UserRepositoryFirestore: A User with userId '${user.userId}' already exists."
    }
    documentId.set(user).await()
    cache.saveUser(user)
  }

  override suspend fun editUser(userId: Id, newUser: User) {
    val documentId = db.collection(USERS_COLLECTION_PATH).document(userId)
    val document = documentId.get().await()
    require(document.exists()) { "UserRepositoryFirestore: User $userId not found" }
    documentId.set(newUser.copy(userId = userId)).await()
    cache.saveUser(newUser.copy(userId = userId))
  }

  override suspend fun deleteUser(userId: Id) {
    val documentId = db.collection(USERS_COLLECTION_PATH).document(userId)
    val document = documentId.get().await()

    require(document.exists()) { "UserRepositoryFirestore: User $userId not found" }
    documentId.delete().await()
    cache.deleteUser(userId)
  }

  override suspend fun refreshCache() {
    cache.clearAll()
  }

  /**
   * Converts a Firestore document to a User object.
   *
   * @param document The Firestore document to convert.
   * @return The User object if conversion is successful, or `null` otherwise.
   */
  private fun documentToUser(document: DocumentSnapshot): User? {
    return try {
      val userId = document.id

      val username = document.getString("username") ?: return null
      val name = document.getString("name") ?: ""
      val surname = document.getString("surname") ?: ""
      val bio = document.getString("bio") ?: ""
      val profilePictureURL = document.getString("profilePictureURL") ?: ""

      val userTypeString = document.getString("userType") ?: UserType.REGULAR.name
      val userType =
          try {
            UserType.valueOf(userTypeString)
          } catch (_: Exception) {
            UserType.REGULAR
          }

      val creationDate = document.getTimestamp("creationDate") ?: return null
      val country = document.getString("country") ?: ""

      User(
          userId = userId,
          username = username,
          name = name,
          surname = surname,
          bio = bio,
          profilePictureURL = profilePictureURL,
          userType = userType,
          creationDate = creationDate,
          country = country)
    } catch (e: Exception) {
      Log.e(TAG, "documentToUser: error converting document ${document.id} to User", e)
      null
    }
  }

  /**
   * Converts a Firestore document to a SimpleUser object.
   *
   * @param document The Firestore document to convert.
   * @return The SimpleUser object if conversion is successful, or `null` otherwise.
   */
  private fun documentToSimpleUser(document: DocumentSnapshot): SimpleUser? {
    return try {
      val id = document.id
      val username = document.getString("username") ?: return null
      val profilePictureURL = document.getString("profilePictureURL") ?: ""
      val userType =
          document.getString("userType")?.let { UserType.valueOf(it) } ?: UserType.REGULAR
      SimpleUser(
          userId = id,
          username = username,
          profilePictureURL = profilePictureURL,
          userType = userType)
    } catch (e: Exception) {
      Log.e(TAG, "documentToSimpleUser: error converting document ${document.id}", e)
      null
    }
  }
}
