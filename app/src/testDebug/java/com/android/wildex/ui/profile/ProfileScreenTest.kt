package com.android.wildex.ui.profile

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ImageResult
import coil.request.SuccessResult
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileScreenTest {

  @get:Rule val composeRule = createComposeRule()
  @get:Rule val mainDispatcherRule = MainDispatcherRule()

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
          friendsCount = 42)

  private fun noOpImageLoader(context: Context) =
      ImageLoader.Builder(context)
          .components {
            add(
                object : Interceptor {
                  override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
                    return SuccessResult(
                        drawable = ColorDrawable(Color.GRAY),
                        request = chain.request,
                        dataSource = DataSource.MEMORY)
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

    composeRule.onNodeWithText("User Profile").assertIsDisplayed()
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

    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS).assertIsDisplayed()
    composeRule.onNodeWithTag(ProfileScreenTestTags.COLLECTION).assertIsDisplayed()
    composeRule.onNodeWithTag(ProfileScreenTestTags.MAP).assertIsDisplayed()
    composeRule.onNodeWithTag(ProfileScreenTestTags.FRIENDS).assertIsDisplayed()

    composeRule.onNodeWithText("Animals").assertIsDisplayed()
    composeRule.onNodeWithText("Friends").assertIsDisplayed()
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
          ownerProfile = false,
          onAchievements = { achievements++ },
          onCollection = {},
          onMap = { map++ },
          onFriends = {},
          onFriendRequest = {},
      )
    }

    composeRule.onNodeWithText("Achievements").performClick()
    composeRule.onNodeWithText("Map").performClick()

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

    composeRule
        .onNodeWithTag(ProfileScreenTestTags.FRIEND_REQUEST)
        .assertIsDisplayed()
        .performClick()
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

  // ProfileScreen with viewModel

  @Test
  fun profileScreen_owner_shows_my_profile_settings_and_triggers_callbacks() {
    mainDispatcherRule.runTest {
      val user = sampleUser().copy(userId = "u-1")
      val userRepo = mockk<UserRepository>()
      val achRepo = mockk<UserAchievementsRepository>()
      coEvery { userRepo.getUser("u-1") } returns user
      coEvery { achRepo.getAllAchievementsByUser("u-1") } returns emptyList()

      val vm =
          ProfileScreenViewModel(
              userRepository = userRepo, achievementRepository = achRepo, currentUserId = { "u-1" })

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

      composeRule.onNodeWithText("Achievements").performClick()
      composeRule.onNodeWithText("Map").performClick()

      Assert.assertEquals(1, achievements)
      Assert.assertEquals(1, map)
    }
  }

  @Test
  fun profileScreen_not_owner_hides_settings_and_shows_friend_request() {
    mainDispatcherRule.runTest {
      val user = sampleUser()
      val userRepo = mockk<UserRepository>()
      val achRepo = mockk<UserAchievementsRepository>()
      coEvery { userRepo.getUser("u-1") } returns user
      coEvery { achRepo.getAllAchievementsByUser("u-1") } returns emptyList()

      val vm =
          ProfileScreenViewModel(
              userRepository = userRepo,
              achievementRepository = achRepo,
              currentUserId = { "someone-else" })

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

      composeRule.onNodeWithText("User Profile").assertIsDisplayed()
      composeRule.onAllNodesWithTag(ProfileScreenTestTags.SETTINGS).assertCountEquals(0)
      composeRule
          .onNodeWithTag(ProfileScreenTestTags.FRIEND_REQUEST)
          .assertIsDisplayed()
          .performClick()

      Assert.assertEquals(1, requests)
      Assert.assertEquals("u-1", lastId)
    }
  }
}
