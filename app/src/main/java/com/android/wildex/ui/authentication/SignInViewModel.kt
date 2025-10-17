package com.android.wildex.ui.authentication

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.R
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.authentication.AuthRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Represents the UI state for authentication.
 *
 * @property isLoading Whether an authentication operation is in progress.
 * @property user The currently signed-in [FirebaseUser], or null if not signed in.
 * @property errorMsg The error message to display, or null if there is no error.
 * @property signedOut True if a sign-out operation has completed.
 */
data class AuthUIState(
    val isLoading: Boolean = false,
    val firebaseUser: FirebaseUser? = null,
    val errorMsg: String? = null,
    val signedOut: Boolean = false,
)

/**
 * ViewModel for the Sign-In view.
 *
 * @property repository The repository used to perform authentication operations.
 */
class SignInViewModel(
    private val repository: AuthRepository = RepositoryProvider.authRepository,
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val achievementRepository: UserAchievementsRepository =
        RepositoryProvider.userAchievementsRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(AuthUIState())
  val uiState: StateFlow<AuthUIState> = _uiState

  /** Clears the error message of the UI state. */
  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  /** Initiates the Google sign-in flow and updates the UI state on success or failure. */
  fun signIn(context: Context, credentialManager: CredentialManager) {
    if (_uiState.value.isLoading) return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMsg = null) }

      val signInOptions =
          GetSignInWithGoogleOption.Builder(
                  serverClientId = context.getString(R.string.default_web_client_id)
              )
              .build()
      val signInRequest = GetCredentialRequest.Builder().addCredentialOption(signInOptions).build()

      try {
        val credential = credentialManager.getCredential(context, signInRequest).credential

        repository.signInWithGoogle(credential).fold({ firebaseUser ->
          _uiState.update {
            it.copy(
                isLoading = false,
                firebaseUser = firebaseUser,
                errorMsg = null,
                signedOut = false,
            )
          }

          val userId = firebaseUser.uid
          try {
            userRepository.getUser(userId)
          } catch (_: Exception) {
            val newUser =
                User(
                    userId,
                    "jojo",
                    "John",
                    "Doe",
                    "I am John Doe",
                    "https://media.istockphoto.com/id/1223671392/vector/default-profile-picture-avatar-photo-placeholder-vector-illustration.jpg?s=612x612&w=0&k=20&c=s0aTdmT5aU6b8ot7VKm11DeID6NctRCpB755rA1BIP0=",
                    UserType.REGULAR,
                    Timestamp.now(),
                    "Switzerland",
                    0,
                )
            userRepository.addUser(newUser)
            achievementRepository.initializeUserAchievements(userId)
          }
        }) { failure ->
          _uiState.update {
            it.copy(
                isLoading = false,
                firebaseUser = null,
                errorMsg = failure.localizedMessage,
                signedOut = true,
            )
          }
        }
      } catch (e: GetCredentialCancellationException) {
        // User cancelled the sign-in operation
        _uiState.update {
          it.copy(
              isLoading = false,
              firebaseUser = null,
              errorMsg = context.getString(R.string.cancel_sign_in),
              signedOut = true,
          )
        }
      } catch (e: GetCredentialException) {
        // Other credential errors
        _uiState.update {
          it.copy(
              isLoading = false,
              firebaseUser = null,
              errorMsg = "Failed to get credentials: ${e.localizedMessage}",
              signedOut = true,
          )
        }
      } catch (e: Exception) {
        // Unexpected errors
        _uiState.update {
          it.copy(
              isLoading = false,
              firebaseUser = null,
              errorMsg = "Unexpected error: ${e.localizedMessage}",
              signedOut = true,
          )
        }
      }
    }
  }
}
