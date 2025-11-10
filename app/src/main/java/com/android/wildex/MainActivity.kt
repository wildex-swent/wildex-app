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
import com.android.wildex.ui.theme.WildexTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
      }  }
}

@Composable
fun WildexApp(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
    navController: NavHostController = rememberNavController(),
) {
  var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
  LaunchedEffect(Unit) {
    FirebaseAuth.getInstance().addAuthStateListener { currentUser = it.currentUser }
  }

  val navigationActions = NavigationActions(navController)
  val startDestination = if (currentUser == null) Screen.Auth.route else Screen.Home.route
  NavHost(navController = navController, startDestination = startDestination) {

    // Auth
    authComposable(credentialManager, navigationActions)

    // Home
    homeComposable(navigationActions)

    // Map
    mapComposable(currentUser, navigationActions, navController)

    // Camera
    cameraComposable(navigationActions)

    // Collection
    collectionComposable(navigationActions, currentUser, navController)

    // Reports
    reportComposable(navigationActions)

    // Post Details
    postDetailComposable(navigationActions, navController)

    // Profile
    profileComposable(navigationActions, navController)

    // Edit Profile
    editProfileComposable(navigationActions)
  }
}

private fun NavGraphBuilder.editProfileComposable(navigationActions: NavigationActions) {
  composable("${Screen.EditProfile.PATH}/{isNewUser}") { backStackEntry ->
    val isNewUser = backStackEntry.arguments?.getBoolean("isNewUser") ?: false
    EditProfileScreen(
        onGoBack = { navigationActions.goBack() },
        onSave = { navigationActions.navigateTo(Screen.Home) },
        isNewUser = isNewUser,
    )
  }
}

private fun NavGraphBuilder.profileComposable(
    navigationActions: NavigationActions,
    navController: NavHostController,
) {
  composable("${Screen.Profile.PATH}/{userUid}") { backStackEntry ->
    val userId = backStackEntry.arguments?.getString("userUid")
    if (userId != null) {
      ProfileScreen(
          userUid = userId,
          onGoBack = { navigationActions.goBack() },
          onCollection = { navigationActions.navigateTo(Screen.Collection(it)) },
      )
    } else navController.popBackStack()
  }
}

private fun NavGraphBuilder.postDetailComposable(
    navigationActions: NavigationActions,
    navController: NavHostController,
) {
  composable("${Screen.PostDetails.PATH}/{postUid}") { backStackEntry ->
    val postId = backStackEntry.arguments?.getString("postUid")
    if (postId != null) {
      PostDetailsScreen(
          postId = postId,
          onGoBack = { navigationActions.goBack() },
          onProfile = { navigationActions.navigateTo(Screen.Profile(it)) },
      )
    } else navController.popBackStack()
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
    currentUser: FirebaseUser?,
    navController: NavHostController,
) {
  composable("${Screen.Collection.PATH}/{userUid}") { backStackEntry ->
    val userId = backStackEntry.arguments?.getString("userUid")
    if (userId != null) {
      CollectionScreen(
          userUid = userId,
          onAnimalClick = { navigationActions.navigateTo(Screen.AnimalInformationScreen(it)) },
          onProfileClick = { navigationActions.navigateTo(Screen.Profile(currentUser?.uid ?: "")) },
          onGoBack = { navigationActions.goBack() },
          bottomBar = {
            if (userId == currentUser?.uid)
                BottomNavigationMenu(Tab.Collection) {
                  navigationActions.navigateTo(it.destination)
                }
          },
      )
    } else navController.popBackStack()
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
    currentUser: FirebaseUser?,
    navigationActions: NavigationActions,
    navController: NavHostController,
) {
  composable("${Screen.Map.PATH}/{userUid}") { backStackEntry ->
    val userId = backStackEntry.arguments?.getString("userUid")
    if (userId != null) {
      MapScreen(
          userId = userId,
          bottomBar = {
            if (userId == currentUser?.uid) {
              BottomNavigationMenu(Tab.Map) { navigationActions.navigateTo(it.destination) }
            }
          },
      )
    } else navController.popBackStack()
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
    credentialManager: CredentialManager,
    navigationActions: NavigationActions,
) {
  composable(Screen.Auth.route) {
    SignInScreen(
        credentialManager = credentialManager,
        onSignedIn = { navigationActions.navigateTo(Screen.Home) },
    )
  }
}
