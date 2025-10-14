package com.android.wildex.ui.authentication

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
import com.android.wildex.model.authentication.AuthRepositoryFirebase
import com.android.wildex.utils.FakeCredentialManager
import com.android.wildex.utils.FakeJwtGenerator
import com.android.wildex.utils.FirebaseEmulator
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

const val UI_WAIT_TIMEOUT = 5_000L

@RunWith(AndroidJUnit4::class)
class AuthRepositoryFirebaseTest {
  private lateinit var auth: FirebaseAuth
  private lateinit var repository: AuthRepositoryFirebase

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
        .onNodeWithTag(SignInScreeTestTags.LOGIN_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onNodeWithTag(SignInScreeTestTags.WELCOME).isDisplayed()
    }
  }

  @Test
  fun signInScreenIsCorrectlyDisplayed() {
    composeTestRule.setContent { SignInScreen() }

    composeTestRule.onNodeWithTag(SignInScreeTestTags.APP_LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreeTestTags.LOGIN_BUTTON).assertIsDisplayed()
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
        .onNodeWithTag(SignInScreeTestTags.LOGIN_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) { FirebaseEmulator.auth.currentUser != null }

    assertEquals(email, FirebaseEmulator.auth.currentUser!!.email)

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onNodeWithTag(SignInScreeTestTags.WELCOME).isDisplayed()
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
}
