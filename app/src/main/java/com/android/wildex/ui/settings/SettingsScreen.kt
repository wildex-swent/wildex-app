package com.android.wildex.ui.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

object SettingsScreenTestTags {
  const val GO_BACK_BUTTON = "go_back_button"
  const val EDIT_PROFILE_SETTING = "edit_profile_setting"
  const val EDIT_PROFILE_BUTTON = "edit_profile_button"
  const val DELETE_ACCOUNT_BUTTON = "delete_account_button"
  const val DELETE_ACCOUNT_DIALOG = "delete_account_dialog"
  const val DELETE_ACCOUNT_CONFIRM_BUTTON = "delete_account_confirm_button"
  const val DELETE_ACCOUNT_DISMISS_BUTTON = "delete_account_dismiss_button"
  const val NOTIFICATIONS_SETTING = "notifications_setting"
  const val NOTIFICATIONS_TOGGLE = "notifications_toggle"
  const val USER_STATUS_SETTING = "user_status_setting"
  const val REGULAR_USER_STATUS_BUTTON = "regular_user_status_button"
  const val PROFESSIONAL_USER_STATUS_BUTTON = "professional_user_status_button"
  const val APPEARANCE_MODE_SETTING = "appearance_mode_setting"
  const val AUTOMATIC_MODE_BUTTON = "automatic_mode_button"
  const val LIGHT_MODE_BUTTON = "light_mode_button"
  const val DARK_MODE_BUTTON = "dark_mode_button"
  const val SCREEN_TITLE = "settings_screen_title"
}

/**
 * Main Composable that defines the entire settings screen and loads the UI state
 *
 * @param settingsScreenViewModel ViewModel in charge of updating the UI state and linking the
 *   screen to the repositories
 * @param onGoBack callback function called when the user wants to go back to the profile page
 * @param onEditProfileClick callback function for when the user wants to edit his profile, so that
 *   he is taken to the edit profile screen
 * @param onAccountDeleteOrSignOut callback function called when the user wants to delete his
 *   account or sign out, so that he is taken back to the authentication screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsScreenViewModel: SettingsScreenViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onAccountDeleteOrSignOut: () -> Unit = {}
) {}
