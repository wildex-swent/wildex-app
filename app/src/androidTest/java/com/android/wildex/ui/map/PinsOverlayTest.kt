package com.android.wildex.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import coil.annotation.ExperimentalCoilApi
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.After
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
    try {
      val out = renderBasePin(src = src, borderColor = Color.GREEN, scale = 1.25f)
      assertNotNull(out)
      assertTrue(out.width > 0)
      assertTrue(out.height > 0)
    } finally {
      src.recycle()
    }
  }

  @Test
  fun composeOverlays_noRipple_noExclamation_keepsSize() {
    val base =
        Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
    try {
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
    } finally {
      base.recycle()
    }
  }

  @Test
  fun composeOverlays_withRipple_drawsWithoutError() {
    val base =
        Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
    try {
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
    } finally {
      base.recycle()
    }
  }

  @Test
  fun composeOverlays_withExclamation_increasesHeight_and_clampsOffset() {
    val base =
        Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
    try {
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
          "height should increase to make room for badge",
          withExclHugeOffset.height > noExcl.height)
    } finally {
      base.recycle()
    }
  }

  @Test
  fun composeOverlays_withGlobalAlphaZero_outputsTransparentPixels() {
    val base =
        Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
    try {
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
    } finally {
      base.recycle()
    }
  }

  @Test
  fun sharedCoil_returnsSameInstance() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    val a = SharedCoil.get(ctx)
    val b = SharedCoil.get(ctx)
    assertSame(a, b)
  }

  @Test
  fun fetchBitmapViaCoil_loadsBitmap_fromLocalPngFile_bitmapBranch() = runTest {
    val ctx: Context = InstrumentationRegistry.getInstrumentation().targetContext
    val tmp = File(ctx.cacheDir, "app_logo.png")
    val src =
        Bitmap.createBitmap(24, 18, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.YELLOW) }
    try {
      FileOutputStream(tmp).use { out -> src.compress(Bitmap.CompressFormat.PNG, 100, out) }
      val fileUri = Uri.fromFile(tmp).toString()
      val loaded = fetchBitmapViaCoil(ctx, fileUri)
      assertNotNull(loaded)
      checkNotNull(loaded)
      assertEquals(256, loaded.width)
      assertEquals(192, loaded.height)
      assertEquals(Color.alpha(Color.YELLOW), Color.alpha(loaded.getPixel(0, 0)))
      loaded.recycle()
    } finally {
      src.recycle()
      tmp.delete()
    }
  }

  @Test
  fun fetchBitmapViaCoil_loadsDrawable_fromResourceUri_nonBitmapBranch() = runTest {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    val pkg = ctx.packageName
    val resUri = "android.resource://$pkg/${com.android.wildex.R.drawable.ic_pin_base}"
    val loaded = fetchBitmapViaCoil(ctx, resUri)
    assertNotNull(loaded)
    checkNotNull(loaded)
    assertTrue(loaded.width > 0)
    assertTrue(loaded.height > 0)
    loaded.recycle()
  }

  @Test
  fun fetchBitmapViaCoil_returnsNull_onBadUrl_failureBranch() = runTest {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    val loaded = fetchBitmapViaCoil(ctx, "this-is-not-a-valid-url")
    assertNull(loaded)
  }

  // ---------- Cleanup between tests to prevent cross-test flakiness ----------

  @OptIn(ExperimentalCoilApi::class)
  @After
  fun clearCoilCaches() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    val loader = SharedCoil.get(ctx)
    loader.memoryCache?.clear()
    loader.diskCache?.clear()
  }
}
