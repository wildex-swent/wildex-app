package com.android.wildex.ui.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.view.Window
import android.view.WindowInsets
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.android.wildex.model.user.AppearanceMode

private val DarkColorScheme =
    darkColorScheme(
        primary = WildexBlue,
        secondary = White,
        tertiary = White,
        background = WildexBlack,
        onPrimary = WildexBlack,
        onSecondary = WildexBlack,
        onTertiary = WildexBlack,
        onBackground = White,
        surface = WildexBlack,
        onSurface = White,
        surfaceVariant = WildexBlack,
        onSurfaceVariant = White,
        error = DarkErrorRed,
        onError = WildexBlack,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = WildexGreen,
        secondary = WildexBlack,
        tertiary = WildexBlack,
        background = White,
        onPrimary = White,
        onSecondary = White,
        onTertiary = White,
        onBackground = WildexBlack,
        surface = White,
        onSurface = WildexBlack,
        surfaceVariant = White,
        onSurfaceVariant = WildexBlack,
        error = ErrorRed,
        onError = White,
    )

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun WildexTheme(
    theme: AppearanceMode = AppearanceMode.AUTOMATIC,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
  val isDarkTheme =
      when (theme) {
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
        AppearanceMode.AUTOMATIC -> isSystemInDarkTheme()
      }
  val colorScheme =
      when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          val context = LocalContext.current
          if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDarkTheme -> DarkColorScheme
        else -> LightColorScheme
      }
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null && view.isAttachedToWindow) {
        // Set Status bar color to match the theme
        setStatusBarColor(window, WildexGreen.toArgb())
      }
    }
  }
  val configuration = LocalConfiguration.current
  val isTablet = configuration.screenWidthDp >= 600
  val typography =
      if (isTablet) {
        TabletTypography
      } else {
        PhoneTypography
      }

  MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}

fun setStatusBarColor(window: Window, color: Int) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) { // Android 15+
    window.decorView.setOnApplyWindowInsetsListener { view, insets ->
      val statusBarInsets = insets.getInsets(WindowInsets.Type.statusBars())
      view.setBackgroundColor(color)
      view.setPadding(0, statusBarInsets.top, 0, 0)
      insets
    }
  }
}
