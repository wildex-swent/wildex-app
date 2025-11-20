package com.android.wildex.model.friendRequest

import com.android.wildex.model.utils.Id

/** Represents a repository that manages Friend Requests. */
interface FriendRequestRepository {

  /** Initialize a Friend Request between two users: one sender and one receiver. */
  suspend fun initializeFriendRequest(senderId: Id, receiverId: Id)

  /** Retrieves all Friend Requests associated with a specific user as a sender. */
  suspend fun getAllFriendRequestsBySender(senderId: Id): List<FriendRequest>

  /** Retrieves all Friend Requests associated with a specific user as a receiver. */
  suspend fun getAllFriendRequestsByReceiver(receiverId: Id): List<FriendRequest>

  /** Deletes a Friend Request from the repository. */
  suspend fun deleteFriendRequest(friendRequest: FriendRequest)

  /** Deletes all Friend Requests of a user, no matter if he's a sender or a receiver. */
  suspend fun deleteAllFriendRequestsOfUser(userId: Id)
}
