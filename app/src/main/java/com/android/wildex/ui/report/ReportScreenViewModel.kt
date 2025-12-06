package com.android.wildex.ui.report

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.report.Report
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the UI state of the report screen.
 *
 * @property reports A list of [ReportUIState] objects representing the reports to be displayed.
 * @property currentUser The [SimpleUser] object representing the current user.
 * @property errorMsg An optional error message to be displayed.
 * @property isLoading A boolean indicating whether the screen is currently loading.
 * @property isRefreshing A boolean indicating whether the screen is currently refreshing.
 * @property isError A boolean indicating whether an error has occurred.
 */
data class ReportScreenUIState(
    val reports: List<ReportUIState> = emptyList(),
    val currentUser: SimpleUser = defaultUser,
    val errorMsg: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isError: Boolean = false,
)

/**
 * Represents the UI state of a single report.
 *
 * @property reportId The ID of the report.
 * @property imageURL The URL of the image associated with the report.
 * @property location The location of the report.
 * @property date The date of the report.
 * @property description The description of the report.
 * @property author The [SimpleUser] object representing the author of the report.
 * @property assigneeUsername The ID of the assignee of the report.
 */
data class ReportUIState(
    val reportId: Id,
    val imageURL: URL,
    val location: String,
    val date: String,
    val description: String,
    val author: SimpleUser,
    val assigned: Boolean,
)

/** Default placeholder user used when no valid user is loaded. */
private val defaultUser: SimpleUser =
    SimpleUser(
        userId = "defaultUserId",
        username = "defaultUsername",
        profilePictureURL = "",
        userType = UserType.REGULAR,
    )

/**
 * ViewModel for the report screen. Loads the reports and user data and handles UI state updates
 * like canceling or assigning reports.
 *
 * @property reportRepository The repository for managing reports.
 * @property userRepository The repository for managing users.
 * @property currentUserId The ID of the current user.
 */
class ReportScreenViewModel(
    private val reportRepository: ReportRepository = RepositoryProvider.reportRepository,
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val currentUserId: Id =
        try {
          Firebase.auth.uid
        } catch (_: Exception) {
          defaultUser.userId
        } ?: defaultUser.userId,
) : ViewModel() {
  private val _uiState = MutableStateFlow(ReportScreenUIState())
  val uiState: StateFlow<ReportScreenUIState> = _uiState.asStateFlow()

  /** Initializes the UI state by loading reports and the current user. */
  fun loadUIState() {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState() }
  }

  /**
   * Updates the UI state by fetching reports and the current user. Updates [_uiState] with new
   * values.
   */
  private suspend fun updateUIState() {
    try {
      val currentUser =
          try {
            userRepository.getSimpleUser(currentUserId)
          } catch (e: Exception) {
            handleException("Error loading current user data", e)
            defaultUser
          }

      _uiState.value = _uiState.value.copy(currentUser = currentUser)

      val reports =
          try {
            if (currentUser.userType == UserType.PROFESSIONAL) reportRepository.getAllReports()
            else reportRepository.getAllReportsByAuthor(currentUserId)
          } catch (e: Exception) {
            handleException("Error loading reports", e)
            emptyList()
          }

      val reportUIStates = reportsToReportUIStates(reports)

      _uiState.value =
          _uiState.value.copy(
              reports = reportUIStates,
              isLoading = false,
              isRefreshing = false,
              isError = false,
          )
    } catch (e: Exception) {
      handleException("Failed to update UI state", e)
    }
  }

  /** Refreshes the UI state by updating reports and the current user. */
  fun refreshUIState() {
    _uiState.value = _uiState.value.copy(isRefreshing = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState() }
  }

  /** Converts reports to [ReportUIState] objects with author data. */
  private suspend fun reportsToReportUIStates(reports: List<Report>): List<ReportUIState> =
      coroutineScope {
        reports
            .map { report -> async { buildReportUiStateOrNull(report) } }
            .awaitAll()
            .filterNotNull()
      }

  /** Builds a [ReportUIState] for a single report or null if something goes wrong. */
  private suspend fun buildReportUiStateOrNull(report: Report): ReportUIState? {
    return try {
      val author = loadAuthorOrDefault(report)

      ReportUIState(
          reportId = report.reportId,
          imageURL = report.imageURL,
          location = report.location.generalName,
          date = formatDate(report.date),
          description = report.description,
          author = author,
          assigned = report.assigneeId != null,
      )
    } catch (e: Exception) {
      handleException("Error building UI state for report ${report.reportId}", e)
      null
    }
  }

  /** Loads the author for a report, falling back to [defaultUser] on failure. */
  private suspend fun loadAuthorOrDefault(report: Report): SimpleUser {
    return try {
      userRepository.getSimpleUser(report.authorId)
    } catch (e: Exception) {
      handleException(
          "Error loading author ${report.authorId} user data for report ${report.reportId}",
          e,
      )
      defaultUser
    }
  }

  /** Clears any existing error message from the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets a new error message in the UI state. */
  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  /** Shows an offline error message when trying to refresh while offline. */
  fun refreshOffline() {
    setErrorMsg("You are currently offline\nYou can not refresh for now :/")
  }

  /** Returns a formatted date string from a [Timestamp]. */
  private fun formatDate(ts: Timestamp): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(ts.toDate())
  }

  /** Handles internal exceptions and updates the UI state accordingly. */
  private fun handleException(message: String, e: Exception) {
    Log.e("ReportScreenViewModel", message, e)
    setErrorMsg("$message: ${e.message}")
    _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false, isError = true)
  }
}
