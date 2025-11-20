package com.android.wildex.model.relationship

import com.android.wildex.model.utils.Id

/** Represents a repository that manages Friend Requests. */
interface FriendRequestRepository {

  /** Initialize a Friend Request between two users: one sender and one receiver. */
  suspend fun initializeFriendRequest(senderId: Id, receiverId: Id)

  /** Retrieves all Friend Requests associated with a specific user as a sender. */
  suspend fun getAllFriendRequestsBySender(senderId: Id): List<Relationship>

  /** Retrieves all Friend Requests associated with a specific user as a receiver. */
  suspend fun getAllFriendRequestsByReceiver(receiverId: Id): List<Relationship>

  /** Adds a new Friend Request to the repository. */
  suspend fun acceptFriendRequest(relationship: Relationship)

  /** Deletes a Friend Request from the repository. */
  suspend fun deleteFriendRequest(relationship: Relationship)
}
