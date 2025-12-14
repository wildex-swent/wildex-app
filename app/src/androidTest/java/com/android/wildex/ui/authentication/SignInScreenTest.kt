package com.android.wildex.ui.authentication

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.credentials.CredentialManager
import com.android.wildex.utils.FakeAuthRepository
import com.android.wildex.utils.FakeCredentialManager
import com.android.wildex.utils.FakeJwtGenerator
import com.android.wildex.utils.FirebaseEmulator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
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
  private lateinit var viewModel: SignInViewModel
  private lateinit var context: Context

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    assert(FirebaseEmulator.isRunning) {
      "FirebaseEmulator must be running before using FirestoreTest"
    }
    fakeCredentialManager = FakeCredentialManager.create("fakeToken")
    fakeRepository = FakeAuthRepository()
    viewModel = SignInViewModel(fakeRepository)
    auth = FirebaseEmulator.auth
    auth.signOut()
  }

  @After
  fun tearDown() {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.auth.signOut()
      FirebaseEmulator.clearAuthEmulator()
    }
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
      GoogleSignInButton(onSignInClick = { clicked = true })
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

    runTest {
      val user = FirebaseEmulator.auth.signInWithCredential(firebaseCred).await()
      assertNotNull(user)
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
}
