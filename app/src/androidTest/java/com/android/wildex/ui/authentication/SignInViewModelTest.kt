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
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.authentication.AuthRepositoryFirebase
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserSettingsRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.usecase.user.InitializeUserUseCase
import com.android.wildex.utils.FakeCredentialManager
import com.android.wildex.utils.FirebaseEmulator
import com.android.wildex.utils.LocalRepositories
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SignInViewModelTest {

  private lateinit var context: Context
  private lateinit var authRepository: AuthRepositoryFirebase
  private lateinit var credentialManager: CredentialManager
  private lateinit var viewModel: SignInViewModel

  private lateinit var userRepository: UserRepository
  private lateinit var userAnimalsRepository: UserAnimalsRepository
  private lateinit var userAchievementsRepository: UserAchievementsRepository
  private lateinit var userSettingsRepository: UserSettingsRepository
  private val fakeUserIdToken = "fakeUserIdToken"
  private val testDispatcher = StandardTestDispatcher()

  init {
    assert(FirebaseEmulator.isRunning) {
      "FirebaseEmulator must be running before using FirestoreTest"
    }
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()
    userRepository = LocalRepositories.userRepository
    userAnimalsRepository = LocalRepositories.userAnimalsRepository
    userAchievementsRepository = LocalRepositories.userAchievementsRepository
    userSettingsRepository = LocalRepositories.userSettingsRepository
    val initializeUserUseCase =
        InitializeUserUseCase(
            userRepository,
            userSettingsRepository,
            userAnimalsRepository,
            userAchievementsRepository,
        )
    authRepository = mockk(relaxed = true)
    credentialManager = FakeCredentialManager.create("fakeToken")
    viewModel =
        SignInViewModel(
            authRepository,
            userRepository,
            userSettingsRepository,
            initializeUserUseCase,
        )
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    LocalRepositories.clearAll()
  }

  @Test
  fun signInSuccessUpdatesUIStateWithUsername_newUser() {
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
      coEvery { authRepository.signInWithGoogle(fakeCredential) } returns Result.success(fakeUser)

      viewModel.signIn(context, credentialManager) {}
      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertFalse(state.isLoading)
      assertNull(state.errorMsg)

      coVerify { authRepository.signInWithGoogle(fakeCredential) }
    }
  }

  @Test
  fun signInSuccessUpdatesUIStateWithUsername_oldUser() {
    runTest(timeout = 60.seconds) {
      val fakeUser = mock(FirebaseUser::class.java)
      Mockito.`when`(fakeUser.uid).thenReturn("fake-uid")

      runBlocking {
        val user =
            User(
                "fake-uid",
                "fake-username",
                "",
                "",
                "",
                "",
                UserType.REGULAR,
                Timestamp.now(),
                "",
            )

        userRepository.addUser(user)
        userAchievementsRepository.initializeUserAchievements(user.userId)
        userSettingsRepository.initializeUserSettings(user.userId)
        userAnimalsRepository.initializeUserAnimals(user.userId)
      }

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
      coEvery { authRepository.signInWithGoogle(fakeCredential) } returns Result.success(fakeUser)

      viewModel.signIn(context, credentialManager) {}
      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertFalse(state.isLoading)
      assertNull(state.errorMsg)

      coVerify { authRepository.signInWithGoogle(fakeCredential) }
    }
  }

  @Test
  fun signInCancelledUpdatesUIStateWithCancelMessage() = runTest {
    coEvery { credentialManager.getCredential(any(), any<GetCredentialRequest>()) } throws
        GetCredentialCancellationException("user canceled")

    viewModel.signIn(context, credentialManager) {}
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.errorMsg?.contains("cancel", ignoreCase = true) == true)
  }

  @Test
  fun signInFailsDueToCredentialErrorUpdatesErrorMsg() = runTest {
    coEvery { credentialManager.getCredential(any(), any<GetCredentialRequest>()) } throws
        GetCredentialUnknownException("bad credential")

    viewModel.signIn(context, credentialManager) {}
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.errorMsg?.contains("Failed to get credentials") == true)
  }

  @Test
  fun signInFailsDueToRepositoryErrorUpdatesUIState() = runTest {
    val fakeCredential =
        CustomCredential(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, bundleOf("id_token" to fakeUserIdToken))
    val response = mockk<GetCredentialResponse>()
    every { response.credential } returns fakeCredential
    coEvery { credentialManager.getCredential(any(), any<GetCredentialRequest>()) } returns response

    val fakeError = RuntimeException("auth failed")
    coEvery { authRepository.signInWithGoogle(fakeCredential) } returns Result.failure(fakeError)

    viewModel.signIn(context, credentialManager) {}
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.errorMsg?.contains("auth failed") == true)
  }

  @Test
  fun clearErrorMsgResetsError() = runTest {
    coEvery { credentialManager.getCredential(any(), any<GetCredentialRequest>()) } throws
        GetCredentialUnknownException("test error")

    viewModel.signIn(context, credentialManager) {}
    advanceUntilIdle()

    viewModel.clearErrorMsg()
    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
  }

  @Test
  fun signInHandlesUnexpectedException() {
    runTest {
      coEvery { credentialManager.getCredential(any(), any<GetCredentialRequest>()) } throws
          RuntimeException("unexpected crash")

      viewModel.signIn(context, credentialManager) {}
      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertFalse(state.isLoading)
      assertTrue(state.errorMsg?.contains("Unexpected error") == true)
    }
  }
}
