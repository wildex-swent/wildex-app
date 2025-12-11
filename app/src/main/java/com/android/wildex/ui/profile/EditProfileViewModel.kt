package com.android.wildex.ui.profile

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Id
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditProfileUIState(
    val name: String = "",
    val surname: String = "",
    val username: String = "",
    val description: String = "",
    val country: String = "",
    val profileSaved: Boolean = false,
    val pendingProfileImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val isError: Boolean = false,
    val invalidNameMsg: String? = null,
    val invalidSurnameMsg: String? = null,
    val invalidUsernameMsg: String? = null,
) {
  val isValid: Boolean
    get() =
        invalidNameMsg == null &&
            invalidSurnameMsg == null &&
            invalidUsernameMsg == null &&
            name.isNotBlank() &&
            surname.isNotBlank() &&
            username.isNotBlank()
}

class EditProfileViewModel(
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val storageRepository: StorageRepository = RepositoryProvider.storageRepository,
    private val currentUserId: Id = Firebase.auth.uid ?: "",
) : ViewModel() {
  private val _uiState = MutableStateFlow(EditProfileUIState())
  val uiState: StateFlow<EditProfileUIState> = _uiState.asStateFlow()

  private var usernameList: List<String> = emptyList()
  private var currentUserName: String = ""

  fun loadUIState() {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)
    viewModelScope.launch {
      usernameList = userRepository.getAllUsers().map { it.username }
      currentUserName = userRepository.getUser(currentUserId).username
      updateUIState()
    }
  }

  private suspend fun updateUIState() {
    try {
      val user = userRepository.getUser(currentUserId)
      _uiState.value =
          _uiState.value.copy(
              name = user.name,
              surname = user.surname,
              username = user.username,
              description = user.bio,
              country = user.country,
              pendingProfileImageUri =
                  if (!user.profilePictureURL.isBlank()) user.profilePictureURL.toUri() else null,
              isLoading = false,
              errorMsg = null,
              isError = false,
          )
    } catch (e: Exception) {
      Log.e("EditProfileScreenViewModel", "Error refreshing UI state", e)
      setErrorMsg("Unexpected error: ${e.message ?: "unknown"}")
      _uiState.value = _uiState.value.copy(isError = true, isLoading = false)
    }
  }

  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  fun saveProfileChanges(onSave: () -> Unit) {
    if (!_uiState.value.isValid) {
      setErrorMsg("At least one field is not valid")
      return
    }
    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(isLoading = true, isError = false)
        val user = userRepository.getUser(currentUserId)
        val newURL =
            _uiState.value.pendingProfileImageUri?.let {
              try {
                storageRepository.uploadUserProfilePicture(currentUserId, it)
              } catch (_: Exception) {
                user.profilePictureURL
              }
            } ?: user.profilePictureURL

        val newUser =
            User(
                userId = currentUserId,
                username = _uiState.value.username,
                name = _uiState.value.name,
                surname = _uiState.value.surname,
                bio = _uiState.value.description,
                profilePictureURL = newURL,
                userType = user.userType,
                creationDate = user.creationDate,
                country = _uiState.value.country,
            )
        userRepository.editUser(userId = currentUserId, newUser = newUser)
        // Reset the pending Uri after successful upload
        clearErrorMsg()
        _uiState.value =
            _uiState.value.copy(isLoading = false, isError = false, profileSaved = true)
        onSave()
      } catch (e: Exception) {
        Log.e("EditProfileViewModel", "Error saving profile changes", e)
        setErrorMsg("Failed to save profile changes: ${e.message ?: "unknown"}")
        _uiState.value = _uiState.value.copy(isError = true, isLoading = false)
      }
    }
  }

  fun setName(name: String) {
    _uiState.value =
        _uiState.value.copy(
            name = name,
            invalidNameMsg = if (name.isBlank()) "Name cannot be empty" else null,
        )
  }

  fun setSurname(surname: String) {
    _uiState.value =
        _uiState.value.copy(
            surname = surname,
            invalidSurnameMsg = if (surname.isBlank()) "Surname cannot be empty" else null,
        )
  }

  fun setUsername(username: String) {
    _uiState.value =
        _uiState.value.copy(
            username = username,
            invalidUsernameMsg = when {
                username.isBlank() -> "Username cannot be empty"
                username.length > 20 -> "Username cannot exceed 20 characters"
                usernameList.contains(username) && currentUserName != username -> "Username is already taken"
                else -> null
            }
        )
  }

  fun setDescription(description: String) {
    _uiState.value = _uiState.value.copy(description = description)
  }

  fun setCountry(country: String) {
    _uiState.value = _uiState.value.copy(country = country)
  }

  fun setNewProfileImageUri(uri: Uri?) {
    _uiState.value = _uiState.value.copy(pendingProfileImageUri = uri)
  }

  fun clearProfileSaved() {
    _uiState.value = _uiState.value.copy(profileSaved = false)
  }
}
