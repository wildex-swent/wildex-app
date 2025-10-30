package com.android.wildex.ui.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
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
    val profileImageUrl: URL = "",
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val isError: Boolean = false,
    val invalidNameMsg: String? = null,
    val invalidSurnameMsg: String? = null,
    val invalidUsernameMsg: String? = null,
    val invalidDescriptionMsg: String? = null
) {
    val isValid: Boolean
        get() =
            invalidNameMsg == null &&
                    invalidSurnameMsg == null &&
                    invalidUsernameMsg == null &&
                    invalidDescriptionMsg == null &&
                    name.isNotBlank() &&
                    surname.isNotBlank() &&
                    username.isNotBlank() &&
                    description.isNotBlank()
}

class EditProfileViewModel(
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val storageRepository: StorageRepository = RepositoryProvider.storageRepository,
    private val currentUserId: Id =
        try {
            Firebase.auth.uid
        } catch (_: Exception) {
            ""
        } ?: "",
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditProfileUIState())
    val uiState: StateFlow<EditProfileUIState> = _uiState.asStateFlow()

    fun loadUIState(userId: Id) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)
        viewModelScope.launch { updateUIState() }
    }

    private suspend fun updateUIState() {
        if (currentUserId.isBlank()) {
            setErrorMsg("Empty user id")
            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    isError = true,
                )
            return
        }
        try {
            val user = userRepository.getUser(currentUserId)
            _uiState.value =
                _uiState.value.copy(
                    name = user.name,
                    surname = user.surname,
                    username = user.username,
                    description = user.bio,
                    country = user.country,
                    profileImageUrl = user.profilePictureURL,
                    isLoading = false,
                    errorMsg = _uiState.value.errorMsg,
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

    fun saveProfileChanges(
        profileImageUri: Uri = Uri.EMPTY,
    ) {
        if (!_uiState.value.isValid) {
            setErrorMsg("At least one field is not valid")
            return
        }
        viewModelScope.launch {
            try {
                storageRepository.deleteUserProfilePicture(currentUserId)
                val newURL = storageRepository.uploadUserProfilePicture(currentUserId, profileImageUri)
                val user = userRepository.getUser(currentUserId)
                val newUser =
                    User(
                        userId = currentUserId,
                        username = _uiState.value.username,
                        name = _uiState.value.name,
                        surname = _uiState.value.surname,
                        bio = _uiState.value.description,
                        profilePictureURL =
                            try {
                                newURL
                            } catch (_: Exception) {
                                ""
                            } ?: "",
                        userType = user.userType,
                        creationDate = user.creationDate,
                        country = _uiState.value.country,
                        friendsCount = user.friendsCount,
                    )
                userRepository.editUser(
                    userId = currentUserId,
                    newUser = newUser,
                )
                clearErrorMsg()
            } catch (e: Exception) {
                Log.e("EditProfileViewModel", "Error saving profile changes", e)
                setErrorMsg("Failed to save profile changes: ${e.message ?: "unknown"}")
            }
        }
    }

    fun setName(name: String) {
        _uiState.value =
            _uiState.value.copy(
                name = name, invalidNameMsg = if (name.isBlank()) "Name cannot be empty" else null)
    }

    fun setSurname(surname: String) {
        _uiState.value =
            _uiState.value.copy(
                surname = surname,
                invalidSurnameMsg = if (surname.isBlank()) "Surname cannot be empty" else null)
    }

    fun setUsername(username: String) {
        _uiState.value =
            _uiState.value.copy(
                username = username,
                invalidUsernameMsg = if (username.isBlank()) "Username cannot be empty" else null)
    }

    fun setDescription(description: String) {
        _uiState.value =
            _uiState.value.copy(
                description = description,
                invalidNameMsg = if (description.isBlank()) "Bio cannot be empty" else null)
    }

    fun setNewProfileImageUrl(url: URL) {
        _uiState.value = _uiState.value.copy(profileImageUrl = url)
    }
}
