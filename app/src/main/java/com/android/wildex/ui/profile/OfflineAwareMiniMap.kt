package com.android.wildex.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.ui.map.OfflineMapPlaceholder
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView

/**
 * A MiniMap that is aware of the device's connectivity status. If the device is offline and the map
 * is not ready, it shows a placeholder instead.
 *
 * @param modifier The modifier to be applied to the MiniMap.
 * @param pins The list of points to be displayed as pins on the map.
 * @param styleUri The URI of the map style to be used.
 * @param styleImportId The import ID for custom styles.
 * @param isDark Whether the map should use a dark theme.
 * @param fallbackCenter The center point to use when the map is not ready.
 * @param fallbackZoom The zoom level to use when the map is not ready.
 */
@Composable
fun OfflineAwareMiniMap(
    modifier: Modifier = Modifier,
    pins: List<Point>,
    styleUri: String,
    styleImportId: String,
    isDark: Boolean,
    fallbackCenter: Point = Point.fromLngLat(6.632, 46.519),
    fallbackZoom: Double = 11.0,
) {
  val connectivityObserver = LocalConnectivityObserver.current
  val isOnline by connectivityObserver.isOnline.collectAsState()

  var miniMapView by remember { mutableStateOf<MapView?>(null) }
  var isReady by remember { mutableStateOf(false) }

  // Reset readiness when the content changes (new post, new location, etc.)
  LaunchedEffect(pins, styleUri, isDark) { isReady = false }

  DisposableEffect(miniMapView) {
    val mv = miniMapView ?: return@DisposableEffect onDispose {}
    val c = mv.mapboxMap.subscribeMapLoaded { isReady = true }
    onDispose { c.cancel() }
  }

  val showPlaceholder = !isOnline && !isReady

  if (showPlaceholder) {
    OfflineMapPlaceholder(modifier = modifier, skipLottie = true)
  } else {
    StaticMiniMap(
        modifier = modifier,
        pins = pins,
        styleUri = styleUri,
        styleImportId = styleImportId,
        isDark = isDark,
        fallbackCenter = fallbackCenter,
        fallbackZoom = fallbackZoom,
        context = LocalContext.current,
        mapViewRef = { miniMapView = it },
    )
  }
}
