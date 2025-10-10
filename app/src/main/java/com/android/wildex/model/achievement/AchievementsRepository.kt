package com.android.wildex.model.achievement

/** Represents a repository that manages Achievement items. */
interface AchievementsRepository {

  /** Generates and returns a new unique identifier for an Achievement item. */
  fun getNewAchievementId(): String

  /** Retrieves all Achievement items from the repository. */
  suspend fun getAllAchievements(): List<Achievement>

  /** Retrieves all Achievement items associated with a specific user. */
  suspend fun getAllAchievementsByUser(userId: String): List<Achievement>

  /** Retrieves all Achievement items associated with the currently authenticated user. */
  suspend fun getAllAchievementsByCurrentUser(): List<Achievement>

  /** Retrieves a specific Achievement item by its unique identifier. */
  suspend fun getAchievement(achievementId: String): Achievement

  /** Adds a new Achievement item to the repository. */
  suspend fun addAchievement(achievement: Achievement)

  /** Edits an existing Achievement item in the repository. */
  suspend fun editAchievement(achievementId: String, newValue: Achievement)

  /** Deletes an Achievement item from the repository. */
  suspend fun deleteAchievement(achievementId: String)
}
