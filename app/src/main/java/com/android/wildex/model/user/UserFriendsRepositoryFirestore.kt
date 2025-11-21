package com.android.wildex.model.user

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.utils.Id
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val USER_FRIENDS_COLLECTION_PATH = "userFriends"

/** Represents a repository that manages UserFriends items. */
class UserFriendsRepositoryFirestore(private val db: FirebaseFirestore) : UserFriendsRepository {

  private val collection = db.collection(USER_FRIENDS_COLLECTION_PATH)

  /**
   * Initializes UserFriends for a new User with empty list and zero count.
   *
   * @param userId the ID of the user whose list of friends is to be initialized
   */
  override suspend fun initializeUserFriends(userId: Id) {
    val docRef = collection.document(userId)
    ensureDocumentDoesNotExist(docRef, userId)
    docRef.set(UserFriends(userId = userId)).await()
  }

  /**
   * Retrieves UserFriends associated with a specific User.
   *
   * @param userId The Id of the user whose list of Friends is to be retrieved
   */
  override suspend fun getAllFriendsOfUser(userId: Id): List<User> {
    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    val userFriends = docRef.get().await().toObject(UserFriends::class.java)
    val friends =
        userFriends?.friendsId?.map { RepositoryProvider.userRepository.getUser(it) } ?: emptyList()

    return friends
  }

  /**
   * Get UserFriends count of a specific User.
   *
   * @param userId The Id of the user whose friendsCount is to be retrieved
   */
  override suspend fun getFriendsCountOfUser(userId: Id): Int {
    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    val userFriends = docRef.get().await().toObject(UserFriends::class.java)
    return userFriends?.friendsCount ?: 0
  }

  /**
   * Add a Friend to the UserFriends of a specific User.
   *
   * @param friendId The Id of the friend to add to the user's list of friends
   * @param userId The Id of the user whose UserFriends is to be updated
   */
  override suspend fun addFriendToUserFriendsOfUser(friendId: Id, userId: Id) {
    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    val userFriends = docRef.get().await().toObject(UserFriends::class.java)
    val friendsId = userFriends?.friendsId?.toMutableList() ?: mutableListOf()
    var friendsCount = userFriends?.friendsCount ?: 0
    if (!friendsId.contains(friendId)) {
      friendsId.add(friendId)
      friendsCount = friendsCount.inc()
    }

    docRef
        .set(UserFriends(userId = userId, friendsId = friendsId, friendsCount = friendsCount))
        .await()
  }

  /**
   * Delete a Friend to the UserFriends of a specific User.
   *
   * @param friendId The Id of the friend to delete from the user's list of friends
   * @param userId The Id of the user whose UserFriends is to be updated
   */
  override suspend fun deleteFriendToUserFriendsOfUser(friendId: Id, userId: Id) {
    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)

    val userFriends = docRef.get().await().toObject(UserFriends::class.java)
    val friendsId = userFriends?.friendsId?.toMutableList() ?: mutableListOf()
    var friendsCount = userFriends?.friendsCount ?: 0
    if (friendsId.contains(friendId)) {
      friendsId.remove(friendId)
      friendsCount = friendsCount.dec()
    }

    docRef
        .set(UserFriends(userId = userId, friendsId = friendsId, friendsCount = friendsCount))
        .await()
  }

  /**
   * Delete the UserFriends linked to the given user
   *
   * @param userId The Id of the user whose UserFriends is to be deleted
   */
  override suspend fun deleteUserFriendsOfUser(userId: Id) {
    val docRef = collection.document(userId)
    ensureDocumentExists(docRef, userId)
    docRef.delete().await()
  }

  /**
   * Ensures no userFriends item in the document reference has a specific userId.
   *
   * @param docRef The document reference containing all userFriends.
   * @param userId The ID that no userFriends should have.
   */
  private suspend fun ensureDocumentDoesNotExist(docRef: DocumentReference, userId: Id) {
    val doc = docRef.get().await()
    require(!doc.exists()) { "A userFriends with userId '${userId}' already exists." }
  }

  /**
   * Ensures one userFriends item in the document reference has a specific userId.
   *
   * @param docRef The document reference containing all userFriends.
   * @param userId The ID that one userFriends should have.
   */
  private suspend fun ensureDocumentExists(docRef: DocumentReference, userId: String) {
    val doc = docRef.get().await()
    require(doc.exists()) { "A userFriends with userId '${userId}' does not exist." }
  }
}
