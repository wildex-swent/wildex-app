package com.android.wildex

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.achievement.AchievementsScreen
import com.android.wildex.ui.animal.AnimalInformationScreen
import com.android.wildex.ui.authentication.SignInScreen
import com.android.wildex.ui.authentication.SignInViewModel
import com.android.wildex.ui.camera.CameraScreen
import com.android.wildex.ui.collection.CollectionScreen
import com.android.wildex.ui.home.HomeScreen
import com.android.wildex.ui.map.MapScreen
import com.android.wildex.ui.navigation.BottomNavigationMenu
import com.android.wildex.ui.navigation.NavigationActions
import com.android.wildex.ui.navigation.Screen
import com.android.wildex.ui.navigation.Tab
import com.android.wildex.ui.post.PostDetailsScreen
import com.android.wildex.ui.profile.EditProfileScreen
import com.android.wildex.ui.profile.ProfileScreen
import com.android.wildex.ui.report.ReportDetailsScreen
import com.android.wildex.ui.report.ReportScreen
import com.android.wildex.ui.report.SubmitReportScreen
import com.android.wildex.ui.settings.SettingsScreen
import com.android.wildex.ui.theme.WildexTheme
import com.google.firebase.auth.ktx.auth
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
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    setContent {
      WildexTheme(theme = AppTheme.appearanceMode) {
        Surface(modifier = Modifier.fillMaxSize()) { WildexApp() }
      }
    }
  }
}

@Composable
fun WildexApp(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
    navController: NavHostController = rememberNavController(),
) {
  var currentUserId by remember { mutableStateOf(Firebase.auth.uid) }
  LaunchedEffect(Unit) { Firebase.auth.addAuthStateListener { currentUserId = it.uid } }
  val signInViewModel: SignInViewModel = viewModel()
  val navigationActions = NavigationActions(navController)
  val startDestination = if (currentUserId == null) Screen.Auth.route else Screen.Home.route

  NavHost(navController = navController, startDestination = startDestination) {

    // Auth
    authComposable(navigationActions, credentialManager, signInViewModel)

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
  }
}

private fun NavGraphBuilder.submitFormComposable(navigationActions: NavigationActions) {
  composable(Screen.SubmitReport.route) {
    SubmitReportScreen(
        onSubmitted = { navigationActions.navigateTo(Screen.Report) },
        onGoBack = { navigationActions.goBack() },
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
              }),
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
    if (userId != null) {
      CollectionScreen(
          userUid = userId,
          onAnimalClick = { navigationActions.navigateTo(Screen.AnimalInformation(it)) },
          onProfileClick = { navigationActions.navigateTo(Screen.Profile(it)) },
          onGoBack = { navigationActions.goBack() },
          bottomBar = {
            if (userId == currentUserId)
                BottomNavigation(Tab.Collection, navigationActions, currentUserId)
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
  composable(Screen.Camera.route) {
    CameraScreen(
        bottomBar = {
          if (currentUserId != null) BottomNavigation(Tab.Camera, navigationActions, currentUserId)
        },
        onPost = { navigationActions.navigateTo(Screen.Home) },
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
          isCurrentUser = currentUserId == userId,
          onGoBack = { navigationActions.goBack() },
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
    )
  }
}

private fun NavGraphBuilder.authComposable(
    navigationActions: NavigationActions,
    credentialManager: CredentialManager,
    signInViewModel: SignInViewModel,
) {
  composable(Screen.Auth.route) {
    SignInScreen(
        authViewModel = signInViewModel,
        credentialManager = credentialManager,
        onSignedIn = {
          if (it) navigationActions.navigateTo(Screen.EditProfile(true))
          else navigationActions.navigateTo(Screen.Home)
        },
    )
  }
}
