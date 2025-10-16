package com.android.wildex.ui.authentication

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.R

object SignInScreenTestTags {
  const val APP_LOGO = "appLogo"
  const val LOGIN_BUTTON = "loginButton"
  const val WELCOME = "welcome"
  const val LOADING_INDICATOR = "loadingIndicator"
}

@Composable
fun SignInScreen(
    authViewModel: SignInViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onSignedIn: () -> Unit = {}
) {

  val context = LocalContext.current
  val uiState by authViewModel.uiState.collectAsState()

  LaunchedEffect(uiState.errorMsg, uiState.firebaseUser) {
    // Show error message if login fails
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      authViewModel.clearErrorMsg()
    }

    // Navigate to home screen on successful login
    uiState.firebaseUser?.let {
      Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
      onSignedIn()
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      content = { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Image(
                  painter = painterResource(id = R.drawable.app_logo_name),
                  contentDescription = "App logo",
                  modifier = Modifier.size(200.dp).testTag(SignInScreenTestTags.APP_LOGO))

              Spacer(modifier = Modifier.height(48.dp))

              // Authenticate With Google Button
              if (uiState.firebaseUser == null) {
                if (uiState.isLoading) {
                  CircularProgressIndicator(
                      modifier =
                          Modifier.size(48.dp).testTag(SignInScreenTestTags.LOADING_INDICATOR))
                } else {
                  GoogleSignInButton(
                      context = context,
                      onSignInClick = { authViewModel.signIn(context, credentialManager) })
                }
              } else {
                Text(
                    text = context.getString(R.string.welcome),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(SignInScreenTestTags.WELCOME))
              }
            }
      })
}

@Composable
fun GoogleSignInButton(onSignInClick: () -> Unit, context: Context) {
  OutlinedButton(
      onClick = onSignInClick,
      colors = ButtonDefaults.buttonColors(containerColor = Color.White),
      shape = RoundedCornerShape(50),
      border = BorderStroke(1.dp, Color.LightGray),
      modifier =
          Modifier.padding(8.dp)
              .fillMaxWidth(0.8f)
              .height(48.dp)
              .testTag(SignInScreenTestTags.LOGIN_BUTTON)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()) {
              Image(
                  painter = painterResource(id = R.drawable.google_logo),
                  contentDescription = context.getString(R.string.google_logo),
                  modifier = Modifier.size(30.dp).padding(8.dp))

              Text(
                  text = context.getString(R.string.sign_in),
                  color = Color.Gray,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Medium)
            }
      }
}
