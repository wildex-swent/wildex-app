package com.android.wildex.utils.offline

import com.android.wildex.model.cache.usersettings.IUserSettingsCache
import com.android.wildex.model.user.AppearanceMode

class FakeUserSettingsCache : IUserSettingsCache {
  private var enableNotifications: Boolean? = null
  private var appearanceMode: AppearanceMode? = null

  init {
    enableNotifications = null
    appearanceMode = null
  }

  override suspend fun getEnableNotification(): Boolean? = enableNotifications

  override suspend fun setEnableNotification(enable: Boolean) {
    enableNotifications = enable
  }

  override suspend fun getAppearanceMode(): AppearanceMode? = appearanceMode

  override suspend fun setAppearanceMode(mode: AppearanceMode) {
    appearanceMode = mode
  }

  override suspend fun clear() {
    enableNotifications = null
    appearanceMode = null
  }
}
