package com.android.wildex.ui.profile

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ImageResult
import coil.request.SuccessResult
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepositoryFirestore
import com.android.wildex.model.user.UserType
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private fun ComposeContentTestRule.scrollToTagWithinScroll(tag: String) {
    onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag(tag))
  }

  private fun sampleUser() =
      User(
          userId = "u-1",
          username = "jane_doe",
          name = "Jane",
          surname = "Doe",
          bio = "Bio of Jane",
          profilePictureURL = "https://example.com/pic.jpg",
          userType = UserType.REGULAR,
          creationDate = Timestamp(0, 0),
          country = "Switzerland",
          friendsCount = 42,
      )

  private fun noOpImageLoader(context: Context) =
      ImageLoader.Builder(context)
          .components {
            add(
                object : Interceptor {
                  override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
                    return SuccessResult(
                        drawable = ColorDrawable(Color.GRAY),
                        request = chain.request,
                        dataSource = DataSource.MEMORY,
                    )
                  }
                })
          }
          .build()

  private fun setThemedContent(block: @Composable () -> Unit) {
    composeRule.setContent {
      val ctx = LocalContext.current
      CompositionLocalProvider(LocalImageLoader provides noOpImageLoader(ctx)) {
        MaterialTheme { block() }
      }
    }
  }

  @Test
  fun topBar_shows_User_Profile_when_not_owner_and_no_settings_button() {
    var back = 0
    setThemedContent {
      ProfileTopAppBar(ownerProfile = false, onGoBack = { back++ }, onSettings = {})
    }
    composeRule.onNodeWithTag(ProfileScreenTestTags.GO_BACK).performClick()
    composeRule.onAllNodesWithTag(ProfileScreenTestTags.SETTINGS).assertCountEquals(0)
    Assert.assertEquals(1, back)
  }

  @Test
  fun topBar_shows_My_Profile_when_owner_and_settings_click() {
    var settings = 0
    setThemedContent {
      ProfileTopAppBar(ownerProfile = true, onGoBack = {}, onSettings = { settings++ })
    }
    composeRule.onNodeWithText("My Profile").assertIsDisplayed()
    composeRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS).assertIsDisplayed().performClick()
    Assert.assertEquals(1, settings)
  }

  @Test
  fun profileContent_shows_core_elements_and_texts() {
    val user = sampleUser()
    setThemedContent {
      ProfileContent(
          pd = PaddingValues(0.dp),
          user = user,
          ownerProfile = true,
          onAchievements = {},
          onCollection = {},
          onMap = {},
          onFriends = {},
          onFriendRequest = {},
      )
    }
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_NAME).assertTextContains("Jane Doe")
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_USERNAME).assertTextContains("jane_doe")
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_COUNTRY)
        .assertTextContains("Switzerland")
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_DESCRIPTION).assertIsDisplayed()
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS).assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.COLLECTION).assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.MAP).assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.FRIENDS).assertExists()
    composeRule.onNodeWithText("Animals").assertExists()
    composeRule.onNodeWithText("Friends").assertExists()
  }

  @Test
  fun animals_and_friends_clicks_ignored_when_not_owner() {
    val user = sampleUser()
    var collection = 0
    var friends = 0
    setThemedContent {
      ProfileContent(
          pd = PaddingValues(0.dp),
          user = user,
          ownerProfile = false,
          onAchievements = {},
          onCollection = { collection++ },
          onMap = {},
          onFriends = { friends++ },
          onFriendRequest = {},
      )
    }
    composeRule.onNodeWithTag(ProfileScreenTestTags.COLLECTION).performClick()
    composeRule.onNodeWithTag(ProfileScreenTestTags.FRIENDS).performClick()
    Assert.assertEquals(0, collection)
    Assert.assertEquals(0, friends)
  }

  @Test
  fun animals_and_friends_clicks_fire_when_owner() {
    val user = sampleUser()
    var collection = 0
    var friends = 0
    setThemedContent {
      ProfileContent(
          pd = PaddingValues(0.dp),
          user = user,
          ownerProfile = true,
          onAchievements = {},
          onCollection = { collection++ },
          onMap = {},
          onFriends = { friends++ },
          onFriendRequest = {},
      )
    }
    composeRule.onNodeWithTag(ProfileScreenTestTags.COLLECTION).performClick()
    composeRule.onNodeWithTag(ProfileScreenTestTags.FRIENDS).performClick()
    Assert.assertEquals(1, collection)
    Assert.assertEquals(1, friends)
  }

  @Test
  fun achievements_and_map_callbacks_fire() {
    val user = sampleUser()
    var achievements = 0
    var map = 0
    setThemedContent {
      ProfileContent(
          pd = PaddingValues(0.dp),
          user = user,
          ownerProfile = true,
          onAchievements = { achievements++ },
          onCollection = {},
          onMap = { map++ },
          onFriends = {},
          onFriendRequest = {},
      )
    }
    composeRule.scrollToTagWithinScroll(ProfileScreenTestTags.ACHIEVEMENTS_CTA)
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_CTA).performClick()
    composeRule.scrollToTagWithinScroll(ProfileScreenTestTags.MAP_CTA)
    composeRule.onNodeWithTag(ProfileScreenTestTags.MAP_CTA).performClick()
    Assert.assertEquals(1, achievements)
    Assert.assertEquals(1, map)
  }

  @Test
  fun friend_request_visible_and_clickable_when_not_owner() {
    val user = sampleUser()
    var requests = 0
    setThemedContent {
      ProfileContent(
          pd = PaddingValues(0.dp),
          user = user,
          ownerProfile = false,
          onAchievements = {},
          onCollection = {},
          onMap = {},
          onFriends = {},
          onFriendRequest = { requests++ },
      )
    }
    composeRule.scrollToTagWithinScroll(ProfileScreenTestTags.FRIEND_REQUEST)
    composeRule.onNodeWithTag(ProfileScreenTestTags.FRIEND_REQUEST).assertExists().performClick()
    Assert.assertEquals(1, requests)
  }

  @Test
  fun friend_request_hidden_when_owner() {
    val user = sampleUser()
    setThemedContent {
      ProfileContent(
          pd = PaddingValues(0.dp),
          user = user,
          ownerProfile = true,
          onAchievements = {},
          onCollection = {},
          onMap = {},
          onFriends = {},
          onFriendRequest = {},
      )
    }
    composeRule.onAllNodesWithTag(ProfileScreenTestTags.FRIEND_REQUEST).assertCountEquals(0)
  }

  @Test
  fun localImageLoader_is_provided_to_composition() {
    setThemedContent {
      val loader = LocalImageLoader.current
      Text(
          text = if (loader != null) "HAS_LOADER" else "NO_LOADER",
          modifier = Modifier.testTag("LOADER_CHECK"),
      )
    }
    composeRule.onNodeWithTag("LOADER_CHECK").assertTextContains("HAS_LOADER")
  }

  @Test
  fun collection_multiple_clicks_fire_when_owner() {
    val user = sampleUser()
    var collectionClicks = 0
    setThemedContent {
      ProfileContent(
          pd = PaddingValues(0.dp),
          user = user,
          ownerProfile = true,
          onAchievements = {},
          onCollection = { collectionClicks++ },
          onMap = {},
          onFriends = {},
          onFriendRequest = {},
      )
    }
    repeat(3) { composeRule.onNodeWithTag(ProfileScreenTestTags.COLLECTION).performClick() }
    Assert.assertEquals(3, collectionClicks)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun profileScreen_shows_description_node_even_when_bio_empty() = runTest {
    val user = sampleUser().copy(bio = "")
    val userRepo = mockk<UserRepositoryFirestore>()
    coEvery { userRepo.getUser("u-1") } returns user
    val achRepo =
        object : UserAchievementsRepository {
          override suspend fun getAllAchievementsByUser(userId: String): List<Achievement> =
              emptyList()

          override suspend fun getAllAchievementsByCurrentUser(): List<Achievement> = emptyList()

          override suspend fun initializeUserAchievements(userId: String) {}

          override suspend fun updateUserAchievements(userId: String, listIds: List<String>) {}

          override suspend fun getAchievementsCountOfUser(userId: String): Int = 0
        }

    val vm =
        ProfileScreenViewModel(
            userRepository = userRepo,
            achievementRepository = achRepo,
            currentUserId = "someone-else",
        )

    setThemedContent {
      ProfileScreen(
          profileScreenViewModel = vm,
          userUid = "u-1",
          onFriendRequest = {},
      )
    }

    advanceUntilIdle()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_DESCRIPTION).assertIsDisplayed()
  }

  class FakeAchievementsRepo(private val achievements: List<Achievement> = emptyList()) :
      UserAchievementsRepository {
    override suspend fun getAllAchievementsByUser(userId: String): List<Achievement> = achievements

    override suspend fun getAllAchievementsByCurrentUser(): List<Achievement> = achievements

    override suspend fun initializeUserAchievements(userId: String) {}

    override suspend fun updateUserAchievements(userId: String, listIds: List<String>) {}

    override suspend fun getAchievementsCountOfUser(userId: String): Int = achievements.size
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun profileScreen_owner_shows_my_profile_settings_and_triggers_callbacks() = runTest {
    val user = sampleUser().copy(userId = "u-1")
    val userRepo = mockk<UserRepositoryFirestore>()
    coEvery { userRepo.getUser("u-1") } returns user
    val achRepo = FakeAchievementsRepo(emptyList())
    val vm =
        ProfileScreenViewModel(
            userRepository = userRepo,
            achievementRepository = achRepo,
            currentUserId = "u-1",
        )

    var achievements = 0
    var map = 0

    setThemedContent {
      ProfileScreen(
          profileScreenViewModel = vm,
          userUid = "u-1",
          onAchievements = { if (it == "u-1") achievements++ },
          onMap = { if (it == "u-1") map++ },
      )
    }

    advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("My Profile").assertIsDisplayed()
    composeRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS).assertIsDisplayed()
    composeRule.onAllNodesWithTag(ProfileScreenTestTags.FRIEND_REQUEST).assertCountEquals(0)

    composeRule.scrollToTagWithinScroll(ProfileScreenTestTags.ACHIEVEMENTS_CTA)
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_CTA).performClick()
    composeRule.scrollToTagWithinScroll(ProfileScreenTestTags.MAP_CTA)
    composeRule.onNodeWithTag(ProfileScreenTestTags.MAP_CTA).performClick()

    Assert.assertEquals(1, achievements)
    Assert.assertEquals(1, map)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun profileScreen_not_owner_hides_settings_and_shows_friend_request() = runTest {
    val user = sampleUser()
    val userRepo = mockk<UserRepositoryFirestore>()
    coEvery { userRepo.getUser("u-1") } returns user
    val achRepo = FakeAchievementsRepo(emptyList())

    val vm =
        ProfileScreenViewModel(
            userRepository = userRepo,
            achievementRepository = achRepo,
            currentUserId = "someone-else",
        )

    var requests = 0
    var lastId = ""

    setThemedContent {
      ProfileScreen(
          profileScreenViewModel = vm,
          userUid = "u-1",
          onFriendRequest = {
            requests++
            lastId = it
          },
      )
    }

    advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onAllNodesWithTag(ProfileScreenTestTags.SETTINGS).assertCountEquals(0)
    composeRule.scrollToTagWithinScroll(ProfileScreenTestTags.FRIEND_REQUEST)
    composeRule.onNodeWithTag(ProfileScreenTestTags.FRIEND_REQUEST).assertExists().performClick()

    Assert.assertEquals(1, requests)
    Assert.assertEquals("u-1", lastId)
  }

  @Test
  fun achievements_and_map_multiple_clicks_fire_repeatedly() {
    val user = sampleUser()
    var achievements = 0
    var map = 0

    setThemedContent {
      ProfileContent(
          pd = PaddingValues(0.dp),
          user = user,
          ownerProfile = true,
          onAchievements = { achievements++ },
          onCollection = {},
          onMap = { map++ },
          onFriends = {},
          onFriendRequest = {},
      )
    }

    repeat(3) {
      composeRule.scrollToTagWithinScroll(ProfileScreenTestTags.ACHIEVEMENTS_CTA)
      composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_CTA).performClick()
    }
    repeat(2) {
      composeRule.scrollToTagWithinScroll(ProfileScreenTestTags.MAP_CTA)
      composeRule.onNodeWithTag(ProfileScreenTestTags.MAP_CTA).performClick()
    }

    Assert.assertEquals(3, achievements)
    Assert.assertEquals(2, map)
  }

  @Test
  fun back_button_multiple_clicks_and_settings_absence() {
    var back = 0
    setThemedContent {
      ProfileTopAppBar(ownerProfile = false, onGoBack = { back++ }, onSettings = {})
    }
    composeRule.onNodeWithTag(ProfileScreenTestTags.GO_BACK).performClick()
    composeRule.onNodeWithTag(ProfileScreenTestTags.GO_BACK).performClick()
    composeRule.onAllNodesWithTag(ProfileScreenTestTags.SETTINGS).assertCountEquals(0)
    Assert.assertEquals(2, back)
  }

  @Test
  fun owner_collection_and_friends_multiple_clicks() {
    val user = sampleUser()
    var collection = 0
    var friends = 0
    setThemedContent {
      ProfileContent(
          pd = PaddingValues(0.dp),
          user = user,
          ownerProfile = true,
          onAchievements = {},
          onCollection = { collection++ },
          onMap = {},
          onFriends = { friends++ },
          onFriendRequest = {},
      )
    }
    repeat(4) { composeRule.onNodeWithTag(ProfileScreenTestTags.COLLECTION).performClick() }
    repeat(3) { composeRule.onNodeWithTag(ProfileScreenTestTags.FRIENDS).performClick() }
    Assert.assertEquals(4, collection)
    Assert.assertEquals(3, friends)
  }

  @Test
  fun profileImageAndName_uses_default_values() {
    setThemedContent { ProfileImageAndName() }
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_NAME).assertTextContains("Name Surname")
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_USERNAME).assertTextContains("Username")
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_COUNTRY).assertTextContains("Country")
  }

  @Test
  fun profileDescription_uses_default_text_and_tag() {
    setThemedContent { ProfileDescription() }
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_DESCRIPTION).assertIsDisplayed()
    composeRule.onNodeWithText("Bio:...").assertIsDisplayed()
  }

  @Test
  fun profileAchievements_and_map_defaults_render_and_click() {
    setThemedContent {
      androidx.compose.foundation.layout.Column {
        ProfileAchievements(ownerProfile = true)
        ProfileMap()
      }
    }
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS).assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_CTA).assertExists().performClick()
    composeRule.onNodeWithTag(ProfileScreenTestTags.MAP).assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.MAP_CTA).assertExists().performClick()
  }

  @Test
  fun profileFriendRequest_default_button_shown_and_clickable() {
    setThemedContent { ProfileFriendRequest() }
    composeRule.onNodeWithTag(ProfileScreenTestTags.FRIEND_REQUEST).assertExists().performClick()
    composeRule.onNodeWithText("Send Friend Request").assertExists()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun profileScreen_friendRequest_multiple_clicks_passes_id_each_time() = runTest {
    val user = sampleUser()
    val userRepo = mockk<UserRepositoryFirestore>()
    coEvery { userRepo.getUser("u-1") } returns user
    val achRepo = FakeAchievementsRepo(emptyList())
    val vm =
        ProfileScreenViewModel(
            userRepository = userRepo,
            achievementRepository = achRepo,
            currentUserId = "someone-else",
        )

    var requests = 0
    var lastId = ""

    setThemedContent {
      ProfileScreen(
          profileScreenViewModel = vm,
          userUid = "u-1",
          onFriendRequest = {
            requests++
            lastId = it
          },
      )
    }

    advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.scrollToTagWithinScroll(ProfileScreenTestTags.FRIEND_REQUEST)
    val req = composeRule.onNodeWithTag(ProfileScreenTestTags.FRIEND_REQUEST)
    req.performClick()
    req.performClick()

    Assert.assertEquals(2, requests)
    Assert.assertEquals("u-1", lastId)
  }

  @Test
  fun profileFriendRequest_is_enabled_and_shows_default_text() {
    setThemedContent { ProfileFriendRequest() }
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.FRIEND_REQUEST)
        .assertExists()
        .assertIsEnabled()
        .performClick()
    composeRule.onNodeWithText("Send Friend Request").assertExists()
  }

  @Test
  fun achievements_initial_window_is_first_three() {
    val items =
        (1..5).map { i ->
          Achievement(
              achievementId = "a$i",
              name = "A$i",
              pictureURL = "url$i",
              description = "",
              condition = { true })
        }
    setThemedContent {
      ProfileAchievements(
          id = "u-1",
          onAchievements = {},
          ownerProfile = true,
          listAchievement = items,
      )
    }
    // Visible should be A1, A2, A3
    composeRule.onNodeWithText("A1").assertExists()
    composeRule.onNodeWithText("A2").assertExists()
    composeRule.onNodeWithText("A3").assertExists()
    composeRule.onNodeWithText("A4").assertDoesNotExist()
    composeRule.onNodeWithText("A5").assertDoesNotExist()
  }

  @Test
  fun achievements_next_advances_window_and_wraps() {
    val items =
        (1..5).map { i ->
          Achievement(
              achievementId = "a$i",
              name = "A$i",
              pictureURL = "url$i",
              description = "",
              condition = { true })
        }
    setThemedContent {
      ProfileContent(
          pd = PaddingValues(0.dp),
          user = sampleUser(),
          ownerProfile = true,
          achievements = items,
          onAchievements = {},
          onCollection = {},
          onMap = {},
          onFriends = {},
          onFriendRequest = {},
      )
    }

    composeRule.scrollToTagWithinScroll(ProfileScreenTestTags.ACHIEVEMENTS)

    // Initial: A1 A2 A3
    composeRule.onNodeWithText("A1").assertExists()
    composeRule.onNodeWithText("A2").assertExists()
    composeRule.onNodeWithText("A3").assertExists()

    // Next → A2 A3 A4
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_NEXT).performClick()
    composeRule.onNodeWithText("A2").assertExists()
    composeRule.onNodeWithText("A3").assertExists()
    composeRule.onNodeWithText("A4").assertExists()
    composeRule.onNodeWithText("A1").assertDoesNotExist()
    composeRule.onNodeWithText("A5").assertDoesNotExist()

    // Next → A3 A4 A5
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_NEXT).performClick()
    composeRule.onNodeWithText("A3").assertExists()
    composeRule.onNodeWithText("A4").assertExists()
    composeRule.onNodeWithText("A5").assertExists()
    composeRule.onNodeWithText("A1").assertDoesNotExist()
    composeRule.onNodeWithText("A2").assertDoesNotExist()

    // Next (wrap) → A4 A5 A1
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_NEXT).performClick()
    composeRule.onNodeWithText("A4").assertExists()
    composeRule.onNodeWithText("A5").assertExists()
    composeRule.onNodeWithText("A1").assertExists()
    composeRule.onNodeWithText("A2").assertDoesNotExist()
    composeRule.onNodeWithText("A3").assertDoesNotExist()
  }

  @Test
  fun achievements_prev_moves_back_and_wraps() {
    val items =
        (1..5).map { i ->
          Achievement(
              achievementId = "a$i",
              name = "A$i",
              pictureURL = "url$i",
              description = "",
              condition = { true })
        }
    setThemedContent {
      ProfileContent(
          pd = PaddingValues(0.dp),
          user = sampleUser(),
          ownerProfile = true,
          achievements = items,
          onAchievements = {},
          onCollection = {},
          onMap = {},
          onFriends = {},
          onFriendRequest = {},
      )
    }

    composeRule.scrollToTagWithinScroll(ProfileScreenTestTags.ACHIEVEMENTS)

    // Initial: A1 A2 A3
    composeRule.onNodeWithText("A1").assertExists()
    composeRule.onNodeWithText("A2").assertExists()
    composeRule.onNodeWithText("A3").assertExists()

    // Prev (wrap) → A5 A1 A2
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_PREV).performClick()
    composeRule.onNodeWithText("A5").assertExists()
    composeRule.onNodeWithText("A1").assertExists()
    composeRule.onNodeWithText("A2").assertExists()
    composeRule.onNodeWithText("A3").assertDoesNotExist()
    composeRule.onNodeWithText("A4").assertDoesNotExist()

    // Prev → A4 A5 A1
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_PREV).performClick()
    composeRule.onNodeWithText("A4").assertExists()
    composeRule.onNodeWithText("A5").assertExists()
    composeRule.onNodeWithText("A1").assertExists()
    composeRule.onNodeWithText("A2").assertDoesNotExist()
    composeRule.onNodeWithText("A3").assertDoesNotExist()
  }

  @Test
  fun achievements_cta_visible_for_owner_and_clicks() {
    val items = (1..2).map { i -> Achievement("a$i", "A$i", "url$i", "", { true }) }
    var clicks = 0
    setThemedContent {
      ProfileAchievements(
          id = "user-x",
          onAchievements = { if (it == "user-x") clicks++ },
          ownerProfile = true,
          listAchievement = items,
      )
    }
    composeRule
        .onNodeWithText("View all achievements", substring = true)
        .assertExists()
        .performClick()
    Assert.assertEquals(1, clicks)
  }

  @Test
  fun achievements_cta_hidden_for_non_owner() {
    val items = (1..2).map { i -> Achievement("a$i", "A$i", "url$i", "", { true }) }
    setThemedContent {
      ProfileAchievements(
          id = "user-x",
          onAchievements = {},
          ownerProfile = false,
          listAchievement = items,
      )
    }
    composeRule.onAllNodesWithText("View all achievements", substring = true).assertCountEquals(0)
  }
}
