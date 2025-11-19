package com.android.wildex.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import com.android.wildex.model.map.MapPin
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.Field
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

  @Test
  fun fetchBitmapViaCoil_nonBitmapDrawable_withZeroIntrinsicSize_rasterizesAtLeast1x1() = runTest {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    val original = getSharedCoilInstanceField().get(null) as ImageLoader?
    val zeroIntrinsicDrawable =
        object : Drawable() {
          override fun draw(canvas: Canvas) {}

          override fun setAlpha(alpha: Int) {}

          override fun getAlpha(): Int = 255

          override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}

          @Deprecated("Deprecated in Java") override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

          override fun getIntrinsicWidth(): Int = 0

          override fun getIntrinsicHeight(): Int = 0
        }
    val base = SharedCoil.get(ctx)
    val fake =
        object : ImageLoader by base {
          override suspend fun execute(request: ImageRequest): ImageResult {
            return SuccessResult(
                drawable = zeroIntrinsicDrawable,
                request = request,
                dataSource = coil.decode.DataSource.MEMORY,
                memoryCacheKey = null)
          }
        }
    try {
      getSharedCoilInstanceField().set(null, fake)
      val loaded = fetchBitmapViaCoil(ctx, "anything://does-not-matter")
      assertNotNull(loaded)
      checkNotNull(loaded)
      assertEquals(1, loaded.width)
      assertEquals(1, loaded.height)
      loaded.recycle()
    } finally {
      getSharedCoilInstanceField().set(null, original)
    }
  }

  @Test
  fun fetchBitmapViaCoil_whenImageLoaderThrows_returnsNull_and_hitsCatchBranch() = runTest {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    val original = getSharedCoilInstanceField().get(null) as ImageLoader?
    val base = SharedCoil.get(ctx)
    val throwing =
        object : ImageLoader by base {
          override suspend fun execute(request: ImageRequest): ImageResult {
            throw RuntimeException("Boom from fake ImageLoader")
          }
        }
    try {
      getSharedCoilInstanceField().set(null, throwing)
      val loaded = fetchBitmapViaCoil(ctx, "fake://trigger-throw")
      assertNull(loaded)
    } finally {
      getSharedCoilInstanceField().set(null, original)
    }
  }

  @Test
  fun baseKey_equality_and_hash_are_valueBased() {
    val k1 = BaseKey(url = "u", borderColor = 0x112233, scaleKey = 100)
    val k2 = BaseKey(url = "u", borderColor = 0x112233, scaleKey = 100)
    val k3 = BaseKey(url = "u", borderColor = 0x112233, scaleKey = 125)
    val k4 = BaseKey(url = "v", borderColor = 0x112233, scaleKey = 100)
    assertEquals(k1, k2)
    assertEquals(k1.hashCode(), k2.hashCode())
    assertNotEquals(k1, k3)
    assertNotEquals(k1, k4)
    val map = hashMapOf<BaseKey, String>()
    map[k1] = "A"
    map[k2] = "B"
    map[k3] = "C"
    map[k4] = "D"
    assertEquals("B", map[k1])
    assertEquals("B", map[k2])
    assertEquals("C", map[k3])
    assertEquals("D", map[k4])
  }

  @Test
  fun computeBobbingIds_includesOnlyUnassignedReportsExcludingSelected() {
    val id1: Id = "report-1"
    val id2: Id = "report-2"
    val id3: Id = "report-3"
    val pins =
        listOf(
            MapPin.ReportPin(
                id = id1,
                authorId = "author-1",
                location = Location(latitude = 0.0, longitude = 0.0),
                imageURL = "",
                assigneeId = null,
            ),
            MapPin.ReportPin(
                id = id2,
                authorId = "author-2",
                location = Location(latitude = 1.0, longitude = 1.0),
                imageURL = "",
                assigneeId = "some-assignee",
            ),
            MapPin.ReportPin(
                id = id3,
                authorId = "author-3",
                location = Location(latitude = 2.0, longitude = 2.0),
                imageURL = "",
                assigneeId = null,
            ),
        )
    val result = computeBobbingIds(pins, selectedId = id3)
    assertTrue(result.contains(id1))
    assertFalse(result.contains(id2))
    assertFalse(result.contains(id3))
  }

  @Test
  fun disposePointManager_alwaysClearsCaches_evenWithNullManager() {
    val annotationById =
        mutableMapOf(
            "a" to mockPointAnnotation(),
            "b" to mockPointAnnotation(),
        )
    val baseCache =
        mutableMapOf<BaseKey, Bitmap>(
            BaseKey("u", 0, 100) to Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
        )
    disposePointManager(
        mapView = null,
        manager = null,
        annotationById = annotationById,
        baseCache = baseCache,
    )
    assertTrue(annotationById.isEmpty())
    assertTrue(baseCache.isEmpty())
  }

  private fun mockPointAnnotation(): PointAnnotation {
    val annotation = mockk<PointAnnotation>(relaxed = true)
    var lastBitmap: Bitmap? = null

    every { annotation.iconImageBitmap = any() } answers { lastBitmap = firstArg() }
    every { annotation.iconImageBitmap } answers { lastBitmap }
    return annotation
  }

  private fun getSharedCoilInstanceField(): Field {
    val f = SharedCoil::class.java.getDeclaredField("instance")
    f.isAccessible = true
    return f
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
