package com.android.wildex.model.cache.user

import androidx.datastore.core.DataStore
import com.android.wildex.datastore.UserCacheStorage
import com.android.wildex.model.ConnectivityObserver
import com.android.wildex.model.user.User
import com.android.wildex.model.utils.Id
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private const val STALE_DURATION_MS = 10 * 60 * 1000L // 10 minutes

class UserCache(
    private val userDataStore: DataStore<UserCacheStorage>,
    private val connectivityObserver: ConnectivityObserver,
) : IUserCache {
  private fun isStale(lastUpdated: Long): Boolean {
    val isOnline = connectivityObserver.isOnline.value
    val currentTime = System.currentTimeMillis()
    return isOnline && (currentTime - lastUpdated) > STALE_DURATION_MS
  }

  override suspend fun getUser(userId: Id): User? {
    return userDataStore.data
        .map {
          val cached = it.usersMap[userId]
          if (cached != null && !isStale(cached.lastUpdated)) {
            cached.toUser()
          } else {
            null
          }
        }
        .firstOrNull()
  }

  override suspend fun getAllUsers(): List<User>? {
    return userDataStore.data
        .map { proto ->
          val users = proto.usersMap.values
          if (users.isNotEmpty() && users.all { !isStale(it.lastUpdated) }) {
            users.map { it.toUser() }
          } else {
            null
          }
        }
        .firstOrNull()
  }

  override suspend fun saveUser(user: User) {
    userDataStore.updateData { it.toBuilder().putUsers(user.userId, user.toProto()).build() }
  }

  override suspend fun saveUsers(users: List<User>) {
    users.forEach { saveUser(it) }
  }

  override suspend fun deleteUser(userId: Id) {
    userDataStore.updateData { it.toBuilder().removeUsers(userId).build() }
  }

  override suspend fun clearAll() {
    userDataStore.updateData { it.toBuilder().clearUsers().build() }
  }
}
