package com.android.wildex.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class TypeTest {

  private fun assertTextStyleEquals(
      actual: TextStyle,
      expectedFamily: FontFamily,
      expectedWeight: FontWeight,
      expectedSize: Int,
      expectedLineHeight: Int,
      expectedLetterSpacing: Float? = null,
      expectedStyle: FontStyle? = null,
  ) {
    assertEquals(expectedFamily, actual.fontFamily)
    assertEquals(expectedWeight, actual.fontWeight)
    assertEquals(expectedSize.sp, actual.fontSize)
    assertEquals(expectedLineHeight.sp, actual.lineHeight)
    expectedLetterSpacing?.let { assertEquals(it.sp, actual.letterSpacing) }
    expectedStyle?.let { assertEquals(it, actual.fontStyle) }
  }

  @Test
  fun phoneTypography_displayLarge() {
    assertTextStyleEquals(PhoneTypography.displayLarge, FiraSans, FontWeight.Normal, 57, 64, -0.25f)
  }

  @Test
  fun phoneTypography_displayMedium() {
    assertTextStyleEquals(PhoneTypography.displayMedium, FiraSans, FontWeight.Normal, 45, 52)
  }

  @Test
  fun phoneTypography_displaySmall() {
    assertTextStyleEquals(PhoneTypography.displaySmall, FiraSans, FontWeight.Normal, 36, 44)
  }

  @Test
  fun phoneTypography_headlineLarge() {
    assertTextStyleEquals(PhoneTypography.headlineLarge, FiraSans, FontWeight.Normal, 32, 40)
  }

  @Test
  fun phoneTypography_headlineMedium() {
    assertTextStyleEquals(PhoneTypography.headlineMedium, FiraSans, FontWeight.Normal, 28, 36)
  }

  @Test
  fun phoneTypography_headlineSmall() {
    assertTextStyleEquals(PhoneTypography.headlineSmall, FiraSans, FontWeight.Normal, 24, 32)
  }

  @Test
  fun phoneTypography_titleLarge() {
    assertTextStyleEquals(PhoneTypography.titleLarge, FiraSans, FontWeight.SemiBold, 22, 28)
  }

  @Test
  fun phoneTypography_titleMedium() {
    assertTextStyleEquals(PhoneTypography.titleMedium, FiraSans, FontWeight.Medium, 16, 24, 0.15f)
  }

  @Test
  fun phoneTypography_titleSmall() {
    assertTextStyleEquals(PhoneTypography.titleSmall, FiraSans, FontWeight.Medium, 14, 20, 0.1f)
  }

  @Test
  fun phoneTypography_bodyLarge() {
    assertTextStyleEquals(PhoneTypography.bodyLarge, FiraSans, FontWeight.Normal, 16, 24, 0.5f)
  }

  @Test
  fun phoneTypography_bodyMedium() {
    assertTextStyleEquals(PhoneTypography.bodyMedium, FiraSans, FontWeight.Normal, 14, 20, 0.25f)
  }

  @Test
  fun phoneTypography_bodySmall() {
    assertTextStyleEquals(PhoneTypography.bodySmall, FiraSans, FontWeight.Normal, 12, 16, 0.4f)
  }

  @Test
  fun phoneTypography_labelLarge() {
    assertTextStyleEquals(PhoneTypography.labelLarge, FiraSans, FontWeight.SemiBold, 14, 20, 0.1f)
  }

  @Test
  fun phoneTypography_labelMedium() {
    assertTextStyleEquals(PhoneTypography.labelMedium, FiraSans, FontWeight.Medium, 12, 16, 0.5f)
  }

  @Test
  fun phoneTypography_labelSmall() {
    assertTextStyleEquals(PhoneTypography.labelSmall, FiraSans, FontWeight.Medium, 11, 16, 0.5f)
  }

  @Test
  fun tabletTypography_displayLarge() {
    assertTextStyleEquals(
        TabletTypography.displayLarge, FiraSans, FontWeight.Normal, 68, 76, -0.25f)
  }

  @Test
  fun tabletTypography_displayMedium() {
    assertTextStyleEquals(TabletTypography.displayMedium, FiraSans, FontWeight.Normal, 54, 62)
  }

  @Test
  fun tabletTypography_displaySmall() {
    assertTextStyleEquals(TabletTypography.displaySmall, FiraSans, FontWeight.Normal, 42, 50)
  }

  @Test
  fun tabletTypography_headlineLarge() {
    assertTextStyleEquals(TabletTypography.headlineLarge, FiraSans, FontWeight.Normal, 38, 46)
  }

  @Test
  fun tabletTypography_headlineMedium() {
    assertTextStyleEquals(TabletTypography.headlineMedium, FiraSans, FontWeight.Normal, 34, 42)
  }

  @Test
  fun tabletTypography_headlineSmall() {
    assertTextStyleEquals(TabletTypography.headlineSmall, FiraSans, FontWeight.Normal, 28, 36)
  }

  @Test
  fun tabletTypography_titleLarge() {
    assertTextStyleEquals(TabletTypography.titleLarge, FiraSans, FontWeight.SemiBold, 26, 32)
  }

  @Test
  fun tabletTypography_titleMedium() {
    assertTextStyleEquals(TabletTypography.titleMedium, FiraSans, FontWeight.Medium, 18, 26, 0.15f)
  }

  @Test
  fun tabletTypography_titleSmall() {
    assertTextStyleEquals(TabletTypography.titleSmall, FiraSans, FontWeight.Medium, 16, 22, 0.1f)
  }

  @Test
  fun tabletTypography_bodyLarge() {
    assertTextStyleEquals(TabletTypography.bodyLarge, FiraSans, FontWeight.Normal, 18, 26, 0.5f)
  }

  @Test
  fun tabletTypography_bodyMedium() {
    assertTextStyleEquals(TabletTypography.bodyMedium, FiraSans, FontWeight.Normal, 16, 22, 0.25f)
  }

  @Test
  fun tabletTypography_bodySmall() {
    assertTextStyleEquals(TabletTypography.bodySmall, FiraSans, FontWeight.Normal, 13, 20, 0.4f)
  }

  @Test
  fun tabletTypography_labelLarge() {
    assertTextStyleEquals(TabletTypography.labelLarge, FiraSans, FontWeight.SemiBold, 16, 22, 0.1f)
  }

  @Test
  fun tabletTypography_labelMedium() {
    assertTextStyleEquals(TabletTypography.labelMedium, FiraSans, FontWeight.Medium, 13, 18, 0.5f)
  }

  @Test
  fun tabletTypography_labelSmall() {
    assertTextStyleEquals(TabletTypography.labelSmall, FiraSans, FontWeight.Medium, 12, 16, 0.5f)
  }
}
