package com.android.wildex.model.cache.user

import com.android.wildex.model.user.UserAnimals
import com.android.wildex.model.utils.Id

/** Cache interface for user animals data. */
interface IUserAnimalsCache {
  /**
   * Retrieves UserAnimals by user ID from the cache.
   *
   * @param userId The ID of the user whose animals to retrieve.
   * @return The UserAnimals object if found, or null if not found.
   */
  suspend fun getUserAnimals(userId: Id): UserAnimals?

  suspend fun getAnimalsCountOfUser(userId: Id): Int?

  /**
   * Saves UserAnimals to the cache.
   *
   * @param userAnimals The UserAnimals object to save.
   */
  suspend fun saveUserAnimals(userAnimals: UserAnimals)

  /**
   * Deletes UserAnimals associated with a specific user ID from the cache.
   *
   * @param userId The ID of the user whose animals to delete.
   */
  suspend fun deleteUserAnimals(userId: Id)

  /** Clears all user animals data from the cache. */
  suspend fun clearAll()
}
