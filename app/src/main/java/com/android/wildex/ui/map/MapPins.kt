package com.android.wildex.ui.map

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
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
import com.mapbox.maps.plugin.annotation.generated.*

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

  val annotationById = remember { mutableStateMapOf<Id, PointAnnotation>() }
  var manager by remember(mapView) { mutableStateOf<PointAnnotationManager?>(null) }
  var previousSelectedId by remember { mutableStateOf<Id?>(null) }

  // Create and dispose annotation manager
  DisposableEffect(mapView) {
    if (mapView != null) {
      manager =
          mapView.annotations.createPointAnnotationManager().apply {
            addClickListener { annotation ->
              annotation.getData()?.asString?.let(onPinClick)
              true
            }
          }
    }
    onDispose {
      val mgr = manager
      runCatching { mgr?.deleteAll() }
      runCatching { mgr?.let { mapView?.annotations?.removeAnnotationManager(it) } }
      manager = null
      annotationById.clear()
    }
  }

  // Create or update annotations
  LaunchedEffect(manager, pins, currentTab) {
    val mgr = manager ?: return@LaunchedEffect

    val currentIds = pins.map { it.id }.toSet()
    val toRemove = annotationById.keys - currentIds
    toRemove.forEach { id -> annotationById.remove(id)?.let { runCatching { mgr.delete(it) } } }

    pins.forEach { pin ->
      val targetUrl = pin.imageURL.ifBlank { fallbackUrl }
      val bmp = fetchBitmapViaCoil(ctx, targetUrl)
      if (bmp == null) return@forEach

      // 👇 determine color intensity based on pin type
      val borderColor =
          when (pin) {
            is MapPin.ReportPin ->
                if (pin.assigneeId != null)
                    ui.bg.copy(alpha = 0.4f).toArgb() // duller tone for assigned reports
                else ui.bg.toArgb() // normal color for unassigned
            else -> ui.bg.toArgb() // regular posts
          }

      val icon =
          makeRoundPin(
              src = bmp,
              borderColor = borderColor,
              scale = if (pin.id == selectedId) 1.25f else 1.0f)

      val existing = annotationById[pin.id]
      if (existing == null) {
        val pt = Point.fromLngLat(pin.location.longitude, pin.location.latitude)
        val created =
            mgr.create(
                PointAnnotationOptions()
                    .withPoint(pt)
                    .withIconImage(icon)
                    .withData(JsonPrimitive(pin.id)))
        annotationById[pin.id] = created
      } else {
        existing.iconImageBitmap = icon
        runCatching { mgr.update(existing) }
      }
    }
  }

  // Animate size change when selecting/deselecting pins
  LaunchedEffect(selectedId) {
    val mgr = manager ?: return@LaunchedEffect

    // Shrink old
    previousSelectedId?.let { oldId ->
      val ann = annotationById[oldId] ?: return@let
      val pin = pins.firstOrNull { it.id == oldId } ?: return@let
      val bmp = fetchBitmapViaCoil(ctx, pin.imageURL.ifBlank { fallbackUrl }) ?: return@let
      ann.iconImageBitmap = makeRoundPin(bmp, ui.bg.toArgb(), 1.0f)
      runCatching { mgr.update(ann) }
    }

    // Grow new
    selectedId?.let { newId ->
      val ann = annotationById[newId] ?: return@let
      val pin = pins.firstOrNull { it.id == newId } ?: return@let
      val bmp = fetchBitmapViaCoil(ctx, pin.imageURL.ifBlank { fallbackUrl }) ?: return@let
      ann.iconImageBitmap = makeRoundPin(bmp, ui.bg.toArgb(), 1.25f)
      runCatching { mgr.update(ann) }
    }

    previousSelectedId = selectedId
  }

  Box(modifier = modifier)
}

/** Draws a circular post/author image with a colored ring and stem. */
@WorkerThread
private fun makeRoundPin(src: Bitmap?, borderColor: Int, scale: Float = 1.0f): Bitmap {
  val baseCircle = 100
  val padding = 12
  val barWidth = 10
  val barHeight = 80

  val circleBox = (baseCircle * scale).toInt()
  val width = circleBox
  val height = circleBox + (barHeight * scale).toInt()

  val out = createBitmap(width, height)
  val c = Canvas(out)
  val p = Paint(Paint.ANTI_ALIAS_FLAG)

  val cx = width / 2f
  val cy = circleBox / 2f
  val imgRadius = (circleBox / 2f) - padding * scale
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

  // stem
  p.style = Paint.Style.FILL
  p.color = borderColor
  val stickWidth = barWidth * scale
  val overlap = 3f * scale
  val barRect =
      RectF(
          cx - stickWidth / 2f,
          circleBottom - overlap,
          cx + stickWidth / 2f,
          circleBottom - overlap + barHeight * scale)
  c.drawRoundRect(barRect, stickWidth / 2f, stickWidth / 2f, p)

  // ring
  p.style = Paint.Style.STROKE
  p.color = borderColor
  p.strokeWidth = strokeWidth
  c.drawCircle(cx, cy, imgRadius, p)

  return out
}

/** Loads an image using Coil. Coil already caches images in memory and disk. */
private suspend fun fetchBitmapViaCoil(ctx: Context, url: String): Bitmap? {
  return try {
    val loader =
        ImageLoader.Builder(ctx)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    val req = ImageRequest.Builder(ctx).data(url).allowHardware(false).size(256).build()
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
  } catch (_: Exception) {
    null
  }
}
