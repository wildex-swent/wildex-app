package com.android.wildex.ui.authentication

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.user.OnBoardingStage
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.user.UserFriendsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserSettingsRepository
import com.android.wildex.model.user.UserTokensRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.utils.CountryDropdownTestTags
import com.android.wildex.usecase.user.InitializeUserUseCase
import com.android.wildex.utils.FakeAuthRepository
import com.android.wildex.utils.FakeCredentialManager
import com.android.wildex.utils.FakeJwtGenerator
import com.android.wildex.utils.FirebaseEmulator
import com.android.wildex.utils.LocalRepositories
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock

class SignInScreenTest {
  private lateinit var auth: FirebaseAuth
  private lateinit var fakeCredentialManager: CredentialManager
  private lateinit var fakeRepository: FakeAuthRepository
  private lateinit var userRepository: UserRepository
  private lateinit var userSettingsRepository: UserSettingsRepository
  private lateinit var userTokensRepository: UserTokensRepository
  private lateinit var userAnimalsRepository: UserAnimalsRepository
  private lateinit var userAchievementsRepository: UserAchievementsRepository
  private lateinit var userFriendsRepository: UserFriendsRepository
  private lateinit var storageRepository: StorageRepository
  private lateinit var initializeUserUseCase: InitializeUserUseCase
  private lateinit var viewModel: SignInViewModel
  private lateinit var context: Context

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    assert(FirebaseEmulator.isRunning) {
      "FirebaseEmulator must be running before using FirestoreTest"
    }
    userRepository = LocalRepositories.userRepository
    userSettingsRepository = LocalRepositories.userSettingsRepository
    userAnimalsRepository = LocalRepositories.userAnimalsRepository
    userAchievementsRepository = LocalRepositories.userAchievementsRepository
    userFriendsRepository = LocalRepositories.userFriendsRepository
    userTokensRepository = LocalRepositories.userTokensRepository
    storageRepository = LocalRepositories.storageRepository
    initializeUserUseCase =
        InitializeUserUseCase(
            userSettingsRepository,
            userAnimalsRepository,
            userAchievementsRepository,
            userFriendsRepository,
            userTokensRepository,
        )
    fakeCredentialManager = FakeCredentialManager.create("fakeToken")
    fakeRepository = FakeAuthRepository()
    viewModel =
        SignInViewModel(
            fakeRepository,
            userRepository,
            userSettingsRepository,
            userTokensRepository,
            storageRepository,
            initializeUserUseCase,
        )
    auth = FirebaseEmulator.auth
    auth.signOut()
  }

  @After
  fun tearDown() {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.auth.signOut()
      FirebaseEmulator.clearAuthEmulator()
    }
    LocalRepositories.clearAll()
  }

  @Test
  fun signInScreenShowsLoginButtonWhenNoUserSignedIn() {
    composeTestRule.setContent {
      SignInScreen(authViewModel = viewModel, credentialManager = fakeCredentialManager)
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOADING_INDICATOR).assertIsNotDisplayed()
  }

  @Test
  fun appLogoIsAlwaysDisplayed() {
    composeTestRule.setContent {
      SignInScreen(authViewModel = viewModel, credentialManager = fakeCredentialManager)
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOADING_INDICATOR).assertIsNotDisplayed()
  }

  @Test
  fun loginButtonVisibleWhenUserIsNull() {
    composeTestRule.setContent {
      SignInScreen(authViewModel = viewModel, credentialManager = fakeCredentialManager)
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertExists()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.WELCOME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOADING_INDICATOR).assertIsNotDisplayed()
  }

  @Test
  fun googleSignInButtonClickTriggersOnClick() {
    var clicked = false
    composeTestRule.setContent {
      context = LocalContext.current
      GoogleSignInButton(onSignInClick = { clicked = true }, appearsOn = true)
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    assertTrue(clicked)
  }

  @Test
  fun initialTagsAreDisplayed() {
    composeTestRule.setContent { SignInScreen(authViewModel = viewModel) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun canSignInWithGoogle() {
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken("12345", email = "test@example.com")
    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)

    val fakeUser = mock(FirebaseUser::class.java)
    Mockito.`when`(fakeUser.uid).thenReturn("fake-uid")

    fakeRepository.signInResult = Result.success(fakeUser)

    composeTestRule.setContent {
      SignInScreen(authViewModel = viewModel, credentialManager = fakeCredentialManager)
    }

    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON)
        .assertIsDisplayed()
        .performClick()
  }

  @Test
  fun signInScreenIsCorrectlyDisplayed() {
    composeTestRule.setContent { SignInScreen() }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun canSignInWithExistingAccount() {
    val email = "existing@test.com"
    val fakeIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken(name = "Existing user", email = email)
    val firebaseCred = GoogleAuthProvider.getCredential(fakeIdToken, null)

    runBlocking {
      val user = FirebaseEmulator.auth.signInWithCredential(firebaseCred).await()
      assertNotNull(user)
      userRepository.addUser(
          User(
              userId = "fake-uid",
              username = "username",
              name = "name",
              surname = "surname",
              bio = "bio",
              profilePictureURL = "",
              userType = UserType.REGULAR,
              creationDate = Timestamp.now(),
              country = "country",
              onBoardingStage = OnBoardingStage.COMPLETE,
          ))
    }

    FirebaseEmulator.auth.signOut()

    val fakeUser = mock(FirebaseUser::class.java)
    Mockito.`when`(fakeUser.uid).thenReturn("fake-uid")
    Mockito.`when`(fakeUser.email).thenReturn(email)
    fakeRepository.signInResult = Result.success(fakeUser)

    composeTestRule.setContent {
      SignInScreen(authViewModel = viewModel, credentialManager = fakeCredentialManager)
    }

    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON)
        .assertIsDisplayed()
        .performClick()

    assertEquals(email, fakeUser.email)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun fullProfileCreation() = runTest {
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken("12345", email = "test@example.com")
    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)

    val fakeUser = mock(FirebaseUser::class.java)
    Mockito.`when`(fakeUser.uid).thenReturn("fake-uid")

    val delayedSignalForAuth = CompletableDeferred<Unit>()
    val delayedSignalForToken = CompletableDeferred<Unit>()
    val delayedAuthRepository =
        object : FakeAuthRepository() {
          override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> {
            delayedSignalForAuth.await()
            return signInResult ?: Result.failure(RuntimeException("No result set"))
          }
        }
    val delayedUserTokensRepository =
        object : LocalRepositories.UserTokensRepositoryImpl() {
          override suspend fun addTokenToUser(userId: Id, token: String) {
            delayedSignalForToken.await()
            super.addTokenToUser(userId, token)
          }
        }
    val delayedVM =
        SignInViewModel(
            delayedAuthRepository,
            userRepository,
            userSettingsRepository,
            delayedUserTokensRepository,
            storageRepository,
            initializeUserUseCase,
        )

    delayedAuthRepository.signInResult = Result.success(fakeUser)

    composeTestRule.setContent {
      SignInScreen(authViewModel = delayedVM, credentialManager = fakeCredentialManager)
    }
    composeTestRule.waitForIdle()
    composeTestRule.assertSignInContentIsDisplayed()

    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOADING_INDICATOR).assertIsDisplayed()
    delayedSignalForAuth.complete(Unit)

    advanceUntilIdle()
    composeTestRule.waitForIdle()

    composeTestRule.assertNamingScreenIsDisplayed()
    composeTestRule.onNodeWithTag(NamingScreenTestTags.NEXT_BUTTON).assertIsNotEnabled()
    composeTestRule.onNodeWithTag(NamingScreenTestTags.NAME_FIELD).performTextInput("Test name")
    composeTestRule
        .onNodeWithTag(NamingScreenTestTags.SURNAME_FIELD)
        .performTextInput("Test surname")
    composeTestRule
        .onNodeWithTag(NamingScreenTestTags.USERNAME_FIELD)
        .performTextInput("Test username")
    composeTestRule.onNodeWithTag(NamingScreenTestTags.NEXT_BUTTON).assertIsEnabled().performClick()

    advanceUntilIdle()
    composeTestRule.waitForIdle()

    composeTestRule.assertOptionalInfoScreenIsDisplayed()
    composeTestRule.onNodeWithTag(OptionalInfoScreenTestTags.BIO_FIELD).performTextInput("Test bio")
    composeTestRule
        .onNodeWithTag(OptionalInfoScreenTestTags.BACK_BUTTON)
        .assertIsEnabled()
        .performClick()
    composeTestRule.onNodeWithTag(NamingScreenTestTags.NEXT_BUTTON).assertIsEnabled().performClick()
    composeTestRule
        .onNodeWithTag(OptionalInfoScreenTestTags.NEXT_BUTTON)
        .assertIsEnabled()
        .performClick()

    advanceUntilIdle()
    composeTestRule.waitForIdle()

    composeTestRule.assertUserTypeScreenIsDisplayed()

    composeTestRule
        .onNodeWithTag(UserTypeScreenTestTags.buttonTestTag(UserType.PROFESSIONAL))
        .assertIsNotSelected()
        .performClick()
    composeTestRule
        .onNodeWithTag(UserTypeScreenTestTags.BACK_BUTTON)
        .assertIsEnabled()
        .performClick()
    composeTestRule
        .onNodeWithTag(OptionalInfoScreenTestTags.NEXT_BUTTON)
        .assertIsEnabled()
        .performClick()
    composeTestRule
        .onNodeWithTag(UserTypeScreenTestTags.COMPLETE_BUTTON)
        .assertIsEnabled()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.assertAwaitingCompleteScreenIsDisplayed()
    delayedSignalForToken.complete(Unit)
  }

  private fun ComposeTestRule.assertSignInContentIsDisplayed() {
    onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
    onNodeWithTag(SignInScreenTestTags.WELCOME).assertIsDisplayed()
    onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }

  private fun ComposeTestRule.assertNamingScreenIsDisplayed() {
    onNodeWithTag(NamingScreenTestTags.NAMING_SCREEN).assertIsDisplayed()
    onNodeWithTag(NamingScreenTestTags.WELCOME_TEXT).assertIsDisplayed()
    onNodeWithTag(NamingScreenTestTags.NAME_FIELD).assertIsDisplayed()
    onNodeWithTag(NamingScreenTestTags.SURNAME_FIELD).assertIsDisplayed()
    onNodeWithTag(NamingScreenTestTags.USERNAME_FIELD).assertIsDisplayed()
    onNodeWithTag(NamingScreenTestTags.NEXT_BUTTON).assertIsDisplayed()
  }

  private fun ComposeTestRule.assertOptionalInfoScreenIsDisplayed() {
    onNodeWithTag(OptionalInfoScreenTestTags.OPTIONAL_INFO_SCREEN).assertIsDisplayed()
    onNodeWithTag(OptionalInfoScreenTestTags.PROFILE_PICTURE).assertIsDisplayed()
    onNodeWithTag(CountryDropdownTestTags.COUNTRY_DROPDOWN).assertIsDisplayed()
    onNodeWithTag(OptionalInfoScreenTestTags.BIO_FIELD).assertIsDisplayed()
    onNodeWithTag(OptionalInfoScreenTestTags.NEXT_BUTTON).assertIsDisplayed()
    onNodeWithTag(OptionalInfoScreenTestTags.BACK_BUTTON).assertIsDisplayed()
  }

  private fun ComposeTestRule.assertUserTypeScreenIsDisplayed() {
    onNodeWithTag(UserTypeScreenTestTags.USER_TYPE_SCREEN).assertIsDisplayed()
    onNodeWithTag(UserTypeScreenTestTags.SELECT_TITLE).assertIsDisplayed()
    onNodeWithTag(UserTypeScreenTestTags.CHOICE_BUTTON_ROW).assertIsDisplayed()
    onNodeWithTag(UserTypeScreenTestTags.SELECT_INFO).assertIsDisplayed()
    onNodeWithTag(UserTypeScreenTestTags.SELECT_SETTING_INFO).assertIsDisplayed()
    UserType.entries.forEach {
      onNodeWithTag(UserTypeScreenTestTags.buttonTestTag(it)).assertIsDisplayed()
    }
    onNodeWithTag(UserTypeScreenTestTags.COMPLETE_BUTTON).assertIsDisplayed()
    onNodeWithTag(UserTypeScreenTestTags.BACK_BUTTON).assertIsDisplayed()
  }

  private fun ComposeTestRule.assertAwaitingCompleteScreenIsDisplayed() {
    onNodeWithTag(SignInScreenTestTags.WAITING_SCREEN).assertIsDisplayed()
    onNodeWithTag(SignInScreenTestTags.INITIALIZING_TITLE).assertIsDisplayed()
    onNodeWithTag(SignInScreenTestTags.INITIALIZING_ANIMATION).assertIsDisplayed()
  }
}
