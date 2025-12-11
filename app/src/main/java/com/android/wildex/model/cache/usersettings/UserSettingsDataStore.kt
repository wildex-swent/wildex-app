package com.android.wildex.model.cache.usersettings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.wildex.model.user.AppearanceMode
import kotlinx.coroutines.flow.first

val Context.userSettingsDataStore by preferencesDataStore(name = "user_settings_cache")

/** Preference keys for user settings. */
object UserSettingsPreferencesKey {
  val ENABLE_NOTIFICATIONS = booleanPreferencesKey("enable_notifications")
  val APPEARANCE_MODE = intPreferencesKey("appearance_mode")
}

/** Implementation of IUserSettingsCache using DataStore for persistent storage. */
class UserSettingsCache(private val context: Context) : IUserSettingsCache {
  override suspend fun getEnableNotification(): Boolean? {
    val preferences = context.userSettingsDataStore.data.first()
    return preferences[UserSettingsPreferencesKey.ENABLE_NOTIFICATIONS]
  }

  override suspend fun setEnableNotification(enable: Boolean) {
    context.userSettingsDataStore.edit { preferences ->
      preferences[UserSettingsPreferencesKey.ENABLE_NOTIFICATIONS] = enable
    }
  }

  override suspend fun getAppearanceMode(): AppearanceMode? {
    val preferences = context.userSettingsDataStore.data.first()
    return preferences[UserSettingsPreferencesKey.APPEARANCE_MODE]?.let {
      AppearanceMode.entries[it]
    }
  }

  override suspend fun setAppearanceMode(mode: AppearanceMode) {
    context.userSettingsDataStore.edit { preferences ->
      preferences[UserSettingsPreferencesKey.APPEARANCE_MODE] = mode.ordinal
    }
  }

  override suspend fun clear() {
    context.userSettingsDataStore.edit { it.clear() }
  }
}
