package com.android.wildex

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.wildex.model.DefaultConnectivityObserver
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.notification.NotificationChannelType
import com.android.wildex.model.notification.NotificationGroupType
import com.android.wildex.model.social.FileSearchDataStorage
import com.android.wildex.model.social.SearchDataUpdater
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.user.OnBoardingStage
import com.android.wildex.model.user.USERS_COLLECTION_PATH
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
import com.android.wildex.ui.navigation.BottomNavigationMenu
import com.android.wildex.ui.navigation.NavigationActions
import com.android.wildex.ui.navigation.Screen
import com.android.wildex.ui.navigation.Tab
import com.android.wildex.ui.notification.NotificationScreen
import com.android.wildex.ui.post.PostDetailsScreen
import com.android.wildex.ui.profile.EditProfileScreen
import com.android.wildex.ui.profile.ProfileScreen
import com.android.wildex.ui.report.ReportDetailsScreen
import com.android.wildex.ui.report.ReportScreen
import com.android.wildex.ui.report.SubmitReportScreen
import com.android.wildex.ui.settings.SettingsScreen
import com.android.wildex.ui.social.FriendScreen
import com.android.wildex.ui.theme.WildexTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.common.MapboxOptions
import okhttp3.OkHttpClient

/** Provide an OkHttpClient client for network requests. */
object HttpClientProvider {
  val client: OkHttpClient = OkHttpClient()
}

object AppTheme {
  var appearanceMode by mutableStateOf(AppearanceMode.AUTOMATIC)
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    RepositoryProvider.init(this)
    createNotificationGroups()
    createNotificationChannels()
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    setContent {
      CompositionLocalProvider(
          LocalConnectivityObserver provides DefaultConnectivityObserver(applicationContext)
      ) {
        WildexTheme(theme = AppTheme.appearanceMode) {
          Surface(modifier = Modifier.fillMaxSize()) { WildexApp() }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  private fun createNotificationChannels() {
    NotificationChannelType.entries.forEach {
      val channel =
          NotificationChannel(it.channelId, it.channelName, it.importance).apply {
            description = it.channelDesc
            group = it.group.groupId
          }
      val notificationManager = getSystemService(NotificationManager::class.java)
      notificationManager.createNotificationChannel(channel)
    }
  }

  private fun createNotificationGroups() {
    NotificationGroupType.entries.forEach {
      val group = NotificationChannelGroup(it.groupId, it.groupName)
      val notificationManager = getSystemService(NotificationManager::class.java)
      notificationManager.createNotificationChannelGroup(group)
    }
  }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WildexApp(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
    navController: NavHostController = rememberNavController(),
) {
  var currentUserId by remember { mutableStateOf(Firebase.auth.uid) }
  var onboardingComplete: Boolean? by remember { mutableStateOf(null) }

  LaunchedEffect(Unit) { Firebase.auth.addAuthStateListener { currentUserId = it.uid } }

  DisposableEffect(currentUserId) {
    val listenerRegistration =
        if (currentUserId != null) {
          Firebase.firestore
              .collection(USERS_COLLECTION_PATH)
              .document(currentUserId!!)
              .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                  snapshot.getString("onBoardingStage")?.let {
                    onboardingComplete = it == OnBoardingStage.COMPLETE.name
                  }
                }
              }
        } else {
          onboardingComplete = false
          null
        }

    onDispose { listenerRegistration?.remove() }
  }
  LaunchedEffect(Unit) {
    SearchDataUpdater(storage = FileSearchDataStorage(context)).updateSearchData()
  }

  val navigationActions = NavigationActions(navController)
  if (onboardingComplete == null) return
  val startDestination = if (!onboardingComplete!!) Screen.Auth.route else Screen.Home.route

  val currentIntent by rememberUpdatedState((context as ComponentActivity).intent)
  LaunchedEffect(currentIntent) {
    val extras = currentIntent.extras
    if (currentUserId != null) {
      extras?.getString("path")?.let { navigationActions.navigateTo(Screen.fromString(it)) }
      extras?.getString("notificationId")?.let {
        RepositoryProvider.notificationRepository.markNotificationAsRead(it)
      }
    }
  }

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
    locationPickerComposable(navigationActions, navController)

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
    navController: NavHostController,
) {
  composable(Screen.LocationPicker.route) {
    LocationPickerScreen(
        onBack = { navigationActions.goBack() },
        onLocationPicked = { picked: Location ->
          // Put result into previous back stack entry, then go back
          navController.previousBackStackEntry
              ?.savedStateHandle
              ?.set(LOCATION_PICKER_RESULT_KEY, picked)
          navigationActions.goBack()
        },
    )
  }
}

private fun NavGraphBuilder.settingsComposable(navigationActions: NavigationActions) {
  composable(Screen.Settings.route) {
    SettingsScreen(
        onGoBack = { navigationActions.goBack() },
        onEditProfileClick = { navigationActions.navigateTo(Screen.EditProfile(false)) },
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
  composable(
      Screen.EditProfile.PATH,
      arguments =
          listOf(
              navArgument("isNewUser") {
                type = NavType.BoolType
                defaultValue = false
              }
          ),
  ) { backStackEntry ->
    val isNewUser = backStackEntry.arguments?.getBoolean("isNewUser") ?: false
    EditProfileScreen(
        onGoBack = { navigationActions.goBack() },
        onSave = { navigationActions.navigateTo(Screen.Home) },
        isNewUser = isNewUser,
    )
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
    )
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
      )
    }
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

private fun NavGraphBuilder.authComposable(credentialManager: CredentialManager) {
  composable(Screen.Auth.route) { SignInScreen(credentialManager = credentialManager) }
}
