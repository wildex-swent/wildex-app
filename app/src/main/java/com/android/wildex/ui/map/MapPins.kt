package com.android.wildex.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.WorkerThread
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.android.wildex.model.map.MapPin
import com.android.wildex.model.utils.Id
import com.google.gson.JsonPrimitive
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import kotlin.collections.forEach
import kotlin.collections.set
import kotlinx.coroutines.launch

@Composable
fun PinsOverlay(
    modifier: Modifier,
    mapView: MapView?,
    pins: List<MapPin>,
    onPinClick: (Id) -> Unit,
) {
  val ctx = LocalContext.current

  // fallback URL for any missing/failed photos
  val fallbackUrl =
      "https://media.istockphoto.com/id/1223671392/vector/default-profile-picture-avatar-photo-placeholder-vector-illustration.jpg?s=612x612&w=0&k=20&c=s0aTdmT5aU6b8ot7VKm11DeID6NctRCpB755rA1BIP0="

  // caches
  val iconCache = remember { mutableStateMapOf<String, Bitmap>() } // url -> bitmap (circular)
  val annotationById = remember {
    mutableStateMapOf<Id, com.mapbox.maps.plugin.annotation.generated.PointAnnotation>()
  }

  var manager by remember(mapView) { mutableStateOf<PointAnnotationManager?>(null) }

  // lifecycle: create / dispose annotation manager
  DisposableEffect(mapView) {
    if (mapView != null) {
      manager =
          mapView.annotations.createPointAnnotationManager().apply {
            addClickListener(
                OnPointAnnotationClickListener { annotation ->
                  annotation.getData()?.asString?.let(onPinClick)
                  true
                })
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

  LaunchedEffect(manager, pins) {
    val mgr = manager ?: return@LaunchedEffect

    val currentIds = pins.map { it.id }.toSet()
    val toRemove = annotationById.keys - currentIds
    toRemove.forEach { id -> annotationById.remove(id)?.let { runCatching { mgr.delete(it) } } }

    pins.forEach { pin ->
      // 1) get or create fallback circular bitmap (cache by fallbackUrl)
      val fallback =
          iconCache.getOrPut(fallbackUrl) {
            val bmp = fetchBitmapViaCoil(ctx, fallbackUrl)
            makeCircularBitmap(bmp) // returns non-null circular 128x128 or a generated circle
          }

      // 2) if annotation missing, create it immediately with fallback
      val existing = annotationById[pin.id]
      if (existing == null) {
        val pt = Point.fromLngLat(pin.location.longitude, pin.location.latitude)
        val opts =
            PointAnnotationOptions()
                .withPoint(pt)
                .withIconImage(fallback)
                .withData(JsonPrimitive(pin.id))
        val created = mgr.create(opts)
        annotationById[pin.id] = created
      }

      // 3) fetch/upgrade to real photo (circular); skip if already cached
      val targetUrl = pin.imageURL.ifBlank { fallbackUrl }
      val cached = iconCache[targetUrl]
      if (cached != null) {
        // update annotation icon if it’s still using the fallback
        val ann = annotationById[pin.id]
        if (ann != null && ann.iconImageBitmap != cached) {
          ann.iconImageBitmap = cached
          runCatching { mgr.update(ann) }
        }
      } else {
        // load asynchronously, then cache + update
        launch {
          val raw = fetchBitmapViaCoil(ctx, targetUrl)
          val rounded = makeCircularBitmap(raw)
          iconCache[targetUrl] = rounded
          val ann = annotationById[pin.id]
          if (ann != null) {
            ann.iconImageBitmap = rounded
            runCatching { mgr.update(ann) }
          }
        }
      }
    }
  }

  Box(modifier = modifier)
}

@WorkerThread
private fun makeCircularBitmap(src: Bitmap?): Bitmap {
  // if null, make a simple colored circle (128x128)
  val size = 128
  val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
  val c = Canvas(out)
  val paint = Paint(Paint.ANTI_ALIAS_FLAG)

  if (src == null) {
    paint.color = Color.rgb(200, 200, 200)
    c.drawCircle(size / 2f, size / 2f, size * 0.5f, paint)
    return out
  }

  // scale to square
  val minSide = minOf(src.width, src.height)
  val srcRect =
      Rect(
          (src.width - minSide) / 2,
          (src.height - minSide) / 2,
          (src.width + minSide) / 2,
          (src.height + minSide) / 2,
      )
  val dstRect = Rect(0, 0, size, size)

  // draw circle mask
  val path = Path().apply { addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW) }
  c.save()
  c.clipPath(path)
  c.drawBitmap(src, srcRect, dstRect, null)
  c.restore()

  return out
}

/* ---------- Image helper (fetch only; no custom drawing) ---------- */
private suspend fun fetchBitmapViaCoil(ctx: Context, url: String): Bitmap? {
  return try {
    val loader =
        ImageLoader.Builder(ctx)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    val req = ImageRequest.Builder(ctx).data(url).allowHardware(false).size(128).build()
    val result = loader.execute(req)
    if (result is SuccessResult) {
      val d = result.drawable
      if (d is BitmapDrawable) d.bitmap
      else {
        val bmp =
            Bitmap.createBitmap(
                maxOf(1, d.intrinsicWidth),
                maxOf(1, d.intrinsicHeight),
                Bitmap.Config.ARGB_8888,
            )
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
