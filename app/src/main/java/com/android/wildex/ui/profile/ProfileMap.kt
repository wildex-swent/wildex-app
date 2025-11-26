package com.android.wildex.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.android.wildex.AppTheme
import com.android.wildex.R
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.utils.Id
import com.mapbox.common.toValue
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.util.isEmpty
import kotlin.math.*

/**
 * Profile map card with static mini-map and "View Map" button.
 *
 * @param id The profile ID to view on the map.
 * @param onMap Callback when "View Map" button is clicked.
 * @param pins List of geodetic points to show as pins on the mini-map.
 */
@Composable
fun ProfileMap(id: Id = "", onMap: (Id) -> Unit = {}, pins: List<Point> = emptyList()) {
  val cs = colorScheme
  val context = LocalContext.current
  val styleUri = context.getString(R.string.map_style)
  val styleImportId = context.getString(R.string.map_standard_import)
  val isDark =
      when (AppTheme.appearanceMode) {
        AppearanceMode.DARK -> true
        AppearanceMode.LIGHT -> false
        AppearanceMode.AUTOMATIC -> isSystemInDarkTheme()
      }
  ElevatedCard(
      modifier =
          Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag(ProfileScreenTestTags.MAP),
      shape = RoundedCornerShape(14.dp),
  ) {
    Column(
        modifier =
            Modifier.border(
                    1.dp,
                    cs.onBackground.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(12.dp)) {
          StaticMiniMap(
              modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
              pins = pins,
              styleUri = styleUri,
              styleImportId = styleImportId,
              isDark = isDark,
              context = context,
          )
          Button(
              onClick = { onMap(id) },
              modifier = Modifier.padding(top = 10.dp).testTag(ProfileScreenTestTags.MAP_CTA),
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = cs.primary,
                      contentColor = cs.onPrimary,
                  ),
          ) {
            Text(text = LocalContext.current.getString(R.string.view_map))
          }
        }
  }
}

/**
 * Static (non-interactive) Mapbox map with simple pins.
 * - All gestures disabled (no pan/zoom/rotate).
 * - Uses a local subset of pins within a radius to avoid global zoom.
 * - Caps min zoom so the globe is never shown.
 */
@Composable
fun StaticMiniMap(
    modifier: Modifier = Modifier,
    pins: List<Point>,
    styleUri: String,
    styleImportId: String,
    isDark: Boolean,
    fallbackCenter: Point = Point.fromLngLat(6.632, 46.519),
    fallbackZoom: Double = 11.0,
    context: Context,
) {
  val fitPadding = EdgeInsets(30.0, 30.0, 30.0, 30.0)

  val localPins =
      filterPinsWithinRadius(pins).ifEmpty { pins.takeIf { it.isNotEmpty() } ?: emptyList() }

  Box(modifier) {
    MapboxMap(modifier = Modifier.matchParentSize(), scaleBar = {}, logo = {}, attribution = {}) {
      MapEffect(localPins, isDark, styleUri, styleImportId) { mapView ->
        configureStaticGestures(mapView)

        val mapboxMap = mapView.mapboxMap
        mapboxMap.loadStyle(styleUri) { style ->
          applyLightPreset(style, styleImportId, isDark)

          val pinBitmap = loadVectorAsBitmap(context, R.drawable.ic_map_pin)
          val manager = createPinManager(mapView)
          addPins(manager, localPins, pinBitmap)

          updateCameraForPins(
              mapboxMap = mapboxMap,
              pins = localPins,
              fallbackCenter = fallbackCenter,
              fallbackZoom = fallbackZoom,
              fitPadding = fitPadding,
          )
        }
      }
    }
  }
}

/**
 * Load a vector drawable as a Bitmap for Mapbox pin icon.
 *
 * @param context The Android context.
 * @param resId The resource ID of the vector drawable.
 * @return The loaded Bitmap.
 */
private fun loadVectorAsBitmap(
    context: Context,
    resId: Int,
): Bitmap {
  val d = AppCompatResources.getDrawable(context, resId) ?: return createBitmap(1, 1)
  val bmp = createBitmap(64, 64)
  val canvas = Canvas(bmp)
  d.setBounds(0, 0, 64, 64)
  d.draw(canvas)
  return bmp
}

/* ---------------- Map helpers ---------------- */

/** Configure the MapView to disable all user gestures for a static map. */
private fun configureStaticGestures(mapView: MapView) {
  mapView.gestures.apply {
    rotateEnabled = false
    pinchToZoomEnabled = false
    scrollEnabled = false
    pitchEnabled = false
    doubleTapToZoomInEnabled = false
    quickZoomEnabled = false
  }
}

/** Apply light/dark preset to the map style import configuration. */
private fun applyLightPreset(style: Style, styleImportId: String, isDark: Boolean) {
  runCatching {
    style.setStyleImportConfigProperty(
        styleImportId,
        "lightPreset",
        (if (isDark) "night" else "day").toValue(),
    )
  }
}

/** Create a PointAnnotationManager and clear existing annotations. */
private fun createPinManager(mapView: MapView): PointAnnotationManager {
  return mapView.annotations.createPointAnnotationManager().apply { deleteAll() }
}

/** Add pins to the PointAnnotationManager. */
private fun addPins(
    manager: PointAnnotationManager,
    pins: List<Point>,
    pinBitmap: Bitmap,
) {
  pins.forEach { pt ->
    manager.create(
        PointAnnotationOptions().withPoint(pt).withIconImage(pinBitmap),
    )
  }
}

/** Update the camera to fit the given pins with padding, or use fallback if no pins. */
private fun updateCameraForPins(
    mapboxMap: MapboxMap,
    pins: List<Point>,
    fallbackCenter: Point,
    fallbackZoom: Double,
    minZoom: Double = 6.0,
    fitPadding: EdgeInsets,
) {
  if (pins.isEmpty()) {
    setFallbackCamera(mapboxMap, fallbackCenter, fallbackZoom)
    return
  }

  mapboxMap.cameraForCoordinates(
      pins,
      CameraOptions.Builder().build(),
      fitPadding,
      null,
      null,
  ) { cam ->
    if (cam.isEmpty) return@cameraForCoordinates

    val zoom = cam.zoom ?: fallbackZoom
    val cappedZoom = max(zoom, minZoom)
    mapboxMap.setCamera(
        CameraOptions.Builder()
            .center(cam.center ?: fallbackCenter)
            .zoom(cappedZoom)
            .bearing(0.0)
            .pitch(0.0)
            .build())
  }
}

/** Set the camera to a fallback center and zoom. */
private fun setFallbackCamera(
    mapboxMap: MapboxMap,
    fallbackCenter: Point,
    fallbackZoom: Double,
) {
  mapboxMap.setCamera(
      CameraOptions.Builder()
          .center(fallbackCenter)
          .zoom(fallbackZoom)
          .bearing(0.0)
          .pitch(0.0)
          .build())
}

/* ---------------- Local-subset helpers ---------------- */
/** Computes the median of a list of Double values, or null if the list is empty. */
private fun median(values: List<Double>): Double? =
    values.sorted().let { if (it.isEmpty()) null else it[it.size / 2] }

/** Computes the Haversine distance in kilometers between two geodetic points. */
private fun haversineKm(a: Point, b: Point): Double {
  val radius = 6371.0
  val dLat = Math.toRadians(b.latitude() - a.latitude())
  val dLon = Math.toRadians(b.longitude() - a.longitude())
  val lat1 = Math.toRadians(a.latitude())
  val lat2 = Math.toRadians(b.latitude())
  val sinDLat = sin(dLat / 2)
  val sinDLon = sin(dLon / 2)
  val h = sinDLat * sinDLat + cos(lat1) * cos(lat2) * sinDLon * sinDLon
  return 2 * radius * asin(min(1.0, sqrt(h)))
}

/** Keep pins within [kmRadius] of the median geodetic center. */
private fun filterPinsWithinRadius(pins: List<Point>, kmRadius: Double = 200.0): List<Point> {
  if (pins.isEmpty()) return emptyList()
  val center =
      Point.fromLngLat(
          median(pins.map { it.longitude() }) ?: pins.first().longitude(),
          median(pins.map { it.latitude() }) ?: pins.first().latitude(),
      )
  return pins.filter { haversineKm(center, it) <= kmRadius }
}
