package com.android.wildex.ui.map

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.android.wildex.BuildConfig
import com.android.wildex.model.map.MapPin
import com.android.wildex.model.map.PinDetails
import com.android.wildex.model.report.Report
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.theme.WildexTheme
import com.android.wildex.utils.LocalRepositories
import com.google.firebase.Timestamp
import com.mapbox.common.MapboxOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapScreenTest {
  private val user1 =
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
      )
  private val user2 =
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
      )
  private val post1 =
      Post(
          postId = "p1",
          authorId = "u1",
          pictureURL = "",
          location = Location(46.5197, 6.6323, "Lausanne"),
          description = "A nice post",
          date = Timestamp.now(),
          animalId = "fox")
  private val report1 =
      Report(
          reportId = "r1",
          imageURL = "",
          location = Location(46.52, 6.63, "Riponne"),
          date = Timestamp.now(),
          description = "Injured animal",
          authorId = "u2",
          assigneeId = "u1",
      )

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.ACCESS_FINE_LOCATION,
      )

  private val userRepository = LocalRepositories.userRepository
  private val likeRepository = LocalRepositories.likeRepository
  private val commentRepository = LocalRepositories.commentRepository
  private val reportRepository = LocalRepositories.reportRepository
  private val postsRepository = LocalRepositories.postsRepository
  private val animalRepository = LocalRepositories.animalRepository
  private val currentUserId: Id = "u1"

  private lateinit var viewModel: MapScreenViewModel

  @Before
  fun setup() {
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    runBlocking {
      userRepository.addUser(user1)
      userRepository.addUser(user2)
      postsRepository.addPost(post1)
      reportRepository.addReport(report1)
    }
    viewModel =
        MapScreenViewModel(
            postRepository = postsRepository,
            userRepository = userRepository,
            likeRepository = likeRepository,
            commentRepository = commentRepository,
            reportRepository = reportRepository,
            animalRepository = animalRepository,
            currentUserId = currentUserId,
        )
    viewModel.onLocationPermissionResult(true)
  }

  @After
  fun tearDown() {
    LocalRepositories.clearAll()
  }

  private fun compose(content: @Composable () -> Unit) =
      composeTestRule.setContent { WildexTheme { content() } }

  private fun setMapScreen(
      vm: MapScreenViewModel = viewModel,
      uid: Id = currentUserId,
      skipThread: Boolean = true,
      isCurrentUser: Boolean = true,
  ) = compose {
    CompositionLocalProvider(LocalSkipWorkerThread provides skipThread) {
      MapScreen(userId = uid, bottomBar = {}, viewModel = vm, isCurrentUser = isCurrentUser)
    }
  }

  private fun setSelectionCard(
      selection: PinDetails?,
      tab: MapTab,
      onPost: (Id) -> Unit = {},
      onReport: (Id) -> Unit = {},
      onDismiss: () -> Unit = {},
      onToggleLike: (Id) -> Unit = {},
  ) = compose {
    SelectionBottomCard(
        modifier = Modifier,
        selection = selection,
        activeTab = tab,
        onPost = onPost,
        onReport = onReport,
        onDismiss = onDismiss,
        onToggleLike = onToggleLike,
        isCurrentUser = true)
  }

  private fun node(tag: String, unmerged: Boolean = false) =
      composeTestRule.onNodeWithTag(tag, useUnmergedTree = unmerged)

  private fun nodeText(text: String, substring: Boolean = false) =
      composeTestRule.onNodeWithText(text, substring = substring)

  // ---------- Tests ----------
  @Test
  fun mapScreen_initialDisplay_coreElementsVisible_selectionHidden() {
    setMapScreen(uid = "u3", skipThread = false, isCurrentUser = false)
    composeTestRule.waitForIdle()
    node(MapContentTestTags.ROOT).assertIsDisplayed()
    node(MapContentTestTags.TAB_SWITCHER).assertIsDisplayed()
    node(MapContentTestTags.FAB_RECENTER).assertIsDisplayed()
    node(MapContentTestTags.MAP_CANVAS).assertIsDisplayed()
    node(MapContentTestTags.MAP_PINS).assertIsDisplayed()
    node(MapContentTestTags.REFRESH).assertIsDisplayed()
    node(MapContentTestTags.BACK_BUTTON).assertIsDisplayed()
    node(MapContentTestTags.REFRESH_SPINNER, unmerged = true).assertIsDisplayed()
    node(MapContentTestTags.SELECTION_CARD).assertIsNotDisplayed()
  }

  @Test
  fun mapScreen_selectPostPin_showsPostCard() {
    setMapScreen()
    composeTestRule.waitForIdle()
    runBlocking { viewModel.loadUIState(currentUserId) }
    val postPinId =
        requireNotNull(viewModel.uiState.value.pins.firstOrNull { it is MapPin.PostPin }?.id)
    viewModel.onPinSelected(postPinId)
    composeTestRule.waitForIdle()
    val before = (viewModel.uiState.value.selected as? PinDetails.PostDetails)?.likeCount
    requireNotNull(before)
    node(MapContentTestTags.SELECTION_LIKE_BUTTON).assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    val after = (viewModel.uiState.value.selected as? PinDetails.PostDetails)?.likeCount
    requireNotNull(after)
    assert(before != after)
    node(MapContentTestTags.SELECTION_CARD).assertIsDisplayed()
    node(MapContentTestTags.SELECTION_POST_IMAGE).assertIsDisplayed()
    node(MapContentTestTags.SELECTION_COMMENT_ICON).assertIsDisplayed()
    node(MapContentTestTags.SELECTION_AUTHOR_IMAGE).assertIsDisplayed()
  }

  @Test
  fun mapScreen_clearSelection_hidesSelectionCard() {
    setMapScreen()
    composeTestRule.waitForIdle()
    runBlocking { viewModel.loadUIState(currentUserId) }
    val anyId = requireNotNull(viewModel.uiState.value.pins.firstOrNull()?.id)
    viewModel.onPinSelected(anyId)
    composeTestRule.waitForIdle()
    viewModel.onPinSelected(anyId)
    composeTestRule.waitForIdle()
    node(MapContentTestTags.SELECTION_CARD).assertIsDisplayed()
    node(MapContentTestTags.SELECTION_CLOSE).assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    node(MapContentTestTags.SELECTION_CARD).assertIsNotDisplayed()
  }

  @Test
  fun mapScreen_recenterFab_click_setsAndConsumesNonce() {
    setMapScreen()
    composeTestRule.waitForIdle()
    node(MapContentTestTags.FAB_RECENTER).assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assert(viewModel.renderState.value.recenterNonce == null)
  }

  @Test
  fun selectionBottomCard_report_assigned_row_and_open_click_and_all_tags_displayed() {
    val assignee = SimpleUser(user1.userId, user1.username, user1.profilePictureURL, user1.userType)
    val author = SimpleUser(user2.userId, user2.username, user2.profilePictureURL, user1.userType)
    setSelectionCard(
        selection = PinDetails.ReportDetails(report1, author = author, assignee = assignee),
        tab = MapTab.Reports,
    )
    node(MapContentTestTags.SELECTION_REPORT_IMAGE).assertExists()
    node(MapContentTestTags.SELECTION_AUTHOR_IMAGE).assertExists()
    nodeText("Assigned to").assertExists()
    nodeText("alice").assertExists()
    composeTestRule.onNode(hasContentDescription("Assignee"), useUnmergedTree = true).assertExists()
    node(MapContentTestTags.REPORT_ASSIGNED_ROW).assertExists()
    node(MapContentTestTags.SELECTION_LOCATION).assertExists()
    node(MapContentTestTags.SELECTION_REPORT_DESCRIPTION).assertExists()
  }

  @Test
  fun selectionBottomCard_report_not_assigned_showsNotAssignedChip() {
    var opened: Id? = null
    val notAssignedReport = report1.copy(assigneeId = null)
    setSelectionCard(
        selection =
            PinDetails.ReportDetails(report = notAssignedReport, author = null, assignee = null),
        tab = MapTab.Reports,
        onReport = { opened = it },
    )
    node(MapContentTestTags.REPORT_ASSIGNED_ROW).assertIsDisplayed()
    node(MapContentTestTags.SELECTION_OPEN_BUTTON).assertIsDisplayed().performClick()
    nodeText("Not assigned :(").assertIsDisplayed()
    nodeText("Someone reported:", substring = true).assertIsDisplayed()
    assert(opened == "r1")
  }

  @Test
  fun mapRefreshButton_rotation_animates_whenRefreshing_advanceClock() {
    composeTestRule.mainClock.autoAdvance = false
    var refreshing by mutableStateOf(false)
    compose { MapRefreshButton(isRefreshing = refreshing, onRefresh = {}) }
    node(MapContentTestTags.REFRESH_SPINNER, unmerged = true).assertIsDisplayed()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1200L)
    composeTestRule.mainClock.autoAdvance = true
  }

  @Test
  fun mapScreen_errorMsg_isClearedByLaunchedEffect() {
    val badVm =
        MapScreenViewModel(
            postRepository = postsRepository,
            userRepository = userRepository,
            likeRepository = likeRepository,
            reportRepository = reportRepository,
            animalRepository = animalRepository,
            currentUserId = "",
        )
    setMapScreen(vm = badVm, uid = "")
    composeTestRule.waitForIdle()
    assert(badVm.uiState.value.errorMsg == null)
    assert(badVm.uiState.value.isError)
  }

  @Test
  fun mapScreen_renderError_isClearedByLaunchedEffect() {
    setMapScreen()
    composeTestRule.waitForIdle()
    val fld = MapScreenViewModel::class.java.getDeclaredField("_renderState")
    fld.isAccessible = true
    @Suppress("UNCHECKED_CAST") val flow = fld.get(viewModel) as MutableStateFlow<MapRenderState>
    composeTestRule.runOnUiThread { flow.value = flow.value.copy(renderError = "boom") }
    composeTestRule.waitForIdle()
    assert(flow.value.renderError == null)
  }

  @Test
  fun recenterFab_calls_onAskLocation_whenPermissionDenied() {
    var asked = false
    compose {
      RecenterFab(
          modifier = Modifier.testTag(MapContentTestTags.FAB_RECENTER),
          isLocationGranted = false,
          onRecenter = {},
          onAskLocation = { asked = true },
      )
    }
    node(MapContentTestTags.FAB_RECENTER).assertIsDisplayed().performClick()
    assert(asked)
  }

  @Test
  fun selectionBottomCard_post_open_and_like_callbacks_fire() {
    var opened: Id? = null
    var liked: Id? = null
    val details =
        PinDetails.PostDetails(
            post = post1,
            author = null,
            likedByMe = false,
            animalName = "fox",
            likeCount = 0,
            commentCount = 0)
    setSelectionCard(
        selection = details,
        tab = MapTab.Posts,
        onPost = { opened = it },
        onToggleLike = { liked = it },
    )
    node(MapContentTestTags.SELECTION_LIKE_BUTTON).assertIsDisplayed().performClick()
    node(MapContentTestTags.SELECTION_OPEN_BUTTON).assertIsDisplayed().performClick()
    assert(liked == "p1")
    assert(opened == "p1")
  }

  @Test
  fun mapTabSwitcher_expand_then_select_other_tab_calls_onTabSelected_and_collapses() {
    var picked: MapTab? = null
    compose {
      WildexTheme {
        Box(Modifier.fillMaxSize()) {
          MapTabSwitcher(
              activeTab = MapTab.Posts,
              availableTabs = listOf(MapTab.Posts, MapTab.MyPosts, MapTab.Reports),
              onTabSelected = { picked = it },
          )
        }
      }
    }
    node(MapContentTestTags.MAIN_TAB_SWITCHER, unmerged = true).assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    node(MapContentTestTags.getPinTag(MapTab.Reports), unmerged = true)
        .assertIsDisplayed()
        .performClick()
    assert(picked == MapTab.Reports)
  }

  @Test
  fun selectionBottomCard_post_myPosts_title_uses_you_saw_and_unknown_location() {
    val details =
        PinDetails.PostDetails(
            post = post1.copy(location = Location(46.5, 6.6, "")),
            author =
                SimpleUser(user1.userId, user1.username, user1.profilePictureURL, user1.userType),
            likedByMe = true,
            animalName = "owl",
            likeCount = 0,
            commentCount = 0)
    setSelectionCard(selection = details, tab = MapTab.MyPosts)
    nodeText("You saw an Owl", substring = true).assertIsDisplayed()
    node(MapContentTestTags.SELECTION_LOCATION).assertIsDisplayed()
    nodeText("Unknown").assertIsDisplayed()
    node(MapContentTestTags.SELECTION_LIKE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Like", useUnmergedTree = true).assertExists()
  }

  @Test
  fun articleWithWord_branches() {
    assert(articleWithWord("") == "a")
    assert(articleWithWord("owl") == "an")
    assert(articleWithWord("fox") == "a")
  }

  @Test
  fun selectionBottomCard_null_selection_returns_early() {
    compose {
      WildexTheme {
        SelectionBottomCard(
            modifier = Modifier.testTag(MapContentTestTags.SELECTION_CARD),
            selection = null,
            activeTab = MapTab.Posts,
            onPost = {},
            onReport = {},
            onDismiss = {},
            onToggleLike = {},
            isCurrentUser = true)
      }
    }
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_CARD).assertDoesNotExist()
  }

  @Test
  fun backButton_calls_onGoBack_whenClicked() {
    var clicked = false
    compose {
      WildexTheme {
        BackButton(
            modifier = Modifier.testTag(MapContentTestTags.BACK_BUTTON),
            onGoBack = { clicked = true },
        )
      }
    }
    node(MapContentTestTags.BACK_BUTTON).assertIsDisplayed().performClick()
    assert(clicked)
  }
}
