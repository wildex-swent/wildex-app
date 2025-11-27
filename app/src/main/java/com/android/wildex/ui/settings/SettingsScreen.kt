package com.android.wildex.ui.settings

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.AppTheme
import com.android.wildex.R
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.user.UserType
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.navigation.NavigationTestTags

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
    onAccountDeleteOrSignOut: () -> Unit = {},
) {
  val uiState by settingsScreenViewModel.uiState.collectAsState()
  val context = LocalContext.current
  val screenHeight = LocalWindowInfo.current.containerSize.height.dp
  val screenWidth = LocalWindowInfo.current.containerSize.width.dp
  var showDeletionValidation by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) { settingsScreenViewModel.loadUIState() }
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
        Column {
          FloatingActionButton(
              onClick = { settingsScreenViewModel.signOut { onAccountDeleteOrSignOut() } },
              shape = RoundedCornerShape(16.dp),
              containerColor = colorScheme.onTertiary,
              elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
              modifier =
                  Modifier.padding(bottom = 16.dp)
                      .padding(horizontal = screenWidth / 25)
                      .fillMaxWidth()
                      .border(2.dp, colorScheme.tertiary, RoundedCornerShape(16.dp))
                      .height(55.dp)
                      .testTag(SettingsScreenTestTags.SIGN_OUT_BUTTON),
          ) {
            Text(
                text = context.getString(R.string.sign_out),
                style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colorScheme.tertiary,
                modifier = Modifier.padding(horizontal = 30.dp),
            )
          }
          FloatingActionButton(
              onClick = { showDeletionValidation = true },
              shape = RoundedCornerShape(16.dp),
              containerColor = colorScheme.tertiary,
              elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
              modifier =
                  Modifier.padding(bottom = 16.dp)
                      .padding(horizontal = screenWidth / 25)
                      .fillMaxWidth()
                      .border(2.dp, colorScheme.tertiary, RoundedCornerShape(16.dp))
                      .height(55.dp)
                      .testTag(SettingsScreenTestTags.DELETE_ACCOUNT_BUTTON),
          ) {
            Text(
                text = context.getString(R.string.delete_account),
                style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colorScheme.onTertiary,
                modifier = Modifier.padding(horizontal = 30.dp),
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
            screenHeight,
            screenWidth,
            onEditProfileClick,
            paddingValues,
            uiState,
            settingsScreenViewModel,
        )
        if (showDeletionValidation) {
          AlertDialog(
              onDismissRequest = { showDeletionValidation = false },
              title = {
                Text(
                    text = context.getString(R.string.delete_account),
                    style = typography.titleLarge)
              },
              text = {
                Text(
                    text = context.getString(R.string.delete_account_confirmation),
                    style = typography.bodyMedium)
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
                      style = typography.bodyMedium)
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
        }
      }
    }
  }
}

/**
 * Container Composable for the actual settings available to the user (appearance, notifications,
 * user status, edit profile)
 *
 * @param screenHeight height in Dp of the device's screen, allows dynamic positioning
 * @param screenWidth width in Dp of the device's screen
 * @param onEditProfileClick callback function to be called when clicking on the Edit Profile
 *   setting
 * @param paddingValues padding values of the scaffold passed down to the column
 * @param uiState settings UI state, containing info on the current notification enablement status,
 *   the current user status and the current appearance mode
 * @param settingsScreenViewModel ViewModel in charge of updating the UI state and linking the
 *   screen to the repositories
 */
@Composable
fun SettingsContent(
    screenHeight: Dp,
    screenWidth: Dp,
    onEditProfileClick: () -> Unit,
    paddingValues: PaddingValues,
    uiState: SettingsUIState,
    settingsScreenViewModel: SettingsScreenViewModel,
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

  LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
    val settingHeight = screenHeight / 12
    val paddingHorizontal = screenWidth / 25
    item {
      EditProfileOption(paddingHorizontal, settingHeight, onEditProfileClick)
      SettingsDivider()
    }
    item {
      NotificationOption(paddingHorizontal, settingHeight, uiState.notificationsEnabled) { newState
        ->
        settingsScreenViewModel.setNotificationsEnabled(newState)
      }
      SettingsDivider()
    }
    item {
      UserStatusOption(
          paddingHorizontal = paddingHorizontal,
          settingHeight = settingHeight,
          screenWidth = screenWidth,
          currentUserStatus = uiState.userType,
          groupButtonsColors = groupButtonsColors,
          onUserStatusChanged = { newUserStatusString ->
            val newUserType =
                when (newUserStatusString) {
                  "Regular" -> UserType.REGULAR
                  "Professional" -> UserType.PROFESSIONAL
                  else ->
                      throw IllegalArgumentException(
                          "The new user Type [$newUserStatusString] is not recognized")
                }
            settingsScreenViewModel.setUserType(newUserType)
          },
      )
      SettingsDivider()
    }
    item {
      AppearanceModeOption(
          paddingHorizontal = paddingHorizontal,
          screenWidth = screenWidth,
          settingHeight = settingHeight,
          currentAppearanceMode = uiState.appearanceMode,
          onAppearanceModeChanged = { newAppearanceModeString ->
            val newAppearanceMode =
                when (newAppearanceModeString) {
                  "Auto" -> AppearanceMode.AUTOMATIC
                  "Light" -> AppearanceMode.LIGHT
                  "Dark" -> AppearanceMode.DARK
                  else ->
                      throw IllegalArgumentException(
                          "The new appearance mode [$newAppearanceModeString] is not recognized")
                }
            settingsScreenViewModel.setAppearanceMode(newAppearanceMode)
            AppTheme.appearanceMode = newAppearanceMode
          },
          groupButtonsColors = groupButtonsColors,
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
  TopAppBar(
      title = {
        Text(
            modifier = Modifier.testTag(SettingsScreenTestTags.SCREEN_TITLE),
            text = LocalContext.current.getString(R.string.settings),
            style = typography.titleLarge,
        )
      },
      navigationIcon = {
        IconButton(
            onClick = { onGoBack() },
            modifier = Modifier.testTag(SettingsScreenTestTags.GO_BACK_BUTTON),
        ) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
 * @param settingHeight height of a setting component, depending on the current device's screen
 *   height
 * @param paddingHorizontal padding to be applied horizontally around the Row, depending on the
 *   current device's screen width
 * @param icon icon to display at the start of the Row, representing the setting's subject
 * @param settingName title of the setting
 * @param interactableElement interactable component, placed at the end of the row, that allows
 *   interacting with the setting's value
 */
@Composable
fun SettingTemplate(
    settingHeight: Dp,
    paddingHorizontal: Dp,
    testTag: String = "",
    icon: ImageVector,
    settingName: String,
    interactableElement: @Composable (() -> Unit),
) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .height(settingHeight)
              .padding(horizontal = paddingHorizontal)
              .testTag(testTag),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start,
  ) {
    Icon(
        imageVector = icon,
        contentDescription = settingName,
        tint = colorScheme.onBackground,
        modifier = Modifier.padding(end = paddingHorizontal),
    )
    Text(
        text = settingName,
        color = colorScheme.onBackground,
        style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.weight(1f),
    )
    interactableElement()
  }
}

/**
 * Edit Profile option that redirects you to the EditProfileScreen when you click on the
 * interactable component
 *
 * @param paddingHorizontal padding to be applied horizontally around the setting's content
 * @param settingHeight height of the edit profile component
 * @param onEditProfileClick callback function to be called when the user clicks on the interactable
 *   component of the setting
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileOption(
    paddingHorizontal: Dp,
    settingHeight: Dp,
    onEditProfileClick: () -> Unit = {},
) {
  SettingTemplate(
      settingHeight = settingHeight,
      paddingHorizontal = paddingHorizontal,
      testTag = SettingsScreenTestTags.EDIT_PROFILE_SETTING,
      icon = Icons.Outlined.Edit,
      settingName = LocalContext.current.getString(R.string.edit_profile),
  ) {
    IconButton(
        onClick = { onEditProfileClick() },
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
 * @param paddingHorizontal padding to be applied horizontally around the setting's content
 * @param settingHeight height of the notification component
 * @param currentNotificationState current setting value of the logged in user
 * @param onNotificationStateChanged callback function to be called when the user turns the
 *   notifications on or off
 */
@Composable
fun NotificationOption(
    paddingHorizontal: Dp,
    settingHeight: Dp,
    currentNotificationState: Boolean,
    onNotificationStateChanged: (Boolean) -> Unit,
) {
  SettingTemplate(
      settingHeight = settingHeight,
      paddingHorizontal = paddingHorizontal,
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
                colorScheme.tertiary,
                colorScheme.onTertiary,
                colorScheme.tertiary,
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
        onCheckedChange = { onNotificationStateChanged(it) },
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
 * User Status setting, allowing the user to change his status from regular to professional, or the
 * opposite
 *
 * @param paddingHorizontal padding to be applied horizontally around the setting's content
 * @param settingHeight height of the user status component
 * @param screenWidth width of the current device's screen
 * @param currentUserStatus current user status of the logged in user
 * @param onUserStatusChanged callback function to be called when the user changes his status
 * @param groupButtonsColors colors to be applied to the interactable element, so that it follows
 *   the application's theme
 */
@Composable
fun UserStatusOption(
    paddingHorizontal: Dp,
    settingHeight: Dp,
    screenWidth: Dp,
    currentUserStatus: UserType,
    onUserStatusChanged: (String) -> Unit,
    groupButtonsColors: SegmentedButtonColors,
) {
  val options =
      listOf(
          LocalContext.current.getString(R.string.regular_status),
          LocalContext.current.getString(R.string.professional_status),
      )
  val testTags =
      listOf(
          SettingsScreenTestTags.REGULAR_USER_STATUS_BUTTON,
          SettingsScreenTestTags.PROFESSIONAL_USER_STATUS_BUTTON,
      )
  val selectedIndex = UserType.entries.indexOf(currentUserStatus)

  SettingTemplate(
      settingHeight = settingHeight,
      paddingHorizontal = paddingHorizontal,
      testTag = SettingsScreenTestTags.USER_STATUS_SETTING,
      icon = Icons.Outlined.Person,
      settingName = LocalContext.current.getString(R.string.user_status),
  ) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.width(screenWidth.div(1.9f))) {
      options.forEachIndexed { index, option ->
        SegmentedButton(
            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            onClick = { onUserStatusChanged(option) },
            selected = selectedIndex == index,
            colors = groupButtonsColors,
            modifier = Modifier.height(35.dp).testTag(testTags[index]),
        ) {
          Text(
              text = option,
              color =
                  if (index == selectedIndex) colorScheme.onPrimary else colorScheme.onBackground,
              style = typography.bodySmall.copy(fontSize = 9.sp),
          )
        }
      }
    }
  }
}

/**
 * Appearance mode setting, allowing the user to change the appearance of the app. The user can
 * choose between light and dark mode, or the system's automatic choice of appearance.
 *
 * @param paddingHorizontal padding to be applied horizontally around the setting's content
 * @param screenWidth width of the current device's screen
 * @param settingHeight height of the appearance mode component
 * @param currentAppearanceMode current appearance mode defined in the user's UserSettings
 * @param onAppearanceModeChanged callback function to be called when the user wants to change the
 *   app's appearance mode
 * @param groupButtonsColors colors to be applied to the interactable component, so that it follows
 *   the application's theme
 */
@Composable
fun AppearanceModeOption(
    paddingHorizontal: Dp,
    screenWidth: Dp,
    settingHeight: Dp,
    currentAppearanceMode: AppearanceMode,
    onAppearanceModeChanged: (String) -> Unit,
    groupButtonsColors: SegmentedButtonColors,
) {
  SettingTemplate(
      settingHeight = settingHeight,
      paddingHorizontal = paddingHorizontal,
      testTag = SettingsScreenTestTags.APPEARANCE_MODE_SETTING,
      icon = Icons.Outlined.LightMode,
      settingName = LocalContext.current.getString(R.string.appearance),
  ) {
    val options =
        listOf(
            LocalContext.current.getString(R.string.system_default),
            LocalContext.current.getString(R.string.light_mode),
            LocalContext.current.getString(R.string.dark_mode),
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

    SingleChoiceSegmentedButtonRow(modifier = Modifier.width(screenWidth.div(1.8f))) {
      options.forEachIndexed { index, option ->
        SegmentedButton(
            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            onClick = { onAppearanceModeChanged(option) },
            selected = index == selectedIndex,
            modifier = Modifier.height(35.dp).testTag(testTags[index]),
            icon = {},
            colors = groupButtonsColors,
        ) {
          Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
                imageVector =
                    if (index == selectedIndex) checkedIcons[index] else unCheckedIcons[index],
                contentDescription = option,
                tint =
                    if (index == selectedIndex) colorScheme.onPrimary else colorScheme.onBackground,
                modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
            )

            Spacer(modifier = Modifier.width(2.dp))
              AppearanceModeOptionText(
                  option,
                  index,
                  selectedIndex
              )
          }
        }
      }
    }
  }
}

@Composable
private fun AppearanceModeOptionText(
    option: String,
    index: Int,
    selectedIndex: Int
){
    Spacer(modifier = Modifier.width(2.dp))
    Text(
        text = option,
        color =
        if (index == selectedIndex) colorScheme.onPrimary else colorScheme.onBackground,
        style = typography.bodySmall.copy(fontSize = 9.sp),
    )
}
