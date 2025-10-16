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
   * Takes a list of all the user's posts IDs and updates the user's achievements accordingly.
   * Useful when a new post is added or an existing post is deleted. For example, if a user reaches
   * a milestone in the number of posts, this function will update their achievements to reflect
   * that. and if he deletes a post and falls below a milestone, the corresponding achievement will
   * be removed.
   *
   * @param userId The unique identifier of the user whose achievements are to be updated.
   * @param listIds A list of post IDs that may influence the user's achievements.
   */
  suspend fun updateUserAchievements(userId: String, listIds: List<Id>)

  /**
   * get achievement count of the user
   *
   * @param userId The unique identifier of the user whose achievements counts are to be retrieved.
   * @return The count of Achievement items associated with the specified user.
   */
  suspend fun getAchievementsCountOfUser(userId: String): Int
}
