package com.android.wildex.model.user

import com.android.wildex.model.utils.Id

/** Represents a repository that manages UserSettings items. */
interface UserSettingsRepository {

  /** Initializes UserSettings for a new User with default settings. */
  suspend fun initializeUserSettings(userId: Id)

  /** Retrieves whether a specific user enabled notifications or not */
  suspend fun getEnableNotification(userId: Id): Boolean

  /** Sets whether a specific user enables notifications or not */
  suspend fun setEnableNotification(userId: Id, enable: Boolean)

  /** Retrieves the appearance mode preference for a specific user */
  suspend fun getAppearanceMode(userId: Id): AppearanceMode

  /** Sets the appearance mode preference for a specific user */
  suspend fun setAppearanceMode(userId: Id, mode: AppearanceMode)

  /** Deletes the UserSettings of the given user*/
  suspend fun deleteUserSettings(userId: Id)
}
