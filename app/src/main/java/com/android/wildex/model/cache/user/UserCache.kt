package com.android.wildex.model.cache.user

import android.content.Context
import com.android.wildex.model.user.User
import com.android.wildex.model.utils.Id
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class UserCache(private val context: Context) : IUserCache {
  override suspend fun getUser(userId: Id): User? {
    return context.userDataStore.data.map { it.usersMap[userId]?.toUser() }.firstOrNull()
  }

  override suspend fun getAllUsers(): List<User>? {
    return context.userDataStore.data
        .map { proto -> proto.usersMap.values.map { it.toUser() } }
        .firstOrNull()
  }

  override suspend fun saveUser(user: User) {
    context.userDataStore.updateData {
      it.toBuilder().putUsers(user.userId, user.toProto()).build()
    }
  }

  override suspend fun saveUsers(users: List<User>) {
    context.userDataStore.updateData {
      val builder = it.toBuilder()
      users.forEach { user -> builder.putUsers(user.userId, user.toProto()) }
      builder.build()
    }
  }

  override suspend fun deleteUser(userId: Id) {
    context.userDataStore.updateData { it.toBuilder().removeUsers(userId).build() }
  }

  override suspend fun clearAll() {
    context.userDataStore.updateData { it.toBuilder().clearUsers().build() }
  }
}
