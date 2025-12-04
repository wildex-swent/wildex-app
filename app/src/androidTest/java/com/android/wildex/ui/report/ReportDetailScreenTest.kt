package com.android.wildex.ui.report

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.report.Report
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.utils.offline.OfflineScreenTestTags
import com.android.wildex.utils.LocalRepositories
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert.assertFalse
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
        )
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
        )
    userRepository.addUser(professionalUser)
    userRepository.addUser(regularUser)
    val report =
        Report(
            reportId = "reportId1",
            imageURL =
                "https://leesbird.com/wp-content/uploads/2012/01/ring-billed-gull-imm-injured-wing-1c.jpg",
            location = Location(0.3, 0.3, name = "Test Location"),
            date = Timestamp.now(),
            description = "A test report description that should be visible in the UI.",
            authorId = "user2",
            assigneeId = "user1",
        )
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
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.HERO_IMAGE).assertExists()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.INFO_BAR).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.INFO_AUTHOR_PICTURE).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.INFO_AUTHOR_NAME).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.INFO_DATE).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.INFO_LOCATION_PILL).assertIsDisplayed()
    composeRule.scrollToTagWithinScroll(ReportDetailsScreenTestTags.ASSIGNEE_CARD)
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.ASSIGNEE_CARD).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.ASSIGNEE_TEXT).assertIsDisplayed()
    composeRule.scrollToTagWithinScroll(ReportDetailsScreenTestTags.COMMENTS_HEADER)
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.COMMENTS_HEADER).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.COMMENTS_COUNT).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.COMMENT_INPUT_BAR).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.COMMENT_INPUT_FIELD).assertIsDisplayed()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.COMMENT_INPUT_SEND).assertIsDisplayed()
  }

  @Test
  fun reportDetailsScreen_longDescription_showsDescriptionToggle_andToggles() {
    runBlocking {
      val longDescription = "Very long description ".repeat(500)
      val longDescReport =
          Report(
              reportId = "longDescReport",
              imageURL = "url",
              location = Location(0.0, 0.0, name = "LongLoc"),
              date = Timestamp.now(),
              description = longDescription,
              authorId = "user2",
              assigneeId = "user1",
          )
      reportRepository.addReport(longDescReport)
      val vm =
          ReportDetailsScreenViewModel(
              reportRepository = reportRepository,
              userRepository = userRepository,
              commentRepository = commentRepository,
              currentUserId = "user1",
          )
      composeRule.setContent {
        ReportDetailsScreen(
            reportId = "longDescReport",
            reportDetailsViewModel = vm,
        )
      }
      composeRule.waitForIdle()
      composeRule.scrollToTagWithinScroll(ReportDetailsScreenTestTags.DESCRIPTION_CARD)
      composeRule.onNodeWithTag(ReportDetailsScreenTestTags.DESCRIPTION_CARD).assertIsDisplayed()
      composeRule.onNodeWithTag(ReportDetailsScreenTestTags.DESCRIPTION_TEXT).assertIsDisplayed()
      composeRule.onNodeWithTag(ReportDetailsScreenTestTags.DESCRIPTION_TOGGLE).assertIsDisplayed()
    }
  }

  @Test
  fun reportDetailsScreen_addLongComment_showsCommentTags_withTogglePresent() {
    composeRule.setContent {
      ReportDetailsScreen(
          reportId = "reportId1",
          reportDetailsViewModel = reportDetailsViewModel,
      )
    }
    composeRule.waitForIdle()
    val longComment = "Very long comment ".repeat(50)
    composeRule
        .onNodeWithTag(ReportDetailsScreenTestTags.COMMENT_INPUT_FIELD)
        .performTextInput(longComment)
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.COMMENT_INPUT_SEND).performClick()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.SCREEN).performClick()
    composeRule.waitForIdle()
    composeRule.scrollToTagWithinScroll(ReportDetailsScreenTestTags.COMMENT_CARD)
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.COMMENT_CARD).assertExists()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.COMMENT_AUTHOR).assertExists()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.COMMENT_DATE).assertExists()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.COMMENT_BODY).assertExists()
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.COMMENT_TOGGLE).assertExists()
  }

  @Test
  fun navigationOptionsBottomSheet_shownFromLocationPill_andButtonsDoNotCrash() {
    composeRule.setContent {
      ReportDetailsScreen(
          reportId = "reportId1",
          reportDetailsViewModel = reportDetailsViewModel,
      )
    }
    composeRule.waitForIdle()
    fun openSheet() {
      composeRule
          .onNodeWithTag(ReportDetailsScreenTestTags.INFO_LOCATION_PILL)
          .assertIsDisplayed()
          .performClick()
      composeRule.onNodeWithTag(NavigationSheetTestTags.SHEET).assertIsDisplayed()
    }
    openSheet()
    composeRule.onNodeWithTag(NavigationSheetTestTags.TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(NavigationSheetTestTags.LOCATION).assertIsDisplayed()
    composeRule.onNodeWithTag(NavigationSheetTestTags.BTN_GOOGLE_MAPS).assertIsDisplayed()
    composeRule.onNodeWithTag(NavigationSheetTestTags.BTN_COPY).assertIsDisplayed()
    composeRule.onNodeWithTag(NavigationSheetTestTags.BTN_SHARE).assertIsDisplayed()
    composeRule.onNodeWithTag(NavigationSheetTestTags.BTN_COPY).performClick()
    composeRule.waitForIdle()
    openSheet()
    composeRule.onNodeWithTag(NavigationSheetTestTags.BTN_SHARE).performClick()
    composeRule.waitForIdle()
  }

  @Test
  fun reportDetailsScreen_regularCreator_deleteFlow_showsCancelConfirmAndCanceledCompletion() {
    runBlocking {
      val regular =
          User(
              userId = "regularCreatorFlow",
              username = "RegularCreatorFlow",
              name = "Reg",
              surname = "User",
              bio = "",
              profilePictureURL = "url",
              userType = UserType.REGULAR,
              creationDate = Timestamp.now(),
              country = "CH",
          )
      userRepository.addUser(regular)
      val report =
          Report(
              reportId = "regularFlowReport",
              imageURL = "url",
              location = Location(0.0, 0.0, name = "Loc"),
              date = Timestamp.now(),
              description = "desc",
              authorId = "regularCreatorFlow",
              assigneeId = null,
          )
      reportRepository.addReport(report)
      val vm =
          ReportDetailsScreenViewModel(
              reportRepository = reportRepository,
              userRepository = userRepository,
              commentRepository = commentRepository,
              currentUserId = "regularCreatorFlow",
          )
      composeRule.setContent {
        ReportDetailsScreen(
            reportId = "regularFlowReport",
            reportDetailsViewModel = vm,
        )
      }
      composeRule.waitForIdle()
      composeRule.scrollToTagWithinScroll(ReportActionsTestTags.ACTION_ROW)
      composeRule
          .onNodeWithTag(ReportActionsTestTags.ACTION_CANCEL, useUnmergedTree = true)
          .assertIsDisplayed()
      composeRule
          .onNodeWithTag(ReportActionsTestTags.ACTION_SELF_ASSIGN, useUnmergedTree = true)
          .assertDoesNotExist()
      composeRule
          .onNodeWithTag(ReportActionsTestTags.ACTION_RESOLVE, useUnmergedTree = true)
          .assertDoesNotExist()
      composeRule
          .onNodeWithTag(ReportActionsTestTags.ACTION_UNSELFASSIGN, useUnmergedTree = true)
          .assertDoesNotExist()
      composeRule
          .onNodeWithTag(ReportActionsTestTags.ACTION_CANCEL, useUnmergedTree = true)
          .performClick()
      composeRule.onAllNodesWithText("Delete this report?").onFirst().assertIsDisplayed()
      composeRule
          .onAllNodesWithText("This action is irreversible and will permanently delete the report.")
          .onFirst()
          .assertIsDisplayed()
      composeRule.onAllNodesWithText("Delete report").onFirst().assertIsDisplayed()
      composeRule.onAllNodesWithText("No, keep it").onFirst().assertIsDisplayed()
      composeRule.onAllNodesWithText("Delete report").onFirst().performClick()
    }
  }

  @Test
  fun reportDetailsScreen_professionalCreator_noAssignee_showsAssignAndDelete_andOpensDialogs() {
    runBlocking {
      val pro =
          User(
              userId = "proCreator",
              username = "ProCreator",
              name = "Pro",
              surname = "Creator",
              bio = "",
              profilePictureURL = "url",
              userType = UserType.PROFESSIONAL,
              creationDate = Timestamp.now(),
              country = "CH",
          )
      userRepository.addUser(pro)
      val report =
          Report(
              reportId = "noAssigneeReport",
              imageURL = "url",
              location = Location(0.0, 0.0, name = "Loc"),
              date = Timestamp.now(),
              description = "desc",
              authorId = "proCreator",
              assigneeId = null,
          )
      reportRepository.addReport(report)
      val vm =
          ReportDetailsScreenViewModel(
              reportRepository = reportRepository,
              userRepository = userRepository,
              commentRepository = commentRepository,
              currentUserId = "proCreator",
          )
      composeRule.setContent {
        ReportDetailsScreen(
            reportId = "noAssigneeReport",
            reportDetailsViewModel = vm,
        )
      }
      composeRule.waitForIdle()
      composeRule.scrollToTagWithinScroll(ReportActionsTestTags.ACTION_ROW)
      composeRule
          .onNodeWithTag(ReportActionsTestTags.ACTION_SELF_ASSIGN, useUnmergedTree = true)
          .assertIsDisplayed()
      composeRule
          .onNodeWithTag(ReportActionsTestTags.ACTION_CANCEL, useUnmergedTree = true)
          .assertIsDisplayed()
      composeRule
          .onNodeWithTag(ReportActionsTestTags.ACTION_RESOLVE, useUnmergedTree = true)
          .assertDoesNotExist()
      composeRule
          .onNodeWithTag(ReportActionsTestTags.ACTION_UNSELFASSIGN, useUnmergedTree = true)
          .assertDoesNotExist()
      composeRule
          .onNodeWithTag(ReportActionsTestTags.ACTION_SELF_ASSIGN, useUnmergedTree = true)
          .performClick()
      composeRule.onAllNodesWithText("Assign this report to you?").onFirst().assertIsDisplayed()
      composeRule
          .onAllNodesWithText("You will be marked as the person handling this situation.")
          .onFirst()
          .assertIsDisplayed()
      composeRule.onAllNodesWithText("Assign to me").onFirst().assertIsDisplayed()
      composeRule.onAllNodesWithText("No, keep it").onFirst().assertIsDisplayed()
      composeRule
          .onNodeWithTag(ReportActionsTestTags.ACTION_CANCEL, useUnmergedTree = true)
          .performClick()
      composeRule.onAllNodesWithText("Delete this report?").onFirst().assertIsDisplayed()
    }
  }

  @Test
  fun reportDetailsScreen_professionalAssignedToCurrent_showsResolveAndUnassign_andCompletesResolveFlow() {
    composeRule.setContent {
      ReportDetailsScreen(
          reportId = "reportId1",
          reportDetailsViewModel = reportDetailsViewModel,
      )
    }
    composeRule.waitForIdle()
    composeRule.scrollToTagWithinScroll(ReportActionsTestTags.ACTION_ROW)
    composeRule
        .onNodeWithTag(ReportActionsTestTags.ACTION_RESOLVE, useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(ReportActionsTestTags.ACTION_UNSELFASSIGN, useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(ReportActionsTestTags.ACTION_CANCEL, useUnmergedTree = true)
        .assertDoesNotExist()
    composeRule
        .onNodeWithTag(ReportActionsTestTags.ACTION_SELF_ASSIGN, useUnmergedTree = true)
        .assertDoesNotExist()
    composeRule
        .onNodeWithTag(ReportActionsTestTags.ACTION_UNSELFASSIGN, useUnmergedTree = true)
        .performClick()

    composeRule.onAllNodesWithText("Stop handling this report?").onFirst().assertIsDisplayed()
    composeRule
        .onAllNodesWithText("You will no longer be assigned to this report.")
        .onFirst()
        .assertIsDisplayed()
    composeRule.onAllNodesWithText("Unassign").onFirst().assertIsDisplayed()

    // Optionally dismiss so we can continue the test
    composeRule.onAllNodesWithText("No, keep it").onFirst().performClick()
    composeRule
        .onNodeWithTag(ReportActionsTestTags.ACTION_RESOLVE, useUnmergedTree = true)
        .performClick()
    composeRule.onAllNodesWithText("Mark this report as resolved?").onFirst().assertIsDisplayed()
    composeRule
        .onAllNodesWithText("This action is irreversible and will permanently delete the report.")
        .onFirst()
        .assertIsDisplayed()
    composeRule.onAllNodesWithText("Resolve").onFirst().assertIsDisplayed()
    composeRule.onAllNodesWithText("No, keep it").onFirst().assertIsDisplayed()
    composeRule.onAllNodesWithText("Resolve").onFirst().performClick()
  }

  @Test
  fun reportDetailsScreen_professionalCreator_assignedToOther_showsDeleteOnly_andOpensCancelDialog() {
    runBlocking {
      val pro =
          User(
              userId = "proCreator2",
              username = "ProCreator2",
              name = "Pro2",
              surname = "Creator2",
              bio = "",
              profilePictureURL = "url",
              userType = UserType.PROFESSIONAL,
              creationDate = Timestamp.now(),
              country = "CH",
          )
      val otherAssignee =
          User(
              userId = "otherPro",
              username = "OtherPro",
              name = "Other",
              surname = "Pro",
              bio = "",
              profilePictureURL = "url2",
              userType = UserType.PROFESSIONAL,
              creationDate = Timestamp.now(),
              country = "CH",
          )
      userRepository.addUser(pro)
      userRepository.addUser(otherAssignee)
      val report =
          Report(
              reportId = "assignedToOtherReport",
              imageURL = "url",
              location = Location(0.0, 0.0, name = "Loc"),
              date = Timestamp.now(),
              description = "desc",
              authorId = "proCreator2",
              assigneeId = "otherPro",
          )
      reportRepository.addReport(report)
      val vm =
          ReportDetailsScreenViewModel(
              reportRepository = reportRepository,
              userRepository = userRepository,
              commentRepository = commentRepository,
              currentUserId = "proCreator2",
          )
      composeRule.setContent {
        ReportDetailsScreen(
            reportId = "assignedToOtherReport",
            reportDetailsViewModel = vm,
        )
      }
      composeRule.waitForIdle()
      composeRule.scrollToTagWithinScroll(ReportActionsTestTags.ACTION_ROW)
      composeRule
          .onNodeWithTag(ReportActionsTestTags.ACTION_CANCEL, useUnmergedTree = true)
          .assertIsDisplayed()
      composeRule
          .onNodeWithTag(ReportActionsTestTags.ACTION_SELF_ASSIGN, useUnmergedTree = true)
          .assertDoesNotExist()
      composeRule
          .onNodeWithTag(ReportActionsTestTags.ACTION_RESOLVE, useUnmergedTree = true)
          .assertDoesNotExist()
      composeRule
          .onNodeWithTag(ReportActionsTestTags.ACTION_UNSELFASSIGN, useUnmergedTree = true)
          .assertDoesNotExist()
      composeRule
          .onNodeWithTag(ReportActionsTestTags.ACTION_CANCEL, useUnmergedTree = true)
          .performClick()
      composeRule.onAllNodesWithText("Delete this report?").onFirst().assertIsDisplayed()
    }
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
  fun reportCompletionDialog_canceled_showsTitleMessageAndConfirm() {
    var confirmed = false
    composeRule.setContent {
      ReportCompletionDialog(
          type = ReportCompletionType.CANCELED,
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
    fun offlineScreenIsDisplayedWhenOfflineReportDetails() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConnectivityObserver provides false) {
                ReportDetailsScreen(
                    reportId = "reportId1",
                    reportDetailsViewModel = reportDetailsViewModel,
                )
            }
        }
        composeRule.onNodeWithTag(OfflineScreenTestTags.OFFLINE_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(OfflineScreenTestTags.OFFLINE_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(OfflineScreenTestTags.OFFLINE_SUBTITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(OfflineScreenTestTags.OFFLINE_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithTag(OfflineScreenTestTags.ANIMATION).assertIsDisplayed()
    }

  @Test
  fun commentInputIsDisabledWhenOffline() {
    composeRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides false) {
        ReportDetailsScreen(
            reportId = "reportId1",
            reportDetailsViewModel = reportDetailsViewModel,
        )
      }
    }
    composeRule.onNodeWithTag(ReportDetailsScreenTestTags.COMMENT_INPUT_FIELD).assertIsNotEnabled()
  }
}

private fun ComposeContentTestRule.scrollToTagWithinScroll(tag: String) {
  onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag(tag))
}
