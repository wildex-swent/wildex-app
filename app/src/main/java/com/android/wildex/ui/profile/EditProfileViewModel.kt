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

/**
 * UI state for the Edit Profile screen.
 *
 * Holds form fields, validation messages and upload / saving flags.
 */
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

/**
 * ViewModel for editing the current user's profile.
 *
 * Responsible for loading existing user data, validating fields and saving updated profile
 * information (including optional profile picture upload).
 */
class EditProfileViewModel(
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val storageRepository: StorageRepository = RepositoryProvider.storageRepository,
    private val currentUserId: Id = Firebase.auth.uid ?: "",
) : ViewModel() {
  private val _uiState = MutableStateFlow(EditProfileUIState())
  val uiState: StateFlow<EditProfileUIState> = _uiState.asStateFlow()

  private var usernameList: List<String> = emptyList()
  private var currentUserName: String = ""

  /** Loads initial UI state and caches existing usernames for validation. */
  fun loadUIState() {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)
    viewModelScope.launch {
      usernameList = userRepository.getAllUsers().map { it.username }
      currentUserName = userRepository.getUser(currentUserId).username
      updateUIState()
    }
  }

  /**
   * Suspends to fetch the current user and populate the form fields.
   *
   * Handles repository errors and updates error state accordingly.
   */
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

  /** Clears visible error. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  /**
   * Saves profile changes, optionally uploading a new profile image.
   *
   * Inline comments: this method may perform an upload to remote storage, build a new User object,
   * and call the repository to update the user. Errors set the error state.
   */
  fun saveProfileChanges() {
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
                // Attempt profile picture upload; fallback to existing URL on failure
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
                onBoardingStage = user.onBoardingStage,
            )
        // Persist user changes
        userRepository.editUser(userId = currentUserId, newUser = newUser)
        // Reset the pending Uri after successful upload
        clearErrorMsg()
        _uiState.value =
            _uiState.value.copy(isLoading = false, isError = false, profileSaved = true)
      } catch (e: Exception) {
        Log.e("EditProfileViewModel", "Error saving profile changes", e)
        setErrorMsg("Failed to save profile changes: ${e.message ?: "unknown"}")
        _uiState.value = _uiState.value.copy(isError = true, isLoading = false)
      }
    }
  }

  /** Update name with basic validation. */
  fun setName(name: String) {
    _uiState.value =
        _uiState.value.copy(
            name = name,
            invalidNameMsg = if (name.isBlank()) "Name cannot be empty" else null,
        )
  }

  /** Update surname with basic validation. */
  fun setSurname(surname: String) {
    _uiState.value =
        _uiState.value.copy(
            surname = surname,
            invalidSurnameMsg = if (surname.isBlank()) "Surname cannot be empty" else null,
        )
  }

  /** Update username with uniqueness and length validation. */
  fun setUsername(username: String) {
    _uiState.value =
        _uiState.value.copy(
            username = username,
            invalidUsernameMsg =
                when {
                  username.isBlank() -> "Username cannot be empty"
                  username.length > 20 -> "Username cannot exceed 20 characters"
                  usernameList.contains(username) && currentUserName != username ->
                      "Username is already taken"
                  else -> null
                })
  }

  /** Update description field. */
  fun setDescription(description: String) {
    _uiState.value = _uiState.value.copy(description = description)
  }

  /** Update country field. */
  fun setCountry(country: String) {
    _uiState.value = _uiState.value.copy(country = country)
  }

  /** Set a newly selected profile image URI (pending upload). */
  fun setNewProfileImageUri(uri: Uri?) {
    _uiState.value = _uiState.value.copy(pendingProfileImageUri = uri)
  }

  /** Clear the profileSaved flag after showing success. */
  fun clearProfileSaved() {
    _uiState.value = _uiState.value.copy(profileSaved = false)
  }
}
