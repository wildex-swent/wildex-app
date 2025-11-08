package com.android.wildex.ui.map

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.android.wildex.BuildConfig
import com.android.wildex.model.map.MapPin
import com.android.wildex.model.map.PinDetails
import com.android.wildex.model.report.Report
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.theme.WildexTheme
import com.android.wildex.utils.LocalRepositories
import com.google.firebase.Timestamp
import com.mapbox.common.MapboxOptions
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.ACCESS_FINE_LOCATION,
      )

  private val userRepository = LocalRepositories.userRepository
  private val likeRepository = LocalRepositories.likeRepository
  private val reportRepository = LocalRepositories.reportRepository
  private val postsRepository = LocalRepositories.postsRepository
  private val animalRepository = LocalRepositories.animalRepository
  private val currentUserId: Id = "u1"

  private lateinit var viewModel: MapScreenViewModel

  @Before
  fun setup() {
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN

    runBlocking {
      userRepository.addUser(
          User(
              userId = "u1",
              username = "alice",
              name = "Alice",
              surname = "L",
              bio = "",
              profilePictureURL = "",
              userType = UserType.PROFESSIONAL,
              creationDate = Timestamp.now(),
              country = "CH",
              friendsCount = 0))
      userRepository.addUser(
          User(
              userId = "u2",
              username = "bob",
              name = "Bob",
              surname = "K",
              bio = "",
              profilePictureURL = "",
              userType = UserType.PROFESSIONAL,
              creationDate = Timestamp.now(),
              country = "CH",
              friendsCount = 0))

      postsRepository.addPost(
          Post(
              postId = "p1",
              authorId = "u1",
              pictureURL = "",
              location = Location(46.5197, 6.6323, "Lausanne"),
              description = "A nice post",
              date = Timestamp.now(),
              animalId = "fox",
              likesCount = 3,
              commentsCount = 1))

      reportRepository.addReport(
          Report(
              reportId = "r1",
              imageURL = "",
              location = Location(46.52, 6.63, "Riponne"),
              date = Timestamp.now(),
              description = "Injured animal",
              authorId = "u2",
              assigneeId = null))
    }

    viewModel =
        MapScreenViewModel(
            postRepository = postsRepository,
            userRepository = userRepository,
            likeRepository = likeRepository,
            reportRepository = reportRepository,
            animalRepository = animalRepository,
            currentUserId = currentUserId,
        )

    composeTestRule.setContent {
      WildexTheme {
        CompositionLocalProvider(LocalSkipMapbox provides true) {
          MapScreen(
              userId = currentUserId,
              bottomBar = {},
              viewModel = viewModel,
          )
        }
      }
    }
    composeTestRule.waitForIdle()
    viewModel.onLocationPermissionResult(true)
  }

  @After
  fun tearDown() {
    LocalRepositories.clearAll()
  }

  @Test
  fun mapScreen_initialDisplay_coreElementsVisible_selectionHidden() {
    composeTestRule.onNodeWithTag(MapContentTestTags.ROOT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.TAB_SWITCHER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.FAB_RECENTER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.REFRESH).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MapContentTestTags.REFRESH_SPINNER, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_CARD).assertIsNotDisplayed()
  }

  @Test
  fun mapScreen_selectPostPin_showsPostCard() {
    runBlocking { viewModel.loadUIState(currentUserId) }
    val postPinId =
        requireNotNull(viewModel.uiState.value.pins.firstOrNull { it is MapPin.PostPin }?.id)
    viewModel.onPinSelected(postPinId)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_POST_IMAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_COMMENT_ICON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_AUTHOR_IMAGE).assertIsDisplayed()
  }

  @Test
  fun mapScreen_toggleLike_updatesLikesCountInVm() {
    runBlocking { viewModel.loadUIState(currentUserId) }
    val postPin =
        requireNotNull(
            viewModel.uiState.value.pins.firstOrNull { it is MapPin.PostPin } as? MapPin.PostPin)
    viewModel.onPinSelected(postPin.id)
    composeTestRule.waitForIdle()
    val before = (viewModel.uiState.value.selected as? PinDetails.PostDetails)?.post?.likesCount
    requireNotNull(before)
    composeTestRule
        .onNodeWithTag(MapContentTestTags.SELECTION_LIKE_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()
    val after = (viewModel.uiState.value.selected as? PinDetails.PostDetails)?.post?.likesCount
    requireNotNull(after)
    assert(before != after)
  }

  @Test
  fun mapScreen_clearSelection_hidesSelectionCard() {
    runBlocking { viewModel.loadUIState(currentUserId) }
    val anyId = requireNotNull(viewModel.uiState.value.pins.firstOrNull()?.id)
    viewModel.onPinSelected(anyId)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_CARD).assertIsDisplayed()
    viewModel.clearSelection()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_CARD).assertIsNotDisplayed()
  }

  @Test
  fun mapScreen_recenterFab_click_setsAndConsumesNonce() {
    composeTestRule.waitForIdle()
    val before = viewModel.renderState.value.recenterNonce
    composeTestRule
        .onNodeWithTag(MapContentTestTags.FAB_RECENTER)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()
    assert(viewModel.renderState.value.recenterNonce != before)
  }

  @Test
  fun mapScreen_refreshButton_isClickable_andDoesNotCrash() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(MapContentTestTags.REFRESH).assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(MapContentTestTags.ROOT).assertIsDisplayed()
  }

  @Test
  fun mapScreen_selectReportPin_showsReportCard_andAssignmentChip() {
    composeTestRule.runOnUiThread { viewModel.onTabSelected(MapTab.Reports, currentUserId) }
    val reportPinId =
        requireNotNull(viewModel.uiState.value.pins.firstOrNull { it is MapPin.ReportPin }?.id)
    viewModel.onPinSelected(reportPinId)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_REPORT_IMAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_AUTHOR_IMAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_OPEN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.REPORT_ASSIGNED_ROW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_LOCATION).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MapContentTestTags.SELECTION_REPORT_DESCRIPTION)
        .assertIsDisplayed()
    val hasNotAssigned =
        composeTestRule.onAllNodesWithText("Not assigned :(").fetchSemanticsNodes().isNotEmpty()
    val hasAssignedTo =
        composeTestRule.onAllNodesWithText("Assigned to").fetchSemanticsNodes().isNotEmpty()
    assert(hasNotAssigned || hasAssignedTo)
  }
}
