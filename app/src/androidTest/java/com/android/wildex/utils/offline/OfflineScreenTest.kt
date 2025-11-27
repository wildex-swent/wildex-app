package com.android.wildex.utils.offline

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.wildex.ui.utils.offline.OfflineScreen
import com.android.wildex.ui.utils.offline.OfflineScreenTestTags
import org.junit.Rule
import org.junit.Test

class OfflineScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun offline_screen_displays_correctly() {
    composeTestRule.setContent { OfflineScreen() }

    composeTestRule.onNodeWithTag(OfflineScreenTestTags.OFFLINE_SCREEN).isDisplayed()
    composeTestRule.onNodeWithTag(OfflineScreenTestTags.OFFLINE_TITLE).isDisplayed()
    composeTestRule.onNodeWithTag(OfflineScreenTestTags.OFFLINE_SUBTITLE).isDisplayed()
    composeTestRule.onNodeWithTag(OfflineScreenTestTags.OFFLINE_MESSAGE).isDisplayed()
    composeTestRule.onNodeWithTag(OfflineScreenTestTags.ANIMATION).isDisplayed()
  }
}
