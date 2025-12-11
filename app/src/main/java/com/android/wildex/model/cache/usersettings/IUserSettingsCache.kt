package com.android.wildex.model.cache.usersettings

import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.utils.Id

/** Cache interface for user settings. */
interface IUserSettingsCache {
  /** Initializes the user settings in the cache with default values. */
  suspend fun initializeUserSettings(userId: Id)

  /**
   * Retrieves the notification enablement setting.
   *
   * @return Boolean indicating if notifications are enabled, or null if not set.
   */
  suspend fun getEnableNotification(userId: Id): Boolean?

  /**
   * Sets the notification enablement setting.
   *
   * @param enable Boolean indicating if notifications should be enabled.
   */
  suspend fun setEnableNotification(userId: Id, enable: Boolean)

  /**
   * Retrieves the appearance mode setting.
   *
   * @return AppearanceMode indicating the current appearance mode, or null if not set.
   */
  suspend fun getAppearanceMode(userId: Id): AppearanceMode?

  /**
   * Sets the appearance mode setting.
   *
   * @param mode AppearanceMode to be set.
   */
  suspend fun setAppearanceMode(userId: Id, mode: AppearanceMode)

  /** Clears all user settings from the cache. */
  suspend fun clear(userId: Id)
}
