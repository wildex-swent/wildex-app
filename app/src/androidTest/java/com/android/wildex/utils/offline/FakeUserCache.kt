package com.android.wildex.utils.offline

import com.android.wildex.model.cache.user.IUserCache
import com.android.wildex.model.user.User
import com.android.wildex.model.utils.Id

class FakeUserCache : IUserCache {
  val setOfUsers = mutableSetOf<User>()

  init {
    setOfUsers.clear()
  }

  override suspend fun getUser(userId: Id): User? {
    return setOfUsers.find { it.userId == userId }
  }

  override suspend fun getAllUsers(): List<User>? {
    return setOfUsers.toList()
  }

  override suspend fun saveUser(user: User) {
    setOfUsers.add(user)
  }

  override suspend fun saveUsers(users: List<User>) {
    users.forEach { setOfUsers.add(it) }
  }

  override suspend fun deleteUser(userId: Id) {
    setOfUsers.removeIf { it.userId == userId }
  }

  override suspend fun clearAll() {
    setOfUsers.clear()
  }
}
