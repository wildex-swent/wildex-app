package com.android.wildex.model.cache.usersettings

import com.android.wildex.model.user.AppearanceMode

/** Cache interface for user settings. */
interface IUserSettingsCache {
  /**
   * Retrieves the notification enablement setting.
   *
   * @return Boolean indicating if notifications are enabled, or null if not set.
   */
  suspend fun getEnableNotification(): Boolean?

  /**
   * Sets the notification enablement setting.
   *
   * @param enable Boolean indicating if notifications should be enabled.
   */
  suspend fun setEnableNotification(enable: Boolean)

  /**
   * Retrieves the appearance mode setting.
   *
   * @return AppearanceMode indicating the current appearance mode, or null if not set.
   */
  suspend fun getAppearanceMode(): AppearanceMode?

  /**
   * Sets the appearance mode setting.
   *
   * @param mode AppearanceMode to be set.
   */
  suspend fun setAppearanceMode(mode: AppearanceMode)

  /** Clears all user settings from the cache. */
  suspend fun clear()
}
