package com.android.wildex.ui.report

import com.android.wildex.model.report.Report
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Location
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ReportScreenViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var reportRepository: ReportRepository

  private lateinit var userRepository: UserRepository

  private lateinit var viewModel: ReportScreenViewModel

  private val report1 =
      Report(
          reportId = "report1",
          imageURL = "https://example.com/report_pic1",
          location = Location(50.0, 8.0, "Test Location 1", "Test Location 1", "Test Location 1"),
          date = Timestamp.now(),
          description = "Test report 1",
          authorId = "user1",
          assigneeId = "user3",
      )

  private val report2 =
      Report(
          reportId = "report2",
          imageURL = "https://example.com/report_pic2",
          location = Location(80.0, 10.0, "Test Location 2", "Test Location 2", "Test Location 2"),
          date = Timestamp.now(),
          description = "Test report 2",
          authorId = "user2",
          assigneeId = null,
      )

  private val report3 =
      Report(
          reportId = "report3",
          imageURL = "https://example.com/report_pic3",
          location =
              Location(100.0, 100.0, "Test Location 3", "Test Location 3", "Test Location 3"),
          date = Timestamp.now(),
          description = "Test report 3",
          authorId = "user2",
          assigneeId = "user3",
      )

  private val simpleUser1 =
      SimpleUser(
          userId = "user1",
          username = "user1name",
          profilePictureURL = "user1URL",
          userType = UserType.REGULAR)

  private val simpleUser2 =
      SimpleUser(
          userId = "user2",
          username = "user2name",
          profilePictureURL = "user2URL",
          userType = UserType.REGULAR)

  private val user2 =
      User(
          userId = "user2",
          username = "user2name",
          name = "Bob",
          surname = "Smith",
          bio = "This is my bob bio",
          profilePictureURL = "user2URL",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "France")

  private val simpleUser3 =
      SimpleUser(
          userId = "user3",
          username = "user3name",
          profilePictureURL = "user3URL",
          userType = UserType.PROFESSIONAL)

  private val user3 =
      User(
          userId = "user3",
          username = "user3name",
          name = "Mark",
          surname = "Adams",
          bio = "This is my Mark bio",
          profilePictureURL = "user3URL",
          userType = UserType.PROFESSIONAL,
          creationDate = Timestamp.now(),
          country = "Germany")

  private val defaultUser: SimpleUser =
      SimpleUser(
          userId = "defaultUserId",
          username = "defaultUsername",
          profilePictureURL = "",
          userType = UserType.REGULAR)

  @Before
  fun setUp() {
    reportRepository = mockk()
    userRepository = mockk()
    viewModel =
        ReportScreenViewModel(
            reportRepository = reportRepository,
            userRepository = userRepository,
            currentUserId = "user3")
    coEvery { userRepository.getSimpleUser("user1") } returns simpleUser1
    coEvery { userRepository.getSimpleUser("user2") } returns simpleUser2
    coEvery { userRepository.getSimpleUser("user3") } returns simpleUser3
    coEvery { userRepository.getUser("user2") } returns user2
    coEvery { userRepository.getUser("user3") } returns user3
    coEvery { reportRepository.getReport("report1") } returns report1
    coEvery { reportRepository.getReport("report2") } returns report2
    coEvery { reportRepository.getReport("report3") } returns report3
    coEvery { reportRepository.getAllReports() } returns listOf(report1, report2, report3)
    coEvery { reportRepository.deleteReport(any()) } just Runs
    coEvery { reportRepository.editReport(any(), any()) } just Runs
  }

  @Test
  fun viewModel_initializes_default_UI_state() {
    val initialState = viewModel.uiState.value
    assertTrue(initialState.reports.isEmpty())
    assertEquals(defaultUser, initialState.currentUser)
    assertNull(initialState.errorMsg)
    assertFalse(initialState.isLoading)
    assertFalse(initialState.isRefreshing)
    assertFalse(initialState.isError)
  }

  @Test
  fun loadUIState_updates_UI_state_correctly_for_currentUser2() {
    mainDispatcherRule.runTest {
      viewModel =
          ReportScreenViewModel(
              reportRepository = reportRepository,
              userRepository = userRepository,
              currentUserId = "user2")
      coEvery { reportRepository.getAllReportsByAuthor("user2") } returns listOf(report2, report3)

      viewModel.loadUIState()
      advanceUntilIdle()
      val state = viewModel.uiState.value

      val expectedReportIds = listOf("report2", "report3")
      val actualReportIds = state.reports.map { it.reportId }
      // user 2 is a regular user so he only sees his own reports
      assertEquals(expectedReportIds, actualReportIds)

      val expectedReportImageUrls =
          listOf("https://example.com/report_pic2", "https://example.com/report_pic3")
      val actualReportImageUrls = state.reports.map { it.imageURL }
      assertEquals(expectedReportImageUrls, actualReportImageUrls)

      val expectedReportLocations =
          listOf(
              Location(80.0, 10.0, "Test Location 2", "Test Location 2", "Test Location 2"),
              Location(100.0, 100.0, "Test Location 3", "Test Location 3", "Test Location 3"),
          )
      val actualReportLocations = state.reports.map { it.location }
      assertEquals(expectedReportLocations, actualReportLocations)

      val expectedReportDescriptions = listOf("Test report 2", "Test report 3")
      val actualReportDescriptions = state.reports.map { it.description }
      assertEquals(expectedReportDescriptions, actualReportDescriptions)

      val expectedReportAuthors = listOf(simpleUser2, simpleUser2)
      val actualReportAuthors = state.reports.map { it.author }
      assertEquals(expectedReportAuthors, actualReportAuthors)

      val expectedReportAssignees = listOf(false, true)
      val actualReportAssignees = state.reports.map { it.assigned }
      assertEquals(expectedReportAssignees, actualReportAssignees)

      assertEquals(simpleUser2, state.currentUser)
      assertNull(state.errorMsg)
      assertFalse(state.isLoading)
      assertFalse(state.isRefreshing)
      assertFalse(state.isError)
    }
  }

  @Test
  fun loadUIState_updates_UI_state_correctly_for_currentUser3() {
    mainDispatcherRule.runTest {
      viewModel.loadUIState()
      advanceUntilIdle()

      val state = viewModel.uiState.value
      val expectedReportIds = listOf("report1", "report2", "report3")
      val actualReportIds = state.reports.map { it.reportId }
      // user 3 is a professional user so he sees all reports
      assertEquals(expectedReportIds, actualReportIds)
      assertEquals(simpleUser3, state.currentUser)
      assertNull(state.errorMsg)
      assertFalse(state.isLoading)
      assertFalse(state.isRefreshing)
      assertFalse(state.isError)
    }
  }

  @Test
  fun clearErrorMsg_sets_errorMsg_to_null() =
      mainDispatcherRule.runTest {
        viewModel.loadUIState()
        viewModel.clearErrorMsg()
        assertNull(viewModel.uiState.value.errorMsg)
      }

  @Test
  fun loadUIState_sets_error_when_User_fails() {
    mainDispatcherRule.runTest {
      coEvery { userRepository.getSimpleUser("user3") } throws Exception("Error getting user")
      coEvery { reportRepository.getAllReportsByAuthor("user3") } returns emptyList()

      viewModel.loadUIState()
      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertEquals("Failed to update UI state: Error getting user", state.errorMsg)
    }
  }

  @Test
  fun loadUIState_sets_error_when_getReports_fails() {
    mainDispatcherRule.runTest {
      coEvery { reportRepository.getAllReports() } throws Exception("Failed to load reports")

      viewModel.loadUIState()
      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertEquals("Failed to update UI state: Failed to load reports", state.errorMsg)
    }
  }

  @Test
  fun refreshOffline_sets_error_report_screen() {
    mainDispatcherRule.runTest {
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
