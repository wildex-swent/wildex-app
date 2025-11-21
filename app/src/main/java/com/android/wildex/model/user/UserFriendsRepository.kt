package com.android.wildex.model.user

import com.android.wildex.model.utils.Id

/** Represents a repository that manages UserFriends items. */
interface UserFriendsRepository {

  /** Initializes UserFriends for a new User with empty list and zero count. */
  suspend fun initializeUserFriends(userId: Id)

  /** Retrieves UserFriends associated with a specific User. */
  suspend fun getAllFriendsOfUser(userId: Id): List<Id>

  /** Get UserFriends count of a specific User. */
  suspend fun getFriendsCountOfUser(userId: Id): Int

  /** Add a Friend to the UserFriends of a specific User. */
  suspend fun addFriendToUserFriendsOfUser(friendId: Id, userId: Id)

  /** Delete a Friend to the UserFriends of a specific User. */
  suspend fun deleteFriendToUserFriendsOfUser(friendId: Id, userId: Id)

  /** Delete the UserFriends linked to the given user */
  suspend fun deleteUserFriendsOfUser(userId: Id)
}
