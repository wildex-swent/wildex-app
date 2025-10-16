package com.android.wildex.ui.profile

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
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
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileScreenTest {

  @get:Rule val composeRule = createComposeRule()

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

  private fun setThemedContent(block: @androidx.compose.runtime.Composable () -> Unit) {
    composeRule.setContent {
      val ctx = LocalContext.current
      CompositionLocalProvider(LocalImageLoader provides noOpImageLoader(ctx)) {
        MaterialTheme { block() }
      }
    }
  }

  @Test
  fun topBar_shows_User_Profile_when_not_owner_and_buttons_click() {
    var back = 0
    var settings = 0

    setThemedContent {
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
    setThemedContent { ProfileTopAppBar(ownerProfile = true, onGoBack = {}, onSettings = {}) }
    composeRule.onNodeWithText("My Profile").assertIsDisplayed()
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

    setThemedContent {
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

    setThemedContent {
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
