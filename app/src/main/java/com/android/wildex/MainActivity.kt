package com.android.wildex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.wildex.ui.theme.WildexTheme
import okhttp3.OkHttpClient

/** Provide an OkHttpClient client for network requests. */
object HttpClientProvider {
  val client: OkHttpClient = OkHttpClient()
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Firebase.auth.signInAnonymously()
    setContent { WildexTheme { Surface(modifier = Modifier.fillMaxSize()) { WildexApp() } } }
  }
}

@Composable
fun WildexApp() {
  Text(text = "Welcome to Wildex!")
  // HomeScreen()
}
