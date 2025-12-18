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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
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
import com.android.wildex.ui.navigation.AppNavigationHost
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
          LocalConnectivityObserver provides DefaultConnectivityObserver(applicationContext)) {
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
  if (onboardingComplete == null) return
  LaunchedEffect(Unit) {
    SearchDataUpdater(storage = FileSearchDataStorage(context)).updateSearchData()
  }

  val currentIntent by rememberUpdatedState((context as ComponentActivity).intent)
  LaunchedEffect(currentIntent) {
    val extras = currentIntent.extras
    if (currentUserId != null) {
      extras?.getString("path")?.let { navController.navigate(it) }
      extras?.getString("notificationId")?.let {
        RepositoryProvider.notificationRepository.markNotificationAsRead(it)
      }
    }
  }
  AppNavigationHost(onboardingComplete!!, currentUserId, credentialManager, navController)
}
