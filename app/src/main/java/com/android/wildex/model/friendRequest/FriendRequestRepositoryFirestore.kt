package com.android.wildex.model.friendRequest

import com.android.wildex.model.utils.Id
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val FRIEND_REQUESTS_COLLECTION_PATH = "friendRequests"

private object FriendRequestsFields {
  const val SENDER_ID = "senderId"
  const val RECEIVER_ID = "receiverId"
}

/** Represents a repository that manages Friend Requests. */
class FriendRequestRepositoryFirestore(private val db: FirebaseFirestore) :
    FriendRequestRepository {

  private val collection = db.collection(FRIEND_REQUESTS_COLLECTION_PATH)

  /**
   * Initialize a Friend Request between two users: one sender and one receiver.
   *
   * @param senderId the Id of the user who is the sender
   * @param receiverId the Id of the user who is the receiver
   */
  override suspend fun initializeFriendRequest(senderId: Id, receiverId: Id) {
    collection.document().set(FriendRequest(senderId = senderId, receiverId = receiverId)).await()
  }

  /**
   * Retrieves all Friend Requests associated with a specific user as a sender.
   *
   * @param senderId the Id of the user who is a sender
   * @return The list of all Friend Requests with a specified sender
   */
  override suspend fun getAllFriendRequestsBySender(senderId: Id): List<FriendRequest> {
    return getAllFriendRequestsBySenderOrReceiver(userId = senderId, isSender = true)
  }

  /**
   * Retrieves all Friend Requests associated with a specific user as a receiver.
   *
   * @param receiverId the Id of the user who is a receiver
   * @return The list of all Friend Requests with a specified receiver
   */
  override suspend fun getAllFriendRequestsByReceiver(receiverId: Id): List<FriendRequest> {
    return getAllFriendRequestsBySenderOrReceiver(userId = receiverId, isSender = false)
  }

  /**
   * Accepts a Friend Request of the repository.
   *
   * @param friendRequest the friend request that was accepted
   */
  override suspend fun acceptFriendRequest(friendRequest: FriendRequest) {
    deleteFriendRequest(friendRequest)

    TODO("Remove this as a comment once the UserFriendRepository is implemented")
    /*
    RepositoryProvider.userFriendsRepository.addFriendToUserFriendsOfUser(
        friendId = friendRequest.senderId, userId = friendRequest.receiverId)

    RepositoryProvider.userFriendsRepository.addFriendToUserFriendsOfUser(
        friendId = friendRequest.receiverId, userId = friendRequest.senderId)
        */
  }

  /**
   * Refuses a Friend Request of the repository.
   *
   * @param friendRequest the friend request that was refuses
   */
  override suspend fun refuseFriendRequest(friendRequest: FriendRequest) {
    deleteFriendRequest(friendRequest)
  }

  /**
   * Deletes all Friend Requests of a user, no matter if he's a sender or a receiver.
   *
   * @param userId The Id of the user whose friend requests are to be deleted
   */
  override suspend fun deleteAllFriendRequestsOfUser(userId: Id) {
    getAllFriendRequestsBySender(userId).forEach { deleteFriendRequest(it) }
    getAllFriendRequestsByReceiver(userId).forEach { deleteFriendRequest(it) }
  }

  /**
   * Deletes a Friend Request from the repository.
   *
   * @param friendRequest The friend request who will be deleted
   */
  private suspend fun deleteFriendRequest(friendRequest: FriendRequest) {
    getFriendRequestDocRef(friendRequest.senderId, friendRequest.receiverId).delete().await()
  }

  /**
   * Retrieves the friend requests of a given user, initiated or received by him
   *
   * @param userId the user whose friend requests we want to fetch
   * @param isSender true if we want the friend requests initiated by the given user, false
   *   otherwise
   */
  private suspend fun getAllFriendRequestsBySenderOrReceiver(
      userId: Id,
      isSender: Boolean
  ): List<FriendRequest> {
    return collection
        .whereEqualTo(
            if (isSender) FriendRequestsFields.SENDER_ID else FriendRequestsFields.RECEIVER_ID,
            userId)
        .get()
        .await()
        .documents
        .mapNotNull { documentToFriendRequest(it) }
  }

  /**
   * Retrieves the document reference of the friend request with the given sender and receiver
   *
   * @param senderId the sender of the friend request whose document reference is to be retrieved
   * @param receiverId the receiver of the friend request whose document reference is to be
   *   retrieved
   */
  private suspend fun getFriendRequestDocRef(senderId: Id, receiverId: Id): DocumentReference {
    return collection
        .whereEqualTo(FriendRequestsFields.SENDER_ID, senderId)
        .whereEqualTo(FriendRequestsFields.RECEIVER_ID, receiverId)
        .get()
        .await()
        .documents
        .first() // assuming only one relationship between two users should exist
        .reference
  }

  /**
   * Converts a document reference to a friend request.
   *
   * @param document The document to be converted into a friend request.
   * @return The transformed [FriendRequest] object.
   */
  private fun documentToFriendRequest(document: DocumentSnapshot): FriendRequest? {
    return try {
      val senderId =
          document.getString(FriendRequestsFields.SENDER_ID)
              ?: throwMissingFieldException(FriendRequestsFields.SENDER_ID)
      val receiverId =
          document.getString(FriendRequestsFields.RECEIVER_ID)
              ?: throwMissingFieldException(FriendRequestsFields.RECEIVER_ID)

      FriendRequest(senderId = senderId, receiverId = receiverId)
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Throws an error for the missing comment field.
   *
   * @param field The name of the missing field.
   * @throws IllegalArgumentException if the field is missing.
   */
  private fun throwMissingFieldException(field: String): Nothing {
    throw IllegalArgumentException("Missing required field in Relationship: $field")
  }
}
