package com.android.wildex.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private fun buildViewModelWithData(user: User): ProfileScreenViewModel {
    val userRepository = mockk<UserRepository>()
    val achievementsRepository = mockk<UserAchievementsRepository>()
    coEvery { userRepository.getUser(user.userId) } returns user
    coEvery { achievementsRepository.getAllAchievementsByUser(user.userId) } returns
        listOf(mockk<Achievement>(), mockk())
    return ProfileScreenViewModel(
        userRepository = userRepository,
        achievementRepository = achievementsRepository,
        currentUserId = { user.userId },
        uid = user.userId)
  }

  @Test
  fun profileScreen_shows_tags_and_triggers_callbacks() {
    val testUser =
        User(
            userId = "uid-1",
            username = "the_username",
            name = "John",
            surname = "Doe",
            bio = "This is a bio",
            profilePictureURL = "https://example.com/pic.jpg",
            userType = UserType.REGULAR,
            creationDate = Timestamp.Companion.now(),
            country = "Switzerland",
            friendsCount = 7)
    val vm = buildViewModelWithData(testUser)

    var goBackClicked = false
    var settingsClicked = false
    var collectionClickedWith: String? = null
    var friendsClickedWith: String? = null
    var achievementsClickedWith: String? = null
    var mapClickedWith: String? = null

    composeRule.setContent {
      ProfileScreen(
          profileScreenViewModel = vm,
          userUid = testUser.userId,
          onGoBack = { goBackClicked = true },
          onSettings = { settingsClicked = true },
          onCollection = { id -> collectionClickedWith = id },
          onAchievements = { id -> achievementsClickedWith = id },
          onFriends = { id -> friendsClickedWith = id },
          onMap = { id -> mapClickedWith = id },
      )
    }

    composeRule.waitForIdle()

    composeRule
        .onNodeWithTag(ProfileScreenTestTags.GO_BACK, useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.SETTINGS, useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.COLLECTION, useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.FRIENDS, useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS, useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule.onNodeWithTag(ProfileScreenTestTags.MAP, useUnmergedTree = true).assertIsDisplayed()
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_NAME, useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_USERNAME, useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_COUNTRY, useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_DESCRIPTION, useUnmergedTree = true)
        .assertIsDisplayed()

    composeRule.onNodeWithText("John Doe").assertIsDisplayed()
    composeRule.onNodeWithText("the_username").assertIsDisplayed()
    composeRule.onNodeWithText("Switzerland").assertIsDisplayed()
    composeRule.onNodeWithText("This is a bio").assertIsDisplayed()

    composeRule.onNodeWithTag(ProfileScreenTestTags.GO_BACK, useUnmergedTree = true).performClick()
    composeRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS, useUnmergedTree = true).performClick()
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.COLLECTION, useUnmergedTree = true)
        .performClick()
    composeRule.onNodeWithTag(ProfileScreenTestTags.FRIENDS, useUnmergedTree = true).performClick()
    composeRule.onNodeWithText("Achievements", substring = false).performClick()
    composeRule.onNodeWithText("Map", substring = false).performClick()

    Assert.assertEquals(true, goBackClicked)
    Assert.assertEquals(true, settingsClicked)
    Assert.assertEquals(testUser.userId, collectionClickedWith)
    Assert.assertEquals(testUser.userId, friendsClickedWith)
    Assert.assertEquals(testUser.userId, achievementsClickedWith)
    Assert.assertEquals(testUser.userId, mapClickedWith)
  }

  @Test
  fun topBar_shows_User_Profile_when_not_owner() {
    composeRule.setContent {
      ProfileTopAppBar(ownerProfile = false, onGoBack = {}, onSettings = {})
    }
    composeRule.onNodeWithText("User Profile").assertIsDisplayed()
  }

  @Test
  fun animals_and_friends_clicks_ignored_when_not_owner() {
    var animalsClicked = false
    var friendsClicked = false

    composeRule.setContent {
      Row {
        ProfileAnimals(id = "uid-1", onCollection = { animalsClicked = true }, ownerProfile = false)
        ProfileFriends(id = "uid-1", onFriends = { friendsClicked = true }, ownerProfile = false)
      }
    }

    composeRule
        .onNodeWithTag(ProfileScreenTestTags.COLLECTION, useUnmergedTree = true)
        .performClick()
    composeRule.onNodeWithTag(ProfileScreenTestTags.FRIENDS, useUnmergedTree = true).performClick()

    Assert.assertFalse(animalsClicked)
    Assert.assertFalse(friendsClicked)
  }
}
