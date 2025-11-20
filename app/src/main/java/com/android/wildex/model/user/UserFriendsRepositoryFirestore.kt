package com.android.wildex.model.user

import com.android.wildex.model.utils.Id
import com.google.firebase.firestore.FirebaseFirestore

const val USER_FRIENDS_COLLECTION_PATH = "userFriends"

class UserFriendsRepositoryFirestore(private val db: FirebaseFirestore) : UserFriendsRepository {
  override suspend fun initializeUserFriends(userId: Id) {
    TODO("Not yet implemented")
  }

  override suspend fun getAllFriendsOfUser(userId: Id): List<Id> {
    TODO("Not yet implemented")
  }

  override suspend fun getFriendsCountOfUser(userId: Id): Int {
    TODO("Not yet implemented")
  }

  override suspend fun addFriendToUserFriendsOfUser(friendId: Id, userId: Id) {
    TODO("Not yet implemented")
  }

  override suspend fun deleteFriendToUserFriendsOfUser(friendId: Id, userId: Id) {
    TODO("Not yet implemented")
  }

  override suspend fun deleteUserFriendsOfUser(userId: Id) {
    TODO("Not yet implemented")
  }
}
