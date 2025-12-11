package com.android.wildex.ui.camera

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SaveToGalleryScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var uri: Uri

  @Before
  fun setup() {
    uri = mockk()
  }

  @Test
  fun pictureAndButtonsAreDisplayed() {
    composeTestRule.setContent { SaveToGalleryScreen(photoUri = uri, onSave = {}, onDiscard = {}) }

    composeTestRule.onNodeWithTag(SaveToGalleryScreenTestTags.PICTURE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SaveToGalleryScreenTestTags.SAVE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SaveToGalleryScreenTestTags.DISCARD_BUTTON).assertIsDisplayed()
  }

  @Test
  fun clickingSaveShowsDialog() {
    composeTestRule.setContent { SaveToGalleryScreen(photoUri = uri, onSave = {}, onDiscard = {}) }

    composeTestRule.onNodeWithTag(SaveToGalleryScreenTestTags.SAVE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(SaveToGalleryScreenTestTags.SAVE_TEXT_FIRST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SaveToGalleryScreenTestTags.SAVE_TEXT_SECOND).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SaveToGalleryScreenTestTags.SAVE_TEXT_THIRD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SaveToGalleryScreenTestTags.SAVE_CANCEL).assertIsDisplayed()
  }

  @Test
  fun clickingDiscardShowsDialog() {
    composeTestRule.setContent { SaveToGalleryScreen(photoUri = uri, onSave = {}, onDiscard = {}) }

    composeTestRule.onNodeWithTag(SaveToGalleryScreenTestTags.DISCARD_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(SaveToGalleryScreenTestTags.DISCARD_TEXT_FIRST)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SaveToGalleryScreenTestTags.DISCARD_TEXT_SECOND)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SaveToGalleryScreenTestTags.DISCARD_TEXT_THIRD)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(SaveToGalleryScreenTestTags.DISCARD_CANCEL).assertIsDisplayed()
  }
}
