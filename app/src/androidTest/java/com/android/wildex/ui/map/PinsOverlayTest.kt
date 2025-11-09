package com.android.wildex.ui.map

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class PinsOverlayTest {

  @Test
  fun renderBasePin_withNullSrc_producesBitmap() {
    val out = renderBasePin(src = null, borderColor = Color.RED, scale = 1f)
    assertNotNull(out)
    assertTrue(out.width > 0)
    assertTrue(out.height > 0)
  }

  @Test
  fun renderBasePin_withSquareSrc_producesBitmap() {
    val src =
        Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
    val out = renderBasePin(src = src, borderColor = Color.GREEN, scale = 1.25f)
    assertNotNull(out)
    assertTrue(out.width > 0)
    assertTrue(out.height > 0)
  }

  @Test
  fun composeOverlays_noRipple_noExclamation_keepsSize() {
    val base =
        Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
    val out =
        composeOverlays(
            base = base,
            globalAlpha = 1f,
            rippleProgress = null,
            showExclamation = false,
            exclamationOffsetPx = 0f,
            borderColor = Color.BLACK,
            scale = 1f)
    assertEquals(128, out.width)
    assertEquals(128, out.height)
  }

  @Test
  fun composeOverlays_withRipple_drawsWithoutError() {
    val base =
        Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
    val out =
        composeOverlays(
            base = base,
            globalAlpha = 1f,
            rippleProgress = 0.35f,
            showExclamation = false,
            exclamationOffsetPx = 0f,
            borderColor = Color.CYAN,
            scale = 1.1f)
    assertNotNull(out)
    assertEquals(base.width, out.width)
    assertEquals(base.height, out.height)
  }

  @Test
  fun composeOverlays_withExclamation_increasesHeight_and_clampsOffset() {
    val base =
        Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
    val noExcl =
        composeOverlays(
            base = base,
            globalAlpha = 1f,
            rippleProgress = null,
            showExclamation = false,
            exclamationOffsetPx = 0f,
            borderColor = Color.BLACK,
            scale = 1f)
    val withExclHugeOffset =
        composeOverlays(
            base = base,
            globalAlpha = 1f,
            rippleProgress = null,
            showExclamation = true,
            exclamationOffsetPx = 10_000f,
            borderColor = Color.BLACK,
            scale = 1f)
    assertEquals(base.width, withExclHugeOffset.width)
    assertTrue(
        "height should increase to make room for badge", withExclHugeOffset.height > noExcl.height)
  }

  @Test
  fun composeOverlays_withGlobalAlphaZero_outputsTransparentPixels() {
    val base =
        Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
    val out =
        composeOverlays(
            base = base,
            globalAlpha = 0f,
            rippleProgress = 0.6f,
            showExclamation = true,
            exclamationOffsetPx = 100f,
            borderColor = Color.MAGENTA,
            scale = 1f)
    val p1 = out.getPixel(0, 0)
    val p2 = out.getPixel(out.width / 2, out.height / 2)
    assertEquals(0, Color.alpha(p1))
    assertEquals(0, Color.alpha(p2))
  }
}
