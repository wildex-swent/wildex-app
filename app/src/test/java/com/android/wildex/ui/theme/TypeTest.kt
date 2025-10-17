package com.android.wildex.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class TypeTest {

  @Test
  fun testTypographyBodyLarge() {
    val bodyLarge = Typography.bodyLarge
    assertEquals(FontFamily.Default, bodyLarge.fontFamily)
    assertEquals(FontWeight.Normal, bodyLarge.fontWeight)
    assertEquals(16.sp, bodyLarge.fontSize)
    assertEquals(24.sp, bodyLarge.lineHeight)
    assertEquals(0.5.sp, bodyLarge.letterSpacing)
  }
}
