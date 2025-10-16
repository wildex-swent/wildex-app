package com.android.wildex.ui.authentication

import android.content.Context
import androidx.core.os.bundleOf
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.model.authentication.AuthRepositoryFirebase
import com.android.wildex.utils.FakeCredentialManager
import com.android.wildex.utils.FirebaseEmulator
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class SignInViewModelTest {

  private lateinit var context: Context
  private lateinit var repository: AuthRepositoryFirebase
  private lateinit var credentialManager: CredentialManager
  private lateinit var viewModel: SignInViewModel
  private val fakeUserIdToken = "fakeUserIdToken"
  private val testDispatcher = StandardTestDispatcher()

  init {
    assert(FirebaseEmulator.isRunning) {
      "FirebaseEmulator must be running before using FirestoreTest"
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()
    repository = mockk(relaxed = true)
    credentialManager = FakeCredentialManager.create("fakeToken")
    viewModel = SignInViewModel(repository)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun signInSuccessUpdatesUIStateWithUser() {
    runTest(timeout = 60.seconds) {
      val fakeUser = mock(FirebaseUser::class.java)
      Mockito.`when`(fakeUser.uid).thenReturn("fake-uid")

      val fakeCredential =
          CustomCredential(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, bundleOf("id_token" to fakeUserIdToken))
      val response = mockk<GetCredentialResponse>()
      every { response.credential } returns fakeCredential

      mockkObject(GoogleIdTokenCredential)
      val googleIdTokenCredential = mockk<GoogleIdTokenCredential>()
      every { googleIdTokenCredential.idToken } returns fakeUserIdToken
      every { GoogleIdTokenCredential.createFrom(any()) } returns googleIdTokenCredential

      coEvery { credentialManager.getCredential(any(), any<GetCredentialRequest>()) } returns
          response
      coEvery { repository.signInWithGoogle(fakeCredential) } returns Result.success(fakeUser)

      viewModel.signIn(context, credentialManager)
      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertFalse(state.isLoading)
      assertEquals(fakeUser, state.firebaseUser)
      assertNull(state.errorMsg)
      assertFalse(state.signedOut)

      coVerify { repository.signInWithGoogle(fakeCredential) }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun signInCancelledUpdatesUIStateWithCancelMessage() = runTest {
    coEvery { credentialManager.getCredential(any(), any<GetCredentialRequest>()) } throws
        GetCredentialCancellationException("user canceled")

    viewModel.signIn(context, credentialManager)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.firebaseUser)
    assertTrue(state.errorMsg?.contains("cancel", ignoreCase = true) == true)
    assertTrue(state.signedOut)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun signInFailsDueToCredentialErrorUpdatesErrorMsg() = runTest {
    coEvery { credentialManager.getCredential(any(), any<GetCredentialRequest>()) } throws
        GetCredentialUnknownException("bad credential")

    viewModel.signIn(context, credentialManager)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.firebaseUser)
    assertTrue(state.errorMsg?.contains("Failed to get credentials") == true)
    assertTrue(state.signedOut)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun signInFailsDueToRepositoryErrorUpdatesUIState() = runTest {
    val fakeCredential =
        CustomCredential(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, bundleOf("id_token" to fakeUserIdToken))
    val response = mockk<GetCredentialResponse>()
    every { response.credential } returns fakeCredential
    coEvery { credentialManager.getCredential(any(), any<GetCredentialRequest>()) } returns response

    val fakeError = RuntimeException("auth failed")
    coEvery { repository.signInWithGoogle(fakeCredential) } returns Result.failure(fakeError)

    viewModel.signIn(context, credentialManager)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.firebaseUser)
    assertTrue(state.errorMsg?.contains("auth failed") == true)
    assertTrue(state.signedOut)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun clearErrorMsgResetsError() = runTest {
    coEvery { credentialManager.getCredential(any(), any<GetCredentialRequest>()) } throws
        GetCredentialUnknownException("test error")

    viewModel.signIn(context, credentialManager)
    advanceUntilIdle()

    viewModel.clearErrorMsg()
    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun signInHandlesUnexpectedException() {
    runTest {
      coEvery { credentialManager.getCredential(any(), any<GetCredentialRequest>()) } throws
          RuntimeException("unexpected crash")

      viewModel.signIn(context, credentialManager)
      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertFalse(state.isLoading)
      assertNull(state.firebaseUser)
      assertTrue(state.errorMsg?.contains("Unexpected error") == true)
      assertTrue(state.signedOut)
    }
  }
}
