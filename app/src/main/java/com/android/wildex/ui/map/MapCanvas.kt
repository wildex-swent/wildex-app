package com.android.wildex.ui.map

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.android.wildex.model.utils.Location
import com.mapbox.common.toValue
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location

data class MapUiColors(
    val bg: Color, // background color
    val fg: Color, // content color
)

@Composable
fun MapCanvas(
    modifier: Modifier,
    mapViewRef: (MapView) -> Unit,
    styleUri: String,
    styleImportId: String,
    isDark: Boolean,
    showUserLocation: Boolean,
    indicatorListener: OnIndicatorPositionChangedListener,
    centerCoordinates: Location,
) {
  MapboxMap(modifier = modifier) {
    MapEffect(isDark, showUserLocation, styleUri, styleImportId) { mv ->
      mapViewRef(mv)
      val mapboxMap = mv.mapboxMap
      mapboxMap.loadStyle(styleUri) { style ->
        mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(centerCoordinates.longitude, centerCoordinates.latitude))
                .zoom(12.0)
                .build())
        runCatching {
          style.setStyleImportConfigProperty(
              styleImportId,
              "lightPreset",
              (if (isDark) "night" else "day").toValue(),
          )
        }
      }
      mv.location.updateSettings {
        enabled = showUserLocation
        pulsingEnabled = showUserLocation
      }
      mv.location.removeOnIndicatorPositionChangedListener(indicatorListener)

      if (showUserLocation) {
        mv.location.addOnIndicatorPositionChangedListener(indicatorListener)
      }
    }
  }
}

/** One source of truth for Map tab/card colors. */
fun colorsForMapTab(tab: MapTab, cs: ColorScheme): MapUiColors =
    when (tab) {
      MapTab.Posts -> MapUiColors(cs.primary, cs.onPrimary)
      MapTab.MyPosts -> MapUiColors(cs.secondary, cs.onSecondary)
      MapTab.Reports -> MapUiColors(cs.tertiary, cs.onTertiary)
    }
