package com.android.wildex.ui.map

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
  val user1 =
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
          friendsCount = 0)
  val user2 =
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
          friendsCount = 0)
  val post1 =
      Post(
          postId = "p1",
          authorId = "u1",
          pictureURL = "",
          location = Location(46.5197, 6.6323, "Lausanne"),
          description = "A nice post",
          date = Timestamp.now(),
          animalId = "fox",
          likesCount = 3,
          commentsCount = 1)
  val report1 =
      Report(
          reportId = "r1",
          imageURL = "",
          location = Location(46.52, 6.63, "Riponne"),
          date = Timestamp.now(),
          description = "Injured animal",
          authorId = "u2",
          assigneeId = "u1")

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

  @Test
  fun mapScreen_initialDisplay_coreElementsVisible_selectionHidden() {
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
    runBlocking { viewModel.loadUIState(currentUserId) }
    val postPinId =
        requireNotNull(viewModel.uiState.value.pins.firstOrNull { it is MapPin.PostPin }?.id)
    viewModel.onPinSelected(postPinId)
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
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_POST_IMAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_COMMENT_ICON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_AUTHOR_IMAGE).assertIsDisplayed()
  }

  @Test
  fun mapScreen_clearSelection_hidesSelectionCard() {
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
    runBlocking { viewModel.loadUIState(currentUserId) }
    val anyId = requireNotNull(viewModel.uiState.value.pins.firstOrNull()?.id)
    viewModel.onPinSelected(anyId)
    composeTestRule.waitForIdle()
    viewModel.onPinSelected(anyId)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_CARD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MapContentTestTags.SELECTION_CLOSE)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_CARD).assertIsNotDisplayed()
  }

  @Test
  fun mapScreen_recenterFab_click_setsAndConsumesNonce() {
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
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(MapContentTestTags.FAB_RECENTER)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()
    assert(viewModel.renderState.value.recenterNonce == null)
  }

  @Test
  fun selectionBottomCard_report_assigned_row_and_open_click_and_all_tags_displayed() {
    var opened: Id? = null
    val assignee =
        SimpleUser(
            userId = user1.userId,
            username = user1.username,
            profilePictureURL = user1.profilePictureURL)
    val author =
        SimpleUser(
            userId = user2.userId,
            username = user2.username,
            profilePictureURL = user2.profilePictureURL)

    composeTestRule.setContent {
      WildexTheme {
        SelectionBottomCard(
            modifier = Modifier,
            selection = PinDetails.ReportDetails(report1, author = author, assignee = assignee),
            activeTab = MapTab.Reports,
            onPost = {},
            onReport = { opened = it },
            onDismiss = {},
            onToggleLike = {},
        )
      }
    }

    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_REPORT_IMAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_AUTHOR_IMAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_OPEN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.REPORT_ASSIGNED_ROW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_LOCATION).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MapContentTestTags.SELECTION_REPORT_DESCRIPTION)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Assigned to").assertIsDisplayed()
    composeTestRule.onNodeWithText("alice").assertIsDisplayed()
    composeTestRule.onNode(hasContentDescription("Assignee"), useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithTag(MapContentTestTags.SELECTION_OPEN_BUTTON).performClick()
    assert(opened == "r1")
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
            currentUserId = "")
    composeTestRule.setContent {
      WildexTheme {
        CompositionLocalProvider(LocalSkipMapbox provides true) {
          MapScreen(
              userId = "",
              bottomBar = {},
              viewModel = badVm,
          )
        }
      }
    }
    composeTestRule.waitForIdle()
    assert(badVm.uiState.value.errorMsg == null)
    assert(badVm.uiState.value.isError)
  }

  @Test
  fun mapScreen_renderError_isClearedByLaunchedEffect() {
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
    composeTestRule.setContent {
      WildexTheme {
        RecenterFab(
            modifier = Modifier.testTag(MapContentTestTags.FAB_RECENTER),
            isLocationGranted = false,
            current = MapTab.Posts,
            onRecenter = {},
            onAskLocation = { asked = true })
      }
    }
    composeTestRule
        .onNodeWithTag(MapContentTestTags.FAB_RECENTER)
        .assertIsDisplayed()
        .performClick()
    assert(asked)
  }

  @Test
  fun selectionBottomCard_post_open_and_like_callbacks_fire() {
    var opened: Id? = null
    var liked: Id? = null
    val details =
        PinDetails.PostDetails(post = post1, author = null, likedByMe = false, animalName = "fox")
    composeTestRule.setContent {
      WildexTheme {
        SelectionBottomCard(
            modifier = Modifier,
            selection = details,
            activeTab = MapTab.Posts,
            onPost = { opened = it },
            onReport = {},
            onDismiss = {},
            onToggleLike = { liked = it },
        )
      }
    }
    composeTestRule
        .onNodeWithTag(MapContentTestTags.SELECTION_LIKE_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule
        .onNodeWithTag(MapContentTestTags.SELECTION_OPEN_BUTTON)
        .assertIsDisplayed()
        .performClick()
    assert(liked == "p1")
    assert(opened == "p1")
  }

  @Test
  fun mapTabSwitcher_expand_then_select_other_tab_calls_onTabSelected_and_collapses() {
    var picked: MapTab? = null
    composeTestRule.setContent {
      WildexTheme {
        Box(Modifier.fillMaxSize()) {
          MapTabSwitcher(
              activeTab = MapTab.Posts,
              availableTabs = listOf(MapTab.Posts, MapTab.MyPosts, MapTab.Reports),
              onTabSelected = { picked = it })
        }
      }
    }
    composeTestRule
        .onNodeWithTag(MapContentTestTags.MAIN_TAB_SWITCHER, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag("MapTabSwitcher-Reports", useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()
    assert(picked == MapTab.Reports)
  }
}
