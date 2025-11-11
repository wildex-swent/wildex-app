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

/**
 * UI colors for the map tab/card.
 *
 * @param bg Background color.
 * @param fg Content color.
 */
data class MapUiColors(
    val bg: Color, // background color
    val fg: Color, // content color
)

/**
 * Composable that displays a Mapbox map with specified settings.
 *
 * @param modifier Modifier to be applied to the Mapbox map.
 * @param mapViewRef Lambda to obtain a reference to the MapView.
 * @param styleUri URI of the map style to be used.
 * @param styleImportId ID of the style import for light/dark mode switching.
 * @param isDark Boolean indicating whether dark mode is enabled.
 * @param showUserLocation Boolean indicating whether to show the user's location.
 * @param indicatorListener Listener for changes in the user's location indicator position.
 * @param centerCoordinates Coordinates to center the map on initial load.
 */
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

/**
 * Returns the UI colors for the specified map tab based on the provided color scheme.
 *
 * @param tab The map tab for which to get the colors.
 * @param cs The color scheme to use for determining the colors.
 * @return The UI colors for the specified map tab.
 */
fun colorsForMapTab(tab: MapTab, cs: ColorScheme): MapUiColors =
    when (tab) {
      MapTab.Posts -> MapUiColors(cs.primary, cs.onPrimary)
      MapTab.MyPosts -> MapUiColors(cs.secondary, cs.onSecondary)
      MapTab.Reports -> MapUiColors(cs.tertiary, cs.onTertiary)
    }
