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

/**
 * UI state for the notification screen.
 *
 * Holds the list of notifications and simple loading / error flags.
 */
data class NotificationScreenUIState(
    val notifications: List<NotificationUIState> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMsg: String? = null,
    val isError: Boolean = false,
)

/** Presentation model for a single notification in the UI. */
data class NotificationUIState(
    val notificationId: String,
    val notificationRoute: String,
    val notificationTitle: String,
    val notificationDescription: String,
    val notificationRelativeTime: String,
    val notificationReadState: Boolean,
    val author: SimpleUser,
)

/**
 * ViewModel that loads and manages notifications for the current user.
 *
 * @param notificationRepository Repository used to read and update notification data.
 * @param userRepository Repository used to fetch author information.
 * @param currentUserId Current authenticated user id.
 */
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

  /**
   * Loads the UI state (initial load).
   *
   * This sets loading flags and launches a coroutine that delegates to [updateUIState].
   */
  fun loadUIState() {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState() }
  }

  /**
   * Suspend function that fetches notifications and updates the UI state.
   *
   * This performs repository calls and handles errors by updating the error-related state.
   *
   * @param calledFromRefresh If true, forces a repository cache refresh for user info first.
   */
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

  /**
   * Fetches and transforms repository notification models into UI models.
   *
   * Note: mapping includes fetching simple user info for each notification author; failures for
   * individual notifications are ignored (skipped) so UI shows the rest.
   */
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
            // ignore individual failures and omit that notification from the list
            null
          }
        }
    return notificationUIStates
  }

  /** Triggered by pull-to-refresh: forces repositories to refresh and reloades UI state. */
  fun refreshUIState() {
    _uiState.value = _uiState.value.copy(isRefreshing = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState(calledFromRefresh = true) }
  }

  /**
   * Set an error message in UI state.
   *
   * @param msg Error message to display.
   */
  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  /** Clear any visible error message. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Shows an offline error message when trying to refresh while offline. */
  fun refreshOffline() {
    setErrorMsg("You are currently offline\nYou can not refresh for now :/")
  }

  /**
   * Marks a single notification as read and refreshes state.
   *
   * Inline comment: this updates repository and reloads the whole UI; errors set the error state.
   */
  fun markAsRead(notificationId: Id) {
    _uiState.value = _uiState.value.copy(isLoading = true)
    viewModelScope.launch {
      try {
        notificationRepository.markNotificationAsRead(notificationId)
        updateUIState()
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                errorMsg = null,
                isError = false,
            )
      } catch (e: Exception) {
        setErrorMsg("Error marking notification as read : ${e.localizedMessage}")
        _uiState.value =
            _uiState.value.copy(isError = true, isLoading = false, isRefreshing = false)
      }
    }
  }

  /**
   * Marks all user notifications as read.
   *
   * Inline comment: performs a repository operation then refreshes UI; error handling mirrors
   * single-mark flow.
   */
  fun markAllAsRead() {
    _uiState.value = _uiState.value.copy(isLoading = true)
    viewModelScope.launch {
      try {
        notificationRepository.markAllNotificationsForUserAsRead(currentUserId)
        updateUIState()
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                errorMsg = null,
                isError = false,
            )
      } catch (e: Exception) {
        setErrorMsg("Error marking all notifications as read : ${e.localizedMessage}")
        _uiState.value =
            _uiState.value.copy(isError = true, isLoading = false, isRefreshing = false)
      }
    }
  }

  /** Deletes a single notification for the user and refreshes UI. */
  fun clearNotification(notificationId: Id) {
    _uiState.value = _uiState.value.copy(isLoading = true)
    viewModelScope.launch {
      try {
        notificationRepository.deleteNotification(notificationId)
        updateUIState()
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                errorMsg = null,
                isError = false,
            )
      } catch (e: Exception) {
        setErrorMsg("Error clearing notification : ${e.localizedMessage}")
        _uiState.value =
            _uiState.value.copy(isError = true, isLoading = false, isRefreshing = false)
      }
    }
  }

  /** Deletes all notifications for the current user and refreshes UI. */
  fun clearAllNotifications() {
    _uiState.value = _uiState.value.copy(isLoading = true)
    viewModelScope.launch {
      try {
        notificationRepository.deleteAllNotificationsForUser(currentUserId)
        updateUIState()
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                errorMsg = null,
                isError = false,
            )
      } catch (e: Exception) {
        setErrorMsg("Error clearing all notifications : ${e.localizedMessage}")
        _uiState.value =
            _uiState.value.copy(isError = true, isLoading = false, isRefreshing = false)
      }
    }
  }

  /**
   * Compute a human-friendly relative time string from a Firebase Timestamp.
   *
   * Inline comment: uses ChronoUnit differences from largest to smallest and returns the first
   * non-zero unit (years, months, weeks, days, hours, minutes, seconds).
   */
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
      years > 0 -> "$years years ago"
      months > 0 -> "$months months ago"
      weeks > 0 -> "$weeks weeks ago"
      days > 0 -> "$days days ago"
      hours > 0 -> "$hours hours ago"
      minutes > 0 -> "$minutes minutes ago"
      seconds < 5 -> "now"
      else -> "$seconds seconds ago"
    }
  }
}
