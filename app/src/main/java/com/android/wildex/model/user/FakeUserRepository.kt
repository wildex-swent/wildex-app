package com.android.wildex.model.user

import com.android.wildex.model.utils.Id

class FakeUserRepository : UserRepository {
  val users = mutableListOf<User>()
  private var counter = 0

  override fun getNewUid(): String {
    return "userId-${counter++}"
  }

  override suspend fun getUser(userId: Id): User {
    return users.find { it.userId == userId }!!
  }

  override suspend fun getSimpleUser(userId: Id): SimpleUser {
    return users
      .find { it.userId == userId }
      ?.let {
        SimpleUser(
          userId = it.userId,
          username = it.username,
          profilePictureURL = it.profilePictureURL,
        )
      }!!
  }

  override suspend fun addUser(user: User) {
    users.add(user)
  }

  override suspend fun editUser(
    userId: Id,
    newUser: User
  ) {
    users.removeIf { it.userId == userId }
    users.add(newUser)
  }

  override suspend fun deleteUser(userId: Id) {
    users.removeIf { it.userId == userId }
  }
}