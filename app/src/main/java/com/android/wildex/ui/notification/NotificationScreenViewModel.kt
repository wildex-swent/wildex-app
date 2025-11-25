package com.android.wildex.ui.notification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val defaultSimpleUser =
    SimpleUser(
        userId = "VIBPCGUCWaaVw5cYwKkpBm4AvDA2",
        username = "defaultUsername",
        profilePictureURL =
            "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/users%2FVIBPCGUCWaaVw5cYwKkpBm4AvDA2.jpg?alt=media&token=c87cf885-89ef-4470-9272-bc76144b6c3f",
        userType = UserType.REGULAR,
    )
private val sampleNotifications =
    listOf(
        NotificationUIState(
            notificationId = "1",
            notificationType = NotificationType.LIKE,
            simpleUser = defaultSimpleUser,
            notificationTitle = "Jean has liked your post",
            notificationDescription = "3min ago"),
        NotificationUIState(
            notificationId = "2",
            notificationType = NotificationType.POST,
            simpleUser = defaultSimpleUser,
            notificationTitle = "Bob spotted a tiger",
            notificationDescription = "15min ago"),
        NotificationUIState(
            notificationId = "3",
            notificationType = NotificationType.COMMENT,
            simpleUser = defaultSimpleUser,
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
    val simpleUser: SimpleUser,
    val notificationType: NotificationType = NotificationType.POST,
    val notificationTitle: String = "",
    val notificationDescription: String = ""
)

class NotificationScreenViewModel() : ViewModel() {
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
      _uiState.value =
          _uiState.value.copy(
              // notifications = fetchedNotifications,
              isLoading = false,
              isRefreshing = false,
              errorMsg = null,
              isError = false,
          )
    } catch (e: Exception) {
      handleException("Error loading notifications", e)
      _uiState.value = _uiState.value.copy(isError = true, isLoading = false, isRefreshing = false)
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
