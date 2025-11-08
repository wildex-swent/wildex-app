package com.android.wildex.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.user.UserType

object SettingsScreenTestTags {
    const val GO_BACK_BUTTON = "go_back_button"
    const val EDIT_PROFILE_BUTTON = "edit_profile_button"
    const val NOTIFICATIONS_TOGGLE = "notifications_toggle"
    const val SCREEN_TITLE = "settings_screen_title"
}

@Composable
fun SettingsScreen(
  settingsScreenViewModel : SettingsScreenViewModel = viewModel(),
  onGoBack: () -> Unit = {},
  onEditProfileClick: () -> Unit = {}
) {
  val uiState by settingsScreenViewModel.uiState.collectAsState()
  val context = LocalContext.current
  val screenHeight = LocalConfiguration.current.screenHeightDp.dp

  LaunchedEffect(Unit) { settingsScreenViewModel.loadUIState() }
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      settingsScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold (
    modifier = Modifier.fillMaxSize(),
    topBar = { SettingsScreenTopBar(onGoBack) }
  ){ paddingValues ->
    LazyColumn (
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ){
      val settingHeight = screenHeight / 12
      item{
        EditProfileOption(settingHeight, onEditProfileClick)
        SettingsDivider()
      }
      item {
        NotificationOption(
          settingHeight,
          uiState.notificationsEnabled
        ) { newState ->
          settingsScreenViewModel.setNotificationsEnabled(newState)
        }
        SettingsDivider()
      }
      item {
        UserStatusOption(
          settingHeight,
          uiState.userType
        ) { newUserStatusString ->
          val newUserType = when(newUserStatusString) {
            "Regular" -> UserType.REGULAR
            "Professional" -> UserType.PROFESSIONAL
            else -> throw IllegalArgumentException("The new user Type [$newUserStatusString] is not recognized")
          }
          settingsScreenViewModel.setUserType(newUserType)
        }
        SettingsDivider()
      }
      item{
        AppearanceModeOption(
          settingHeight,
          uiState.appearanceMode
        ) { newAppearanceModeString ->
          val newAppearanceMode = when(newAppearanceModeString) {
            "Automatic" -> AppearanceMode.AUTOMATIC
            "Light" -> AppearanceMode.LIGHT
            "Dark" -> AppearanceMode.DARK
            else -> throw IllegalArgumentException("The new appearance mode [$newAppearanceModeString] is not recognized")
          }
          settingsScreenViewModel.setAppearanceMode(newAppearanceMode)
        }
        SettingsDivider()
      }

    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenTopBar(
  onGoBack: () -> Unit
) {
  TopAppBar(
    title = {
      Text(
        modifier = Modifier.testTag(SettingsScreenTestTags.SCREEN_TITLE),
        text = "Settings"
      )
    },
    navigationIcon = {
      IconButton(
        onClick = { onGoBack() },
        modifier = Modifier.testTag(SettingsScreenTestTags.GO_BACK_BUTTON)
      ) {
        Icon(
          imageVector = Icons.Default.ArrowBack,
          contentDescription = "Go Back",
          tint = colorScheme.onBackground
        )
      }
    }
  )
}

@Composable
fun SettingsDivider(){
  HorizontalDivider(
    thickness = 5.dp,
    color = colorScheme.onBackground,
    modifier = Modifier.fillMaxWidth()
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileOption(
  settingHeight: Dp,
  onEditProfileClick: () -> Unit = {}
){
  Row (
    modifier = Modifier.fillMaxWidth().height(settingHeight),
    verticalAlignment = Alignment.CenterVertically
  ){
    Icon(
      imageVector = Icons.Outlined.Edit,
      contentDescription = "Edit Profile Setting",
      tint = colorScheme.onBackground,
    )
    Text(
      text = "Edit Profile",
      color = colorScheme.onBackground,
      modifier = Modifier
        .weight(1f)
        .padding(start = 16.dp)
    )
    IconButton(
      onClick = { onEditProfileClick() },
      modifier = Modifier.testTag(SettingsScreenTestTags.EDIT_PROFILE_BUTTON)
    ) {
      Icon(
        imageVector = Icons.Filled.ArrowForward,
        contentDescription = "Edit Profile Navigate Icon",
        tint = colorScheme.onBackground
      )
    }
  }
}

@Composable
fun NotificationOption(
  settingHeight: Dp,
  currentNotificationState: Boolean,
  onNotificationStateChanged: (Boolean) -> Unit
){
  var checked by remember { mutableStateOf(currentNotificationState) }
  Row (
    modifier = Modifier.fillMaxWidth().height(settingHeight),
    verticalAlignment = Alignment.CenterVertically
  ){
    Icon(
      imageVector = Icons.Outlined.Notifications,
      contentDescription = "Notification Setting",
      tint = colorScheme.onBackground,
    )
    Text(
      text = "Notifications",
      color = colorScheme.onBackground,
      modifier = Modifier
        .weight(1f)
        .padding(start = 16.dp)
    )
    Switch(
      modifier = Modifier.testTag(SettingsScreenTestTags.NOTIFICATIONS_TOGGLE),
      checked = checked,
      onCheckedChange = {
        onNotificationStateChanged(it)
        checked = it
      },
      thumbContent = {
        if (checked) {
          Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(SwitchDefaults.IconSize),
          )
        }
      }
    )
  }
}

@Composable
fun UserStatusOption(
  settingHeight: Dp,
  currentUserStatus: UserType,
  onUserStatusChanged: (String) -> Unit
){
  val options = UserType.entries.map { it.toString() }
  var selectedIndex by remember { mutableIntStateOf(UserType.entries.indexOf(currentUserStatus)) }

  Row (
    modifier = Modifier.fillMaxWidth().height(settingHeight),
    verticalAlignment = Alignment.CenterVertically
  ){
    Icon(
      imageVector = Icons.Outlined.Person,
      contentDescription = "User Status Setting",
      tint = colorScheme.onBackground,
    )
    Text(
      text = "User status",
      color = colorScheme.onBackground,
      modifier = Modifier
        .weight(1f)
        .padding(start = 16.dp)
    )
    SingleChoiceSegmentedButtonRow {
      options.forEachIndexed { index, option ->
        SegmentedButton(
          shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
          onClick = {
            onUserStatusChanged(option)
            selectedIndex = index
          },
          selected = selectedIndex == index
        ){
          Text(
            text = option,
            color = if (index == selectedIndex) colorScheme.onPrimary else colorScheme.onBackground
          )
        }
      }
    }
  }
}

@Composable
fun AppearanceModeOption(
  settingHeight: Dp,
  currentAppearanceMode: AppearanceMode,
  onAppearanceModeChanged : (String) -> Unit
){
  Row (
    modifier = Modifier.fillMaxWidth().height(settingHeight),
    verticalAlignment = Alignment.CenterVertically
  ){
    Icon(
      imageVector = Icons.Outlined.Person,
      contentDescription = "User Status Setting",
      tint = colorScheme.onBackground,
    )
    Text(
      text = "User status",
      color = colorScheme.onBackground,
      modifier = Modifier
        .weight(1f)
        .padding(start = 16.dp)
    )
    AppearanceSelectionButtonGroup(currentAppearanceMode, onAppearanceModeChanged)
  }
}

@Composable
fun AppearanceSelectionButtonGroup(
  currentAppearanceMode: AppearanceMode,
  onAppearanceChanged: (String) -> Unit
){
  val options = AppearanceMode.entries.map { it.toString() }
  val unCheckedIcons = listOf(Icons.Outlined.Autorenew, Icons.Outlined.LightMode, Icons.Outlined.DarkMode)
  val checkedIcons = listOf(Icons.Filled.Autorenew, Icons.Filled.LightMode, Icons.Filled.DarkMode)
  var selectedIndex by remember { mutableIntStateOf(AppearanceMode.entries.indexOf(currentAppearanceMode)) }

  SingleChoiceSegmentedButtonRow {
    options.forEachIndexed { index, option ->
      SegmentedButton(
        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
        onClick = {
          onAppearanceChanged(option)
          selectedIndex = index
        },
        selected = index == selectedIndex
      ){
        Row (
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ){
          Icon(
            imageVector = if (index == selectedIndex) checkedIcons[index] else unCheckedIcons[index],
            contentDescription = option,
            tint = if (index == selectedIndex) colorScheme.onPrimary else colorScheme.onBackground
          )
          Text(
            text = option,
            color = if (index == selectedIndex) colorScheme.onPrimary else colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp)
          )
        }
      }
    }
  }
}
