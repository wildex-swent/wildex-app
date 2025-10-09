package com.android.wildex.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
  fun testTagsAreCorrectlySet() {
    composeTestRule.setContent { HomeScreen() }

    composeTestRule.onNodeWithTag(HomeScreenTestTags.NOTIFICATION_BELL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.PROFILE_PICTURE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_AUTHOR_PICTURE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_MORE_INFO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_LIKE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_COMMENT).assertIsDisplayed()
  }
}
