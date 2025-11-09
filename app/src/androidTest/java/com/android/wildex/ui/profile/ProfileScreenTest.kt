package com.android.wildex.ui.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasProgressBarRangeInfo
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
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.InputKey
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepositoryFirestore
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Input
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.LoadingScreenTestTags
import com.android.wildex.utils.LocalRepositories
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private fun ComposeContentTestRule.scrollToTagWithinScroll(tag: String) {
    onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag(tag))
  }

  private val sampleUser =
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

  @Test
  fun topBar_combined_owner_and_not_owner() {
    val owner = mutableStateOf(false)
    var back = 0
    var settings = 0
    composeRule.setContent {
      ProfileTopBar(ownerProfile = owner.value, onGoBack = { back++ }, onSettings = { settings++ })
    }
    composeRule.onNodeWithTag(ProfileScreenTestTags.GO_BACK).performClick()
    composeRule.onAllNodesWithTag(ProfileScreenTestTags.SETTINGS).assertCountEquals(0)
    Assert.assertEquals(1, back)
    composeRule.runOnUiThread { owner.value = true }
    composeRule.onNodeWithText("My Profile").assertIsDisplayed()
    composeRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS).assertIsDisplayed().performClick()
    Assert.assertEquals(1, settings)
  }

  @Test
  fun profileContent_core_and_owner_gating_combined() {
    val owner = mutableStateOf(false)
    var collection = 0
    var friends = 0
    composeRule.setContent {
      ProfileContent(
          user = sampleUser,
          ownerProfile = owner.value,
          onAchievements = {},
          onCollection = { collection++ },
          onMap = {},
          onFriends = { friends++ },
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
    composeRule.onNodeWithTag(ProfileScreenTestTags.COLLECTION).performClick()
    composeRule.onNodeWithTag(ProfileScreenTestTags.FRIENDS).performClick()
    Assert.assertEquals(0, collection)
    Assert.assertEquals(0, friends)
    composeRule.runOnUiThread { owner.value = true }
    composeRule.onNodeWithTag(ProfileScreenTestTags.COLLECTION).performClick()
    composeRule.onNodeWithTag(ProfileScreenTestTags.FRIENDS).performClick()
    repeat(3) { composeRule.onNodeWithTag(ProfileScreenTestTags.COLLECTION).performClick() }
    repeat(2) { composeRule.onNodeWithTag(ProfileScreenTestTags.FRIENDS).performClick() }
    Assert.assertEquals(4, collection)
    Assert.assertEquals(3, friends)
  }

  @Test
  fun achievements_and_map_ctas_combined() {
    var achievements = 0
    var map = 0
    composeRule.setContent {
      ProfileContent(
          user = sampleUser,
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
    repeat(2) {
      composeRule.scrollToTagWithinScroll(ProfileScreenTestTags.ACHIEVEMENTS_CTA)
      composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_CTA).performClick()
    }
    composeRule.scrollToTagWithinScroll(ProfileScreenTestTags.MAP_CTA)
    composeRule.onNodeWithTag(ProfileScreenTestTags.MAP_CTA).performClick()
    composeRule.scrollToTagWithinScroll(ProfileScreenTestTags.MAP_CTA)
    composeRule.onNodeWithTag(ProfileScreenTestTags.MAP_CTA).performClick()
    Assert.assertEquals(3, achievements)
    Assert.assertEquals(2, map)
  }

  @Test
  fun friend_request_visibility_and_clicks_in_content() {
    val owner = mutableStateOf(false)
    var requests = 0
    composeRule.setContent {
      ProfileContent(
          user = sampleUser,
          ownerProfile = owner.value,
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
    composeRule.runOnUiThread { owner.value = true }
    composeRule.onAllNodesWithTag(ProfileScreenTestTags.FRIEND_REQUEST).assertCountEquals(0)
  }

  @Test
  fun profileScreen_shows_description_node_even_when_bio_empty() = runTest {
    val user = sampleUser.copy(bio = "")
    val userRepo = mockk<UserRepositoryFirestore>()
    coEvery { userRepo.getUser("u-1") } returns user
    val achRepo =
        object : UserAchievementsRepository {
          override suspend fun getAllAchievementsByUser(userId: String): List<Achievement> =
              emptyList()

          override suspend fun getAllAchievementsByCurrentUser(): List<Achievement> = emptyList()

          override suspend fun getAllAchievements(): List<Achievement> = emptyList()

          override suspend fun updateUserAchievements(userId: String, inputs: Input) {}

          override suspend fun initializeUserAchievements(userId: String) {}

          override suspend fun getAchievementsCountOfUser(userId: String): Int = 0

          override suspend fun deleteUserAchievements(userId: Id) {}
        }
    val vm =
        ProfileScreenViewModel(
            userRepository = userRepo,
            achievementRepository = achRepo,
            currentUserId = "someone-else",
        )
    composeRule.setContent {
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

    override suspend fun getAllAchievements(): List<Achievement> = achievements

    override suspend fun updateUserAchievements(userId: String, inputs: Input) {}

    override suspend fun initializeUserAchievements(userId: String) {}

    override suspend fun getAchievementsCountOfUser(userId: String): Int = achievements.size

    override suspend fun deleteUserAchievements(userId: Id) {}
  }

  @Test
  fun profileScreen_owner_shows_my_profile_and_callbacks() = runTest {
    val user = sampleUser.copy(userId = "u-1")
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
    composeRule.setContent {
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

  @Test
  fun profileScreen_not_owner_shows_friend_request_and_passes_id() = runTest {
    val user = sampleUser
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
    composeRule.setContent {
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
    val req = composeRule.onNodeWithTag(ProfileScreenTestTags.FRIEND_REQUEST)
    req.performClick()
    req.performClick()
    Assert.assertEquals(2, requests)
    Assert.assertEquals("u-1", lastId)
  }

  @Test
  fun profile_defaults_and_map_achievements_defaults() {
    composeRule.setContent {
      androidx.compose.foundation.layout.Column {
        ProfileImageAndName()
        ProfileDescription()
        ProfileAchievements(ownerProfile = true)
        ProfileMap()
      }
    }
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_NAME).assertTextContains("Name Surname")
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_USERNAME).assertTextContains("Username")
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_COUNTRY).assertTextContains("Country")
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_DESCRIPTION).assertIsDisplayed()
    composeRule.onNodeWithText("Bio:...").assertIsDisplayed()
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS).assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_CTA).assertExists().performClick()
    composeRule.onNodeWithTag(ProfileScreenTestTags.MAP).assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.MAP_CTA).assertExists().performClick()
  }

  @Test
  fun profileFriendRequest_default_button_enabled_and_text() {
    composeRule.setContent { ProfileFriendRequest() }
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
              expects = setOf(InputKey.POST_IDS),
              condition = { true },
          )
        }
    composeRule.setContent {
      ProfileAchievements(
          id = "u-1",
          onAchievements = {},
          ownerProfile = true,
          listAchievement = items,
      )
    }
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
              expects = setOf(InputKey.POST_IDS),
              condition = { true },
          )
        }
    composeRule.setContent {
      ProfileContent(
          user = sampleUser,
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
    composeRule.onNodeWithText("A1").assertExists()
    composeRule.onNodeWithText("A2").assertExists()
    composeRule.onNodeWithText("A3").assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_NEXT).performClick()
    composeRule.onNodeWithText("A2").assertExists()
    composeRule.onNodeWithText("A3").assertExists()
    composeRule.onNodeWithText("A4").assertExists()
    composeRule.onNodeWithText("A1").assertDoesNotExist()
    composeRule.onNodeWithText("A5").assertDoesNotExist()
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_NEXT).performClick()
    composeRule.onNodeWithText("A3").assertExists()
    composeRule.onNodeWithText("A4").assertExists()
    composeRule.onNodeWithText("A5").assertExists()
    composeRule.onNodeWithText("A1").assertDoesNotExist()
    composeRule.onNodeWithText("A2").assertDoesNotExist()
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
              expects = setOf(InputKey.POST_IDS),
              condition = { true },
          )
        }
    composeRule.setContent {
      ProfileContent(
          user = sampleUser,
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
    composeRule.onNodeWithText("A1").assertExists()
    composeRule.onNodeWithText("A2").assertExists()
    composeRule.onNodeWithText("A3").assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_PREV).performClick()
    composeRule.onNodeWithText("A5").assertExists()
    composeRule.onNodeWithText("A1").assertExists()
    composeRule.onNodeWithText("A2").assertExists()
    composeRule.onNodeWithText("A3").assertDoesNotExist()
    composeRule.onNodeWithText("A4").assertDoesNotExist()
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_PREV).performClick()
    composeRule.onNodeWithText("A4").assertExists()
    composeRule.onNodeWithText("A5").assertExists()
    composeRule.onNodeWithText("A1").assertExists()
    composeRule.onNodeWithText("A2").assertDoesNotExist()
    composeRule.onNodeWithText("A3").assertDoesNotExist()
  }

  @Test
  fun achievements_cta_visible_for_owner_and_clicks() {
    val items =
        (1..2).map { i ->
          Achievement("a$i", "A$i", "url$i", "", setOf(InputKey.POST_IDS)) { true }
        }
    var clicks = 0
    composeRule.setContent {
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
    val items =
        (1..2).map { i ->
          Achievement("a$i", "A$i", "url$i", "", setOf(InputKey.POST_IDS)) { true }
        }
    composeRule.setContent {
      ProfileAchievements(
          id = "user-x",
          onAchievements = {},
          ownerProfile = false,
          listAchievement = items,
      )
    }
    composeRule.onAllNodesWithText("View all achievements", substring = true).assertCountEquals(0)
  }

  @Test
  fun profileLoading_showsCircularProgress() {
    composeRule.setContent { LoadingScreen(pd = PaddingValues(0.dp)) }
    composeRule
        .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
        .assertIsDisplayed()
  }

  @Test
  fun profileNotFound_showsErrorMessage() {
    composeRule.setContent { LoadingFail(pd = PaddingValues(0.dp)) }
    composeRule.onNodeWithText("Loading failed. Please try again.").assertIsDisplayed()
  }

  @Test
  fun loadingScreen_showsWhileFetchingPosts() {
    val fetchSignal = CompletableDeferred<Unit>()
    val delayedUsersRepo =
        object : LocalRepositories.UserRepositoryImpl() {
          override suspend fun getUser(userId: Id): User {
            fetchSignal.await()
            return super.getUser(userId)
          }
        }
    runBlocking {
      delayedUsersRepo.addUser(user = sampleUser)
      val vm =
          ProfileScreenViewModel(
              userRepository = delayedUsersRepo,
              achievementRepository = FakeAchievementsRepo(),
              currentUserId = "currentUserId-1",
          )

      composeRule.setContent { ProfileScreen(vm, "u-1") }
      composeRule
          .onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN, useUnmergedTree = true)
          .assertIsDisplayed()
      fetchSignal.complete(Unit)
      composeRule.waitForIdle()
      composeRule
          .onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN, useUnmergedTree = true)
          .assertIsNotDisplayed()
    }
  }

  @Test
  fun failScreenShown_whenUserLookupFails() {
    val vm =
        ProfileScreenViewModel(
            userRepository = LocalRepositories.UserRepositoryImpl(),
            achievementRepository = FakeAchievementsRepo(),
            currentUserId = "currentUserId-1",
        )
    composeRule.setContent { ProfileScreen(vm, "") }
    composeRule.waitForIdle()

    composeRule
        .onNodeWithTag(LoadingScreenTestTags.LOADING_FAIL, useUnmergedTree = true)
        .assertIsDisplayed()
  }
}
