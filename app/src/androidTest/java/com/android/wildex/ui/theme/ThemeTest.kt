package com.android.wildex.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.wildex.model.user.AppearanceMode
import org.junit.Assert.assertEquals
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

            val theme =
                when (darkTheme) {
                  true -> AppearanceMode.DARK
                  false -> AppearanceMode.LIGHT
                }

            @Composable
            override fun Content() {
              CompositionLocalProvider(LocalView provides this) {
                WildexTheme(theme = theme, dynamicColor = dynamicColor) { content() }
              }
            }
          }
      fakeView.Content()
    }
  }

  @Test
  fun lightTheme_staticBranch_appliesTheme_andPrimaryMatchesPalette() {
    var primarySeen: Color? = null
    setThemedContent(darkTheme = false, dynamicColor = false) {
      // Capture the applied primary to assert it matches LightColorScheme.primary
      primarySeen = MaterialTheme.colorScheme.primary
      Text("LightThemeTest", color = MaterialTheme.colorScheme.primary)
    }
    composeRule.onNodeWithText("LightThemeTest").assertIsDisplayed()
    // LightColorScheme.primary = Green (0xFF082C0B)
    assertEquals(Color(0xFF082C0B), primarySeen)
  }

  @Test
  fun darkTheme_staticBranch_appliesTheme_andPrimaryMatchesPalette() {
    var primarySeen: Color? = null
    setThemedContent(darkTheme = true, dynamicColor = false) {
      primarySeen = MaterialTheme.colorScheme.primary
      Text("DarkThemeTest", color = MaterialTheme.colorScheme.primary)
    }
    composeRule.onNodeWithText("DarkThemeTest").assertIsDisplayed()
    // DarkColorScheme.primary = LightGreen (0xFFd4f7d7)
    assertEquals(Color(0xFFd4f7d7), primarySeen)
  }

  @Test
  fun dynamicLightTheme_branch_composes() {
    setThemedContent(darkTheme = false, dynamicColor = true) {
      Text("DynamicLightThemeTest", color = MaterialTheme.colorScheme.primary)
    }
    composeRule.onNodeWithText("DynamicLightThemeTest").assertIsDisplayed()
    // Note: On SDK < 31, WildexTheme falls back to LightColorScheme; we don't assert the color
    // value here because dynamic palettes vary by device.
  }

  @Test
  fun dynamicDarkTheme_branch_composes() {
    setThemedContent(darkTheme = true, dynamicColor = true) {
      Text("DynamicDarkThemeTest", color = MaterialTheme.colorScheme.primary)
    }
    composeRule.onNodeWithText("DynamicDarkThemeTest").assertIsDisplayed()
    // Same note as above re: device-dependent dynamic palettes.
  }

  @Test
  fun theme_inEditMode_skipsSideEffect() {
    // We only verify that composition succeeds in edit mode; the SideEffect that touches Window
    // is skipped by Theme.kt when view.isInEditMode == true.
    setThemedContent(darkTheme = false, dynamicColor = false, isInEditMode = true) {
      Text("EditModeThemeTest")
    }
    composeRule.onNodeWithText("EditModeThemeTest").assertIsDisplayed()
  }
}
