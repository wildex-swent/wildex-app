// kotlin
package com.android.wildex.ui.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.google.firebase.Timestamp
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProfileScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Before
  fun setUpFingerprint() {
    ShadowBuild.setFingerprint("robolectric-wildex")
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
            profilePictureURL = "",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "Switzerland",
            friendsCount = 10)

    var goBackClicked = false
    var settingsClicked = false
    var collectionClickedWith: String? = null
    var friendsClickedWith: String? = null
    var achievementsClickedWith: String? = null
    var mapClickedWith: String? = null

    composeRule.setContent {
      // On évite d’instancier le ViewModel réel: on compose directement le contenu
      ProfileTopAppBar(
          ownerProfile = true,
          onGoBack = { goBackClicked = true },
          onSettings = { settingsClicked = true })
      ProfileContent(
          pd = PaddingValues(0.dp),
          user = testUser,
          ownerProfile = true,
          onAchievements = { achievementsClickedWith = it },
          onCollection = { collectionClickedWith = it },
          onMap = { mapClickedWith = it },
          onFriends = { friendsClickedWith = it })
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

    Assert.assertTrue(goBackClicked)
    Assert.assertTrue(settingsClicked)
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
