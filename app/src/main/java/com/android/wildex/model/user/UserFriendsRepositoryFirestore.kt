package com.android.wildex.model.user

import com.android.wildex.model.utils.Id
import com.google.firebase.firestore.FirebaseFirestore

private const val USER_FRIENDS_COLLECTION_PATH = "userFriends"
private val todo: Nothing = TODO("Not yet implemented")

class UserFriendsRepositoryFirestore(private val db: FirebaseFirestore) : UserFriendsRepository {
  override suspend fun initializeUserFriends(userId: Id) {
    todo
  }

  override suspend fun getAllFriendsOfUser(userId: Id): List<Id> {
    todo
  }

  override suspend fun getFriendsCountOfUser(userId: Id): Int {
    todo
  }

  override suspend fun addFriendToUserFriendsOfUser(friendId: Id, userId: Id) {
    todo
  }

  override suspend fun deleteFriendToUserFriendsOfUser(friendId: Id, userId: Id) {
    todo
  }

  override suspend fun deleteUserFriendsOfUser(userId: Id) {
    todo
  }
}
