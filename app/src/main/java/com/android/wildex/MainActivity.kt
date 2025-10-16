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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.android.wildex.ui.collection.AnimalDetailsScreen
import com.android.wildex.ui.navigation.NavigationActions
import com.android.wildex.ui.navigation.Screen
import com.android.wildex.ui.post.PostDetailsScreen
import com.android.wildex.ui.profile.AchievementsScreen
import com.android.wildex.ui.profile.ProfileScreen
import com.android.wildex.ui.report.ReportDetailsScreen
import com.android.wildex.ui.theme.WildexTheme
import com.google.firebase.auth.FirebaseAuth
import okhttp3.OkHttpClient

/** Provide an OkHttpClient client for network requests. */
object HttpClientProvider {
  val client: OkHttpClient = OkHttpClient()
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { WildexTheme { Surface(modifier = Modifier.fillMaxSize()) { WildexApp() } } }
  }
}

@Composable
fun WildexApp(context: Context = LocalContext.current) {
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)
  val startDestination =
      if (FirebaseAuth.getInstance().currentUser == null) Screen.Auth.name else Screen.Home.name

  NavHost(navController = navController, startDestination = startDestination) {
    navigation(
        startDestination = Screen.Auth.route,
        route = Screen.Auth.name,
    ) {
      composable(Screen.Auth.route) { TODO("Add authentication screen here") }
    }

    navigation(
        startDestination = Screen.Home.route,
        route = Screen.Home.name,
    ) {
      composable(Screen.Home.route) { TODO("Add home screen here") }
      composable(Screen.Settings.route) { TODO("Add settings screen here") }
      composable(Screen.EditProfile.route) { TODO("Add edit profile screen here") }
    }

    navigation(
        startDestination = Screen.Map.route,
        route = Screen.Map.name,
    ) {
      composable(Screen.Map.route) { TODO("Add map screen here") }
    }

    navigation(
        startDestination = Screen.NewPost.route,
        route = Screen.NewPost.name,
    ) {
      composable(Screen.NewPost.route) { TODO("Add new post screen here") }
    }

    navigation(
        startDestination = Screen.Collection.route,
        route = Screen.Collection.name,
    ) {
      composable(Screen.Collection.route) { TODO("Add collection screen here") }
      composable(Screen.AnimalDetails.route) { navBackStackEntry ->
        // Get the Animal UID from the arguments
        val uid = navBackStackEntry.arguments?.getString("animalUid")

        // Create the AnimalDetailsScreen with the Post UID
        uid?.let { AnimalDetailsScreen(onGoBack = { navigationActions.goBack() }, animalId = it) }
            ?: run {
              Log.e("AnimalDetailsScreen", "Animal UID is null")
              Toast.makeText(context, "Animal UID is null", Toast.LENGTH_SHORT).show()
            }
      }
    }

    navigation(
        startDestination = Screen.Report.route,
        route = Screen.Report.name,
    ) {
      composable(Screen.Report.route) { TODO("Add report screen here") }
      composable(Screen.SubmitReport.route) { TODO("Add submit report screen here") }
      composable(Screen.ReportDetails.route) { navBackStackEntry ->
        // Get the Report UID from the arguments
        val uid = navBackStackEntry.arguments?.getString("reportUid")

        // Create the ReportDetailsScreen with the Post UID
        uid?.let {
          ReportDetailsScreen(
              onGoBack = { navigationActions.goBack() },
              reportId = it,
              onProfileClick = { userUid -> navigationActions.navigateTo(Screen.Profile(userUid)) })
        }
            ?: run {
              Log.e("ReportDetailsScreen", "Report UID is null")
              Toast.makeText(context, "Report UID is null", Toast.LENGTH_SHORT).show()
            }
      }
    }

    composable(Screen.PostDetails.route) { navBackStackEntry ->
      // Get the Post UID from the arguments
      val uid = navBackStackEntry.arguments?.getString("postUid")

      // Create the PostDetailsScreen with the Post UID
      uid?.let {
        PostDetailsScreen(
            onGoBack = { navigationActions.goBack() },
            postUid = it,
            onProfileClick = { userUid -> navigationActions.navigateTo(Screen.Profile(userUid)) })
      }
          ?: run {
            Log.e("PostDetailsScreen", "Post UID is null")
            Toast.makeText(context, "Post UID is null", Toast.LENGTH_SHORT).show()
          }
    }

    composable(Screen.Profile.route) { navBackStackEntry ->
      // Get the User UID from the arguments
      val uid = navBackStackEntry.arguments?.getString("userUid")

      // Create the ProfileScreen with the User UID
      uid?.let {
        ProfileScreen(
            onGoBack = { navigationActions.goBack() },
            userId = it,
            onAchievementsClick = { userUid ->
              navigationActions.navigateTo(Screen.Achievements(userUid))
            },
            onSettingsClick = { navigationActions.navigateTo(Screen.Settings) })
      }
          ?: run {
            Log.e("ProfileScreen", "User UID is null")
            Toast.makeText(context, "User UID is null", Toast.LENGTH_SHORT).show()
          }
    }

    composable(Screen.Achievements.route) { navBackStackEntry ->
      // Get the User UID from the arguments
      val uid = navBackStackEntry.arguments?.getString("userUid")

      // Create the AchievementsScreen with the User UID
      uid?.let { AchievementsScreen(userId = it, onGoBack = { navigationActions.goBack() }) }
          ?: run {
            Log.e("AchievementsScreen", "User UID is null")
            Toast.makeText(context, "User UID is null", Toast.LENGTH_SHORT).show()
          }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
  WildexTheme { WildexApp() }
}
