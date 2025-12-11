package com.android.wildex.utils.offline

import com.android.wildex.model.cache.usersettings.IUserSettingsCache
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.utils.Id

/** A fake implementation of IUserSettingsCache for testing purposes. */
class FakeUserSettingsCache : IUserSettingsCache {
  private val enableNotificationsMap = mutableMapOf<Id, Boolean>()
  private val appearanceModeMap = mutableMapOf<Id, AppearanceMode>()

  override suspend fun initializeUserSettings(userId: Id) {
    enableNotificationsMap[userId] = true
    appearanceModeMap[userId] = AppearanceMode.AUTOMATIC
  }

  override suspend fun getEnableNotification(userId: Id): Boolean? {
    return enableNotificationsMap[userId]
  }

  override suspend fun setEnableNotification(userId: Id, enable: Boolean) {
    enableNotificationsMap[userId] = enable
  }

  override suspend fun getAppearanceMode(userId: Id): AppearanceMode? {
    return appearanceModeMap[userId]
  }

  override suspend fun setAppearanceMode(userId: Id, mode: AppearanceMode) {
    appearanceModeMap[userId] = mode
  }

  override suspend fun clear(userId: Id) {
    enableNotificationsMap.remove(userId)
    appearanceModeMap.remove(userId)
  }
}
