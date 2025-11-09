package com.android.wildex.ui.settings

import android.graphics.drawable.Icon
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.Icons
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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.user.UserType
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import kotlin.math.ceil

object SettingsScreenTestTags {
    const val GO_BACK_BUTTON = "go_back_button"
    const val EDIT_PROFILE_BUTTON = "edit_profile_button"
    const val DELETE_ACCOUNT_BUTTON = "delete_account_button"
    const val NOTIFICATIONS_TOGGLE = "notifications_toggle"
    const val SCREEN_TITLE = "settings_screen_title"
}

@Composable
fun SettingsScreen(
  settingsScreenViewModel : SettingsScreenViewModel = viewModel(),
  onGoBack: () -> Unit = {},
  onEditProfileClick: () -> Unit = {},
  onAccountDelete: () -> Unit = {}
) {
  val uiState by settingsScreenViewModel.uiState.collectAsState()
  val context = LocalContext.current
  val screenHeight = LocalConfiguration.current.screenHeightDp.dp
  val screenWidth = LocalConfiguration.current.screenWidthDp.dp

  LaunchedEffect(Unit) { settingsScreenViewModel.loadUIState() }
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      settingsScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold (
    modifier = Modifier.fillMaxSize(),
    topBar = { SettingsScreenTopBar(onGoBack) },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { /* TODO */ },
        shape = RoundedCornerShape(50.dp),
        containerColor = colorScheme.tertiary,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
        modifier = Modifier
          .padding(bottom = 16.dp)
          .height(45.dp)
          .testTag(SettingsScreenTestTags.DELETE_ACCOUNT_BUTTON)
      ) {
        Text(
          text = "Delete account",
          fontSize = 16.sp,
          fontWeight = FontWeight.SemiBold,
          color = colorScheme.onTertiary,
          modifier = Modifier.padding(horizontal = 30.dp)
        )
      }
    },
    floatingActionButtonPosition = FabPosition.Center
  ){ paddingValues ->
    when {
      uiState.isError -> LoadingFail()
      uiState.isLoading -> LoadingScreen()
      else ->
        SettingsContent(
          screenHeight,
          screenWidth,
          onEditProfileClick,
          paddingValues,
          uiState,
          settingsScreenViewModel
        )
    }
  }
}

@Composable
fun SettingsContent(
  screenHeight: Dp,
  screenWidth: Dp,
  onEditProfileClick: () -> Unit,
  paddingValues: PaddingValues,
  uiState: SettingsUIState,
  settingsScreenViewModel: SettingsScreenViewModel
){
  val groupButtonsColors = SegmentedButtonColors(
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

  LazyColumn (
    modifier = Modifier
      .fillMaxSize()
      .padding(paddingValues)
  ){
    val settingHeight = screenHeight / 12
    val paddingHorizontal = screenWidth / 25
    item{
      EditProfileOption(paddingHorizontal, settingHeight, onEditProfileClick)
      SettingsDivider()
    }
    item {
      NotificationOption(
        paddingHorizontal,
        settingHeight,
        uiState.notificationsEnabled
      ) { newState ->
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
          val newUserType = when (newUserStatusString) {
            "Regular" -> UserType.REGULAR
            "Professional" -> UserType.PROFESSIONAL
            else -> throw IllegalArgumentException("The new user Type [$newUserStatusString] is not recognized")
          }
          settingsScreenViewModel.setUserType(newUserType)
        }
      )
      SettingsDivider()
    }
    item{
      AppearanceModeOption(
        paddingHorizontal = paddingHorizontal,
        screenWidth = screenWidth,
        settingHeight = settingHeight,
        currentAppearanceMode = uiState.appearanceMode,
        onAppearanceModeChanged =  { newAppearanceModeString ->
          val newAppearanceMode = when(newAppearanceModeString) {
            "Auto" -> AppearanceMode.AUTOMATIC
            "Light" -> AppearanceMode.LIGHT
            "Dark" -> AppearanceMode.DARK
            else -> throw IllegalArgumentException("The new appearance mode [$newAppearanceModeString] is not recognized")
          }
          settingsScreenViewModel.setAppearanceMode(newAppearanceMode)
        },
        groupButtonsColors = groupButtonsColors
      )
      SettingsDivider()
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
        text = "Settings",
        fontWeight = FontWeight.SemiBold
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
fun SettingsDivider(
  modifier: Modifier = Modifier
){
  HorizontalDivider(
    thickness = 1.dp,
    color = colorScheme.onBackground,
    modifier = modifier.fillMaxWidth()
  )
}

@Composable
fun SettingTemplate(
  settingHeight: Dp,
  paddingHorizontal: Dp,
  icon: ImageVector,
  settingName: String,
  interactableElement: @Composable (() -> Unit)
){
  Row (
    modifier = Modifier.fillMaxWidth().height(settingHeight).padding(horizontal = paddingHorizontal),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Start
  ){
    Icon(
      imageVector = icon,
      contentDescription = settingName,
      tint = colorScheme.onBackground,
      modifier = Modifier.padding(end = paddingHorizontal)
    )
    Text(
      text = settingName,
      color = colorScheme.onBackground,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.weight(1f)
    )
    interactableElement()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileOption(
  paddingHorizontal: Dp,
  settingHeight: Dp,
  onEditProfileClick: () -> Unit = {}
){
  SettingTemplate(
    settingHeight = settingHeight,
    paddingHorizontal = paddingHorizontal,
    icon = Icons.Outlined.Edit,
    settingName = "Edit profile"
  ) {
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
  paddingHorizontal: Dp,
  settingHeight: Dp,
  currentNotificationState: Boolean,
  onNotificationStateChanged: (Boolean) -> Unit
){
  SettingTemplate(
    settingHeight = settingHeight,
    paddingHorizontal = paddingHorizontal,
    icon = Icons.Outlined.Notifications,
    settingName = "Notifications"
  ) {
    Switch(
      modifier = Modifier.testTag(SettingsScreenTestTags.NOTIFICATIONS_TOGGLE),
      checked = currentNotificationState,
      onCheckedChange = {
        onNotificationStateChanged(it)
      },
      thumbContent = {
        if (currentNotificationState) {
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
  paddingHorizontal: Dp,
  settingHeight: Dp,
  screenWidth: Dp,
  currentUserStatus: UserType,
  onUserStatusChanged: (String) -> Unit,
  groupButtonsColors: SegmentedButtonColors
){
  val options = listOf("Regular", "Professional")
  val selectedIndex = UserType.entries.indexOf(currentUserStatus)

  SettingTemplate(
    settingHeight = settingHeight,
    paddingHorizontal = paddingHorizontal,
    icon = Icons.Outlined.Person,
    settingName = "User status"
  ) {
    SingleChoiceSegmentedButtonRow (
      modifier = Modifier.width(screenWidth.div(1.9f))
    ){
      options.forEachIndexed { index, option ->
        SegmentedButton(
          shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
          onClick = {
            onUserStatusChanged(option)
          },
          selected = selectedIndex == index,
          colors = groupButtonsColors,
          modifier = Modifier.height(35.dp)
        ){
          Text(
            text = option,
            fontSize = 12.sp,
            color = if (index == selectedIndex) colorScheme.onPrimary else colorScheme.onBackground
          )
        }
      }
    }
  }
}

@Composable
fun AppearanceModeOption(
  paddingHorizontal: Dp,
  screenWidth: Dp,
  settingHeight: Dp,
  currentAppearanceMode: AppearanceMode,
  onAppearanceModeChanged : (String) -> Unit,
  groupButtonsColors: SegmentedButtonColors
){
  SettingTemplate(
    settingHeight = settingHeight,
    paddingHorizontal = paddingHorizontal,
    icon = Icons.Outlined.LightMode,
    settingName = "Appearance"
  ) {
    val options = listOf("Auto", "Light", "Dark")
    val unCheckedIcons = listOf(Icons.Outlined.Autorenew, Icons.Outlined.LightMode, Icons.Outlined.DarkMode)
    val checkedIcons = listOf(Icons.Filled.Autorenew, Icons.Filled.LightMode, Icons.Filled.DarkMode)
    val selectedIndex = AppearanceMode.entries.indexOf(currentAppearanceMode)

    SingleChoiceSegmentedButtonRow (
      modifier = Modifier.width(screenWidth.div(1.8f))
    ) {
      options.forEachIndexed { index, option ->
        SegmentedButton(
          shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
          onClick = {
            onAppearanceModeChanged(option)
          },
          selected = index == selectedIndex,
          modifier = Modifier.height(35.dp),
          icon = {},
          colors = groupButtonsColors
        ){
          Row (
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ){
            Icon(
              imageVector = if (index == selectedIndex) checkedIcons[index] else unCheckedIcons[index],
              contentDescription = option,
              tint = if (index == selectedIndex) colorScheme.onPrimary else colorScheme.onBackground,
              modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = option,
              fontSize = 12.sp,
              color = if (index == selectedIndex) colorScheme.onPrimary else colorScheme.onBackground,
            )
          }
        }
      }
    }
  }
}
