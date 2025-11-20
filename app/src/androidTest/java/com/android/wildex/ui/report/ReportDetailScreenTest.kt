package com.android.wildex.ui.report

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.android.wildex.model.report.Report
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.social.CommentRepository
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
class ReportDetailScreenTest {

  private val reportRepository: ReportRepository = LocalRepositories.reportRepository
  private val userRepository: UserRepository = LocalRepositories.userRepository
  private val commentRepository: CommentRepository = LocalRepositories.commentRepository
  private lateinit var reportDetailsViewModel: ReportDetailsScreenViewModel

  @get:Rule val composeRule = createComposeRule()

  @Before
  fun setup() = runBlocking {
    val professionalUser =
        User(
            userId = "user1",
            username = "ProUser",
            name = "Pro",
            surname = "User",
            bio = "Professional user",
            profilePictureURL = "pro_url",
            userType = UserType.PROFESSIONAL,
            creationDate = Timestamp.now(),
            country = "Switzerland",
            friendsCount = 0)

    val regularUser =
        User(
            userId = "user2",
            username = "RegularUser",
            name = "Regular",
            surname = "User",
            bio = "Regular user",
            profilePictureURL = "regular_url",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "Switzerland",
            friendsCount = 0)

    userRepository.addUser(professionalUser)
    userRepository.addUser(regularUser)

    val report =
        Report(
            reportId = "reportId1",
            imageURL =
                "https://leesbird.com/wp-content/uploads/2012/01/ring-billed-gull-imm-injured-wing-1c.jpg",
            location = Location(0.3, 0.3),
            date = Timestamp.now(),
            description = "A test report description that should be visible in the UI.",
            authorId = "user2",
            assigneeId = "user1")

    reportRepository.addReport(report)

    reportDetailsViewModel =
        ReportDetailsScreenViewModel(
            reportRepository = reportRepository,
            userRepository = userRepository,
            commentRepository = commentRepository,
            currentUserId = "user1",
        )
  }

  @After
  fun teardown() {
    LocalRepositories.clearAll()
  }

  @Test
  fun backButtonClick_invokesCallback() {
    var backClicked = false
    composeRule.setContent { ReportDetailsTopBar(onGoBack = { backClicked = true }) }
    composeRule
        .onNodeWithTag(ReportDetailsScreenTestTags.BACK_BUTTON, useUnmergedTree = true)
        .performClick()

    assert(backClicked)
  }

  @Test
  fun reportDetailsScreen_displaysCoreSections() {
    composeRule.setContent {
      ReportDetailsScreen(
          reportId = "reportId1",
          reportDetailsViewModel = reportDetailsViewModel,
      )
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.SCREEN).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.PULL_TO_REFRESH).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.CONTENT_LIST).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.HERO_IMAGE_BOX).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.HERO_IMAGE).assertExists()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.HERO_TOP_GRADIENT).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.HERO_BOTTOM_GRADIENT).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.INFO_BAR).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.INFO_AUTHOR_NAME).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.INFO_DATE).assertIsDisplayed()
    composeRule.scrollToTagWithinScroll(ReportDetailsScreenTestTags.ASSIGNEE_CARD)
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.ASSIGNEE_CARD).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.ASSIGNEE_TEXT).assertIsDisplayed()
    composeRule.scrollToTagWithinScroll(ReportDetailsScreenTestTags.ACTION_ROW)
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.ACTION_ROW).assertIsDisplayed()
    composeRule.scrollToTagWithinScroll(ReportDetailsScreenTestTags.COMMENTS_HEADER)
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.COMMENTS_HEADER).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.COMMENTS_COUNT).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.COMMENT_INPUT_BAR).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.COMMENT_INPUT_FIELD).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.COMMENT_INPUT_SEND).assertIsDisplayed()
  }

  @Test
  fun reportCompletionDialog_resolved_showsTitleMessageAndConfirm() {
    var confirmed = false
    composeRule.setContent {
      ReportCompletionDialog(
          type = ReportCompletionType.RESOLVED,
          onConfirm = { confirmed = true },
      )
    }
    composeRule.onNodeWithTag(ReportCompletionDialogTestTags.ANIMATION).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportCompletionDialogTestTags.TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportCompletionDialogTestTags.MESSAGE).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportCompletionDialogTestTags.CONFIRM).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportCompletionDialogTestTags.CONFIRM).performClick()
    assert(confirmed)
  }

  @Test
  fun reportActionConfirmDialog_cancel_showsCorrectTexts() {
    composeRule.setContent {
      ReportActionConfirmDialog(
          action = ReportActionToConfirm.CANCEL,
          onDismiss = {},
          onConfirm = {},
      )
    }
    composeRule.onNodeWithText("Delete this report?").assertIsDisplayed()
    composeRule
        .onNodeWithText("This action is irreversible and will permanently delete the report.")
        .assertIsDisplayed()
    composeRule.onNodeWithText("Delete report").assertIsDisplayed()
    composeRule.onNodeWithText("No, keep it").assertIsDisplayed()
  }

  @Test
  fun navigationOptionsBottomSheet_displaysButtonsAndLabel() {
    composeRule.setContent {
      NavigationOptionsBottomSheet(
          latitude = 46.5197,
          longitude = 6.6323,
          displayLabel = "Lausanne, Switzerland",
          onDismissRequest = {},
      )
    }
    composeRule.onNodeWithTag(NavigationSheetTestTags.SHEET).assertIsDisplayed()
    composeRule.onNodeWithTag(NavigationSheetTestTags.TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(NavigationSheetTestTags.LOCATION).assertIsDisplayed()
    composeRule.onNodeWithTag(NavigationSheetTestTags.BTN_GOOGLE_MAPS).assertIsDisplayed()
    composeRule.onNodeWithTag(NavigationSheetTestTags.BTN_COPY).assertIsDisplayed()
    composeRule.onNodeWithTag(NavigationSheetTestTags.BTN_SHARE).assertIsDisplayed()
  }
}

private fun ComposeContentTestRule.scrollToTagWithinScroll(tag: String) {
  onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag(tag))
}
