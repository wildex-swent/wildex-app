package com.android.wildex.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.AppTheme
import com.android.wildex.R
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.user.UserType
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.navigation.NavigationTestTags
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.mapbox.maps.extension.style.expressions.dsl.generated.color

object SettingsScreenTestTags {
  const val GO_BACK_BUTTON = "go_back_button"
  const val EDIT_PROFILE_SETTING = "edit_profile_setting"
  const val EDIT_PROFILE_BUTTON = "edit_profile_button"
  const val SIGN_OUT_BUTTON = "sign_out_button"
  const val DELETE_ACCOUNT_BUTTON = "delete_account_button"
  const val DELETE_ACCOUNT_DIALOG = "delete_account_dialog"
  const val DELETE_ACCOUNT_CONFIRM_BUTTON = "delete_account_confirm_button"
  const val DELETE_ACCOUNT_DISMISS_BUTTON = "delete_account_dismiss_button"
  const val NOTIFICATIONS_SETTING = "notifications_setting"
  const val NOTIFICATIONS_TOGGLE = "notifications_toggle"
  const val USER_TYPE_SETTING = "user_type_setting"
  const val REGULAR_USER_TYPE_BUTTON = "regular_user_type_button"
  const val PROFESSIONAL_USER_TYPE_BUTTON = "professional_user_type_button"
  const val APPEARANCE_MODE_SETTING = "appearance_mode_setting"
  const val AUTOMATIC_MODE_BUTTON = "automatic_mode_button"
  const val LIGHT_MODE_BUTTON = "light_mode_button"
  const val DARK_MODE_BUTTON = "dark_mode_button"
  const val SCREEN_TITLE = "settings_screen_title"
  const val NOTIFICATIONS_SETTING_DIALOG = "notification_setting_dialog"
  const val NOTIFICATIONS_SETTING_DIALOG_CONFIRM = "notification_setting_dialog_confirm"
  const val NOTIFICATIONS_SETTING_DIALOG_CANCEL = "notification_setting_dialog_cancel"
  const val USER_TYPE_DIALOG = "user_type_dialog"
  const val USER_TYPE_DIALOG_CONFIRM = "user_type_dialog_confirm"
  const val USER_TYPE_DIALOG_CANCEL = "user_type_dialog_cancel"
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    settingsScreenViewModel: SettingsScreenViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onAccountDeleteOrSignOut: () -> Unit = {},
) {
  val uiState by settingsScreenViewModel.uiState.collectAsState()
  val context = LocalContext.current
  var showDeletionValidation by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showUserTypeValidation by remember { mutableStateOf(false) }
  val connectivityObserver = LocalConnectivityObserver.current
  val isOnline by connectivityObserver.isOnline.collectAsState()

  val validSdk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
  val notifPermissionState =
      if (validSdk)
          rememberPermissionState(
              permission = Manifest.permission.POST_NOTIFICATIONS,
              onPermissionResult = { settingsScreenViewModel.setNotificationsEnabled(it) },
          )
      else null

  LaunchedEffect(Unit) {
    settingsScreenViewModel.loadUIState(notifPermissionState?.status?.isGranted ?: true)
  }
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      settingsScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(NavigationTestTags.SETTINGS_SCREEN),
      topBar = { SettingsScreenTopBar(onGoBack) },
      floatingActionButton = {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
        ) {
          FloatingActionButton(
              onClick = {
                settingsScreenViewModel.signOut(isOnline) { onAccountDeleteOrSignOut() }
              },
              shape = RoundedCornerShape(16.dp),
              containerColor = colorScheme.background,
              elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
              modifier =
                  Modifier.fillMaxWidth()
                      .border(2.dp, colorScheme.primary, RoundedCornerShape(16.dp))
                      .testTag(SettingsScreenTestTags.SIGN_OUT_BUTTON),
          ) {
            Text(
                text = context.getString(R.string.sign_out),
                style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colorScheme.onBackground,
            )
          }
          FloatingActionButton(
              onClick = {
                if (isOnline) showDeletionValidation = true
                else settingsScreenViewModel.onOfflineClick()
              },
              shape = RoundedCornerShape(16.dp),
              containerColor = colorScheme.primary,
              elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
              modifier =
                  Modifier.fillMaxWidth()
                      .border(2.dp, colorScheme.primary, RoundedCornerShape(16.dp))
                      .testTag(SettingsScreenTestTags.DELETE_ACCOUNT_BUTTON),
          ) {
            Text(
                text = context.getString(R.string.delete_account),
                style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colorScheme.onPrimary,
            )
          }
        }
      },
      floatingActionButtonPosition = FabPosition.Center,
  ) { paddingValues ->
    when {
      uiState.isError -> LoadingFail()
      uiState.isLoading -> LoadingScreen()
      else -> {
        SettingsContent(
            onEditProfileClick = onEditProfileClick,
            paddingValues = paddingValues,
            uiState = uiState,
            isOnline = isOnline,
            viewModel = settingsScreenViewModel,
            notifPermissionState = notifPermissionState,
            setUserTypeValidation = { showUserTypeValidation = true },
            showDialog = { showSettingsDialog = true },
        )
        if (showDeletionValidation) {
          AlertDialog(
              onDismissRequest = { showDeletionValidation = false },
              title = {
                Text(
                    text = context.getString(R.string.delete_account),
                    style = typography.titleLarge,
                )
              },
              text = {
                Text(
                    text = context.getString(R.string.delete_account_confirmation),
                    style = typography.bodyMedium,
                )
              },
              modifier = Modifier.testTag(SettingsScreenTestTags.DELETE_ACCOUNT_DIALOG),
              confirmButton = {
                TextButton(
                    modifier =
                        Modifier.testTag(SettingsScreenTestTags.DELETE_ACCOUNT_CONFIRM_BUTTON),
                    onClick = {
                      showDeletionValidation = false
                      settingsScreenViewModel.deleteAccount { onAccountDeleteOrSignOut() }
                    },
                ) {
                  Text(
                      text = context.getString(R.string.delete),
                      color = Color.Red,
                      style = typography.bodyMedium,
                  )
                }
              },
              dismissButton = {
                TextButton(
                    modifier =
                        Modifier.testTag(SettingsScreenTestTags.DELETE_ACCOUNT_DISMISS_BUTTON),
                    onClick = { showDeletionValidation = false },
                ) {
                  Text(text = context.getString(R.string.cancel), style = typography.bodyMedium)
                }
              },
          )
        } else if (showSettingsDialog) SettingsPermissionDialog { showSettingsDialog = false }
        else if (showUserTypeValidation) {
          UserTypeValidationDialog(
              dismissDialog = { showUserTypeValidation = false },
              onUserTypeChanged = { settingsScreenViewModel.setUserType(it) })
        }
      }
    }
  }
}

/**
 * Dialog prompting the user to go to the app settings to enable notifications.
 *
 * @param dismissDialog callback function to be called when the user dismisses the dialog
 */
@Composable
fun SettingsPermissionDialog(dismissDialog: () -> Unit) {
  val context = LocalContext.current
  AlertDialog(
      onDismissRequest = dismissDialog,
      title = { Text(stringResource(R.string.permission_required)) },
      text = { Text(stringResource(R.string.enable_in_settings)) },
      confirmButton = {
        TextButton(
            modifier =
                Modifier.testTag(SettingsScreenTestTags.NOTIFICATIONS_SETTING_DIALOG_CONFIRM),
            onClick = {
              dismissDialog()
              context.startActivity(
                  Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                  })
            },
        ) {
          Text(stringResource(R.string.open_settings))
        }
      },
      dismissButton = {
        TextButton(
            modifier = Modifier.testTag(SettingsScreenTestTags.NOTIFICATIONS_SETTING_DIALOG_CANCEL),
            onClick = dismissDialog,
        ) {
          Text(stringResource(R.string.cancel))
        }
      },
      containerColor = colorScheme.background,
      tonalElevation = 2.dp,
      modifier = Modifier.testTag(SettingsScreenTestTags.NOTIFICATIONS_SETTING_DIALOG),
  )
}

/**
 * Container Composable for the actual settings available to the user (appearance, notifications,
 * user type, edit profile)
 *
 * @param onEditProfileClick callback function to be called when clicking on the Edit Profile
 *   setting
 * @param paddingValues padding values of the scaffold passed down to the column
 * @param uiState settings UI state, containing info on the current notification enablement status,
 *   the current user type and the current appearance mode
 * @param isOnline boolean value indicating whether the user is online or not
 * @param viewModel settings screen view model
 * @param notifPermissionState permission state of the notification permission
 * @param showDialog callback function to be called when the user wants to enable notifications
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsContent(
    paddingValues: PaddingValues,
    uiState: SettingsUIState,
    onEditProfileClick: () -> Unit,
    isOnline: Boolean,
    viewModel: SettingsScreenViewModel,
    notifPermissionState: PermissionState?,
    setUserTypeValidation: () -> Unit,
    showDialog: () -> Unit,
) {
  val groupButtonsColors =
      SegmentedButtonColors(
          activeContainerColor = colorScheme.primary,
          activeContentColor = colorScheme.onPrimary,
          activeBorderColor = colorScheme.primary,
          inactiveContainerColor = colorScheme.background,
          inactiveContentColor = colorScheme.onBackground,
          inactiveBorderColor = colorScheme.primary,
          disabledActiveContainerColor = Color(1),
          disabledActiveContentColor = Color(1),
          disabledActiveBorderColor = Color(1),
          disabledInactiveContainerColor = Color(1),
          disabledInactiveContentColor = Color(1),
          disabledInactiveBorderColor = Color(1),
      )
  val notifPermissionStatus = notifPermissionState?.status

  LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
    item {
      EditProfileOption(
          onEditProfileClick = onEditProfileClick,
          isOnline = isOnline,
          onOfflineClick = { viewModel.onOfflineClick() },
      )
      SettingsDivider()
    }
    item {
      NotificationOption(
          currentNotificationState = uiState.notificationsEnabled,
          onNotificationStateChanged = {
            if (notifPermissionStatus == null || notifPermissionStatus.isGranted)
                viewModel.setNotificationsEnabled(it)
            else if (!notifPermissionStatus.shouldShowRationale)
                notifPermissionState.launchPermissionRequest()
            else showDialog()
          },
          isOnline = isOnline,
          onOfflineClick = { viewModel.onOfflineClick() },
      )
      SettingsDivider()
    }
    item {
      UserTypeOption(
          currentUserType = uiState.userType,
          groupButtonsColors = groupButtonsColors,
          onUserTypeChanged = { viewModel.setUserType(it) },
          isOnline = isOnline,
          setUserTypeValidation = setUserTypeValidation,
          onOfflineClick = { viewModel.onOfflineClick() },
      )
      SettingsDivider()
    }
    item {
      AppearanceModeOption(
          currentAppearanceMode = uiState.appearanceMode,
          onAppearanceModeChanged = {
            viewModel.setAppearanceMode(it)
            AppTheme.appearanceMode = it
          },
          groupButtonsColors = groupButtonsColors,
          isOnline = isOnline,
          onOfflineClick = { viewModel.onOfflineClick() },
      )
      SettingsDivider()
    }
  }
}

/**
 * Settings screen top bar Composable
 *
 * @param onGoBack callback function to be called when the user clicks on the Arrow Back button,
 *   meaning he wants to navigate to the previous screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenTopBar(onGoBack: () -> Unit) {
  CenterAlignedTopAppBar(
      title = {
        Text(
            modifier = Modifier.testTag(SettingsScreenTestTags.SCREEN_TITLE),
            text = stringResource(R.string.settings),
            style = typography.titleLarge,
        )
      },
      navigationIcon = {
        IconButton(
            onClick = onGoBack,
            modifier = Modifier.testTag(SettingsScreenTestTags.GO_BACK_BUTTON),
        ) {
          Icon(
              imageVector = Icons.Default.ChevronLeft,
              contentDescription = "Go Back",
              tint = colorScheme.onBackground,
          )
        }
      },
  )
}

/**
 * Divider component to be placed between settings for better visibility and contrast between
 * settings
 */
@Composable
fun SettingsDivider() {
  HorizontalDivider(
      thickness = 1.dp,
      color = colorScheme.onBackground,
      modifier = Modifier.fillMaxWidth(),
  )
}

/**
 * Default setting component, defining a standard setting format, placing 3 components in a Row in
 * the following order:
 * - an icon representing the setting's subject
 * - a title, i.e. the setting's subject
 * - an interactable element allowing to change the setting's value
 *
 * @param icon icon to display at the start of the Row, representing the setting's subject
 * @param settingName title of the setting
 * @param interactableElement interactable component, placed at the end of the row, that allows
 *   interacting with the setting's value
 */
@Composable
fun SettingTemplate(
    testTag: String,
    icon: ImageVector,
    settingName: String,
    interactableElement: @Composable () -> Unit,
) {
  FlowRow(
      modifier =
          Modifier.fillMaxWidth()
              .testTag(testTag)
              .wrapContentHeight()
              .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      itemVerticalAlignment = Alignment.CenterVertically,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
          imageVector = icon,
          contentDescription = settingName,
          tint = colorScheme.onBackground,
      )
      Text(
          text = settingName,
          color = colorScheme.onBackground,
          style = typography.titleSmall,
          maxLines = 1,
          modifier = Modifier.padding(horizontal = 12.dp),
      )
    }
    interactableElement()
  }
}

/**
 * Edit Profile option that redirects you to the EditProfileScreen when you click on the
 * interactable component
 *
 * @param onEditProfileClick callback function to be called when the user clicks on the interactable
 *   component of the setting
 * @param isOnline boolean value indicating whether the user is online or not
 * @param onOfflineClick callback function to be called when the user is offline
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileOption(
    onEditProfileClick: () -> Unit = {},
    isOnline: Boolean,
    onOfflineClick: () -> Unit,
) {
  SettingTemplate(
      testTag = SettingsScreenTestTags.EDIT_PROFILE_SETTING,
      icon = Icons.Outlined.Edit,
      settingName = LocalContext.current.getString(R.string.edit_profile),
  ) {
    IconButton(
        onClick = { if (isOnline) onEditProfileClick() else onOfflineClick() },
        modifier = Modifier.testTag(SettingsScreenTestTags.EDIT_PROFILE_BUTTON),
    ) {
      Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = "Edit Profile Navigate Icon",
          tint = colorScheme.onBackground,
      )
    }
  }
}

/**
 * Notification setting, allowing the user to turn notifications on and off
 *
 * @param currentNotificationState current setting value of the logged in user
 * @param onNotificationStateChanged callback function to be called when the user turns the
 *   notifications on or off
 * @param isOnline boolean value indicating whether the user is online or not
 * @param onOfflineClick callback function to be called when the user is offline
 */
@Composable
fun NotificationOption(
    currentNotificationState: Boolean,
    onNotificationStateChanged: (Boolean) -> Unit,
    isOnline: Boolean,
    onOfflineClick: () -> Unit,
) {
  SettingTemplate(
      testTag = SettingsScreenTestTags.NOTIFICATIONS_SETTING,
      icon = Icons.Outlined.Notifications,
      settingName = LocalContext.current.getString(R.string.notifications),
  ) {
    Switch(
        modifier = Modifier.testTag(SettingsScreenTestTags.NOTIFICATIONS_TOGGLE),
        checked = currentNotificationState,
        colors =
            SwitchColors(
                colorScheme.onPrimary,
                colorScheme.primary,
                colorScheme.primary,
                colorScheme.primary,
                colorScheme.onBackground,
                colorScheme.background,
                colorScheme.onBackground,
                colorScheme.primary,
                Color(1),
                Color(1),
                Color(1),
                Color(1),
                Color(1),
                Color(1),
                Color(1),
                Color(1),
            ),
        onCheckedChange = { if (isOnline) onNotificationStateChanged(it) else onOfflineClick() },
        thumbContent = {
          if (currentNotificationState) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
          }
        },
    )
  }
}

/**
 * User Type setting, allowing the user to change his type from regular to professional, or the
 * opposite
 *
 * @param currentUserType current user type of the logged in user
 * @param onUserTypeChanged callback function to be called when the user changes his type
 * @param groupButtonsColors colors to be applied to the interactable element, so that it follows
 *   the application's theme
 * @param isOnline boolean value indicating whether the user is online or not
 * @param onOfflineClick callback function to be called when the user is offline
 */
@Composable
fun UserTypeOption(
    currentUserType: UserType,
    onUserTypeChanged: (UserType) -> Unit,
    groupButtonsColors: SegmentedButtonColors,
    isOnline: Boolean,
    setUserTypeValidation: () -> Unit,
    onOfflineClick: () -> Unit,
) {
  val context = LocalContext.current
  val options =
      listOf(
          context.getString(R.string.regular_type),
          context.getString(R.string.professional_type),
      )
  val testTags =
      listOf(
          SettingsScreenTestTags.REGULAR_USER_TYPE_BUTTON,
          SettingsScreenTestTags.PROFESSIONAL_USER_TYPE_BUTTON,
      )
  val selectedIndex = UserType.entries.indexOf(currentUserType)

  SettingTemplate(
      testTag = SettingsScreenTestTags.USER_TYPE_SETTING,
      icon = Icons.Outlined.Person,
      settingName = context.getString(R.string.user_type),
  ) {
    SingleChoiceSegmentedButtonRow {
      options.forEachIndexed { index, option ->
        SegmentedButton(
            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            onClick = {
              if (isOnline) {
                val userType = getUserType(option, context)
                if (currentUserType == UserType.PROFESSIONAL && userType == UserType.REGULAR) {
                  setUserTypeValidation()
                } else {
                  onUserTypeChanged(userType)
                }
              } else onOfflineClick()
            },
            selected = selectedIndex == index,
            colors = groupButtonsColors,
            modifier = Modifier.testTag(testTags[index]),
        ) {
          Text(
              text = option,
              color =
                  if (index == selectedIndex) colorScheme.onPrimary else colorScheme.onBackground,
              style = typography.bodySmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
fun UserTypeValidationDialog(
    dismissDialog: () -> Unit,
    onUserTypeChanged: (UserType) -> Unit,
) {
  AlertDialog(
      onDismissRequest = dismissDialog,
      title = {
        Text(
            text = stringResource(R.string.user_type_change_validation_title),
            color = colorScheme.onBackground,
            style = typography.titleLarge,
        )
      },
      text = {
        Text(
            text = stringResource(R.string.user_type_change_validation_text),
            color = colorScheme.onBackground,
            style = typography.bodyMedium)
      },
      confirmButton = {
        TextButton(
            modifier = Modifier.testTag(SettingsScreenTestTags.USER_TYPE_DIALOG_CONFIRM),
            onClick = {
              dismissDialog()
              onUserTypeChanged(UserType.REGULAR)
            },
        ) {
          Text(
              text = stringResource(R.string.user_type_change_validation_confirm),
              color = colorScheme.primary,
              style = typography.bodyMedium)
        }
      },
      dismissButton = {
        TextButton(
            modifier = Modifier.testTag(SettingsScreenTestTags.USER_TYPE_DIALOG_CANCEL),
            onClick = dismissDialog,
        ) {
          Text(
              text = stringResource(R.string.cancel),
              color = colorScheme.onBackground,
              style = typography.bodyMedium,
          )
        }
      },
      containerColor = colorScheme.background,
      tonalElevation = 2.dp,
      modifier = Modifier.testTag(SettingsScreenTestTags.USER_TYPE_DIALOG),
  )
}

private fun getUserType(option: String, context: Context): UserType =
    when (option) {
      context.getString(R.string.regular_type) -> UserType.REGULAR
      context.getString(R.string.professional_type) -> UserType.PROFESSIONAL
      else -> throw IllegalArgumentException("The user type [$option] is not recognized")
    }

/**
 * Appearance mode setting, allowing the user to change the appearance of the app. The user can
 * choose between light and dark mode, or the system's automatic choice of appearance.
 *
 * @param currentAppearanceMode current appearance mode defined in the user's UserSettings
 * @param onAppearanceModeChanged callback function to be called when the user wants to change the
 *   app's appearance mode
 * @param groupButtonsColors colors to be applied to the interactable component, so that it follows
 *   the application's theme
 * @param isOnline boolean value indicating whether the user is online or not
 * @param onOfflineClick callback function to be called when the user is offline
 */
@Composable
fun AppearanceModeOption(
    currentAppearanceMode: AppearanceMode,
    onAppearanceModeChanged: (AppearanceMode) -> Unit,
    groupButtonsColors: SegmentedButtonColors,
    isOnline: Boolean,
    onOfflineClick: () -> Unit,
) {
  val context = LocalContext.current
  SettingTemplate(
      testTag = SettingsScreenTestTags.APPEARANCE_MODE_SETTING,
      icon = Icons.Outlined.LightMode,
      settingName = context.getString(R.string.appearance),
  ) {
    val options =
        listOf(
            context.getString(R.string.system_default),
            context.getString(R.string.light_mode),
            context.getString(R.string.dark_mode),
        )
    val testTags =
        listOf(
            SettingsScreenTestTags.AUTOMATIC_MODE_BUTTON,
            SettingsScreenTestTags.LIGHT_MODE_BUTTON,
            SettingsScreenTestTags.DARK_MODE_BUTTON,
        )
    val unCheckedIcons =
        listOf(Icons.Outlined.Autorenew, Icons.Outlined.LightMode, Icons.Outlined.DarkMode)
    val checkedIcons = listOf(Icons.Filled.Autorenew, Icons.Filled.LightMode, Icons.Filled.DarkMode)
    val selectedIndex = AppearanceMode.entries.indexOf(currentAppearanceMode)

    SingleChoiceSegmentedButtonRow {
      options.forEachIndexed { index, option ->
        val vectorTintPair =
            if (index == selectedIndex) Pair(checkedIcons[index], colorScheme.onPrimary)
            else Pair(unCheckedIcons[index], colorScheme.onBackground)
        SegmentedButton(
            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            onClick = {
              if (isOnline) {
                val appearanceMode = getAppearanceMode(option, context)
                onAppearanceModeChanged(appearanceMode)
              } else onOfflineClick()
            },
            selected = index == selectedIndex,
            modifier = Modifier.testTag(testTags[index]),
            icon = {
              Icon(
                  imageVector = vectorTintPair.first,
                  contentDescription = option,
                  tint = vectorTintPair.second,
                  modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
              )
            },
            colors = groupButtonsColors,
        ) {
          Spacer(modifier = Modifier.width(2.dp))
          Text(
              text = option,
              color = vectorTintPair.second,
              style = typography.bodySmall,
          )
        }
      }
    }
  }
}

private fun getAppearanceMode(option: String, context: Context): AppearanceMode =
    when (option) {
      context.getString(R.string.system_default) -> AppearanceMode.AUTOMATIC
      context.getString(R.string.light_mode) -> AppearanceMode.LIGHT
      context.getString(R.string.dark_mode) -> AppearanceMode.DARK
      else -> throw IllegalArgumentException("The appearance mode [$option] is not recognized")
    }
