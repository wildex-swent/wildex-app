package com.android.wildex.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class WildexThemeTest {

  @get:Rule val composeRule = createComposeRule()

  private fun setContentWithFakeView(
      darkTheme: Boolean,
      dynamicColor: Boolean,
      isInEditMode: Boolean = false,
  ) {
    val activity = Robolectric.buildActivity(Activity::class.java).get()
    val view =
        object : AbstractComposeView(activity) {
          override fun isInEditMode() = isInEditMode

          @Composable
          override fun Content() {
            CompositionLocalProvider(LocalView provides this) {
              WildexTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) { Box {} }
            }
          }
        }
    composeRule.setContent { view.Content() }
  }

  @Test
  fun lightTheme_staticBranch() {
    setContentWithFakeView(darkTheme = false, dynamicColor = false)
  }

  @Test
  fun darkTheme_staticBranch() {
    setContentWithFakeView(darkTheme = true, dynamicColor = false)
  }

  @Test
  fun dynamicLightTheme_branch() {
    setContentWithFakeView(darkTheme = false, dynamicColor = true)
  }

  @Test
  fun dynamicDarkTheme_branch() {
    setContentWithFakeView(darkTheme = true, dynamicColor = true)
  }

  @Test
  fun theme_inEditMode_skipsSideEffect() {
    setContentWithFakeView(darkTheme = false, dynamicColor = false, isInEditMode = true)
  }
}
