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

/* ----------------------- Image loader  ----------------------- */
@VisibleForTesting
internal object SharedCoil {
  @Volatile private var instance: ImageLoader? = null

  fun get(ctx: Context): ImageLoader =
      instance
          ?: ImageLoader.Builder(ctx)
              .diskCachePolicy(CachePolicy.ENABLED)
              .memoryCachePolicy(CachePolicy.ENABLED)
              .build()
              .also { instance = it }
}

/* ----------------------- Base icon cache (to avoid repeating the rebuilding each frame)  ----------------------- */
@VisibleForTesting
internal data class BaseKey(
    val url: String,
    val borderColor: Int,
    val scaleKey: Int,
)
/* ----------------------- Composable ----------------------- */
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
  var previousSelectedId by remember { mutableStateOf<Id?>(null) }

  val baseCache: MutableMap<BaseKey, Bitmap> = remember { mutableMapOf() }
  val latestOnClick by rememberUpdatedState(onPinClick)
  val latestSelectedId by rememberUpdatedState(selectedId)

  // One global ticker for bobbing “!”
  var bobTime by remember { mutableFloatStateOf(0f) }
  var bobbingIds by remember { mutableStateOf<Set<Id>>(emptySet()) }

  /* ------------- lifecycle of manager + click listener ------------- */
  DisposableEffect(mapView, currentTab) {
    if (mapView != null) {
      manager =
          mapView.annotations.createPointAnnotationManager().apply {
            addClickListener { annotation ->
              annotation.getData()?.asString?.let { latestOnClick(it) }
              true
            }
          }
    }
    onDispose {
      try {
        manager?.deleteAll()
      } catch (_: Throwable) {}
      try {
        manager?.let { mapView?.annotations?.removeAnnotationManager(it) }
      } catch (_: Throwable) {}
      manager = null
      annotationById.clear()
      baseCache.clear()
    }
  }

  /* ------------- apply pin diff (create/update/remove + fade-in) ------------- */
  LaunchedEffect(manager, pins) {
    val mgr = manager ?: return@LaunchedEffect

    val currentIds = pins.map { it.id }.toSet()
    val toRemove = annotationById.keys - currentIds
    toRemove.forEach { id ->
      annotationById.remove(id)?.let { ann ->
        runCatching { mgr.delete(ann) }.onFailure { Log.w(TAG, "delete failed", it) }
      }
    }

    for (pin in pins) {
      val id = pin.id
      val isSelected = id == selectedId
      val showExcl = pin is MapPin.ReportPin && pin.assigneeId.isNullOrBlank() && !isSelected
      val scale = if (isSelected) 1.25f else 1.0f
      val targetUrl = pin.imageURL.ifBlank { fallbackUrl }

      val baseKey = BaseKey(targetUrl, borderColor, (scale * 100f).toInt())
      val base =
          baseCache[baseKey]
              ?: withContext(Dispatchers.Default) {
                    val photo = fetchBitmapViaCoil(ctx, targetUrl)
                    renderBasePin(photo, borderColor, scale)
                  }
                  .also { baseCache[baseKey] = it }

      val existing = annotationById[id]
      if (existing == null) {
        val pt = Point.fromLngLat(pin.location.longitude, pin.location.latitude)
        val frame0 =
            withContext(Dispatchers.Default) {
              composeOverlays(
                  base = base,
                  globalAlpha = 0f,
                  rippleProgress = null,
                  showExclamation = showExcl,
                  exclamationOffsetPx = 0f,
                  borderColor = borderColor,
                  scale = scale)
            }
        val created =
            mgr.create(
                PointAnnotationOptions()
                    .withPoint(pt)
                    .withIconImage(frame0)
                    .withData(JsonPrimitive(id)) // stored as string
                )
        annotationById[id] = created

        var a: Float
        repeat(FADE_STEPS) { it ->
          a = ((it + 1f) / FADE_STEPS)
          val bmp =
              withContext(Dispatchers.Default) {
                composeOverlays(
                    base = base,
                    globalAlpha = a,
                    rippleProgress = null,
                    showExclamation = showExcl,
                    exclamationOffsetPx = 0f,
                    borderColor = borderColor,
                    scale = scale)
              }
          created.iconImageBitmap = bmp
          runCatching { mgr.update(created) }.onFailure { Log.w(TAG, "fade update failed", it) }
          delay(FADE_DELAY_MS)
        }
      } else {
        val bmp =
            withContext(Dispatchers.Default) {
              composeOverlays(
                  base = base,
                  globalAlpha = 1f,
                  rippleProgress = null,
                  showExclamation = showExcl,
                  exclamationOffsetPx = 0f,
                  borderColor = borderColor,
                  scale = scale)
            }
        existing.iconImageBitmap = bmp
        runCatching { mgr.update(existing) }.onFailure { Log.w(TAG, "update failed", it) }
      }
    }
  }

  /* ------------- recompute bobbing ids when pins OR selection changes ------------- */
  LaunchedEffect(pins, selectedId) {
    bobbingIds =
        pins
            .asSequence()
            .filter {
              it is MapPin.ReportPin && it.assigneeId.isNullOrBlank() && it.id != selectedId
            }
            .map { it.id }
            .toSet()
  }

  /* ------------- single bobbing ticker for all unassigned pins ------------- */
  LaunchedEffect(bobbingIds, manager, pins) {
    val mgr = manager ?: return@LaunchedEffect
    if (bobbingIds.isEmpty()) return@LaunchedEffect

    while (isActive && bobbingIds.isNotEmpty()) {
      bobTime += BOB_SPEED
      for (id in bobbingIds) {
        if (latestSelectedId == id) continue
        val ann = annotationById[id] ?: continue
        val pin = pins.firstOrNull { it.id == id } ?: continue
        val targetUrl = pin.imageURL.ifBlank { fallbackUrl }
        val scale = 1.0f
        val baseKey = BaseKey(targetUrl, borderColor, (scale * 100f).toInt())
        val base =
            baseCache[baseKey]
                ?: withContext(Dispatchers.Default) {
                      val photo = fetchBitmapViaCoil(ctx, targetUrl)
                      renderBasePin(photo, borderColor, scale)
                    }
                    .also { baseCache[baseKey] = it }

        val bob = (sin(bobTime * 2f * PI).toFloat() * BOB_AMP_PX)
        val bmp =
            withContext(Dispatchers.Default) {
              composeOverlays(
                  base = base,
                  globalAlpha = 1f,
                  rippleProgress = null,
                  showExclamation = true,
                  exclamationOffsetPx = bob,
                  borderColor = borderColor,
                  scale = scale)
            }
        ann.iconImageBitmap = bmp
        runCatching { mgr.update(ann) }.onFailure { Log.w(TAG, "bob update failed", it) }
      }
      delay(BOB_FRAME_MS)
    }
  }

  /* ------------- selection ripple: only for selected pin ------------- */
  LaunchedEffect(selectedId, manager, pins) {
    val mgr = manager ?: return@LaunchedEffect
    val sel = selectedId ?: return@LaunchedEffect
    val ann = annotationById[sel] ?: return@LaunchedEffect
    val pin = pins.firstOrNull { it.id == sel } ?: return@LaunchedEffect
    val targetUrl = pin.imageURL.ifBlank { fallbackUrl }
    val scale = 1.25f
    val baseKey = BaseKey(targetUrl, borderColor, (scale * 100f).toInt())
    val base =
        baseCache[baseKey]
            ?: withContext(Dispatchers.Default) {
                  val photo = fetchBitmapViaCoil(ctx, targetUrl)
                  renderBasePin(photo, borderColor, scale)
                }
                .also { baseCache[baseKey] = it }

    var t = 0f
    while (isActive && annotationById[sel] === ann) {
      val frame =
          withContext(Dispatchers.Default) {
            composeOverlays(
                base = base,
                globalAlpha = 1f,
                rippleProgress = t,
                showExclamation = false,
                exclamationOffsetPx = 0f,
                borderColor = borderColor,
                scale = scale)
          }
      ann.iconImageBitmap = frame
      runCatching { mgr.update(ann) }.onFailure { Log.w(TAG, "ripple update failed", it) }
      t += RIPPLE_SPEED
      if (t >= 1f) t -= 1f
      delay(RIPPLE_FRAME_MS)
    }
  }

  /* ------------- restore previously selected pin visuals ------------- */
  LaunchedEffect(selectedId) {
    val mgr = manager ?: return@LaunchedEffect
    previousSelectedId?.let { oldId ->
      val ann = annotationById[oldId] ?: return@let
      val pin = pins.firstOrNull { it.id == oldId } ?: return@let
      val targetUrl = pin.imageURL.ifBlank { fallbackUrl }
      val scale = 1.0f
      val showExcl = pin is MapPin.ReportPin && pin.assigneeId.isNullOrBlank()
      val baseKey = BaseKey(targetUrl, borderColor, (scale * 100f).toInt())
      val base =
          baseCache[baseKey]
              ?: withContext(Dispatchers.Default) {
                    val photo = fetchBitmapViaCoil(ctx, targetUrl)
                    renderBasePin(photo, borderColor, scale)
                  }
                  .also { baseCache[baseKey] = it }

      val bmp =
          withContext(Dispatchers.Default) {
            composeOverlays(
                base = base,
                globalAlpha = 1f,
                rippleProgress = null,
                showExclamation = showExcl,
                exclamationOffsetPx = 0f,
                borderColor = borderColor,
                scale = scale)
          }
      ann.iconImageBitmap = bmp
      runCatching { mgr.update(ann) }.onFailure { Log.w(TAG, "restore update failed", it) }
    }
    previousSelectedId = selectedId
  }

  Box(modifier = modifier)
}

/* ----------------------- Rendering helpers ----------------------- */

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
          circleBottom - overlap + BAR_H * scale)
  c.drawRoundRect(barRect, stickWidth / 2f, stickWidth / 2f, p)

  p.style = Paint.Style.STROKE
  p.color = borderColor
  p.strokeWidth = strokeWidth
  c.drawCircle(cx, cy, imgRadius, p)

  return out
}

@WorkerThread
@VisibleForTesting
internal fun composeOverlays(
    base: Bitmap,
    globalAlpha: Float,
    rippleProgress: Float?,
    showExclamation: Boolean,
    exclamationOffsetPx: Float,
    borderColor: Int,
    scale: Float
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
            badgeCy - barH * 0.05f)
    c.drawRoundRect(barRect, barW / 2f, barW / 2f, p)
    val dotR = BADGE_DOT_R * badgeScale
    c.drawCircle(badgeCx, badgeCy + ringR * 0.45f, dotR, p)
  }

  return out
}
/* ----------------------- image loading ----------------------- */
@VisibleForTesting
internal suspend fun fetchBitmapViaCoil(ctx: Context, url: String): Bitmap? =
    withContext(Dispatchers.IO) {
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
