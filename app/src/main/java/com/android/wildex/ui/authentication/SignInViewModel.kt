package com.android.wildex.ui.authentication

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.AppTheme
import com.android.wildex.R
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.authentication.AuthRepository
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserSettingsRepository
import com.android.wildex.usecase.user.InitializeUserUseCase
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Represents the UI state for authentication.
 *
 * @property isLoading Whether an authentication operation is in progress.
 * @property errorMsg The error message to display, or null if there is no error.
 */
data class AuthUIState(
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
)

/**
 * ViewModel for the Sign-In view.
 *
 * @property authRepository The repository used to perform authentication operations.
 */
class SignInViewModel(
    private val authRepository: AuthRepository = RepositoryProvider.authRepository,
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val userSettingsRepository: UserSettingsRepository =
        RepositoryProvider.userSettingsRepository,
    private val initializeUserUseCase: InitializeUserUseCase = InitializeUserUseCase(),
) : ViewModel() {

  private val _uiState = MutableStateFlow(AuthUIState())
  val uiState: StateFlow<AuthUIState> = _uiState

  /** Clears the error message of the UI state. */
  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  /** Initiates the Google sign-in flow and updates the UI state on success or failure. */
  fun signIn(
      context: Context,
      credentialManager: CredentialManager,
      onSignedIn: (Boolean) -> Unit,
  ) {
    if (_uiState.value.isLoading) return

    _uiState.update { it.copy(isLoading = true, errorMsg = null) }
    viewModelScope.launch {
      val signInOptions =
          GetSignInWithGoogleOption.Builder(
                  serverClientId = context.getString(R.string.default_web_client_id))
              .build()
      val signInRequest = GetCredentialRequest.Builder().addCredentialOption(signInOptions).build()

      try {
        val credential = credentialManager.getCredential(context, signInRequest).credential

        authRepository
            .signInWithGoogle(credential)
            .fold(
                onSuccess = { firebaseUser ->
                  val userId = firebaseUser.uid
                  val isNewUser =
                      try {
                        userRepository.getUser(userId)
                        AppTheme.appearanceMode = userSettingsRepository.getAppearanceMode(userId)
                        false
                      } catch (_: Exception) {
                        initializeUserUseCase(userId)
                        AppTheme.appearanceMode = AppearanceMode.AUTOMATIC
                        true
                      }
                  _uiState.update { it.copy(isLoading = false, errorMsg = null) }
                  onSignedIn(isNewUser)
                },
                onFailure = { failure ->
                  _uiState.update {
                    it.copy(isLoading = false, errorMsg = failure.localizedMessage)
                  }
                },
            )
      } catch (_: GetCredentialCancellationException) {
        // User cancelled the sign-in operation
        _uiState.update {
          it.copy(
              isLoading = false,
              errorMsg = context.getString(R.string.cancel_sign_in),
          )
        }
      } catch (e: GetCredentialException) {
        // Other credential errors
        _uiState.update {
          it.copy(
              isLoading = false,
              errorMsg = "Failed to get credentials: ${e.localizedMessage}",
          )
        }
      } catch (e: Exception) {
        // Unexpected errors
        _uiState.update {
          it.copy(
              isLoading = false,
              errorMsg = "Unexpected error: ${e.localizedMessage}",
          )
        }
      }
    }
  }
}
