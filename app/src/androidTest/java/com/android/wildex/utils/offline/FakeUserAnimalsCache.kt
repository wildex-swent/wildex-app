package com.android.wildex.utils.offline

import com.android.wildex.model.cache.user.IUserAnimalsCache
import com.android.wildex.model.user.UserAnimals
import com.android.wildex.model.utils.Id

class FakeUserAnimalsCache : IUserAnimalsCache {
  val cache = mutableMapOf<String, UserAnimals>()

  init {
    cache.clear()
  }

  override suspend fun getUserAnimals(userId: Id): UserAnimals? {
    return cache[userId]
  }

  override suspend fun getAnimalsCountOfUser(userId: Id): Int? {
    return cache[userId]?.animalsCount
  }

  override suspend fun saveUserAnimals(userAnimals: UserAnimals) {
    cache.put(userAnimals.userId, userAnimals)
  }

  override suspend fun deleteUserAnimals(userId: Id) {
    cache.remove(userId)
  }

  override suspend fun clearAll() {
    cache.clear()
  }
}
