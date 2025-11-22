package com.android.wildex.ui.map

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.android.wildex.R
import com.android.wildex.model.map.MapPin
import com.android.wildex.model.utils.Id
import com.google.gson.JsonPrimitive
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlinx.coroutines.*

/* ----------------------- constants  ----------------------- */
private const val TAG = "PinsOverlay"
private const val FADE_STEPS = 6
private const val FADE_DELAY_MS = 18L
private const val RIPPLE_FRAME_MS = 33L
private const val BOB_FRAME_MS = 40L
private const val BOB_AMP_PX = 4f
private const val BOB_SPEED = 0.05f
private const val RIPPLE_SPEED = 0.02f
private const val PHOTO_SIZE = 256
private const val BASE_CIRCLE = 100f
private const val PADDING = 12f
private const val BAR_W = 10f
private const val BAR_H = 80f
private const val BADGE_GAP = 30f
private const val BADGE_RING_R = 12f
private const val BADGE_STROKE = 3.2f
private const val BADGE_BAR_W = 3.6f
private const val BADGE_BAR_H = 9.5f
private const val BADGE_DOT_R = 2.4f

/* ----------------------- main implementation ----------------------- */

/** Coroutine dispatchers used for PinsOverlay operations. */
@VisibleForTesting
internal object PinsDispatchers {
  var computation: CoroutineDispatcher = Dispatchers.Default
  var io: CoroutineDispatcher = Dispatchers.IO
}

/** Singleton Coil ImageLoader to be shared across multiple invocations of PinsOverlay */
@VisibleForTesting
internal object SharedCoil {
  @Volatile private var instance: ImageLoader? = null

  /**
   * Get the singleton ImageLoader instance
   *
   * @param ctx Context to build the ImageLoader if not already created
   */
  fun get(ctx: Context): ImageLoader =
      instance
          ?: ImageLoader.Builder(ctx)
              .diskCachePolicy(CachePolicy.ENABLED)
              .memoryCachePolicy(CachePolicy.ENABLED)
              .build()
              .also { instance = it }
}

/**
 * Base icon cache (to avoid repeating the rebuilding each frame)
 *
 * @param url Image URL
 * @param borderColor Border color integer
 * @param scaleKey Scale factor multiplied by 100 (to avoid float keys)
 */
@VisibleForTesting
internal data class BaseKey(
    val url: String,
    val borderColor: Int,
    val scaleKey: Int,
)

/** Composable overlay that displays map pins on a Mapbox MapView. */
@Composable
fun PinsOverlay(
    modifier: Modifier,
    mapView: MapView?,
    pins: List<MapPin>,
    currentTab: MapTab,
    selectedId: Id?,
    onPinClick: (Id) -> Unit,
) {
  val ctx = LocalContext.current
  val cs = MaterialTheme.colorScheme
  val ui = colorsForMapTab(currentTab, cs)
  val fallbackUrl = ctx.getString(R.string.fallback_url)
  val borderColor = ui.bg.toArgb()

  val annotationById = remember { mutableStateMapOf<Id, PointAnnotation>() }
  var manager by remember(mapView, currentTab) { mutableStateOf<PointAnnotationManager?>(null) }

  val baseCache: MutableMap<BaseKey, Bitmap> = remember { mutableMapOf() }
  val latestOnClick by rememberUpdatedState(onPinClick)
  val latestSelectedId by rememberUpdatedState(selectedId)

  var bobTime by remember { mutableFloatStateOf(0f) }
  var bobbingIds by remember { mutableStateOf<Set<Id>>(emptySet()) }

  /* ------------- lifecycle of manager + click listener ------------- */
  DisposableEffect(mapView, currentTab) {
    manager = createPointManager(mapView, latestOnClick)
    onDispose {
      disposePointManager(mapView, manager, annotationById, baseCache)
      manager = null
    }
  }

  /* ------------- apply pin diff (create/update/remove + fade-in) ------------- */
  LaunchedEffect(manager, pins, selectedId) {
    applyPinDiffAndFade(
        ctx = ctx,
        manager = manager,
        pins = pins,
        selectedId = selectedId,
        fallbackUrl = fallbackUrl,
        borderColor = borderColor,
        annotationById = annotationById,
        baseCache = baseCache,
    )
  }

  /* ------------- recompute bobbing ids when pins OR selection changes ------------- */
  LaunchedEffect(pins, selectedId) { bobbingIds = computeBobbingIds(pins, selectedId) }

  /* ------------- single bobbing ticker for all unassigned pins ------------- */
  LaunchedEffect(bobbingIds, manager, pins) {
    runBobbingLoop(
        ctx = ctx,
        manager = manager,
        pins = pins,
        bobbingIds = bobbingIds,
        latestSelectedId = latestSelectedId,
        fallbackUrl = fallbackUrl,
        borderColor = borderColor,
        baseCache = baseCache,
        bobTimeProvider = { bobTime },
        bobTimeUpdater = { bobTime = it },
        annotationById = annotationById,
    )
  }

  /* ------------- selection ripple: only for selected pin ------------- */
  val selectedAnnotation = selectedId?.let { annotationById[it] }
  LaunchedEffect(selectedId, manager, pins, selectedAnnotation) {
    runSelectionRipple(
        ctx = ctx,
        manager = manager,
        pins = pins,
        selectedId = selectedId,
        selectedAnnotation = selectedAnnotation,
        fallbackUrl = fallbackUrl,
        borderColor = borderColor,
        baseCache = baseCache,
        annotationById = annotationById,
    )
  }

  Box(modifier = modifier)
}

/* ----------------------- manager lifecycle helpers ----------------------- */

/** Create a PointAnnotationManager with click listener */
private fun createPointManager(
    mapView: MapView?,
    latestOnClick: (Id) -> Unit,
): PointAnnotationManager? {
  if (mapView == null) return null
  return mapView.annotations.createPointAnnotationManager().apply {
    addClickListener { annotation ->
      annotation.getData()?.asString?.let { latestOnClick(it) }
      true
    }
  }
}

/** Dispose of PointAnnotationManager and clear caches */
@VisibleForTesting
internal fun disposePointManager(
    mapView: MapView?,
    manager: PointAnnotationManager?,
    annotationById: MutableMap<Id, PointAnnotation>,
    baseCache: MutableMap<BaseKey, Bitmap>,
) {
  runCatching { manager?.deleteAll() }
      .onFailure { Log.w(TAG, "Failed to delete all annotations in onDispose", it) }
  runCatching { manager?.let { mapView?.annotations?.removeAnnotationManager(it) } }
      .onFailure { Log.w(TAG, "Failed to remove PointAnnotationManager in onDispose", it) }
  annotationById.clear()
  baseCache.clear()
}

/* ----------------------- pin diff + fade helpers ----------------------- */

/** Apply pin diff: create/update/remove with fade-in for new pins */
private suspend fun applyPinDiffAndFade(
    ctx: Context,
    manager: PointAnnotationManager?,
    pins: List<MapPin>,
    selectedId: Id?,
    fallbackUrl: String,
    borderColor: Int,
    annotationById: MutableMap<Id, PointAnnotation>,
    baseCache: MutableMap<BaseKey, Bitmap>,
) {
  val mgr = manager ?: return

  removeStaleAnnotations(mgr, pins, annotationById)

  for (pin in pins) {
    upsertPinAnnotation(
        ctx = ctx,
        manager = mgr,
        pin = pin,
        selectedId = selectedId,
        fallbackUrl = fallbackUrl,
        borderColor = borderColor,
        annotationById = annotationById,
        baseCache = baseCache,
    )
  }
}

/** Remove annotations that are no longer present in the pins list */
private fun removeStaleAnnotations(
    manager: PointAnnotationManager,
    pins: List<MapPin>,
    annotationById: MutableMap<Id, PointAnnotation>,
) {
  val currentIds = pins.map { it.id }.toSet()
  val toRemove = annotationById.keys - currentIds
  toRemove.forEach { id ->
    annotationById.remove(id)?.let { ann ->
      runCatching { manager.delete(ann) }.onFailure { Log.w(TAG, "delete failed", it) }
    }
  }
}

/** Create or update a pin annotation with optional fade-in for new pins */
private suspend fun upsertPinAnnotation(
    ctx: Context,
    manager: PointAnnotationManager,
    pin: MapPin,
    selectedId: Id?,
    fallbackUrl: String,
    borderColor: Int,
    annotationById: MutableMap<Id, PointAnnotation>,
    baseCache: MutableMap<BaseKey, Bitmap>,
) {
  val id = pin.id
  val isSelected = id == selectedId
  val scale = if (isSelected) 1.25f else 1.0f

  val (base, showExcl) =
      when (pin) {
        is MapPin.ClusterPin -> {
          renderClusterPin(count = pin.count, borderColor = borderColor, scale = scale) to false
        }
        is MapPin.PostPin -> {
          val targetUrl = pin.imageURL.ifBlank { fallbackUrl }
          val baseKey = BaseKey(targetUrl, borderColor, (scale * 100f).toInt())
          val b = getOrCreateBaseBitmap(ctx, baseKey, borderColor, baseCache, scale)
          b to false
        }
        is MapPin.ReportPin -> {
          val targetUrl = pin.imageURL.ifBlank { fallbackUrl }
          val baseKey = BaseKey(targetUrl, borderColor, (scale * 100f).toInt())
          val b = getOrCreateBaseBitmap(ctx, baseKey, borderColor, baseCache, scale)
          val showExcl = pin.assigneeId.isNullOrBlank() && !isSelected
          b to showExcl
        }
      }

  val existing = annotationById[id]
  if (existing == null) {
    createNewAnnotationWithFade(
        manager = manager,
        pin = pin,
        id = id,
        base = base,
        borderColor = borderColor,
        scale = scale,
        showExcl = showExcl,
        annotationById = annotationById,
    )
  } else {
    updateExistingAnnotation(
        manager = manager,
        existing = existing,
        base = base,
        borderColor = borderColor,
        scale = scale,
        showExcl = showExcl,
    )
  }
}

/** Get or create the base bitmap for a pin, using the cache if available */
private suspend fun getOrCreateBaseBitmap(
    ctx: Context,
    baseKey: BaseKey,
    borderColor: Int,
    baseCache: MutableMap<BaseKey, Bitmap>,
    scale: Float,
): Bitmap {
  baseCache[baseKey]?.let {
    return it
  }

  val photo = fetchBitmapViaCoil(ctx, baseKey.url)
  val built = withContext(PinsDispatchers.computation) { renderBasePin(photo, borderColor, scale) }
  baseCache[baseKey] = built
  return built
}

/** Create a new annotation with a fade-in effect */
private suspend fun createNewAnnotationWithFade(
    manager: PointAnnotationManager,
    pin: MapPin,
    id: Id,
    base: Bitmap,
    borderColor: Int,
    scale: Float,
    showExcl: Boolean,
    annotationById: MutableMap<Id, PointAnnotation>,
) {
  val pt = Point.fromLngLat(pin.location.longitude, pin.location.latitude)

  val frame0 =
      withContext(PinsDispatchers.computation) {
        composeOverlays(
            base = base,
            globalAlpha = 0f,
            rippleProgress = null,
            showExclamation = showExcl,
            exclamationOffsetPx = 0f,
            borderColor = borderColor,
            scale = scale,
        )
      }

  val created =
      manager.create(
          PointAnnotationOptions()
              .withPoint(pt)
              .withIconImage(frame0)
              .withIconAnchor(IconAnchor.BOTTOM)
              .withData(JsonPrimitive(id)),
      )

  annotationById[id] = created

  var a: Float
  repeat(FADE_STEPS) { step ->
    a = (step + 1f) / FADE_STEPS
    val bmp =
        withContext(PinsDispatchers.computation) {
          composeOverlays(
              base = base,
              globalAlpha = a,
              rippleProgress = null,
              showExclamation = showExcl,
              exclamationOffsetPx = 0f,
              borderColor = borderColor,
              scale = scale,
          )
        }
    created.iconImageBitmap = bmp
    runCatching { manager.update(created) }.onFailure { Log.w(TAG, "fade update failed", it) }
    delay(FADE_DELAY_MS)
  }
}

/** Update an existing annotation's bitmap */
private suspend fun updateExistingAnnotation(
    manager: PointAnnotationManager,
    existing: PointAnnotation,
    base: Bitmap,
    borderColor: Int,
    scale: Float,
    showExcl: Boolean,
) {
  val bmp =
      withContext(PinsDispatchers.computation) {
        composeOverlays(
            base = base,
            globalAlpha = 1f,
            rippleProgress = null,
            showExclamation = showExcl,
            exclamationOffsetPx = 0f,
            borderColor = borderColor,
            scale = scale,
        )
      }
  existing.iconImageBitmap = bmp
  runCatching { manager.update(existing) }.onFailure { Log.w(TAG, "update failed", it) }
}

/* ----------------------- bobbing helpers ----------------------- */

/** Compute the set of bobbing pin IDs (unassigned report pins, excluding selected) */
@VisibleForTesting
internal fun computeBobbingIds(pins: List<MapPin>, selectedId: Id?): Set<Id> =
    pins
        .asSequence()
        .filter { it is MapPin.ReportPin && it.assigneeId.isNullOrBlank() && it.id != selectedId }
        .map { it.id }
        .toSet()

private suspend fun runBobbingLoop(
    ctx: Context,
    manager: PointAnnotationManager?,
    pins: List<MapPin>,
    bobbingIds: Set<Id>,
    latestSelectedId: Id?,
    fallbackUrl: String,
    borderColor: Int,
    baseCache: MutableMap<BaseKey, Bitmap>,
    bobTimeProvider: () -> Float,
    bobTimeUpdater: (Float) -> Unit,
    annotationById: Map<Id, PointAnnotation>,
) {
  val mgr = manager ?: return
  if (bobbingIds.isEmpty()) return

  while (currentCoroutineContext().isActive && bobbingIds.isNotEmpty()) {
    val nextTime = bobTimeProvider() + BOB_SPEED
    bobTimeUpdater(nextTime)

    for (id in bobbingIds) {
      if (latestSelectedId == id) continue
      val ann = annotationById[id] ?: continue
      val pin = pins.firstOrNull { it.id == id } ?: continue

      val targetUrl = pin.imageURL.ifBlank { fallbackUrl }
      val scale = 1.0f
      val baseKey = BaseKey(targetUrl, borderColor, (scale * 100f).toInt())
      val base = getOrCreateBaseBitmap(ctx, baseKey, borderColor, baseCache, scale)

      val bobOffset = (sin(nextTime * 2f * PI).toFloat() * BOB_AMP_PX)
      val bmp =
          withContext(PinsDispatchers.computation) {
            composeOverlays(
                base = base,
                globalAlpha = 1f,
                rippleProgress = null,
                showExclamation = true,
                exclamationOffsetPx = bobOffset,
                borderColor = borderColor,
                scale = scale,
            )
          }
      ann.iconImageBitmap = bmp
      runCatching { mgr.update(ann) }.onFailure { Log.w(TAG, "bob update failed", it) }
    }

    delay(BOB_FRAME_MS)
  }
}

/* ----------------------- selection ripple helpers ----------------------- */

/** Run the selection ripple animation for the selected pin */
private suspend fun runSelectionRipple(
    ctx: Context,
    manager: PointAnnotationManager?,
    pins: List<MapPin>,
    selectedId: Id?,
    selectedAnnotation: PointAnnotation?,
    fallbackUrl: String,
    borderColor: Int,
    baseCache: MutableMap<BaseKey, Bitmap>,
    annotationById: Map<Id, PointAnnotation>,
) {
  val mgr = manager ?: return
  val sel = selectedId ?: return
  val ann = selectedAnnotation ?: return
  val pin = pins.firstOrNull { it.id == sel } ?: return

  val targetUrl = pin.imageURL.ifBlank { fallbackUrl }
  val scale = 1.25f
  val baseKey = BaseKey(targetUrl, borderColor, (scale * 100f).toInt())
  val base = getOrCreateBaseBitmap(ctx, baseKey, borderColor, baseCache, scale)

  var t = 0f
  while (currentCoroutineContext().isActive && annotationById[sel] === ann) {
    val frame =
        withContext(PinsDispatchers.computation) {
          composeOverlays(
              base = base,
              globalAlpha = 1f,
              rippleProgress = t,
              showExclamation = false,
              exclamationOffsetPx = 0f,
              borderColor = borderColor,
              scale = scale,
          )
        }
    ann.iconImageBitmap = frame
    runCatching { mgr.update(ann) }.onFailure { Log.w(TAG, "ripple update failed", it) }
    t += RIPPLE_SPEED
    if (t >= 1f) t -= 1f
    delay(RIPPLE_FRAME_MS)
  }
}

/* ----------------------- Rendering helpers ----------------------- */

/** Render the base pin bitmap with optional photo and border. */
@WorkerThread
@VisibleForTesting
internal fun renderBasePin(src: Bitmap?, borderColor: Int, scale: Float): Bitmap {
  val circleBoxNoRipple = BASE_CIRCLE * scale
  val width = circleBoxNoRipple.toInt()
  val height = (circleBoxNoRipple + BAR_H * scale).toInt()
  val out = createBitmap(width, height)
  val c = Canvas(out)
  val p = Paint(Paint.ANTI_ALIAS_FLAG)
  val cx = width / 2f
  val cy = circleBoxNoRipple / 2f
  val imgRadius = (circleBoxNoRipple / 2f) - PADDING * scale
  val strokeWidth = 10f * scale

  if (src != null) {
    val minSide = minOf(src.width, src.height)
    val srcRect =
        Rect(
            (src.width - minSide) / 2,
            (src.height - minSide) / 2,
            (src.width + minSide) / 2,
            (src.height + minSide) / 2,
        )
    val dst = RectF(cx - imgRadius, cy - imgRadius, cx + imgRadius, cy + imgRadius)
    val clip = Path().apply { addCircle(cx, cy, imgRadius, Path.Direction.CW) }
    c.withClip(clip) { drawBitmap(src, srcRect, dst, null) }
  } else {
    p.color = Color.LTGRAY
    c.drawCircle(cx, cy, imgRadius, p)
  }

  val circleBottom = cy + imgRadius
  p.style = Paint.Style.FILL
  p.color = borderColor
  val stickWidth = BAR_W * scale
  val overlap = 3f * scale
  val barRect =
      RectF(
          cx - stickWidth / 2f,
          circleBottom - overlap,
          cx + stickWidth / 2f,
          circleBottom - overlap + BAR_H * scale,
      )
  c.drawRoundRect(barRect, stickWidth / 2f, stickWidth / 2f, p)

  p.style = Paint.Style.STROKE
  p.color = borderColor
  p.strokeWidth = strokeWidth
  c.drawCircle(cx, cy, imgRadius, p)

  return out
}

/** Compose the overlay bitmap with optional ripple and exclamation badge. */
@WorkerThread
@VisibleForTesting
internal fun composeOverlays(
    base: Bitmap,
    globalAlpha: Float,
    rippleProgress: Float?,
    showExclamation: Boolean,
    exclamationOffsetPx: Float,
    borderColor: Int,
    scale: Float,
): Bitmap {
  val circleBoxNoRipple = BASE_CIRCLE * scale
  val imgRadius = (circleBoxNoRipple / 2f) - PADDING * scale
  val bobMax = BOB_AMP_PX * scale
  val badgeHeadroom = if (showExclamation) (BADGE_GAP * scale + bobMax) else 0f
  val outW = base.width
  val outH = (badgeHeadroom + base.height).toInt()
  val out = createBitmap(outW, outH)
  val c = Canvas(out)
  val p =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.alpha = (globalAlpha.coerceIn(0f, 1f) * 255).toInt()
      }
  c.drawBitmap(base, 0f, badgeHeadroom, p)
  val cx = outW / 2f
  val cy = badgeHeadroom + (circleBoxNoRipple / 2f)
  rippleProgress?.let { prog ->
    val rippleMax = 16f * scale
    repeat(3) { i ->
      val basePh = prog + (i / 3f)
      val phase = basePh - floor(basePh)
      val r = imgRadius + (rippleMax * phase)
      val fade = ((1f - phase) * 0.35f)
      val sw = (4.5f * (1f - phase) + 1.2f) * scale
      p.style = Paint.Style.STROKE
      p.strokeWidth = sw
      p.color = borderColor
      p.alpha = (fade * globalAlpha * 255).toInt().coerceIn(0, 255)
      c.drawCircle(cx, cy, r, p)
    }
    p.alpha = (globalAlpha * 255).toInt()
  }
  if (showExclamation) {
    val badgeScale = 0.95f * scale
    val ringR = BADGE_RING_R * badgeScale
    val clampedOffset = exclamationOffsetPx.coerceIn(-bobMax, bobMax)
    val badgeCx = cx
    val badgeCy = (cy - imgRadius) - (BADGE_GAP * scale - 8f * scale) + clampedOffset
    p.style = Paint.Style.STROKE
    p.strokeWidth = BADGE_STROKE * badgeScale
    p.color = borderColor
    c.drawCircle(badgeCx, badgeCy, ringR, p)
    p.style = Paint.Style.FILL
    val barW = BADGE_BAR_W * badgeScale
    val barH = BADGE_BAR_H * badgeScale
    val barRect =
        RectF(
            badgeCx - barW / 2f,
            badgeCy - barH * 0.95f,
            badgeCx + barW / 2f,
            badgeCy - barH * 0.05f,
        )
    c.drawRoundRect(barRect, barW / 2f, barW / 2f, p)
    val dotR = BADGE_DOT_R * badgeScale
    c.drawCircle(badgeCx, badgeCy + ringR * 0.45f, dotR, p)
  }

  return out
}

/** Render a cluster pin bitmap with count text. */
@WorkerThread
@VisibleForTesting
internal fun renderClusterPin(count: Int, borderColor: Int, scale: Float): Bitmap {
  val circleBox = BASE_CIRCLE * scale
  val width = circleBox.toInt()
  val height = width
  val out = createBitmap(width, height)
  val c = Canvas(out)
  val p = Paint(Paint.ANTI_ALIAS_FLAG)
  val cx = width / 2f
  val cy = height / 2f
  val radius = (circleBox / 2f) - PADDING * scale
  p.style = Paint.Style.FILL
  p.color = borderColor
  c.drawCircle(cx, cy, radius, p)
  p.color = Color.WHITE
  p.textAlign = Paint.Align.CENTER
  p.textSize = 32f * scale
  val text = if (count > 99) "99+" else count.toString()
  val textBounds = Rect()
  p.getTextBounds(text, 0, text.length, textBounds)
  val textY = cy - textBounds.exactCenterY()
  c.drawText(text, cx, textY, p)

  return out
}

/* ----------------------- image loading ----------------------- */

/** Fetch a Bitmap from a URL using Coil. */
@VisibleForTesting
internal suspend fun fetchBitmapViaCoil(ctx: Context, url: String): Bitmap? =
    withContext(PinsDispatchers.io) {
      try {
        val loader = SharedCoil.get(ctx)
        val req = ImageRequest.Builder(ctx).data(url).allowHardware(false).size(PHOTO_SIZE).build()
        val result = loader.execute(req)
        if (result is SuccessResult) {
          val d = result.drawable
          if (d is BitmapDrawable) d.bitmap
          else {
            val bmp = createBitmap(maxOf(1, d.intrinsicWidth), maxOf(1, d.intrinsicHeight))
            val c = Canvas(bmp)
            d.setBounds(0, 0, c.width, c.height)
            d.draw(c)
            bmp
          }
        } else null
      } catch (t: Throwable) {
        Log.w(TAG, "Coil load failed for $url", t)
        null
      }
    }
