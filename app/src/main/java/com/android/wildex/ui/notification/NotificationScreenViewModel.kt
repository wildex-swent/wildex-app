package com.android.wildex.ui.notification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val defaultUser =
    User(
        "",
        "defaultUsername",
        "defaultName",
        "defaultSurname",
        "This is a default bio.",
        "https://static.vecteezy.com/system/resources/thumbnails/020/765/399/small/default-profile-account-unknown-icon-black-silhouette-free-vector.jpg",
        UserType.REGULAR,
        Timestamp.now(),
        "DefaultCountry",
        0,
    )
private val sampleNotifications =
    listOf(
        NotificationUIState(
            notificationId = "1",
            notificationType = NotificationType.LIKE,
            profilePictureUrl = defaultUser.profilePictureURL,
            userType = UserType.REGULAR,
            notificationTitle = "Jean has liked your post",
            notificationDescription = "3min ago"),
        NotificationUIState(
            notificationId = "2",
            notificationType = NotificationType.POST,
            profilePictureUrl = defaultUser.profilePictureURL,
            userType = UserType.REGULAR,
            notificationTitle = "Bob spotted a tiger",
            notificationDescription = "15min ago"),
        NotificationUIState(
            notificationId = "3",
            notificationType = NotificationType.COMMENT,
            profilePictureUrl = defaultUser.profilePictureURL,
            userType = UserType.REGULAR,
            notificationTitle = "Alice commented on your post",
            notificationDescription = "Alice said: Wow, amazing!",
        ))

enum class NotificationType {
  POST,
  REPORT,
  LIKE,
  COMMENT,
  FRIEND_REQUEST_RECEIVED,
  FRIEND_REQUEST_ACCEPTED,
}

data class NotificationScreenUIState(
    val notifications: List<NotificationUIState> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMsg: String? = null,
    val isError: Boolean = false,
)

data class NotificationUIState(
    val notificationId: String = "",
    val authorId: Id = "",
    val notificationType: NotificationType = NotificationType.POST,
    val profilePictureUrl: URL = defaultUser.profilePictureURL,
    val userType: UserType = defaultUser.userType,
    val notificationTitle: String = "",
    val notificationDescription: String = ""
)

class NotificationScreenViewModel(
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val currentUserId: Id = Firebase.auth.uid ?: "",
) : ViewModel() {
  /** Backing property for the home screen state. */
  private val _uiState = MutableStateFlow(NotificationScreenUIState())

  /** Public immutable state exposed to the UI layer. */
  val uiState: StateFlow<NotificationScreenUIState> = _uiState.asStateFlow()

  fun loadUIState() {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState() }
  }

  private suspend fun updateUIState() {
    try {
      /* To be implemented in the viewModel PR */
    } catch (e: Exception) {
      handleException("Error loading notifications", e)
    }
  }

  fun refreshUIState() {
    _uiState.value = _uiState.value.copy(isRefreshing = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState() }
  }

  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  private fun handleException(message: String, e: Exception) {
    Log.e("NotificationScreenViewModel", message, e)
    setErrorMsg("$message: ${e.message}")
    _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false, isError = true)
  }
}
