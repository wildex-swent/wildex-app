package com.android.wildex.model.cache.user

import androidx.datastore.core.DataStore
import com.android.wildex.datastore.UserAnimalsCacheStorage
import com.android.wildex.model.ConnectivityObserver
import com.android.wildex.model.user.UserAnimals
import com.android.wildex.model.utils.Id
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private const val STALE_DURATION_MS = 10 * 60 * 1000L // 10 minutes

class UserAnimalsCache(
    private val userAnimalsDataStore: DataStore<UserAnimalsCacheStorage>,
    private val connectivityObserver: ConnectivityObserver
) : IUserAnimalsCache {
  private fun isStale(lastUpdated: Long): Boolean {
    val isOnline = connectivityObserver.isOnline.value
    val currentTime = System.currentTimeMillis()
    return isOnline && (currentTime - lastUpdated) > STALE_DURATION_MS
  }

  override suspend fun getUserAnimals(userId: Id): UserAnimals? {
    return userAnimalsDataStore.data
        .map { cacheStorage ->
          val cached = cacheStorage.userAnimalsMap[userId]
          if (cached != null && !isStale(cached.lastUpdated)) {
            cached.toUserAnimals()
          } else {
            null
          }
        }
        .firstOrNull()
  }

  override suspend fun getAnimalsCountOfUser(userId: Id): Int? {
    return userAnimalsDataStore.data
        .map { cacheStorage ->
          val cached = cacheStorage.userAnimalsMap[userId]
          if (cached != null && !isStale(cached.lastUpdated)) {
            cached.animalsCount
          } else {
            null
          }
        }
        .firstOrNull()
  }

  override suspend fun saveUserAnimals(userAnimals: UserAnimals) {
    userAnimalsDataStore.updateData {
      it.toBuilder().putUserAnimals(userAnimals.userId, userAnimals.toProto()).build()
    }
  }

  override suspend fun deleteUserAnimals(userId: Id) {
    userAnimalsDataStore.updateData { it.toBuilder().removeUserAnimals(userId).build() }
  }

  override suspend fun clearAll() {
    userAnimalsDataStore.updateData { it.toBuilder().clearUserAnimals().build() }
  }
}
