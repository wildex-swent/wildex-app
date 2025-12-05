package com.android.wildex.ui.notification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.notification.NotificationRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Id
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val notificationRoute: String = "",
    val notificationTitle: String = "",
    val notificationDescription: String = ""
)

class NotificationScreenViewModel(
    private val notificationRepository: NotificationRepository =
        RepositoryProvider.notificationRepository,
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val currentUserId: Id = Firebase.auth.uid ?: "",
) : ViewModel() {
  /** Backing property for the notification screen state. */
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
              notifications = fetchNotifications(),
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

  private suspend fun fetchNotifications(): List<NotificationUIState> {
    val notif = notificationRepository.getAllNotificationsForUser(currentUserId)
    val notificationUIStates: List<NotificationUIState> =
        notif.map { n ->
          NotificationUIState(
              notificationId = n.notificationId,
              simpleUser = userRepository.getSimpleUser(n.authorId),
              notificationRoute = n.route,
              notificationTitle = n.title,
              notificationDescription = n.body,
          )
        }
    return notificationUIStates
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

  /** Shows an offline error message when trying to refresh while offline. */
  fun refreshOffline() {
    setErrorMsg("You are currently offline\nYou can not refresh for now :/")
  }

  private fun handleException(message: String, e: Exception) {
    Log.e("NotificationScreenViewModel", message, e)
    setErrorMsg("$message: ${e.message}")
    _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false, isError = true)
  }
}
