package com.android.wildex

import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MainActivityTest {

  @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun OnCreate() {
    composeRule.onNodeWithText("Welcome to Wildex!").assertIsNotDisplayed()
  }
}
