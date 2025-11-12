package com.android.wildex.ui.report

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.wildex.model.report.Report
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Location
import com.android.wildex.utils.LocalRepositories
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportScreenTest {

  private val reportRepository: ReportRepository = LocalRepositories.reportRepository
  private val userRepository: UserRepository = LocalRepositories.userRepository
  private lateinit var reportScreenViewModel: ReportScreenViewModel

  @get:Rule val composeRule = createComposeRule()

  @Before
  fun setup() = runBlocking {
    // create reports
    val report1 =
        Report(
            reportId = "reportId1",
            imageURL =
                "https://leesbird.com/wp-content/uploads/2012/01/ring-billed-gull-imm-injured-wing-1c.jpg",
            location = Location(0.3, 0.3),
            date = Timestamp.now(),
            description = "description1",
            authorId = "user2",
            assigneeId = null)

    val report2 =
        Report(
            reportId = "reportId2",
            imageURL =
                "https://leesbird.com/wp-content/uploads/2012/01/ring-billed-gull-imm-injured-wing-1c.jpg",
            location = Location(0.8, 0.8),
            date = Timestamp.now(),
            description = "description2",
            authorId = "user3",
            assigneeId = "user1")

    reportRepository.addReport(report1)
    reportRepository.addReport(report2)

    // create users
    val user1 =
        User(
            userId = "user1",
            username = "Bob1",
            name = "Bob",
            surname = "Johnson",
            bio = "I love wildlife.",
            profilePictureURL = "urlBob1",
            userType = UserType.PROFESSIONAL,
            creationDate = Timestamp.now(),
            country = "USA",
            friendsCount = 0)

    val user2 =
        User(
            userId = "user2",
            username = "Alice2",
            name = "Alice",
            surname = "Smith",
            bio = "I love wildlife more.",
            profilePictureURL = "urlAlice1",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "England",
            friendsCount = 0)

    val user3 =
        User(
            userId = "user3",
            username = "Charlie3",
            name = "Charlie",
            surname = "Brown",
            bio = "I love wildlife even more.",
            profilePictureURL = "urlCharlie3",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "Germany",
            friendsCount = 0)

    userRepository.addUser(user1)
    userRepository.addUser(user2)
    userRepository.addUser(user3)

    val simpleUser1 = SimpleUser(userId = "user1", username = "Bob1", profilePictureURL = "urlBob1")

    val simpleUser2 =
        SimpleUser(userId = "user2", username = "Alice2", profilePictureURL = "urlAlice2")

    val simpleUser3 =
        SimpleUser(userId = "user3", username = "Charlie3", profilePictureURL = "urlCharlie3")

    reportScreenViewModel =
        ReportScreenViewModel(
            reportRepository = reportRepository,
            userRepository = userRepository,
            currentUserId = "user1")
  }

  @After
  fun teardown() {
    LocalRepositories.clearAll()
  }

  @Test
  fun notificationBellClick_invokesCallback() {
    var notificationClicked = false
    composeRule.setContent {
      ReportScreen(
          reportScreenViewModel = reportScreenViewModel,
          onNotificationClick = { notificationClicked = true })
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(ReportScreenTestTags.NOTIFICATION_BUTTON).performClick()
    assert(notificationClicked)
  }

  @Test
  fun profilePictureClick_invokesCallback() {
    var profilePictureClicked = false
    composeRule.setContent {
      ReportScreen(
          reportScreenViewModel = reportScreenViewModel,
          onProfileClick = { profilePictureClicked = true })
    }
    composeRule.waitForIdle()

    composeRule
        .onNodeWithTag(ReportScreenTestTags.testTagForProfilePicture("user1", "user"))
        .performClick()
    assert(profilePictureClicked)
  }

  @Test
  fun testTagsAreCorrectlySetWhenReports() {
    composeRule.setContent { ReportScreen(reportScreenViewModel = reportScreenViewModel) }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(ReportScreenTestTags.NOTIFICATION_BUTTON).assertIsDisplayed()
    composeRule
        .onNodeWithTag(ReportScreenTestTags.testTagForProfilePicture("user1", "user"))
        .assertIsDisplayed()
    composeRule.onNodeWithTag(ReportScreenTestTags.SCREEN_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportScreenTestTags.REPORT_LIST).assertIsDisplayed()

    composeRule
        .onNodeWithTag(
            ReportScreenTestTags.testTagForProfilePicture("user2", "author"),
            useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(
            ReportScreenTestTags.testTagForProfilePicture("user3", "author"),
            useUnmergedTree = true)
        .assertIsDisplayed()

    composeRule
        .onNodeWithTag(ReportScreenTestTags.testTagForReport("reportId1", "full"))
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(ReportScreenTestTags.testTagForReport("reportId2", "full"))
        .assertIsDisplayed()
  }

  @Test
  fun testTagsAreCorrectlySetWhenNOReports() {
    runBlocking {
      reportRepository.deleteReport("reportId1")
      reportRepository.deleteReport("reportId2")
      reportScreenViewModel.refreshUIState()
    }

    composeRule.setContent { ReportScreen(reportScreenViewModel = reportScreenViewModel) }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(ReportScreenTestTags.NOTIFICATION_BUTTON).assertIsDisplayed()
    composeRule
        .onNodeWithTag(ReportScreenTestTags.testTagForProfilePicture("user1", "user"))
        .assertIsDisplayed()
    composeRule.onNodeWithTag(ReportScreenTestTags.SCREEN_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportScreenTestTags.NO_REPORT_TEXT).assertIsDisplayed()
  }
}
