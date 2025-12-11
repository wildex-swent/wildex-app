package com.android.wildex.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.authentication.AuthRepository
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserSettingsRepository
import com.android.wildex.model.user.UserTokensRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.usecase.user.DeleteUserUseCase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUIState(
    val appearanceMode: AppearanceMode = AppearanceMode.AUTOMATIC,
    val notificationsEnabled: Boolean = true,
    val userType: UserType = UserType.REGULAR,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMsg: String? = null,
)

class SettingsScreenViewModel(
    private val authRepository: AuthRepository = RepositoryProvider.authRepository,
    private val userSettingsRepository: UserSettingsRepository =
        RepositoryProvider.userSettingsRepository,
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val userTokensRepository: UserTokensRepository =
        RepositoryProvider.userTokensRepository,
    private val currentUserId: Id = Firebase.auth.uid ?: "",
    private val deleteUserUseCase: DeleteUserUseCase = DeleteUserUseCase(),
) : ViewModel() {

  /** Backing property for the settings screen state. */
  private val _uiState = MutableStateFlow(SettingsUIState())

  /** Public immutable state exposed to the UI layer. */
  val uiState: StateFlow<SettingsUIState> = _uiState.asStateFlow()

  private suspend fun updateUIState() {
    try {
      val notificationsEnabled = userSettingsRepository.getEnableNotification(currentUserId)
      val appearanceMode = userSettingsRepository.getAppearanceMode(currentUserId)
      val userType = userRepository.getUser(currentUserId).userType
      _uiState.value =
          _uiState.value.copy(
              appearanceMode = appearanceMode,
              notificationsEnabled = notificationsEnabled,
              userType = userType,
              isLoading = false,
              errorMsg = null,
              isError = false,
          )
    } catch (e: Exception) {
      setErrorMsg(e.localizedMessage ?: "Failed to load settings.")
      _uiState.value = _uiState.value.copy(isLoading = false, isError = true)
    }
  }

  fun loadUIState() {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState() }
  }

  /**
   * Enables or disables notifications for the current user.
   *
   * @param enabled `true` to enable notifications, `false` to disable them.
   */
  fun setNotificationsEnabled(enabled: Boolean) {
    viewModelScope.launch {
      try {
        userSettingsRepository.setEnableNotification(currentUserId, enabled)
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
      } catch (e: Exception) {
        setErrorMsg(e.localizedMessage ?: "Failed to update notifications settings.")
      }
    }
  }

  /**
   * Sets the appearance mode for the current user.
   *
   * @param mode The desired [AppearanceMode] to set.
   */
  fun setAppearanceMode(mode: AppearanceMode) {
    viewModelScope.launch {
      try {
        userSettingsRepository.setAppearanceMode(currentUserId, mode)
        _uiState.value = _uiState.value.copy(appearanceMode = mode)
      } catch (e: Exception) {
        setErrorMsg(e.localizedMessage ?: "Failed to update appearance mode.")
      }
    }
  }

  /**
   * Sets the user type for the current user.
   *
   * @param type The desired [UserType] to set.
   */
  fun setUserType(type: UserType) {
    viewModelScope.launch {
      try {
        val user = userRepository.getUser(currentUserId)
        val updatedUser = user.copy(userType = type)
        userRepository.editUser(currentUserId, updatedUser)
        _uiState.value = _uiState.value.copy(userType = type)
      } catch (e: Exception) {
        setErrorMsg(e.localizedMessage ?: "Failed to update user type.")
      }
    }
  }

  /** Deletes the account of the current user */
  fun deleteAccount(onAccountDeleted: () -> Unit) {
    viewModelScope.launch {
      try {
        deleteUserUseCase(currentUserId)
        onAccountDeleted()
      } catch (e: Exception) {
        setErrorMsg(e.localizedMessage ?: "Failed to delete account.")
      }
    }
  }

  fun signOut(isOnline: Boolean, onSignOut: () -> Unit) {
    viewModelScope.launch {
      if (isOnline) {
        userTokensRepository.deleteTokenOfUser(
            currentUserId, userTokensRepository.getCurrentToken())
      }
      authRepository
          .signOut()
          .fold(
              onSuccess = { onSignOut() },
              onFailure = { setErrorMsg(it.localizedMessage ?: "Failed to sign out.") },
          )
    }
  }

  fun onOfflineClick() {
    setErrorMsg("This action is not supported offline. Check your connection and try again.")
  }

  /** Clears any existing error message from the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets a new error message in the UI state. */
  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }
}
