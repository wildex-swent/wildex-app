package com.android.wildex.ui.authentication

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
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
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.user.OnBoardingStage
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserSettingsRepository
import com.android.wildex.model.user.UserTokensRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.usecase.user.InitializeUserUseCase
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUIState(
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val onBoardingStage: OnBoardingStage? = null,
    val onBoardingData: OnBoardingData = OnBoardingData(),
    val invalidNameMsg: String? = null,
    val invalidSurnameMsg: String? = null,
    val invalidUsernameMsg: String? = null,
)

data class OnBoardingData(
    val userId: Id = "",
    val name: String = "",
    val surname: String = "",
    val username: String = "",
    val profilePicture: Uri = "".toUri(),
    val country: String = "",
    val bio: String = "",
    val userType: UserType = UserType.REGULAR,
)

class SignInViewModel(
    private val authRepository: AuthRepository = RepositoryProvider.authRepository,
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val userSettingsRepository: UserSettingsRepository =
        RepositoryProvider.userSettingsRepository,
    private val userTokensRepository: UserTokensRepository =
        RepositoryProvider.userTokensRepository,
    private val storageRepository: StorageRepository = RepositoryProvider.storageRepository,
    private val initializeUserUseCase: InitializeUserUseCase = InitializeUserUseCase(),
) : ViewModel() {

  private val _uiState = MutableStateFlow(AuthUIState())
  val uiState: StateFlow<AuthUIState> = _uiState
  var usernameList: List<String> = emptyList()

  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  fun signIn(context: Context, credentialManager: CredentialManager) {
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
                  try {
                    val userId = firebaseUser.uid
                    val googlePhotoUrl = firebaseUser.photoUrl?.toString() ?: ""
                    updateUIState(userId, googlePhotoUrl)
                  } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, errorMsg = e.localizedMessage) }
                  }
                },
                onFailure = { failure ->
                  _uiState.update {
                    it.copy(isLoading = false, errorMsg = failure.localizedMessage)
                  }
                },
            )
      } catch (_: GetCredentialCancellationException) {
        _uiState.update {
          it.copy(
              isLoading = false,
              errorMsg = context.getString(R.string.cancel_sign_in),
          )
        }
      } catch (e: GetCredentialException) {
        _uiState.update {
          it.copy(
              isLoading = false,
              errorMsg = "Failed to get credentials: ${e.localizedMessage}",
          )
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              isLoading = false,
              errorMsg = "Unexpected error: ${e.localizedMessage}",
          )
        }
      }
    }
  }

  private suspend fun updateUIState(userId: String, googlePhotoUrl: String) {
    val user =
        try {
          userRepository.getUser(userId)
        } catch (_: Exception) {
          val user =
              User(
                  userId = userId,
                  username = "",
                  name = "",
                  surname = "",
                  bio = "",
                  profilePictureURL = googlePhotoUrl,
                  userType = UserType.REGULAR,
                  creationDate = Timestamp.now(),
                  country = "",
                  onBoardingStage = OnBoardingStage.NAMING,
              )
          userRepository.addUser(user)
          user
        }
    val stage =
        if (user.onBoardingStage == OnBoardingStage.COMPLETE) {
          AppTheme.appearanceMode = userSettingsRepository.getAppearanceMode(userId)
          null
        } else {
          AppTheme.appearanceMode = AppearanceMode.AUTOMATIC
          usernameList = userRepository.getAllUsers().map { it.username }
          OnBoardingStage.NAMING
        }
    _uiState.update {
      it.copy(
          isLoading = false,
          errorMsg = null,
          onBoardingStage = stage,
          onBoardingData =
              OnBoardingData(
                  userId = user.userId,
                  name = user.name,
                  surname = user.surname,
                  username = user.username,
                  profilePicture = user.profilePictureURL.toUri(),
                  country = user.country,
                  bio = user.bio,
                  userType = user.userType,
              ),
      )
    }
  }

  fun updateOnBoardingData(data: OnBoardingData) {
    _uiState.update {
      it.copy(
          onBoardingData = data,
          invalidNameMsg = if (data.name.isBlank()) "Name cannot be empty" else null,
          invalidSurnameMsg = if (data.surname.isBlank()) "Surname cannot be empty" else null,
          invalidUsernameMsg =
              when {
                data.username.isBlank() -> "Username cannot be empty"
                data.username.length > 20 -> "Username cannot exceed 20 characters"
                usernameList.contains(data.username) -> "Username is already taken"
                else -> null
              },
      )
    }
  }

  fun goToNextStage() {
    val currentStage = _uiState.value.onBoardingStage
    val nextStage = currentStage?.next() ?: OnBoardingStage.NAMING
    _uiState.update { it.copy(isLoading = true) }
    viewModelScope.launch {
      try {
        updateUser(nextStage)
        _uiState.update { it.copy(isLoading = false, onBoardingStage = nextStage) }
      } catch (e: Exception) {
        _uiState.update { it.copy(isLoading = false, errorMsg = e.localizedMessage) }
      }
    }
  }

  fun goToPreviousStage() {
    val currentStage = _uiState.value.onBoardingStage
    val previousStage = currentStage?.previous() ?: OnBoardingStage.NAMING
    _uiState.update { it.copy(onBoardingStage = previousStage) }
  }

  fun canProceedFromNaming(): Boolean {
    val data = _uiState.value.onBoardingData
    return data.name.isNotBlank() && data.surname.isNotBlank() && data.username.isNotBlank()
  }

  fun finishRegistration() {
    _uiState.update { it.copy(isLoading = true, onBoardingStage = OnBoardingStage.COMPLETE) }
    viewModelScope.launch {
      delay(2000)
      try {
        val userId = _uiState.value.onBoardingData.userId
        initializeUserUseCase(userId)
        userTokensRepository.addTokenToUser(userId, userTokensRepository.getCurrentToken())
        updateUser(OnBoardingStage.COMPLETE)
        _uiState.update { it.copy(isLoading = false) }
      } catch (e: Exception) {
        _uiState.update { it.copy(isLoading = false, errorMsg = e.localizedMessage) }
      }
    }
  }

  private suspend fun updateUser(newStage: OnBoardingStage) {
    val data = _uiState.value.onBoardingData
    val pictureUri = data.profilePicture
    val profilePictureUrl =
        try {
          if (!pictureUri.scheme.isNullOrBlank() && !pictureUri.scheme.equals("https"))
              storageRepository.uploadUserProfilePicture(data.userId, pictureUri)
          else data.profilePicture.toString()
        } catch (e: Exception) {
          data.profilePicture.toString()
        }

    val user =
        User(
            userId = data.userId,
            username = data.username,
            name = data.name,
            surname = data.surname,
            bio = data.bio,
            profilePictureURL = profilePictureUrl,
            userType = data.userType,
            creationDate = Timestamp.now(),
            country = data.country,
            onBoardingStage = newStage,
        )
    userRepository.editUser(user.userId, user)
  }
}
