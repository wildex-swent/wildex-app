package com.android.wildex.utils.offline

import com.android.wildex.model.cache.user.IUserCache
import com.android.wildex.model.user.User
import com.android.wildex.model.utils.Id

class FakeUserCache : IUserCache {
  val cache = mutableMapOf<Id, User>()

  init {
    cache.clear()
  }

  override suspend fun getUser(userId: Id): User? {
    return cache[userId]
  }

  override suspend fun getAllUsers(): List<User>? {
    return cache.values.toList()
  }

  override suspend fun saveUser(user: User) {
    cache.put(user.userId, user)
  }

  override suspend fun saveUsers(users: List<User>) {
    users.forEach { saveUser(it) }
  }

  override suspend fun deleteUser(userId: Id) {
    cache.remove(userId)
  }

  override suspend fun clearAll() {
    cache.clear()
  }
}
