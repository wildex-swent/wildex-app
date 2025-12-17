package com.android.wildex.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.achievement.AchievementsScreen
import com.android.wildex.ui.animal.AnimalInformationScreen
import com.android.wildex.ui.authentication.SignInScreen
import com.android.wildex.ui.camera.CameraScreen
import com.android.wildex.ui.collection.CollectionScreen
import com.android.wildex.ui.home.HomeScreen
import com.android.wildex.ui.locationpicker.LOCATION_PICKER_RESULT_KEY
import com.android.wildex.ui.locationpicker.LocationPickerScreen
import com.android.wildex.ui.map.MapScreen
import com.android.wildex.ui.notification.NotificationScreen
import com.android.wildex.ui.post.PostDetailsScreen
import com.android.wildex.ui.profile.EditProfileScreen
import com.android.wildex.ui.profile.ProfileScreen
import com.android.wildex.ui.report.ReportDetailsScreen
import com.android.wildex.ui.report.ReportScreen
import com.android.wildex.ui.report.SubmitReportScreen
import com.android.wildex.ui.settings.SettingsScreen
import com.android.wildex.ui.social.FriendScreen

@Composable
fun AppNavigationHost(
    onboardingComplete: Boolean,
    currentUserId: String?,
    credentialManager: CredentialManager,
    navController: NavHostController,
) {
  val navigationActions = NavigationActions(navController)
  val startDestination = if (!onboardingComplete) Screen.Auth.route else Screen.Home.route

  NavHost(navController = navController, startDestination = startDestination) {

    // Auth
    authComposable(credentialManager)

    // Home
    homeComposable(navigationActions, currentUserId)

    // Map
    mapComposable(navigationActions, currentUserId)

    // Camera
    cameraComposable(navigationActions, currentUserId)

    // Collection
    collectionComposable(navigationActions, currentUserId)

    // Reports
    reportComposable(navigationActions, currentUserId)

    // Report Details
    reportDetailsComposable(navigationActions)

    // Animal Information
    animalInformationComposable(navigationActions)

    // Post Details
    postDetailComposable(navigationActions)

    // Social: Friends, Requests
    socialComposable(navigationActions)

    // Profile
    profileComposable(navigationActions)

    // Edit Profile
    editProfileComposable(navigationActions)

    // Achievements
    achievementsComposable(navigationActions)

    // Settings
    settingsComposable(navigationActions)

    // Submit Form
    submitFormComposable(navigationActions)

    // Location Picker
    locationPickerComposable(navigationActions) {
      navController.previousBackStackEntry?.savedStateHandle?.set(LOCATION_PICKER_RESULT_KEY, it)
    }

    // Notifications
    notificationsComposable(navigationActions)
  }
}

private fun NavGraphBuilder.notificationsComposable(navigationActions: NavigationActions) {
  composable(Screen.Notifications.route) {
    NotificationScreen(
        onGoBack = { navigationActions.goBack() },
        onProfileClick = { navigationActions.navigateTo(Screen.Profile(it)) },
        onNotificationClick = { navigationActions.navigateTo(Screen.fromString(it)) },
    )
  }
}

private fun NavGraphBuilder.submitFormComposable(navigationActions: NavigationActions) {
  composable(Screen.SubmitReport.route) { backStackEntry ->
    val savedStateHandle = backStackEntry.savedStateHandle
    val serializedLocationFlow = remember {
      savedStateHandle.getStateFlow<Location?>(LOCATION_PICKER_RESULT_KEY, null)
    }
    val pickedLocation by serializedLocationFlow.collectAsStateWithLifecycle()

    SubmitReportScreen(
        onSubmitted = { navigationActions.navigateTo(Screen.Report) },
        onGoBack = { navigationActions.goBack() },
        onPickLocation = { navigationActions.navigateTo(Screen.LocationPicker) },
        serializedLocation = pickedLocation,
        onPickedLocationConsumed = { savedStateHandle[LOCATION_PICKER_RESULT_KEY] = null },
    )
  }
}

private fun NavGraphBuilder.locationPickerComposable(
    navigationActions: NavigationActions,
    setPickedLocation: (Location) -> Unit,
) {
  composable(Screen.LocationPicker.route) {
    LocationPickerScreen(
        onBack = { navigationActions.goBack() },
        onLocationPicked = {
          // Put result into previous back stack entry, then go back
          setPickedLocation(it)
          navigationActions.goBack()
        },
    )
  }
}

private fun NavGraphBuilder.settingsComposable(navigationActions: NavigationActions) {
  composable(Screen.Settings.route) {
    SettingsScreen(
        onGoBack = { navigationActions.goBack() },
        onEditProfileClick = { navigationActions.navigateTo(Screen.EditProfile) },
        onAccountDeleteOrSignOut = { navigationActions.navigateTo(Screen.Auth) },
    )
  }
}

private fun NavGraphBuilder.animalInformationComposable(navigationActions: NavigationActions) {
  composable(Screen.AnimalInformation.PATH) { backStackEntry ->
    val animalUid = backStackEntry.arguments?.getString("animalUid")
    if (animalUid != null) {
      AnimalInformationScreen(animalId = animalUid, onGoBack = { navigationActions.goBack() })
    }
  }
}

private fun NavGraphBuilder.achievementsComposable(navigationActions: NavigationActions) {
  composable(Screen.Achievements.PATH) { backStackEntry ->
    val userId = backStackEntry.arguments?.getString("userUid")
    if (userId != null) {
      AchievementsScreen(onGoBack = { navigationActions.goBack() }, userId = userId)
    }
  }
}

private fun NavGraphBuilder.editProfileComposable(navigationActions: NavigationActions) {
  composable(Screen.EditProfile.route) {
    EditProfileScreen(onGoBack = { navigationActions.goBack() })
  }
}

private fun NavGraphBuilder.profileComposable(navigationActions: NavigationActions) {
  composable(Screen.Profile.PATH) { backStackEntry ->
    val userId = backStackEntry.arguments?.getString("userUid")
    if (userId != null) {
      ProfileScreen(
          userUid = userId,
          onGoBack = { navigationActions.goBack() },
          onCollection = { navigationActions.navigateTo(Screen.Collection(it)) },
          onAchievements = { navigationActions.navigateTo(Screen.Achievements(it)) },
          onMap = { navigationActions.navigateTo(Screen.Map(it)) },
          onSettings = { navigationActions.navigateTo(Screen.Settings) },
          onFriends = { navigationActions.navigateTo(Screen.Social(it)) },
      )
    }
  }
}

private fun NavGraphBuilder.socialComposable(navigationActions: NavigationActions) {
  composable(Screen.Social.PATH) { backStackEntry ->
    val userId = backStackEntry.arguments?.getString("userUid")
    if (userId != null) {
      FriendScreen(
          userId = userId,
          onProfileClick = { navigationActions.navigateTo(Screen.Profile(it)) },
          onGoBack = { navigationActions.goBack() },
      )
    }
  }
}

private fun NavGraphBuilder.postDetailComposable(navigationActions: NavigationActions) {
  composable(Screen.PostDetails.PATH) { backStackEntry ->
    val postId = backStackEntry.arguments?.getString("postUid")
    if (postId != null) {
      PostDetailsScreen(
          postId = postId,
          onGoBack = { navigationActions.goBack() },
          onProfile = { navigationActions.navigateTo(Screen.Profile(it)) },
      )
    }
  }
}

private fun NavGraphBuilder.reportDetailsComposable(navigationActions: NavigationActions) {
  composable(Screen.ReportDetails.PATH) { backStackEntry ->
    val reportId = backStackEntry.arguments?.getString("reportUid")
    if (reportId != null) {
      ReportDetailsScreen(
          reportId = reportId,
          onGoBack = { navigationActions.goBack() },
          onProfile = { navigationActions.navigateTo(Screen.Profile(it)) },
      )
    }
  }
}

private fun NavGraphBuilder.reportComposable(
    navigationActions: NavigationActions,
    currentUserId: Id?,
) {
  composable(Screen.Report.route) {
    ReportScreen(
        bottomBar = {
          if (currentUserId != null) BottomNavigation(Tab.Report, navigationActions, currentUserId)
        },
        onProfileClick = { navigationActions.navigateTo(Screen.Profile(it)) },
        onCurrentProfileClick = { navigationActions.navigateTo(Screen.Profile(currentUserId!!)) },
        onReportClick = { navigationActions.navigateTo(Screen.ReportDetails(it)) },
        onSubmitReportClick = { navigationActions.navigateTo(Screen.SubmitReport) },
        onNotificationClick = { navigationActions.navigateTo(Screen.Notifications) },
    )
  }
}

private fun NavGraphBuilder.collectionComposable(
    navigationActions: NavigationActions,
    currentUserId: Id?,
) {
  composable(Screen.Collection.PATH) { backStackEntry ->
    val userId = backStackEntry.arguments?.getString("userUid")
    val isCurrentUser = userId == currentUserId
    if (userId != null) {
      CollectionScreen(
          userUid = userId,
          onAnimalClick = { navigationActions.navigateTo(Screen.AnimalInformation(it)) },
          onGoBack = { navigationActions.goBack() },
          onProfilePictureClick = { navigationActions.navigateTo(Screen.Profile(currentUserId!!)) },
          bottomBar = {
            if (isCurrentUser) BottomNavigation(Tab.Collection, navigationActions, currentUserId!!)
          },
          onNotificationClick = { navigationActions.navigateTo(Screen.Notifications) },
      )
    }
  }
}

private fun NavGraphBuilder.cameraComposable(
    navigationActions: NavigationActions,
    currentUserId: Id?,
) {
  composable(Screen.Camera.route) { backStackEntry ->
    val savedStateHandle = backStackEntry.savedStateHandle
    val serializedLocationFlow = remember {
      savedStateHandle.getStateFlow<Location?>(LOCATION_PICKER_RESULT_KEY, null)
    }
    val pickedLocation by serializedLocationFlow.collectAsStateWithLifecycle()
    CameraScreen(
        bottomBar = {
          if (currentUserId != null) BottomNavigation(Tab.Camera, navigationActions, currentUserId)
        },
        onPost = { navigationActions.navigateTo(Screen.Home) },
        onPickLocation = { navigationActions.navigateTo(Screen.LocationPicker) },
        serializedLocation = pickedLocation,
        onPickedLocationConsumed = { savedStateHandle[LOCATION_PICKER_RESULT_KEY] = null },
    )
  }
}

private fun NavGraphBuilder.mapComposable(
    navigationActions: NavigationActions,
    currentUserId: Id?,
) {
  composable(Screen.Map.PATH) { backStackEntry ->
    val userId = backStackEntry.arguments?.getString("userUid")
    if (userId != null) {
      MapScreen(
          userId = userId,
          bottomBar = {
            if (userId == currentUserId) {
              BottomNavigation(Tab.Map, navigationActions, currentUserId)
            }
          },
          onPost = { navigationActions.navigateTo(Screen.PostDetails(it)) },
          onReport = { navigationActions.navigateTo(Screen.ReportDetails(it)) },
          isCurrentUser = currentUserId == userId,
          onGoBack = { navigationActions.goBack() },
          onProfile = { navigationActions.navigateTo(Screen.Profile(it)) },
      )
    }
  }
}

private fun NavGraphBuilder.homeComposable(
    navigationActions: NavigationActions,
    currentUserId: Id?,
) {
  composable(Screen.Home.route) {
    HomeScreen(
        bottomBar = {
          if (currentUserId != null) BottomNavigation(Tab.Home, navigationActions, currentUserId)
        },
        onPostClick = { navigationActions.navigateTo(Screen.PostDetails(it)) },
        onProfilePictureClick = { navigationActions.navigateTo(Screen.Profile(it)) },
        onCurrentProfilePictureClick = {
          navigationActions.navigateTo(Screen.Profile(currentUserId!!))
        },
        onNotificationClick = { navigationActions.navigateTo(Screen.Notifications) },
    )
  }
}

@Composable
private fun BottomNavigation(
    tab: Tab,
    navigationActions: NavigationActions,
    currentUserId: String,
) {
  BottomNavigationMenu(tab) {
    when (it) {
      Tab.Home -> navigationActions.navigateTo(Screen.Home)
      Tab.Map -> navigationActions.navigateTo(Screen.Map(currentUserId))
      Tab.Camera -> navigationActions.navigateTo(Screen.Camera)
      Tab.Collection -> navigationActions.navigateTo(Screen.Collection(currentUserId))
      Tab.Report -> navigationActions.navigateTo(Screen.Report)
    }
  }
}

private fun NavGraphBuilder.authComposable(credentialManager: CredentialManager) {
  composable(Screen.Auth.route) { SignInScreen(credentialManager = credentialManager) }
}
