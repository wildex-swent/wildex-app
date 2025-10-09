package com.android.wildex.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.ui.home.HomeScreen
import com.android.wildex.ui.home.HomeScreenTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testTagsAreCorrectlySetWhenNoPost() {
    composeTestRule.setContent { HomeScreen(0) }

    composeTestRule.onNodeWithTag(HomeScreenTestTags.NO_POST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.NOTIFICATION_BELL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.PROFILE_PICTURE).assertIsDisplayed()
  }

  @Test
  fun testTagsAreCorrectlySetWhenPosts() {
    composeTestRule.setContent { HomeScreen(1) }

    composeTestRule.onNodeWithTag(HomeScreenTestTags.NOTIFICATION_BELL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.PROFILE_PICTURE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_AUTHOR_PICTURE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_MORE_INFO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_LIKE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_COMMENT).assertIsDisplayed()
  }

  @Test
  fun clickInteractionsWork() {
    composeTestRule.setContent { HomeScreen(1) }

    composeTestRule.onNodeWithTag(HomeScreenTestTags.NOTIFICATION_BELL).performClick()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.PROFILE_PICTURE).performClick()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_AUTHOR_PICTURE).performClick()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_MORE_INFO).performClick()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_LIKE).performClick()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_COMMENT).performClick()
  }
}
