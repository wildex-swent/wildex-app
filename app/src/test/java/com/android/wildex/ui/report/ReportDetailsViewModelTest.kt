package com.android.wildex.ui.report

import com.android.wildex.model.report.Report
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.social.Comment
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.CommentTag
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Location
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportDetailsViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()
  private lateinit var reportRepository: ReportRepository
  private lateinit var userRepository: UserRepository
  private lateinit var commentRepository: CommentRepository
  private lateinit var viewModel: ReportDetailsScreenViewModel
  private val currentUserId = "current-user"
  private val authorId = "author-user"
  private val assigneeId = "assignee-user"
  private val reportId = "report-1"
  private val timestamp: Timestamp = Timestamp.now()

  private val report =
      Report(
          reportId = reportId,
          imageURL = "https://example.com/report_pic",
          location = Location(46.5, 6.5, "Lausanne"),
          date = timestamp,
          description = "Test report description",
          authorId = authorId,
          assigneeId = assigneeId,
      )

  private val currentUser =
      SimpleUser(
          userId = currentUserId,
          username = "currentUser",
          profilePictureURL = "currentPic",
          userType = UserType.PROFESSIONAL,
      )

  private val authorUser =
      SimpleUser(
          userId = authorId,
          username = "authorUser",
          profilePictureURL = "authorPic",
          userType = UserType.REGULAR,
      )

  private val assigneeUser =
      SimpleUser(
          userId = assigneeId,
          username = "assigneeUser",
          profilePictureURL = "assigneePic",
          userType = UserType.PROFESSIONAL,
      )

  private val defaultUser =
      SimpleUser(
          userId = "defaultUserId",
          username = "defaultUsername",
          profilePictureURL = "",
          userType = UserType.REGULAR,
      )

  @Before
  fun setUp() {
    reportRepository = mockk()
    userRepository = mockk()
    commentRepository = mockk()

    viewModel =
        ReportDetailsScreenViewModel(
            reportRepository = reportRepository,
            userRepository = userRepository,
            commentRepository = commentRepository,
            currentUserId = currentUserId,
        )

    coEvery { reportRepository.getReport(reportId) } returns report
    coEvery { reportRepository.refreshCache() } coAnswers {}
    coEvery { userRepository.getSimpleUser(currentUserId) } returns currentUser
    coEvery { userRepository.getSimpleUser(authorId) } returns authorUser
    coEvery { userRepository.getSimpleUser(assigneeId) } returns assigneeUser
    coEvery { userRepository.refreshCache() } coAnswers {}
    coEvery { commentRepository.getAllCommentsByReport(reportId) } returns emptyList()
    coEvery { commentRepository.deleteAllCommentsOfReport(any()) } just Runs
    coEvery { reportRepository.deleteReport(any()) } just Runs
    coEvery { reportRepository.editReport(any(), any()) } just Runs
    coEvery { commentRepository.getNewCommentId() } returns "comment-1"
    coEvery { commentRepository.addComment(any()) } just Runs
  }

  @Test
  fun viewModel_initializes_default_UI_state() {
    val state = viewModel.uiState.value
    assertEquals("", state.reportId)
    assertEquals("", state.imageURL)
    assertEquals(0.0, state.location.latitude, 0.0)
    assertEquals(0.0, state.location.longitude, 0.0)
    assertEquals("", state.date)
    assertEquals("", state.description)
    assertEquals(defaultUser, state.author)
    assertEquals(defaultUser, state.currentUser)
    assertNull(state.assignee)
    assertFalse(state.isCreatedByCurrentUser)
    assertFalse(state.isAssignedToCurrentUser)
    assertTrue(state.commentsUI.isEmpty())
    assertEquals(0, state.commentsCount)
    assertNull(state.errorMsg)
    assertFalse(state.isLoading)
    assertFalse(state.isRefreshing)
    assertFalse(state.isError)
  }

  @Test
  fun clearErrorMsg_sets_errorMsg_to_null() =
      mainDispatcherRule.runTest {
        coEvery { reportRepository.getReport(reportId) } throws Exception("Boom")
        viewModel.loadReportDetails(reportId)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMsg)
        viewModel.clearErrorMsg()
        assertNull(viewModel.uiState.value.errorMsg)
      }

  @Test
  fun loadReportDetails_success_updates_UI_state_correctly() =
      mainDispatcherRule.runTest {
        val okComment =
            Comment(
                commentId = "c1",
                parentId = reportId,
                authorId = authorId,
                text = "Nice job",
                date = timestamp,
                tag = CommentTag.REPORT_COMMENT,
            )
        val badAuthorId = "bad-author"
        val badComment =
            Comment(
                commentId = "c2",
                parentId = reportId,
                authorId = badAuthorId,
                text = "Should not appear",
                date = timestamp,
                tag = CommentTag.REPORT_COMMENT,
            )
        coEvery { commentRepository.getAllCommentsByReport(reportId) } returns
            listOf(okComment, badComment)
        coEvery { userRepository.getSimpleUser(badAuthorId) } throws Exception("no such user")
        viewModel.loadReportDetails(reportId)
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(reportId, state.reportId)
        assertEquals(report.imageURL, state.imageURL)
        assertEquals(report.location.latitude, state.location.latitude, 0.0)
        assertEquals(report.location.longitude, state.location.longitude, 0.0)
        assertEquals(report.description, state.description)
        assertEquals(authorUser, state.author)
        assertEquals(assigneeUser, state.assignee)
        assertEquals(currentUser, state.currentUser)
        assertEquals(1, state.commentsCount)
        assertEquals(1, state.commentsUI.size)
        assertEquals("Nice job", state.commentsUI.first().text)
        assertNull(state.errorMsg)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertFalse(state.isError)
      }

  @Test
  fun refreshReportDetails_sets_refreshing_and_updates_state() =
      mainDispatcherRule.runTest {
        viewModel.refreshReportDetails(reportId)
        assertTrue(viewModel.uiState.value.isRefreshing)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(reportId, state.reportId)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertFalse(state.isError)
      }

  @Test
  fun loadReportDetails_fatal_error_sets_error_state() =
      mainDispatcherRule.runTest {
        coEvery { reportRepository.getReport(reportId) } throws Exception("DB down")
        viewModel.loadReportDetails(reportId)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.isError)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNotNull(state.errorMsg)
        assertTrue(state.errorMsg!!.contains("Error loading report details for report"))
      }

  @Test
  fun loadReportDetails_non_fatal_author_error_uses_default_author_and_sets_errorMsg() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getSimpleUser(authorId) } throws Exception("not found")
        viewModel.loadReportDetails(reportId)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("defaultUserId", state.author.userId)
        assertNotNull(state.errorMsg)
        assertTrue(state.errorMsg!!.contains("Failed to load author information"))
        assertFalse(state.isError)
      }

  @Test
  fun loadReportDetails_non_fatal_assignee_and_comments_errors_set_errorMsg_and_keep_state_usable() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getSimpleUser(assigneeId) } throws Exception("assignee error")
        coEvery { commentRepository.getAllCommentsByReport(reportId) } throws
            Exception("comment error")
        viewModel.loadReportDetails(reportId)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertNull(state.assignee)
        assertTrue(state.commentsUI.isEmpty())
        assertEquals(0, state.commentsCount)
        assertNotNull(state.errorMsg)
        val msg = state.errorMsg!!
        assertTrue(msg.contains("Failed to load assignee information"))
        assertTrue(msg.contains("Failed to load comments"))
        assertFalse(state.isError)
      }

  @Test
  fun cancelReport_deletes_report_and_comments_and_emits_canceled_event() =
      mainDispatcherRule.runTest {
        viewModel.loadReportDetails(reportId)
        advanceUntilIdle()
        val eventDeferred = async { viewModel.events.first() }
        viewModel.cancelReport()
        advanceUntilIdle()
        coVerify { reportRepository.deleteReport(reportId) }
        coVerify { commentRepository.deleteAllCommentsOfReport(reportId) }
        val event = eventDeferred.await()
        assertTrue(event is ReportDetailsEvent.ShowCompletion)
        assertEquals(
            ReportCompletionType.CANCELED,
            (event as ReportDetailsEvent.ShowCompletion).type,
        )
        coEvery { reportRepository.deleteReport(reportId) } throws Exception("cancel fail")
        viewModel.cancelReport()
        advanceUntilIdle()
        val stateAfterError = viewModel.uiState.value
        assertTrue(stateAfterError.isError)
        assertNotNull(stateAfterError.errorMsg)
        assertTrue(stateAfterError.errorMsg!!.contains("Error canceling report"))
      }

  @Test
  fun resolveReport_deletes_report_and_comments_and_emits_resolved_event() =
      mainDispatcherRule.runTest {
        viewModel.loadReportDetails(reportId)
        advanceUntilIdle()
        val eventDeferred = async { viewModel.events.first() }
        viewModel.resolveReport()
        advanceUntilIdle()
        coVerify { reportRepository.deleteReport(reportId) }
        coVerify { commentRepository.deleteAllCommentsOfReport(reportId) }
        val event = eventDeferred.await()
        assertTrue(event is ReportDetailsEvent.ShowCompletion)
        assertEquals(
            ReportCompletionType.RESOLVED,
            (event as ReportDetailsEvent.ShowCompletion).type,
        )
        coEvery { reportRepository.deleteReport(reportId) } throws Exception("resolve fail")
        viewModel.resolveReport()
        advanceUntilIdle()
        val stateAfterError = viewModel.uiState.value
        assertTrue(stateAfterError.isError)
        assertNotNull(stateAfterError.errorMsg)
        assertTrue(stateAfterError.errorMsg!!.contains("Error resolving report"))
      }

  @Test
  fun selfAssignReport_sets_assignee_to_current_user() =
      mainDispatcherRule.runTest {
        viewModel.loadReportDetails(reportId)
        advanceUntilIdle()
        viewModel.selfAssignReport()
        advanceUntilIdle()
        coVerify {
          reportRepository.editReport(
              reportId,
              report.copy(assigneeId = currentUserId),
          )
        }
        coEvery { reportRepository.getReport(reportId) } throws Exception("self-assign fail")
        viewModel.selfAssignReport()
        advanceUntilIdle()
        val stateAfterError = viewModel.uiState.value
        assertTrue(stateAfterError.isError)
        assertNotNull(stateAfterError.errorMsg)
        assertTrue(stateAfterError.errorMsg!!.contains("Error self-assigning report"))
      }

  @Test
  fun unselfAssignReport_clears_assignee() =
      mainDispatcherRule.runTest {
        val reportWithCurrentAsAssignee = report.copy(assigneeId = currentUserId)
        coEvery { reportRepository.getReport(reportId) } returns reportWithCurrentAsAssignee
        viewModel.loadReportDetails(reportId)
        advanceUntilIdle()
        viewModel.unselfAssignReport()
        advanceUntilIdle()
        coVerify {
          reportRepository.editReport(reportId, reportWithCurrentAsAssignee.copy(assigneeId = null))
        }
        coEvery { reportRepository.getReport(reportId) } throws Exception("unself-assign fail")
        viewModel.unselfAssignReport()
        advanceUntilIdle()
        val stateAfterError = viewModel.uiState.value
        assertTrue(stateAfterError.isError)
        assertNotNull(stateAfterError.errorMsg)
        assertTrue(stateAfterError.errorMsg!!.contains("Error unself-assigning report"))
      }

  @Test
  fun addComment_with_blank_text_does_nothing() =
      mainDispatcherRule.runTest {
        val before = viewModel.uiState.value
        viewModel.addComment("   ")
        advanceUntilIdle()
        val after = viewModel.uiState.value
        assertEquals(before.commentsCount, after.commentsCount)
        assertEquals(before.commentsUI.size, after.commentsUI.size)
        coVerify(exactly = 0) { commentRepository.addComment(any()) }
      }

  @Test
  fun addComment_success_adds_optimistic_comment_and_persists_it() =
      mainDispatcherRule.runTest {
        viewModel.loadReportDetails(reportId)
        advanceUntilIdle()
        viewModel.addComment("Hello world")
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(1, state.commentsCount)
        assertEquals(1, state.commentsUI.size)
        assertEquals("Hello world", state.commentsUI.first().text)
        assertEquals(currentUserId, state.commentsUI.first().author.userId)
        coVerify { commentRepository.addComment(any()) }
      }

  @Test
  fun addComment_failure_rolls_back_and_sets_error() =
      mainDispatcherRule.runTest {
        viewModel.loadReportDetails(reportId)
        advanceUntilIdle()
        val before = viewModel.uiState.value
        coEvery { commentRepository.addComment(any()) } throws Exception("network error")
        viewModel.addComment("Hello world")
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(before.commentsCount, state.commentsCount)
        assertEquals(before.commentsUI.size, state.commentsUI.size)
        assertNotNull(state.errorMsg)
        assertTrue(state.errorMsg!!.contains("Failed to add comment"))
      }

  @Test
  fun refreshOffline_sets_error_report_details() {
    mainDispatcherRule.runTest {
      viewModel.loadReportDetails(reportId)
      advanceUntilIdle()
      val before = viewModel.uiState.value
      viewModel.refreshOffline()
      val after = viewModel.uiState.value
      assertNull(before.errorMsg)
      assertNotNull(after.errorMsg)
      assertTrue(
          after.errorMsg!!.contains("You are currently offline\nYou can not refresh for now :/"))
    }
  }
}
