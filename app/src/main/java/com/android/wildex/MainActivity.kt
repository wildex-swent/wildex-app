package com.android.wildex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.wildex.ui.home.HomeScreen
import com.android.wildex.ui.theme.WildexTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import okhttp3.OkHttpClient

/** Provide an OkHttpClient client for network requests. */
object HttpClientProvider {
  val client: OkHttpClient = OkHttpClient()
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Firebase.auth.signInAnonymously().addOnCompleteListener {
      if (it.isSuccessful) {
        setContent { WildexTheme { Surface(modifier = Modifier.fillMaxSize()) { WildexApp() } } }
      } else {
        throw it.exception ?: Exception("Anonymous sign-in failed for unknown reasons.")
      }
    }
  }
}

@Composable
fun WildexApp() {
  HomeScreen()
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  WildexTheme { WildexApp() }
}
