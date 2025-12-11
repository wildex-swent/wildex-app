package com.android.wildex.model.cache.user

import com.android.wildex.model.user.User
import com.android.wildex.model.utils.Id

interface IUserCache {
  suspend fun getUser(userId: Id): User?

  suspend fun getAllUsers(): List<User>?

  suspend fun saveUser(user: User)

  suspend fun saveUsers(users: List<User>)

  suspend fun deleteUser(userId: Id)

  suspend fun clearAll()
}
