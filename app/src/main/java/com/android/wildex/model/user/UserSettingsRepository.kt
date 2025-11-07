package com.android.wildex.model.user

/** Represents a repository that manages UserSettings items. */
interface UserSettingsRepository {

  /** Initializes UserSettings for a new User with default settings. */
  suspend fun initializeUserSettings(userId: String)

  /** Retrieves whether a specific user enabled notifications or not */
  suspend fun getEnableNotification(userId: String): Boolean

  /** Sets whether a specific user enables notifications or not */
  suspend fun setEnableNotification(userId: String, enable: Boolean)

  /** Retrieves the appearance mode preference for a specific user */
  suspend fun getAppearanceMode(userId: String): AppearanceMode

  /** Sets the appearance mode preference for a specific user */
  suspend fun setAppearanceMode(userId: String, mode: AppearanceMode)
}