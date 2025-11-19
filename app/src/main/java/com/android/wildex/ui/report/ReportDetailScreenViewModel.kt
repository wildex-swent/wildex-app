package com.android.wildex.ui.report

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.social.Comment
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.CommentTag
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.android.wildex.model.utils.URL
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Type of completion used to distinguish between a canceled and a resolved report when showing the
 * completion popup.
 */
enum class ReportCompletionType {
  CANCELED,
  RESOLVED,
}

/** One-shot UI events emitted by [ReportDetailsScreenViewModel]. */
sealed interface ReportDetailsEvent {
  /**
   * Requests the UI to show a completion popup after a report is deleted.
   *
   * @property type Whether the report was canceled or resolved.
   */
  data class ShowCompletion(val type: ReportCompletionType) : ReportDetailsEvent
}

/**
 * Represents a single comment on a report enriched with author data for the UI.
 * *
 *
 * @property author the comment's author.
 * @property text Comment text.
 * @property date Formatted date string for display.
 */
data class ReportCommentWithAuthorUI(
    val author: SimpleUser = defaultSimpleUser,
    val text: String = "",
    val date: String = "",
)

/** Represents the UI state of the report details screen. */
data class ReportDetailsUIState(
    val reportId: Id = "",
    val imageURL: URL = "",
    val location: Location = Location(latitude = 0.0, longitude = 0.0),
    val date: String = "",
    val description: String = "",
    val author: SimpleUser = defaultSimpleUser,
    val assignee: SimpleUser? = null,
    val currentUser: SimpleUser = defaultSimpleUser,
    val isCreatedByCurrentUser: Boolean = false,
    val isAssignedToCurrentUser: Boolean = false,
    val commentsUI: List<ReportCommentWithAuthorUI> = emptyList(),
    val commentsCount: Int = 0,
    val errorMsg: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isError: Boolean = false,
)

/** Default placeholder user used when no valid user is loaded. */
private val defaultSimpleUser =
    SimpleUser(
        userId = "defaultUserId",
        username = "defaultUsername",
        profilePictureURL = "",
        userType = UserType.REGULAR)

/**
 * ViewModel for the report detail screen.
 *
 * @param reportRepository Repository used to load and mutate reports.
 * @param userRepository Repository used to load user and simple user data.
 * @param commentRepository Repository used to load and delete comments on reports.
 * @param currentUserId ID of the currently authenticated user.
 */
class ReportDetailsScreenViewModel(
    private val reportRepository: ReportRepository = RepositoryProvider.reportRepository,
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val commentRepository: CommentRepository = RepositoryProvider.commentRepository,
    private val currentUserId: Id = Firebase.auth.uid ?: "",
) : ViewModel() {

  /** Backing mutable state for the UI. */
  private val _uiState = MutableStateFlow(ReportDetailsUIState())
  val uiState: StateFlow<ReportDetailsUIState> = _uiState.asStateFlow()

  /** Backing event flow for one-shot UI events. */
  private val _events = MutableSharedFlow<ReportDetailsEvent>()
  val events: SharedFlow<ReportDetailsEvent> = _events.asSharedFlow()

  /** Clears the current error message, if any. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Loads report details.
   *
   * @param reportId ID of the report whose details should be loaded.
   */
  fun loadReportDetails(reportId: Id) {
    _uiState.value =
        _uiState.value.copy(
            isLoading = true,
            errorMsg = null,
            isError = false,
        )
    viewModelScope.launch { updateReportDetails(reportId) }
  }

  /**
   * Refreshes report details.
   *
   * @param reportId ID of the report whose details should be refreshed.
   */
  fun refreshReportDetails(reportId: Id) {
    _uiState.value =
        _uiState.value.copy(
            isRefreshing = true,
            errorMsg = null,
            isError = false,
        )
    viewModelScope.launch { updateReportDetails(reportId) }
  }

  /**
   * Fetches the report, its author, assignee, comments and current user data, then updates the UI
   * state.
   *
   * @param reportId ID of the report whose details are being resolved.
   */
  private suspend fun updateReportDetails(reportId: Id) {
    try {
      // ------ Fatal errors ------
      val report = reportRepository.getReport(reportId)
      val currentUser = userRepository.getSimpleUser(currentUserId)

      // ------ Non Fatal errors ------

      // Sooo since these don't break the code, I thought maybe we could collect all errors
      // and show them at once at the end
      var localErrorMsg: String? = null
      fun appendError(msg: String) {
        localErrorMsg = if (localErrorMsg == null) msg else "${localErrorMsg}\n$msg"
      }

      val author =
          try {
            userRepository.getSimpleUser(report.authorId)
          } catch (_: Exception) {
            appendError("Failed to load author information.")
            defaultSimpleUser
          }

      val assigneeId = report.assigneeId
      val assignee: SimpleUser? =
          assigneeId?.let { id ->
            try {
              userRepository.getSimpleUser(id)
            } catch (_: Exception) {
              appendError("Failed to load assignee information.")
              null
            }
          }

      val isCreatedByCurrentUser = report.authorId == currentUserId
      val isAssignedToCurrentUser = assigneeId == currentUserId
      val commentsUI =
          try {
            val raw =
                commentRepository.getAllCommentsByReport(reportId).sortedByDescending { it.date }
            commentsToCommentsUI(raw)
          } catch (_: Exception) {
            appendError("Failed to load comments")
            emptyList()
          }

      _uiState.value =
          ReportDetailsUIState(
              reportId = report.reportId,
              imageURL = report.imageURL,
              location = report.location,
              date = formatDate(report.date),
              description = report.description,
              author = author,
              assignee = assignee,
              currentUser = currentUser,
              isCreatedByCurrentUser = isCreatedByCurrentUser,
              isAssignedToCurrentUser = isAssignedToCurrentUser,
              commentsUI = commentsUI,
              commentsCount = commentsUI.size,
              errorMsg = localErrorMsg,
              isLoading = false,
              isRefreshing = false,
              isError = false,
          )
    } catch (e: Exception) {
      handleException("Error loading report details for report $reportId", e)
    }
  }

  /** Cancels a report created by a regular user. */
  fun cancelReport() {
    val reportId = _uiState.value.reportId
    viewModelScope.launch {
      try {
        reportRepository.deleteReport(reportId)
        commentRepository.deleteAllCommentsOfReport(reportId)
        _events.emit(
            ReportDetailsEvent.ShowCompletion(ReportCompletionType.CANCELED),
        )
      } catch (e: Exception) {
        handleException("Error canceling report $reportId", e)
      }
    }
  }

  /** Self-assigns a report to the current user (for professionals). */
  fun selfAssignReport() {
    val reportId = _uiState.value.reportId
    viewModelScope.launch {
      try {
        val report = reportRepository.getReport(reportId)
        reportRepository.editReport(reportId, report.copy(assigneeId = currentUserId))
        updateReportDetails(reportId)
      } catch (e: Exception) {
        handleException("Error self-assigning report $reportId", e)
      }
    }
  }

  /** Unself-assigns a report currently assigned to the current user. */
  fun unselfAssignReport() {
    val reportId = _uiState.value.reportId
    viewModelScope.launch {
      try {
        val report = reportRepository.getReport(reportId)
        reportRepository.editReport(reportId, report.copy(assigneeId = null))
        updateReportDetails(reportId)
      } catch (e: Exception) {
        handleException("Error unself-assigning report $reportId", e)
      }
    }
  }

  /** Resolves a report by deleting it. */
  fun resolveReport() {
    val reportId = _uiState.value.reportId
    viewModelScope.launch {
      try {
        reportRepository.deleteReport(reportId)
        commentRepository.deleteAllCommentsOfReport(reportId)
        _events.emit(
            ReportDetailsEvent.ShowCompletion(ReportCompletionType.RESOLVED),
        )
      } catch (e: Exception) {
        handleException("Error resolving report $reportId", e)
      }
    }
  }

  /**
   * Formats a [Timestamp] into a user-friendly date string used in the UI.
   *
   * @param ts Timestamp to be formatted.
   * @return Formatted date string.
   */
  private fun formatDate(ts: Timestamp): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(ts.toDate())
  }

  /**
   * Maps raw comments from the repository to UI comments enriched with author info.
   *
   * @param comments List of raw comments to be transformed.
   * @return List of comments formatted for the UI.
   */
  private suspend fun commentsToCommentsUI(
      comments: List<Comment>
  ): List<ReportCommentWithAuthorUI> {
    return comments.mapNotNull { comment ->
      try {
        val author = userRepository.getSimpleUser(comment.authorId)
        ReportCommentWithAuthorUI(
            author = author,
            text = comment.text,
            date = formatDate(comment.date),
        )
      } catch (_: Exception) {
        null
      }
    }
  }

  /** Optimistic comment add with rollback on failure, similar to PostDetails. */
  fun addComment(text: String = "") {
    if (text.isBlank()) return
    viewModelScope.launch {
      val state = _uiState.value
      val reportId = state.reportId
      val now = Timestamp.now()
      val formattedNow = formatDate(now)
      val current = state.currentUser

      val optimistic =
          ReportCommentWithAuthorUI(
              author = current,
              text = text,
              date = formattedNow,
          )

      // 1) Optimistically prepend comment + increase count
      val before = _uiState.value
      _uiState.value =
          before.copy(
              commentsUI = listOf(optimistic) + before.commentsUI,
              commentsCount = before.commentsCount + 1,
          )

      try {
        // 2) Persist comment
        val commentId = commentRepository.getNewCommentId()
        commentRepository.addComment(
            Comment(
                commentId = commentId,
                parentId = reportId,
                authorId = current.userId,
                text = text,
                date = now,
                tag = CommentTag.REPORT_COMMENT,
            ),
        )
      } catch (e: Exception) {
        setErrorMsg("Failed to add comment: ${e.message}")
        val currentState = _uiState.value
        _uiState.value =
            currentState.copy(
                commentsUI = currentState.commentsUI.drop(1),
                commentsCount = (currentState.commentsCount - 1).coerceAtLeast(0),
            )
      }
    }
  }

  /**
   * Sets a new error message in the UI state.
   *
   * @param msg Error message to be stored in the state.
   */
  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  /**
   * Centralized error handler for fatal errors.
   *
   * @param message High-level description of where the error happened.
   * @param e The exception that was thrown.
   */
  private fun handleException(message: String, e: Exception) {
    Log.e("ReportDetailsScreenViewModel", message, e)
    setErrorMsg("$message: ${e.message}")
    _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false, isError = true)
  }
}
