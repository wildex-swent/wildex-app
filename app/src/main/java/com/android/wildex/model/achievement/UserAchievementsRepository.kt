package com.android.wildex.model.achievement

import com.android.wildex.model.utils.Id

/** Represents a repository that manages Achievement items. */
interface UserAchievementsRepository {

  /**
   * Initializes achievements for a new user with empty achievements list and zero count.
   *
   * @param userId The unique identifier of the user to initialize achievements for.
   */
  suspend fun initializeUserAchievements(userId: String)

  /**
   * Retrieves all Achievements associated with a specific user
   *
   * @param userId The unique identifier of the user whose achievements are to be retrieved.
   * @return A list of Achievement items associated with the specified user.
   */
  suspend fun getAllAchievementsByUser(userId: String): List<Achievement>

  /**
   * Retrieves all Achievement items associated with the currently authenticated user.
   *
   * @return A list of Achievement items associated with the current user.
   */
  suspend fun getAllAchievementsByCurrentUser(): List<Achievement>

  /**
   * Takes a map of input keys to lists of IDs and updates the user's achievements accordingly.
   * Useful when multiple types of user activities need to be considered for achievement updates.
   * For example, if a user reaches milestones in different categories (like posts, likes,
   * comments), this function will update their achievements to reflect that.
   *
   * @param userId The unique identifier of the user whose achievements are to be updated.
   * @param inputs A map where keys are input types and values are lists of IDs associated with
   *   those input types.
   */
  suspend fun updateUserAchievements(userId: String, inputs: Map<InputKey, List<Id>>)

  /**
   * get achievement count of the user
   *
   * @param userId The unique identifier of the user whose achievements counts are to be retrieved.
   * @return The count of Achievement items associated with the specified user.
   */
  suspend fun getAchievementsCountOfUser(userId: String): Int
}
