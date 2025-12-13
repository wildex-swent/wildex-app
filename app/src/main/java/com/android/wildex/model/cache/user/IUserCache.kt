package com.android.wildex.model.cache.user

import com.android.wildex.model.user.User
import com.android.wildex.model.utils.Id

/** Cache interface for user data. */
interface IUserCache {
  /**
   * Retrieves a user by their ID from the cache.
   *
   * @param userId The ID of the user to retrieve.
   * @return The User object if found, or null if not found.
   */
  suspend fun getUser(userId: Id): User?

  /**
   * Retrieves all users from the cache.
   *
   * @return A list of User objects if any are found, or null if none are found.
   */
  suspend fun getAllUsers(): List<User>?

  /**
   * Saves a user to the cache.
   *
   * @param user The User object to save.
   */
  suspend fun saveUser(user: User)

  /**
   * Saves multiple users to the cache.
   *
   * @param users A list of User objects to save.
   */
  suspend fun saveUsers(users: List<User>)

  /**
   * Deletes a user from the cache by their ID.
   *
   * @param userId The ID of the user to delete.
   */
  suspend fun deleteUser(userId: Id)

  /** Clears all user data from the cache. */
  suspend fun clearAll()
}
