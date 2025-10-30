package com.android.wildex.ui.map

import android.Manifest
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.R
import com.android.wildex.model.utils.Id
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mapbox.common.toValue
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location

/* ---------- Test tags ---------- */
object MapScreenTestTags {
  const val ROOT = "MapScreen/Root"
  const val MAP = "MapScreen/Map"
  const val TAB_BAR = "MapScreen/TabBar"
  const val TAB_TITLE = "MapScreen/TabTitle"
  const val TAB_PREV = "MapScreen/TabPrev"
  const val TAB_NEXT = "MapScreen/TabNext"
  const val PIN_LAYER = "MapScreen/PinLayer"
  const val SELECTION_CARD = "MapScreen/SelectionCard"
  const val FAB_RECENTER = "MapScreen/RecenterFab"
}

/* ---------- Screen ---------- */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    bottomBar: @Composable () -> Unit,
    viewModel: MapViewModel = viewModel(),
    onPost: (Id) -> Unit = {},
    onReport: (Id) -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val render by viewModel.renderState.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val isDark = isSystemInDarkTheme()
  val styleUri = context.getString(R.string.map_style)
  val standardImportId = context.getString(R.string.map_standard_import)

  val locationPermissions =
      rememberMultiplePermissionsState(
          listOf(
              Manifest.permission.ACCESS_FINE_LOCATION,
              Manifest.permission.ACCESS_COARSE_LOCATION,
          ))
  LaunchedEffect(Unit) { locationPermissions.launchMultiplePermissionRequest() }
  LaunchedEffect(locationPermissions.allPermissionsGranted) {
    viewModel.onLocationPermissionResult(locationPermissions.allPermissionsGranted)
  }

  LaunchedEffect(Unit) { viewModel.loadUIState() }

  var mapView by remember { mutableStateOf<MapView?>(null) }
  var lastPosition by remember { mutableStateOf<Point?>(null) }

  val indicatorListener = remember {
    OnIndicatorPositionChangedListener { p: Point -> lastPosition = p }
  }

  DisposableEffect(mapView) {
    onDispose { mapView?.location?.removeOnIndicatorPositionChangedListener(indicatorListener) }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize().semantics { contentDescription = MapScreenTestTags.ROOT },
      bottomBar = bottomBar,
  ) { inner ->
    Box(Modifier.padding(inner).fillMaxSize()) {

      // Map
      MapCanvas(
          modifier =
              Modifier.fillMaxSize().semantics { contentDescription = MapScreenTestTags.MAP },
          mapViewRef = { mapView = it },
          styleUri = styleUri,
          styleImportId = standardImportId,
          isDark = isDark,
          showUserLocation = render.showUserLocation,
          indicatorListener = indicatorListener,
      )

      PinsOverlay(
          modifier =
              Modifier.fillMaxSize().semantics { contentDescription = MapScreenTestTags.PIN_LAYER },
          mapView = mapView,
          pins = uiState.pins,
          onPinClick = { id -> viewModel.onPinSelected(id) },
      )

      MapTapToClearSelection(mapView = mapView) { viewModel.clearSelection() }

      MapTabBar(
          modifier =
              Modifier.align(Alignment.TopStart).padding(top = 32.dp, start = 10.dp).semantics {
                contentDescription = MapScreenTestTags.TAB_BAR
              },
          tabs = uiState.availableTabs,
          active = uiState.activeTab,
          onPrev = {
            prevOf(uiState.availableTabs, uiState.activeTab)?.let(viewModel::onTabSelected)
          },
          onNext = {
            nextOf(uiState.availableTabs, uiState.activeTab)?.let(viewModel::onTabSelected)
          },
      )

      RecenterFab(
          modifier =
              Modifier.align(Alignment.BottomEnd).padding(16.dp).semantics {
                contentDescription = MapScreenTestTags.FAB_RECENTER
              },
          onClick = { viewModel.requestRecenter() },
      )

      if (uiState.selected != null) {
        SelectionBottomCard(
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
            selection = uiState.selected,
            activeTab = uiState.activeTab,
            onPost = onPost,
            onReport = onReport,
            onToggleLike = viewModel::toggleLike,
            onDismiss = { viewModel.clearSelection() },
        )
      }

      // Recenter camera when asked
      LaunchedEffect(render.recenterNonce) {
        if (render.recenterNonce != null) {
          val target = lastPosition
          val fallback = Point.fromLngLat(6.6323, 46.5197) // Lausanne
          mapView
              ?.mapboxMap
              ?.setCamera(
                  CameraOptions.Builder()
                      .center(target ?: fallback)
                      .zoom(if (target != null) 14.0 else 12.0)
                      .build())
          viewModel.consumeRecenter()
        }
      }
    }
  }
}

@Composable
private fun MapCanvas(
    modifier: Modifier,
    mapViewRef: (MapView) -> Unit,
    styleUri: String,
    styleImportId: String,
    isDark: Boolean,
    showUserLocation: Boolean,
    indicatorListener: OnIndicatorPositionChangedListener,
) {
  MapboxMap(modifier) {
    MapEffect(isDark, showUserLocation, styleUri, styleImportId) { mv ->
      mapViewRef(mv)
      val mapboxMap = mv.mapboxMap
      mapboxMap.loadStyle(styleUri) { style ->
        mapboxMap.setCamera(
            CameraOptions.Builder().center(Point.fromLngLat(6.6323, 46.5197)).zoom(12.0).build())
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

/* ---------- Recenter FAB ---------- */
@Composable
private fun RecenterFab(modifier: Modifier, onClick: () -> Unit) {
  FloatingActionButton(
      modifier = modifier,
      onClick = onClick,
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
  ) {
    Icon(Icons.Default.LocationOn, contentDescription = "Recenter")
  }
}

/* ---------- Dismiss on map tap ---------- */
@Composable
private fun MapTapToClearSelection(mapView: MapView?, onDismiss: () -> Unit) {
  DisposableEffect(mapView) {
    val listener: (Point) -> Boolean = {
      onDismiss()
      true
    }
    mapView?.gestures?.addOnMapClickListener(listener)
    onDispose { mapView?.gestures?.removeOnMapClickListener(listener) }
  }
}

/* ---------- Utilities ---------- */
private fun prevOf(list: List<MapTab>, current: MapTab): MapTab? {
  if (list.isEmpty()) return null
  val i = list.indexOf(current).let { if (it == -1) 0 else it }
  return list[(i - 1 + list.size) % list.size]
}

private fun nextOf(list: List<MapTab>, current: MapTab): MapTab? {
  if (list.isEmpty()) return null
  val i = list.indexOf(current).let { if (it == -1) 0 else it }
  return list[(i + 1) % list.size]
}
