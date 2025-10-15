package com.android.wildex.model.authentication

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.os.bundleOf
import androidx.credentials.CustomCredential
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.WildexApp
import com.android.wildex.ui.authentication.SignInScreen
import com.android.wildex.ui.authentication.SignInScreenTestTags
import com.android.wildex.utils.FakeCredentialManager
import com.android.wildex.utils.FakeJwtGenerator
import com.android.wildex.utils.FirebaseEmulator
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

const val UI_WAIT_TIMEOUT = 5_000L

@RunWith(AndroidJUnit4::class)
class AuthRepositoryFirebaseTest {
  private lateinit var auth: FirebaseAuth
  private lateinit var repository: AuthRepositoryFirebase
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockRepository: AuthRepositoryFirebase

  @get:Rule val composeTestRule = createComposeRule()

  init {
    assert(FirebaseEmulator.isRunning) {
      "FirebaseEmulator must be running before using FirestoreTest"
    }
  }

  @Before
  fun setUp() {
    auth = FirebaseEmulator.auth
    auth.signOut()
    repository = AuthRepositoryFirebase(auth)
    mockAuth = Mockito.mock(FirebaseAuth::class.java)
    mockRepository = AuthRepositoryFirebase(mockAuth)
  }

  @After
  fun tearDown() {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.auth.signOut()
      FirebaseEmulator.clearAuthEmulator()
    }
  }

  @Test
  fun googleSignInIsConfigured() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val resourceId =
        context.resources.getIdentifier("default_web_client_id", "string", context.packageName)

    assumeTrue("Google Sign-In not configured - skipping test", resourceId != 0)

    val clientId = context.getString(resourceId)

    assertTrue(
        "Invalid Google client ID format: $clientId", clientId.endsWith(".googleusercontent.com"))
  }

  @Test
  fun canSignInWithGoogle() {
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken("12345", email = "test@example.com")

    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)

    composeTestRule.setContent { WildexApp(credentialManager = fakeCredentialManager) }
    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onNodeWithTag(SignInScreenTestTags.WELCOME).isDisplayed()
    }
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

    composeTestRule.setContent {
      WildexApp(credentialManager = FakeCredentialManager.create(fakeIdToken))
    }

    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) { FirebaseEmulator.auth.currentUser != null }

    assertEquals(email, FirebaseEmulator.auth.currentUser!!.email)

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onNodeWithTag(SignInScreenTestTags.WELCOME).isDisplayed()
    }
  }

  @Test
  fun signOutSucceeds() {
    val result = repository.signOut()
    assertTrue(result.isSuccess)
  }

  @Test
  fun signInFailsWithWrongCredentialType() {
    val fakeCredential = CustomCredential("WRONG_TYPE", bundleOf())

    runTest {
      val result = repository.signInWithGoogle(fakeCredential)
      assertTrue(result.isFailure)
    }
  }

  @Test
  fun signInWithGoogleThrowsException() {
    runTest {
      val brokenCredential = CustomCredential(
        GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
        bundleOf()
      )
      whenever(mockAuth.signInWithCredential(any())).thenThrow(RuntimeException("Firebase exploded"))

      val result = mockRepository.signInWithGoogle(brokenCredential)
      assertTrue(result.isFailure)
      assertTrue(result.exceptionOrNull()!!.message!!.contains("Login failed"))
    }
  }

  @Test
  fun signInWithGoogleReturnsFailureWhenUserIsNull() {
    runTest {
      val credential = CustomCredential(
        GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
        bundleOf()
      )

      val fakeResult = Mockito.mock(AuthResult::class.java)
      whenever(fakeResult.user).thenReturn(null)
      val fakeTask = Tasks.forResult(fakeResult)

      whenever(mockAuth.signInWithCredential(any())).thenReturn(fakeTask)

      val result = mockRepository.signInWithGoogle(credential)

      assertTrue(result.isFailure)
      assertTrue(result.exceptionOrNull()?.message?.contains("Login failed") == true)
    }
  }

  @Test
  fun signOutThrowsExceptionReturnsFailure() {
    Mockito.doThrow(RuntimeException("Boom")).`when`(mockAuth).signOut()

    val result = mockRepository.signOut()

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()!!.message!!.contains("Logout failed"))
  }
}
