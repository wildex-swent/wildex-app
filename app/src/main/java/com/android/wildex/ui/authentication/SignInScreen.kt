package com.android.wildex.ui.authentication

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.android.wildex.R
import com.android.wildex.model.user.OnBoardingStage
import com.android.wildex.model.user.UserType
import com.android.wildex.ui.achievement.ProgressBar
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.theme.White
import com.android.wildex.ui.theme.WildexBlack
import com.android.wildex.ui.utils.CountryDropdown

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
 * @param onSignedIn Callback invoked when the user has successfully signed in.
 */
@Composable
fun SignInScreen(
    authViewModel: SignInViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onSignedIn: () -> Unit = {},
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
      containerColor = Color.White,
      topBar = {
        val stage = uiState.onBoardingStage
        if (stage != null) {
          Column(
              modifier = Modifier.fillMaxWidth().padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            val currentStep = stage.ordinal
            val maxSteps = OnBoardingStage.entries.size - 1
            val progress = currentStep.toFloat() / maxSteps
            ProgressBar(
                color = colorScheme.primary,
                trackColor = colorScheme.surface.copy(.6f),
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(10.dp),
            )
            Text(
                text = stringResource(R.string.step, currentStep, maxSteps),
                style = typography.titleMedium,
                modifier = Modifier.align(Alignment.Start),
                color = colorScheme.primary,
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
          AnimatedContent(
              targetState = uiState.onBoardingStage,
              transitionSpec = {
                (if (initialState == null || targetState!! > initialState!!)
                        slideInHorizontally(animationSpec = tween(500)) { it } +
                            fadeIn(animationSpec = tween(500)) togetherWith
                            slideOutHorizontally(animationSpec = tween(500)) { -it } +
                                fadeOut(animationSpec = tween(500))
                    else
                        slideInHorizontally(animationSpec = tween(500)) { -it } +
                            fadeIn(animationSpec = tween(500)) togetherWith
                            slideOutHorizontally(animationSpec = tween(500)) { it } +
                                fadeOut(animationSpec = tween(500)))
                    .using(SizeTransform(clip = false))
              },
          ) { stage ->
            when (stage) {
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
              OnBoardingStage.COMPLETE -> {
                LaunchedEffect(Unit) { authViewModel.finishRegistration() }
                WelcomeScreen()
              }
              else ->
                  SignInContent(
                      isLoading = uiState.isLoading,
                      onSignedIn = { authViewModel.signIn(context, credentialManager) },
                      paddingValues = paddingValues,
                      isWaterFull = isWaterFull,
                      onFilled = { isWaterFull = true },
                  )
            }
          }
        }
      },
  )
}

@Composable
fun WelcomeScreen() {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {}
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

@Composable
fun NamingScreen(
    data: OnBoardingData,
    updateData: (OnBoardingData) -> Unit,
    onNext: () -> Unit,
    canProceed: Boolean,
    isLoading: Boolean,
) {

  Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text(
        text = stringResource(R.string.welcome),
        style = typography.displayMedium,
        color = colorScheme.onBackground,
    )

    Spacer(modifier = Modifier.height(32.dp))

    OutlinedTextField(
        value = data.name,
        onValueChange = { updateData(data.copy(name = it)) },
        label = { Text(stringResource(R.string.first_name)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = data.surname,
        onValueChange = { updateData(data.copy(surname = it)) },
        label = { Text(stringResource(R.string.last_name)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = data.username,
        onValueChange = { updateData(data.copy(username = it)) },
        label = { Text(stringResource(R.string.username)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        enabled = canProceed && !isLoading,
        onClick = onNext,
        modifier = Modifier.padding(start = 8.dp),
    ) {
      Text(stringResource(R.string.next))
    }
  }
}

@Composable
fun OptionalInfoScreen(
    data: OnBoardingData,
    updateData: (OnBoardingData) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean,
) {
  val pickImageLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { updateData(data.copy(profilePicture = it)) }
      }

  Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
      AsyncImage(
          model = data.profilePicture,
          contentDescription = "Profile picture",
          modifier =
              Modifier.width(96.dp)
                  .aspectRatio(1f)
                  .clip(CircleShape)
                  .border(1.dp, colorScheme.outline, CircleShape)
                  .clickable(onClick = { pickImageLauncher.launch("image/*") }),
          contentScale = ContentScale.Crop,
      )
      Icon(
          imageVector = Icons.Filled.Create,
          contentDescription = "Change profile picture",
          tint = colorScheme.onPrimary,
          modifier =
              Modifier.align(Alignment.TopEnd)
                  .size(20.dp)
                  .clip(CircleShape)
                  .background(colorScheme.secondary)
                  .padding(4.dp),
      )
    }
    Spacer(modifier = Modifier.height(32.dp))

    CountryDropdown(
        selectedCountry = data.country,
        onCountrySelected = { updateData(data.copy(country = it)) },
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = data.bio,
        onValueChange = { updateData(data.copy(bio = it)) },
        label = { Text(stringResource(R.string.bio)) },
        modifier = Modifier.fillMaxWidth().height(120.dp),
        maxLines = 4,
    )

    Spacer(modifier = Modifier.height(32.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      OutlinedButton(
          enabled = !isLoading,
          onClick = onBack,
          modifier = Modifier.weight(1f).padding(end = 8.dp),
      ) {
        Text(stringResource(R.string.back))
      }

      Button(
          enabled = !isLoading,
          onClick = onNext,
          modifier = Modifier.weight(1f).padding(start = 8.dp),
      ) {
        Text(stringResource(R.string.next))
      }
    }
  }
}

@Composable
fun UserTypeScreen(
    data: OnBoardingData,
    updateData: (OnBoardingData) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean,
) {

  Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text(
        text = stringResource(R.string.account_type),
        style = typography.displayMedium,
        color = colorScheme.onBackground,
    )

    Spacer(modifier = Modifier.height(32.dp))

    Checkbox(
        checked = data.userType == UserType.PROFESSIONAL,
        onCheckedChange = {
          if (it) updateData(data.copy(userType = UserType.PROFESSIONAL))
          else updateData(data.copy(userType = UserType.REGULAR))
        },
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(stringResource(R.string.user_type), style = typography.titleMedium)

    Spacer(modifier = Modifier.height(32.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      OutlinedButton(
          enabled = !isLoading,
          onClick = onBack,
          modifier = Modifier.weight(1f).padding(end = 8.dp),
      ) {
        Text(stringResource(R.string.back))
      }

      Button(
          enabled = !isLoading,
          onClick = onNext,
          modifier = Modifier.weight(1f).padding(start = 8.dp),
      ) {
        Text(stringResource(R.string.complete))
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
