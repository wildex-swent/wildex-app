package com.android.wildex.model.cache.usersettings

import com.android.wildex.model.user.AppearanceMode

interface IUserSettingsCache {
  suspend fun getEnableNotification(): Boolean?

  suspend fun setEnableNotification(enable: Boolean)

  suspend fun getAppearanceMode(): AppearanceMode?

  suspend fun setAppearanceMode(mode: AppearanceMode)

  suspend fun clear()
}
