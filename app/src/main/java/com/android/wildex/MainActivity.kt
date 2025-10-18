package com.android.wildex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.wildex.ui.home.HomeScreen
import com.android.wildex.ui.home.HomeScreenViewModel
import com.android.wildex.ui.theme.WildexTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import okhttp3.OkHttpClient

/** Provide an OkHttpClient client for network requests. */
object HttpClientProvider {
  val client: OkHttpClient = OkHttpClient()
}

const val HOST = "10.0.2.2"
const val FIRESTORE_PORT = 8080
const val AUTH_PORT = 9099

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Firebase.firestore.useEmulator(HOST, FIRESTORE_PORT)
    Firebase.auth.useEmulator(HOST, AUTH_PORT)
    setContent {
      WildexTheme {
        Surface(modifier = Modifier.fillMaxSize()) { HomeScreen(HomeScreenViewModel()) }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    Firebase.firestore.clearPersistence()
    Firebase.auth.signOut()
  }
}

@Composable
fun WildexApp() {
  Text(text = "Welcome to Wildex!")
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  WildexTheme { WildexApp() }
}
