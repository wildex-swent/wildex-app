package com.android.wildex.model.cache.usersettings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.utils.Id
import com.mapbox.maps.extension.style.expressions.dsl.generated.mod
import kotlinx.coroutines.flow.first

val Context.userSettingsDataStore by preferencesDataStore(name = "user_settings_cache")

/** Preference keys for user settings. */
object UserSettingsPreferencesKey {
  fun enableNotificationsKey(userId: Id) = booleanPreferencesKey("enable_notifications_${userId}")

  fun appearanceMode(userId: Id) = intPreferencesKey("appearance_mode_${userId}")
}

/** Implementation of IUserSettingsCache using DataStore for persistent storage. */
class UserSettingsCache(private val context: Context) : IUserSettingsCache {
  override suspend fun initializeUserSettings(userId: Id) {
    setEnableNotification(userId, true)
    setAppearanceMode(userId, AppearanceMode.AUTOMATIC)
  }

  override suspend fun getEnableNotification(userId: Id): Boolean? {
    val preferences = context.userSettingsDataStore.data.first()
    return preferences[UserSettingsPreferencesKey.enableNotificationsKey(userId)]
  }

  override suspend fun setEnableNotification(userId: Id, enable: Boolean) {
    context.userSettingsDataStore.edit { preferences ->
      preferences[UserSettingsPreferencesKey.enableNotificationsKey(userId)] = enable
    }
  }

  override suspend fun getAppearanceMode(userId: Id): AppearanceMode? {
    val preferences = context.userSettingsDataStore.data.first()
    val modeInt = preferences[UserSettingsPreferencesKey.appearanceMode(userId)]
    return modeInt?.let { AppearanceMode.entries[modeInt] }
  }

  override suspend fun setAppearanceMode(userId: Id, mode: AppearanceMode) {
    context.userSettingsDataStore.edit { preferences ->
      preferences[UserSettingsPreferencesKey.appearanceMode(userId)] = mode.ordinal
    }
  }

  override suspend fun clear(userId: Id) {
    context.userSettingsDataStore.edit { preferences ->
      preferences.remove(UserSettingsPreferencesKey.enableNotificationsKey(userId))
      preferences.remove(UserSettingsPreferencesKey.appearanceMode(userId))
    }
  }
}
