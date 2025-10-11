package com.android.wildex.model.achievement

/** Represents a repository that manages Achievement items. */
interface UserAchievementsRepository {

  /**
   * Initializes achievements for a new user
   *
   * @param userId The unique identifier of the user to initialize achievements for.
   */
  suspend fun initializeUserAchievements(userId: String)

  /**
   * Retrieves all Achievement items associated with a specific user
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
   * Adds an Achievement to the user
   * * @param userId The unique identifier of the user to whom the achievement is to be added.
   *
   * @param newValue The Achievement item to be added to the user's achievements.
   */
  suspend fun addAchievementToUser(userId: String, newValue: Achievement)

  /**
   * Update achievement count of the user
   *
   * @param userId The unique identifier of the user whose achievement count is to be updated.
   * @param newValue The new achievement count to be set for the user.
   */
  suspend fun updateAchievementCountOfUser(userId: String, newValue: Int)
}
