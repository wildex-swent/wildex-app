package com.android.wildex.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorTest {

  @Test
  fun testLightThemeColors() {
    assertEquals(Color(0xFF2e6f40), WildexGreen)
    assertEquals(Color(0xFFFF404C), ErrorRed)
  }

  @Test
  fun testDarkThemeColors() {
    assertEquals(Color(0xFF5991f1), WildexBlue)
    assertEquals(Color(0xFFbd000d), DarkErrorRed)
  }

  @Test
  fun testCommonColors() {
    assertEquals(Color.White, White)
    assertEquals(Color(0xFF0e1116), WildexBlack)
  }
}
