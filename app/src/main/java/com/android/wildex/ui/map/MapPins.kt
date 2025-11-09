package com.android.wildex.ui.map

import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
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

/**
 * THIS IS JUST A TEMPORARY PLACEHOLDER. THE ACTUAL PinsOverlay FILE WAS TOO LONG SO I DECIDED TO
 * PUT IT IN ANOTHER PR. WHAT THIS CURRENT FILE DOES IS JUST PLACES RED DOTS WHERE THE POSTS ARE,
 * THERE ARE NO ANIMATIONS TAB-SPECIFIC COLOR RULES.
 */
@Composable
fun PinsOverlay(
    modifier: Modifier,
    mapView: MapView?,
    pins: List<MapPin>,
    currentTab: MapTab,
    selectedId: Id?,
    onPinClick: (Id) -> Unit
) {
  val annotationById = remember { mutableStateMapOf<Id, PointAnnotation>() }
  var manager by remember(mapView) { mutableStateOf<PointAnnotationManager?>(null) }
  val latestOnClick by rememberUpdatedState(onPinClick)

  // Manager lifecycle
  DisposableEffect(mapView) {
    if (mapView != null) {
      manager =
          mapView.annotations.createPointAnnotationManager().apply {
            addClickListener { ann ->
              ann.getData()?.asString?.let { latestOnClick(it) }
              true
            }
          }
    }
    onDispose {
      runCatching { manager?.deleteAll() }
      runCatching { manager?.let { mapView?.annotations?.removeAnnotationManager(it) } }
      manager = null
      annotationById.clear()
    }
  }
  val dotBitmap = remember {
    val size = 40 // bigger dot
    val bmp = createBitmap(size, size)
    val c = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = Color.Red.toArgb()
    c.drawCircle(size / 2f, size / 2f, size / 2f, p)
    bmp
  }
  LaunchedEffect(manager, pins) {
    val mgr = manager ?: return@LaunchedEffect
    val currentIds = pins.map { it.id }.toSet()
    val toRemove = annotationById.keys - currentIds
    toRemove.forEach { id -> annotationById.remove(id)?.let { runCatching { mgr.delete(it) } } }
    for (pin in pins) {
      val existing = annotationById[pin.id]
      if (existing == null) {
        val pt = Point.fromLngLat(pin.location.longitude, pin.location.latitude)
        val created =
            mgr.create(
                PointAnnotationOptions()
                    .withPoint(pt)
                    .withIconImage(dotBitmap)
                    .withData(JsonPrimitive(pin.id)))
        annotationById[pin.id] = created
      } else {
        existing.iconImageBitmap = dotBitmap
        runCatching { mgr.update(existing) }
      }
    }
  }
  Box(modifier)
}
