package com.android.wildex.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class WildexThemeComposeTest {

  @get:Rule val composeRule = createComposeRule()

  private fun setThemedContent(
      darkTheme: Boolean,
      dynamicColor: Boolean,
      isInEditMode: Boolean = false,
      content: @Composable () -> Unit
  ) {
    composeRule.setContent {
      val context = LocalView.current.context
      val fakeView =
          object : AbstractComposeView(context) {
            override fun isInEditMode() = isInEditMode

            @Composable
            override fun Content() {
              CompositionLocalProvider(LocalView provides this) {
                WildexTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) { content() }
              }
            }
          }
      fakeView.Content()
    }
  }

  @Test
  fun lightTheme_staticBranch_appliesTheme() {
    setThemedContent(darkTheme = false, dynamicColor = false) {
      Text("LightThemeTest", color = MaterialTheme.colorScheme.primary)
    }
    composeRule.onNodeWithText("LightThemeTest").assertIsDisplayed()
  }

  @Test
  fun darkTheme_staticBranch_appliesTheme() {
    setThemedContent(darkTheme = true, dynamicColor = false) {
      Text("DarkThemeTest", color = MaterialTheme.colorScheme.primary)
    }
    composeRule.onNodeWithText("DarkThemeTest").assertIsDisplayed()
  }

  @Test
  fun dynamicLightTheme_branch_appliesTheme() {
    setThemedContent(darkTheme = false, dynamicColor = true) {
      Text("DynamicLightThemeTest", color = MaterialTheme.colorScheme.primary)
    }
    composeRule.onNodeWithText("DynamicLightThemeTest").assertIsDisplayed()
  }

  @Test
  fun dynamicDarkTheme_branch_appliesTheme() {
    setThemedContent(darkTheme = true, dynamicColor = true) {
      Text("DynamicDarkThemeTest", color = MaterialTheme.colorScheme.primary)
    }
    composeRule.onNodeWithText("DynamicDarkThemeTest").assertIsDisplayed()
  }

  @Test
  fun theme_inEditMode_skipsSideEffect() {
    setThemedContent(darkTheme = false, dynamicColor = false, isInEditMode = true) {
      Text("EditModeThemeTest")
    }
    composeRule.onNodeWithText("EditModeThemeTest").assertIsDisplayed()
  }
}
