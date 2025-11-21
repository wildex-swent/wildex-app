package com.android.wildex.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorTest {

  @Test
  fun testLightThemeColors() {
    assertEquals(Color(0xFF082C0B), Green)
    assertEquals(Color(0xFFBA5C12), Brown)
    assertEquals(Color(0xFF702632), DarkRed)
    assertEquals(Color(0xFFFF404C), ErrorRed)
    assertEquals(Color(0xFFFEFEFE), LightSurface)
    assertEquals(Color(0xFFF0F0F0), LightSurfaceVariant)
    assertEquals(Color(0xFF1A1A1A), LightOnSurfaceVariant)
  }

  @Test
  fun testDarkThemeColors() {
    assertEquals(Color(0xFFd4f7d7), LightGreen)
    assertEquals(Color(0xFFed8e45), LightBrown)
    assertEquals(Color(0xFFd9919d), LightDarkRed)
    assertEquals(Color(0xFFbd000d), DarkErrorRed)
    assertEquals(Color(0xFF0F1011), DarkBackground)
    assertEquals(Color(0xFF1A1A1A), DarkSurface)
    assertEquals(Color(0xFF242424), DarkSurfaceVariant)
    assertEquals(Color(0xFFF2F2F2), DarkonBackground)
    assertEquals(Color(0xFFEAECEF), DarkonSurface)
    assertEquals(Color(0xFFC7CBD0), DarkonSurfaceVariant)
  }

  @Test
  fun testCommonColors() {
    assertEquals(Color(0xFFFBFBFE), White)
    assertEquals(Color.Black, Black)
  }
}
