package com.android.wildex.model.achievement

/** Represents a repository that manages Achievement items. */
interface UserAchievementsRepository {

  /** Initializes achievements for a new user */
  suspend fun initializeUserAchievements(userId: String)

  /** Retrieves all Achievement items associated with a specific user. */
  suspend fun getAllAchievementsByUser(userId: String): List<Achievement>

  /** Retrieves all Achievement items associated with the currently authenticated user. */
  suspend fun getAllAchievementsByCurrentUser(): List<Achievement>

  /** Adds an Achievement to the user */
  suspend fun addAchievementToUser(userId: String, newValue: Achievement)

  /** Update achievement count of the user */
  suspend fun updateAchievementCountOfUser(userId: String, newValue: Int)
}
