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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CredentialManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.wildex.ui.achievement.AchievementsScreen
import com.android.wildex.ui.authentication.SignInScreen
import com.android.wildex.ui.camera.CameraScreen
import com.android.wildex.ui.collection.AnimalDetailsScreen
import com.android.wildex.ui.collection.CollectionScreen
import com.android.wildex.ui.home.HomeScreen
import com.android.wildex.ui.map.MapScreen
import com.android.wildex.ui.navigation.BottomNavigationMenu
import com.android.wildex.ui.navigation.NavigationActions
import com.android.wildex.ui.navigation.Screen
import com.android.wildex.ui.navigation.Tab
import com.android.wildex.ui.post.PostDetailsScreen
import com.android.wildex.ui.profile.ProfileScreen
import com.android.wildex.ui.report.ReportDetailsScreen
import com.android.wildex.ui.report.ReportScreen
import com.android.wildex.ui.theme.WildexTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import okhttp3.OkHttpClient

/** Provide an OkHttpClient client for network requests. */
object HttpClientProvider {
  val client: OkHttpClient = OkHttpClient()
}

const val HOST = "10.0.2.2"
const val EMULATORS_PORT = 4400
const val FIRESTORE_PORT = 8080
const val AUTH_PORT = 9099

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    Firebase.auth.useEmulator(HOST, AUTH_PORT)
    Firebase.firestore.useEmulator(HOST, FIRESTORE_PORT)
    super.onCreate(savedInstanceState)
    setContent { WildexTheme { Surface(modifier = Modifier.fillMaxSize()) { WildexApp() } } }
  }
}

@Composable
fun WildexApp(context: Context = LocalContext.current) {
  val credentialManager = CredentialManager.create(context)
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)
  val startDestination =
      if (FirebaseAuth.getInstance().currentUser == null) Screen.Auth.route else Screen.Home.route
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

    // Settings
    composable(Screen.Settings.route) { TODO("Add settings screen here") }

    // Edit Profile
    composable(Screen.EditProfile.route) { TODO("Add edit profile screen here") }

    // Map
    composable(Screen.Map.route) {
      MapScreen(
          bottomBar = {
            BottomNavigationMenu(
                Tab.Map,
                onTabSelected = { navigationActions.navigateTo(it.destination) },
            )
          }
      )
    }

    // New Post
    composable(Screen.NewPost.route) {
      CameraScreen(
          bottomBar = {
            BottomNavigationMenu(
                Tab.NewPost,
                onTabSelected = { navigationActions.navigateTo(it.destination) },
            )
          }
      )
    }

    // Collection
    composable(Screen.Collection.route) {
      CollectionScreen(
          bottomBar = {
            BottomNavigationMenu(
                Tab.Collection,
                onTabSelected = { navigationActions.navigateTo(it.destination) },
            )
          }
      )
    }
    composable("${Screen.AnimalDetails.PATH}/{animalUid}") { backStackEntry ->
      val animalId = backStackEntry.arguments?.getString("animalUid")
      if (animalId != null) {
        AnimalDetailsScreen(onGoBack = { navigationActions.goBack() }, animalId = animalId)
      } else {
        Log.e("AnimalDetailsScreen", "Animal UID is null")
        Toast.makeText(context, "Animal UID is null", Toast.LENGTH_SHORT).show()
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
          }
      )
    }
    composable(Screen.SubmitReport.route) { TODO("Add submit report screen here") }
    composable("${Screen.ReportDetails.PATH}/{reportUid}") { backStackEntry ->
      val reportId = backStackEntry.arguments?.getString("reportUid")
      if (reportId != null) {
        ReportDetailsScreen(
            onGoBack = { navigationActions.goBack() },
            reportId = reportId,
            onProfileClick = { userUid -> navigationActions.navigateTo(Screen.Profile(userUid)) },
        )
      } else {
        Log.e("ReportDetailsScreen", "Report UID is null")
        Toast.makeText(context, "Report UID is null", Toast.LENGTH_SHORT).show()
        navController.popBackStack()
      }
    }

    // Post Details
    composable("${Screen.PostDetails.PATH}/{postUid}") { backStackEntry ->
      val postId = backStackEntry.arguments?.getString("postUid")
      if (postId != null) {
        PostDetailsScreen(
            postId = postId,
            onGoBack = { navigationActions.goBack() },
            onProfile = { userUid -> navigationActions.navigateTo(Screen.Profile(userUid)) },
        )
      } else {
        Log.e("PostDetailsScreen", "Post UID is null")
        Toast.makeText(context, "Post UID is null", Toast.LENGTH_SHORT).show()
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
        )
      } else {
        Log.e("ProfileScreen", "User UID is null")
        Toast.makeText(context, "User UID is null", Toast.LENGTH_SHORT).show()
        navController.popBackStack()
      }
    }

    // Achievements
    composable("${Screen.Achievements.PATH}/{userUid}") { backStackEntry ->
      val userId = backStackEntry.arguments?.getString("userUid")
      if (userId != null) {
        AchievementsScreen(userId = userId, onGoBack = { navigationActions.goBack() })
      } else {
        Log.e("AchievementsScreen", "User UID is null")
        Toast.makeText(context, "User UID is null", Toast.LENGTH_SHORT).show()
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
