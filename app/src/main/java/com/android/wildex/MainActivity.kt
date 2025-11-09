package com.android.wildex

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CredentialManager
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import com.android.wildex.ui.profile.ProfileScreen
import com.android.wildex.ui.report.ReportScreen
import com.android.wildex.ui.settings.SettingsScreen
import com.android.wildex.ui.theme.WildexTheme
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.common.MapboxOptions
import okhttp3.OkHttpClient

/** Provide an OkHttpClient client for network requests. */
object HttpClientProvider {
  val client: OkHttpClient = OkHttpClient()
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    setContent { WildexTheme { Surface(modifier = Modifier.fillMaxSize()) { WildexApp() } } }
  }
}

@Composable
fun WildexApp(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
    navController: NavHostController = rememberNavController(),
) {
  var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
  LaunchedEffect(Unit) {
    val authStateListener =
        FirebaseAuth.AuthStateListener { auth -> currentUser = auth.currentUser }
    FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
  }

  val nullUserUID = context.getString(R.string.null_user_uid)
  val navigationActions = NavigationActions(navController)
  val startDestination = if (currentUser == null) Screen.Auth.route else Screen.Home.route
  NavHost(navController = navController, startDestination = startDestination) {

    // Auth
    composable(Screen.Auth.route) {
      SignInScreen(
          credentialManager = credentialManager,
          onSignedIn = { navigationActions.navigateTo(Screen.Home) },
      )
    }

    // Home
    composable(Screen.Home.route) {
      HomeScreen(
          bottomBar = {
            BottomNavigationMenu(
                Tab.Home,
                onTabSelected = { navigationActions.navigateTo(it.destination) },
            )
          },
          onPostClick = { navigationActions.navigateTo(Screen.PostDetails(it)) },
          onProfilePictureClick = { navigationActions.navigateTo(Screen.Profile(it)) },
          onNotificationClick = {},
      )
    }

    //Settings
    composable(Screen.Settings.route) {
      SettingsScreen(
        onGoBack = {navigationActions.goBack()},
        onEditProfileClick = {},
        onAccountDelete = {navigationActions.navigateTo(Screen.Auth)}
      )
    }

    // Map
    composable("${Screen.Map.PATH}/{userUid}") { backStackEntry ->
      val userId = backStackEntry.arguments?.getString("userUid")
      if (userId != null) {
        MapScreen(
            userId = userId,
            bottomBar = {
              if (userId == currentUser?.uid) {
                BottomNavigationMenu(
                    Tab.Map,
                    onTabSelected = { navigationActions.navigateTo(it.destination) },
                )
              }
            },
        )
      } else {
        Log.e("MapScreen", nullUserUID)
        Toast.makeText(context, nullUserUID, Toast.LENGTH_SHORT).show()
        navController.popBackStack()
      }
    }

    // Camera
    composable(Screen.Camera.route) {
      CameraScreen(
          bottomBar = {
            BottomNavigationMenu(
                Tab.Camera,
                onTabSelected = { navigationActions.navigateTo(it.destination) },
            )
          })
    }

    // Collection
    composable("${Screen.Collection.PATH}/{userUid}") { backStackEntry ->
      val userId = backStackEntry.arguments?.getString("userUid")
      if (userId != null) {
        CollectionScreen(
            userUid = userId,
            onAnimalClick = { animalId ->
              // navigationActions.navigateTo(Screen.AnimalInformationScreen(animalId))
            },
            onProfileClick = {
              navigationActions.navigateTo(Screen.Profile(currentUser?.uid ?: ""))
            },
            onNotificationClick = {},
            onGoBack = { navigationActions.goBack() },
            bottomBar = {
              if (userId == currentUser?.uid)
                  BottomNavigationMenu(
                      Tab.Collection,
                      onTabSelected = { navigationActions.navigateTo(it.destination) },
                  )
            },
        )
      } else {
        Log.e("CollectionScreen", nullUserUID)
        Toast.makeText(context, nullUserUID, Toast.LENGTH_SHORT).show()
        navController.popBackStack()
      }
    }

    // Reports
    composable(Screen.Report.route) {
      ReportScreen(
          bottomBar = {
            BottomNavigationMenu(
                Tab.Report,
                onTabSelected = { navigationActions.navigateTo(it.destination) },
            )
          })
    }

    // Post Details
    composable("${Screen.PostDetails.PATH}/{postUid}") { backStackEntry ->
      val postId = backStackEntry.arguments?.getString("postUid")
      val nullPostUID = context.getString(R.string.null_post_uid)
      if (postId != null) {
        PostDetailsScreen(
            postId = postId,
            onGoBack = { navigationActions.goBack() },
            onProfile = { userUid -> navigationActions.navigateTo(Screen.Profile(userUid)) },
        )
      } else {
        Log.e("PostDetailsScreen", nullPostUID)
        Toast.makeText(context, nullPostUID, Toast.LENGTH_SHORT).show()
        navController.popBackStack()
      }
    }

    // Profile
    composable("${Screen.Profile.PATH}/{userUid}") { backStackEntry ->
      val userId = backStackEntry.arguments?.getString("userUid")
      if (userId != null) {
        ProfileScreen(
            userUid = userId,
            onGoBack = { navigationActions.goBack() },
            onCollection = { navigationActions.navigateTo(Screen.Collection(it)) },
            onSettings = {navigationActions.navigateTo(Screen.Settings)}
        )
      } else {
        Log.e("ProfileScreen", nullUserUID)
        Toast.makeText(context, nullUserUID, Toast.LENGTH_SHORT).show()
        navController.popBackStack()
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
  WildexTheme { WildexApp() }
}
