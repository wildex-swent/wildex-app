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
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.achievement.AchievementsScreen
import com.android.wildex.ui.animal.AnimalInformationScreen
import com.android.wildex.ui.authentication.SignInScreen
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
  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
  LaunchedEffect(Unit) { Firebase.auth.addAuthStateListener { currentUser = it.currentUser } }

  val navigationActions = NavigationActions(navController)
  val startDestination = if (currentUser == null) Screen.Auth.route else Screen.Home.route
  NavHost(navController = navController, startDestination = startDestination) {

    // Auth
    authComposable(navigationActions, credentialManager)

    // Home
    homeComposable(navigationActions)

    // Map
    mapComposable(navigationActions, currentUser?.uid)

    // Camera
    cameraComposable(navigationActions)

    // Collection
    collectionComposable(navigationActions, currentUser?.uid)

    // Reports
    reportComposable(navigationActions)

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
      AchievementsScreen(onGoBack = { navigationActions.goBack() })
    }
  }
}

private fun NavGraphBuilder.editProfileComposable(navigationActions: NavigationActions) {
  composable(Screen.EditProfile.PATH) { backStackEntry ->
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

private fun NavGraphBuilder.reportComposable(navigationActions: NavigationActions) {
  composable(Screen.Report.route) {
    ReportScreen(
        bottomBar = {
          BottomNavigationMenu(Tab.Report) { navigationActions.navigateTo(it.destination) }
        }
    )
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
          onProfileClick = { navigationActions.navigateTo(Screen.Profile(currentUserId ?: "")) },
          onGoBack = { navigationActions.goBack() },
          bottomBar = {
            if (userId == currentUserId)
                BottomNavigationMenu(Tab.Collection) {
                  navigationActions.navigateTo(it.destination)
                }
          },
      )
    }
  }
}

private fun NavGraphBuilder.cameraComposable(navigationActions: NavigationActions) {
  composable(Screen.Camera.route) {
    CameraScreen(
        bottomBar = {
          BottomNavigationMenu(Tab.Camera) { navigationActions.navigateTo(it.destination) }
        }
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
              BottomNavigationMenu(Tab.Map) { navigationActions.navigateTo(it.destination) }
            }
          },
      )
    }
  }
}

private fun NavGraphBuilder.homeComposable(navigationActions: NavigationActions) {
  composable(Screen.Home.route) {
    HomeScreen(
        bottomBar = {
          BottomNavigationMenu(Tab.Home) { navigationActions.navigateTo(it.destination) }
        },
        onPostClick = { navigationActions.navigateTo(Screen.PostDetails(it)) },
        onProfilePictureClick = { navigationActions.navigateTo(Screen.Profile(it)) },
        onNotificationClick = {},
    )
  }
}

private fun NavGraphBuilder.authComposable(
    navigationActions: NavigationActions,
    credentialManager: CredentialManager,
) {
  composable(Screen.Auth.route) {
    SignInScreen(
        credentialManager = credentialManager,
        onSignedIn = { navigationActions.navigateTo(Screen.Home) },
    )
  }
}
