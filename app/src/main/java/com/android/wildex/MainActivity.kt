package com.android.wildex

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CredentialManager
import com.android.wildex.ui.authentication.SignInScreen
import com.android.wildex.ui.theme.WildexTheme
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
fun WildexApp(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
) {
  SignInScreen(credentialManager = credentialManager, onSignedIn = { Log.d("Success", "Success") })
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  WildexTheme { WildexApp() }
}
