package com.android.wildex.ui.authentication

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.android.wildex.R
import com.android.wildex.model.user.OnBoardingStage
import com.android.wildex.ui.achievement.ProgressBar
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.theme.White
import com.android.wildex.ui.theme.WildexBlack
import com.android.wildex.ui.theme.WildexLightSurface

object SignInScreenTestTags {
  const val APP_LOGO = "appLogo"
  const val LOGIN_BUTTON = "loginButton"
  const val WELCOME = "welcome"
  const val LOADING_INDICATOR = "loadingIndicator"
}

/**
 * Composable function representing the Sign-In Screen.
 *
 * @param authViewModel The ViewModel managing the state of the Sign-In Screen.
 * @param credentialManager The Credential Manager for handling sign-in credentials.
 */
@Composable
fun SignInScreen(
    authViewModel: SignInViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
) {
  val context = LocalContext.current
  val uiState by authViewModel.uiState.collectAsState()

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      authViewModel.clearErrorMsg()
    }
  }

  var isWaterFull by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { isWaterFull = false }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(NavigationTestTags.SIGN_IN_SCREEN),
      containerColor = WildexLightSurface,
      topBar = {
        val stage = uiState.onBoardingStage
        if (stage != null) {
          val color =
              if (stage == OnBoardingStage.COMPLETE) colorScheme.primary
              else colorScheme.onBackground
          Column(
              modifier = Modifier.fillMaxWidth().background(colorScheme.background).padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            val currentStep = stage.ordinal
            val maxSteps = OnBoardingStage.entries.size - 1
            val progress = currentStep.toFloat() / maxSteps
            ProgressBar(
                color = color,
                trackColor = colorScheme.surface.copy(.6f),
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(10.dp),
            )
            Text(
                text = stringResource(R.string.step, currentStep, maxSteps),
                style = typography.titleMedium,
                modifier = Modifier.align(Alignment.Start),
                color = color,
            )
          }
        }
      },
      content = { paddingValues ->
        Box(
            modifier =
                Modifier.fillMaxSize().padding(paddingValues).background(colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
          val rightFade = remember {
            slideInHorizontally(animationSpec = tween(500)) { it } +
                fadeIn(animationSpec = tween(500)) togetherWith
                slideOutHorizontally(animationSpec = tween(500)) { -it } +
                    fadeOut(animationSpec = tween(500))
          }
          val leftFade = remember {
            slideInHorizontally(animationSpec = tween(500)) { -it } +
                fadeIn(animationSpec = tween(500)) togetherWith
                slideOutHorizontally(animationSpec = tween(500)) { it } +
                    fadeOut(animationSpec = tween(500))
          }
          AnimatedContent(
              targetState = uiState.onBoardingStage,
              transitionSpec = {
                val isGoingRight = initialState == null || targetState!! > initialState!!
                (if (isGoingRight) rightFade else leftFade).using(SizeTransform(clip = false))
              },
          ) { stage ->
            when (stage) {
              null ->
                  SignInContent(
                      isLoading = uiState.isLoading,
                      onSignedIn = { authViewModel.signIn(context, credentialManager) },
                      paddingValues = paddingValues,
                      isWaterFull = isWaterFull,
                      onFilled = { isWaterFull = true },
                  )
              OnBoardingStage.NAMING ->
                  NamingScreen(
                      data = uiState.onBoardingData,
                      updateData = { authViewModel.updateOnBoardingData(it) },
                      onNext = { authViewModel.goToNextStage() },
                      canProceed = authViewModel.canProceedFromNaming(),
                      isLoading = uiState.isLoading,
                  )
              OnBoardingStage.OPTIONAL ->
                  OptionalInfoScreen(
                      data = uiState.onBoardingData,
                      updateData = { authViewModel.updateOnBoardingData(it) },
                      onBack = { authViewModel.goToPreviousStage() },
                      onNext = { authViewModel.goToNextStage() },
                      isLoading = uiState.isLoading,
                  )
              OnBoardingStage.USER_TYPE ->
                  UserTypeScreen(
                      data = uiState.onBoardingData,
                      updateData = { authViewModel.updateOnBoardingData(it) },
                      onBack = { authViewModel.goToPreviousStage() },
                      onNext = { authViewModel.goToNextStage() },
                      isLoading = uiState.isLoading,
                  )
              OnBoardingStage.AWAITING_COMPLETE -> {
                LaunchedEffect(Unit) { authViewModel.finishRegistration() }
                WaitingScreen()
              }
              else -> {
                LaunchedEffect(Unit) { authViewModel.goToNextStage() }
              }
            }
          }
        }
      },
  )
}

@Composable
fun WaitingScreen() {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(text = "Waiting screen", style = typography.titleLarge)
  }
}

@Composable
fun SignInContent(
    isLoading: Boolean,
    paddingValues: PaddingValues,
    isWaterFull: Boolean,
    onFilled: () -> Unit,
    onSignedIn: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
    WaterFillBackground(onFilled = onFilled)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
      LogoAndTagline()

      Spacer(modifier = Modifier.height(10.dp))

      if (isLoading) {
        val composition by
            rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.loading_signin),
            )
        val progress by
            animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever,
            )

        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(120.dp).testTag(SignInScreenTestTags.LOADING_INDICATOR),
        )
      } else {
        GoogleSignInButton(
            appearsOn = isWaterFull,
            onSignInClick = onSignedIn,
        )
      }
    }
  }
}

/** Composable function displaying the app logo and tagline. */
@Composable
fun LogoAndTagline() {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Image(
        painter = painterResource(id = R.drawable.app_logo_sign),
        contentDescription = "App logo",
        modifier =
            Modifier.width(200.dp).wrapContentHeight().testTag(SignInScreenTestTags.APP_LOGO),
    )
    Spacer(modifier = Modifier.height(20.dp))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Image(
          painter = painterResource(id = R.drawable.app_name),
          contentDescription = "App name",
          modifier = Modifier.width(250.dp).wrapContentHeight(),
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
          text = stringResource(R.string.tag_line),
          style = typography.titleMedium,
          color = White.copy(alpha = 0.9f),
          modifier = Modifier.testTag(SignInScreenTestTags.WELCOME),
      )
    }
  }
}

/**
 * Composable function representing the Google Sign-In Button.
 *
 * @param onSignInClick Callback invoked when the sign-in button is clicked.
 * @param appearsOn Boolean flag to control the appearance animation of the button.
 */
@Composable
fun GoogleSignInButton(onSignInClick: () -> Unit, appearsOn: Boolean = true) {
  val alpha by
      animateFloatAsState(
          targetValue = if (appearsOn) 1f else 0f,
          animationSpec = tween(durationMillis = 400),
      )
  Button(
      onClick = onSignInClick,
      colors = ButtonDefaults.buttonColors(containerColor = White),
      shape = RoundedCornerShape(50),
      contentPadding = PaddingValues(horizontal = 50.dp, vertical = 8.dp),
      modifier = Modifier.testTag(SignInScreenTestTags.LOGIN_BUTTON).graphicsLayer(alpha = alpha),
  ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
      Image(
          painter = painterResource(id = R.drawable.google_logo),
          contentDescription = "Google Logo",
          modifier = Modifier.size(30.dp).padding(8.dp).graphicsLayer(alpha = alpha),
      )

      Text(
          text = stringResource(R.string.sign_in),
          modifier = Modifier.graphicsLayer(alpha = alpha),
          color = WildexBlack,
          style = typography.titleMedium,
      )
    }
  }
}
