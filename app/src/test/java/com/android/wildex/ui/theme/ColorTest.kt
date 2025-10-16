package com.android.wildex.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorPaletteTest {

  @Test
  fun testLightThemeColors() {
    assertEquals(Color(0xFF082C0B), Green)
    assertEquals(Color(0xFFBA5C12), Brown)
    assertEquals(Color(0xFF702632), DarkRed)
    assertEquals(Color(0xFFFF404C), ErrorRed)
  }

  @Test
  fun testDarkThemeColors() {
    assertEquals(Color(0xFFd4f7d7), LightGreen)
    assertEquals(Color(0xFFed8e45), LightBrown)
    assertEquals(Color(0xFFd9919d), LightDarkRed)
    assertEquals(Color(0xFFbd000d), DarkErrorRed)
    assertEquals(Color(0xFF121212), DarkGray)
  }

  @Test
  fun testCommonColors() {
    assertEquals(Color(0xFFFBFBFE), White)
    assertEquals(Color.Black, Black)
  }
}
