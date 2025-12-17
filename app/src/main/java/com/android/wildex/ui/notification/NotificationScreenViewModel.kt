package com.android.wildex.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.notification.NotificationRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Id
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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
    val notificationId: String,
    val notificationRoute: String,
    val notificationTitle: String,
    val notificationDescription: String,
    val notificationRelativeTime: String,
    val notificationReadState: Boolean,
    val author: SimpleUser,
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

  private suspend fun updateUIState(calledFromRefresh: Boolean = false) {
    try {
      if (calledFromRefresh) {
        userRepository.refreshCache()
      }
      _uiState.value =
          _uiState.value.copy(
              notifications = fetchNotifications(),
              isLoading = false,
              isRefreshing = false,
              errorMsg = null,
              isError = false,
          )
    } catch (e: Exception) {
      setErrorMsg("Error loading notifications: ${e.localizedMessage}")
      _uiState.value = _uiState.value.copy(isError = true, isLoading = false, isRefreshing = false)
    }
  }

  private suspend fun fetchNotifications(): List<NotificationUIState> {
    val notif =
        notificationRepository.getAllNotificationsForUser(currentUserId).sortedByDescending {
          it.date
        }
    val notificationUIStates: List<NotificationUIState> =
        notif.mapNotNull {
          try {
            NotificationUIState(
                notificationId = it.notificationId,
                notificationRoute = it.route,
                notificationTitle = it.title,
                notificationDescription = it.body,
                notificationRelativeTime = getRelativeTime(it.date),
                notificationReadState = it.read,
                author = userRepository.getSimpleUser(it.authorId),
            )
          } catch (_: Exception) {
            null
          }
        }
    return notificationUIStates
  }

  fun refreshUIState() {
    _uiState.value = _uiState.value.copy(isRefreshing = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState(calledFromRefresh = true) }
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

  fun markAsRead(notificationId: Id) {
    viewModelScope.launch {
      val state = _uiState.value
      val notifications = state.notifications
      val newNotifications =
          notifications.map {
            if (it.notificationId == notificationId) {
              it.copy(notificationReadState = true)
            } else {
              it
            }
          }
      _uiState.value = state.copy(notifications = newNotifications)
      try {
        notificationRepository.markNotificationAsRead(notificationId)
      } catch (e: Exception) {
        setErrorMsg("Error marking notification as read : ${e.localizedMessage}")
        _uiState.value = state
      }
    }
  }

  fun markAllAsRead() {
    viewModelScope.launch {
      val state = _uiState.value
      val notifications = state.notifications
      val newNotifications = notifications.map { it.copy(notificationReadState = true) }
      _uiState.value = state.copy(notifications = newNotifications)
      try {
        notificationRepository.markAllNotificationsForUserAsRead(currentUserId)
      } catch (e: Exception) {
        setErrorMsg("Error marking all notifications as read : ${e.localizedMessage}")
        _uiState.value = state
      }
    }
  }

  fun clearNotification(notificationId: Id) {
    viewModelScope.launch {
      val state = _uiState.value
      val notifications = state.notifications
      val newNotifications = notifications.filter { it.notificationId != notificationId }
      _uiState.value = state.copy(notifications = newNotifications)
      try {
        notificationRepository.deleteNotification(notificationId)
      } catch (e: Exception) {
        setErrorMsg("Error clearing notification : ${e.localizedMessage}")
        _uiState.value = state
      }
    }
  }

  fun clearAllNotifications() {
    viewModelScope.launch {
      val state = _uiState.value
      _uiState.value = state.copy(notifications = emptyList())
      try {
        notificationRepository.deleteAllNotificationsForUser(currentUserId)
      } catch (e: Exception) {
        setErrorMsg("Error clearing all notifications : ${e.localizedMessage}")
        _uiState.value = state
      }
    }
  }

  private fun getRelativeTime(timestamp: Timestamp): String {
    val zone = ZoneId.systemDefault()
    val then = LocalDateTime.ofInstant(timestamp.toInstant(), zone)
    val now = LocalDateTime.now(zone)

    val years = ChronoUnit.YEARS.between(then, now)
    val months = ChronoUnit.MONTHS.between(then, now)
    val weeks = ChronoUnit.WEEKS.between(then, now)
    val days = ChronoUnit.DAYS.between(then, now)
    val hours = ChronoUnit.HOURS.between(then, now)
    val minutes = ChronoUnit.MINUTES.between(then, now)
    val seconds = ChronoUnit.SECONDS.between(then, now)

    return when {
      years > 0 -> formattedStamp(years, "year")
      months > 0 -> formattedStamp(months, "month")
      weeks > 0 -> formattedStamp(weeks, "week")
      days > 0 -> formattedStamp(days, "day")
      hours > 0 -> formattedStamp(hours, "hour")
      minutes > 0 -> formattedStamp(minutes, "minute")
      seconds < 5 -> "now"
      else -> formattedStamp(seconds, "second")
    }
  }

  private fun formattedStamp(value: Long, magnitude: String): String {
    return "$value $magnitude${if (value > 1) "s" else ""} ago"
  }
}
