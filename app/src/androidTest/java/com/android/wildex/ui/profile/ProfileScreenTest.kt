package com.android.wildex.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private fun sampleUser() =
      User(
          userId = "u-1",
          username = "jane_doe",
          name = "Jane",
          surname = "Doe",
          bio = "Bio of Jane",
          profilePictureURL = "https://example.com/pic.jpg",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "Switzerland",
          friendsCount = 42)

  @Test
  fun topBar_shows_User_Profile_when_not_owner_and_buttons_click() {
    var back = 0
    var settings = 0

    composeRule.setContent {
      ProfileTopAppBar(ownerProfile = false, onGoBack = { back++ }, onSettings = { settings++ })
    }

    composeRule.onNodeWithText("User Profile").assertIsDisplayed()
    composeRule.onNodeWithTag(ProfileScreenTestTags.GO_BACK).performClick()
    composeRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS).performClick()

    assertEquals(1, back)
    assertEquals(1, settings)
  }

  @Test
  fun topBar_shows_My_Profile_when_owner() {
    composeRule.setContent { ProfileTopAppBar(ownerProfile = true, onGoBack = {}, onSettings = {}) }

    composeRule.onNodeWithText("My Profile").assertIsDisplayed()
  }

  @Test
  fun profileContent_shows_core_elements_and_texts() {
    val user = sampleUser()

    composeRule.setContent {
      ProfileContent(
          pd = PaddingValues(0.dp),
          user = user,
          ownerProfile = true,
          onAchievements = {},
          onCollection = {},
          onMap = {},
          onFriends = {},
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

    composeRule.setContent {
      ProfileContent(
          pd = PaddingValues(0.dp),
          user = user,
          ownerProfile = false,
          onAchievements = {},
          onCollection = { collection++ },
          onMap = {},
          onFriends = { friends++ },
      )
    }

    composeRule.onNodeWithTag(ProfileScreenTestTags.COLLECTION).performClick()
    composeRule.onNodeWithTag(ProfileScreenTestTags.FRIENDS).performClick()

    assertEquals(0, collection)
    assertEquals(0, friends)
  }

  @Test
  fun animals_and_friends_clicks_fire_when_owner() {
    val user = sampleUser()
    var collection = 0
    var friends = 0

    composeRule.setContent {
      ProfileContent(
          pd = PaddingValues(0.dp),
          user = user,
          ownerProfile = true,
          onAchievements = {},
          onCollection = { collection++ },
          onMap = {},
          onFriends = { friends++ },
      )
    }

    composeRule.onNodeWithTag(ProfileScreenTestTags.COLLECTION).performClick()
    composeRule.onNodeWithTag(ProfileScreenTestTags.FRIENDS).performClick()

    assertEquals(1, collection)
    assertEquals(1, friends)
  }

  @Test
  fun achievements_and_map_callbacks_fire() {
    val user = sampleUser()
    var achievements = 0
    var map = 0

    composeRule.setContent {
      ProfileContent(
          pd = PaddingValues(0.dp),
          user = user,
          ownerProfile = false,
          onAchievements = { achievements++ },
          onCollection = {},
          onMap = { map++ },
          onFriends = {},
      )
    }

    composeRule.onNodeWithText("Achievements").performClick()
    composeRule.onNodeWithText("Map").performClick()

    assertEquals(1, achievements)
    assertEquals(1, map)
  }
}
