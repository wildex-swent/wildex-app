package com.android.wildex.ui.map

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
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
import com.android.wildex.model.map.PinDetails
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingScreen
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

  // Permissions
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
  val isLocationGranted = locationPermissions.allPermissionsGranted

  // Load UI
  LaunchedEffect(Unit) { viewModel.loadUIState() }

  // Map state
  var mapView by remember { mutableStateOf<MapView?>(null) }
  var lastPosition by remember { mutableStateOf<Point?>(null) }

  val indicatorListener = remember {
    OnIndicatorPositionChangedListener { p: Point -> lastPosition = p }
  }

  DisposableEffect(mapView) {
    onDispose { mapView?.location?.removeOnIndicatorPositionChangedListener(indicatorListener) }
  }

  // Track when the map style is loaded
  var isMapReady by remember { mutableStateOf(false) }
  LaunchedEffect(mapView) { mapView?.mapboxMap?.addOnMapLoadedListener { isMapReady = true } }

  // Combined loading state
  val showLoading = uiState.isLoading || !isMapReady

  Scaffold(
      modifier = Modifier.fillMaxSize().semantics { contentDescription = MapScreenTestTags.ROOT },
      bottomBar = bottomBar,
  ) { pd ->
    Box(Modifier.padding(pd).fillMaxSize()) {
      // Map canvas
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

      // Pins
      PinsOverlay(
          modifier =
              Modifier.fillMaxSize().semantics { contentDescription = MapScreenTestTags.PIN_LAYER },
          mapView = mapView,
          pins = uiState.pins,
          currentTab = uiState.activeTab,
          selectedId =
              when (val s = uiState.selected) {
                is PinDetails.PostDetails -> s.post.postId
                is PinDetails.ReportDetails -> s.report.reportId
                else -> null
              },
          onPinClick = { id -> viewModel.onPinSelected(id) },
      )

      // Tap to clear selection
      MapTapToClearSelection(mapView = mapView) { viewModel.clearSelection() }

      // Alternative to tab switcher, I prefer the switcher but this is the one on figma
      /*MapTabBar(
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
      )*/

      // Tab switcher
      MapTabSwitcher(
          modifier = Modifier.align(Alignment.TopEnd).padding(top = 118.dp, end = 8.dp),
          activeTab = uiState.activeTab,
          availableTabs = uiState.availableTabs,
          onTabSelected = { viewModel.onTabSelected(it) },
      )

      // Recenter button
      RecenterFab(
          modifier =
              Modifier.align(Alignment.BottomEnd).padding(16.dp).semantics {
                contentDescription = MapScreenTestTags.FAB_RECENTER
              },
          isLocationGranted = isLocationGranted,
          current = uiState.activeTab,
          onRecenter = { viewModel.requestRecenter() },
          onAskLocation = { locationPermissions.launchMultiplePermissionRequest() })

      MapRefreshButton(
          modifier = Modifier.align(Alignment.TopEnd).padding(top = 64.dp, end = 8.dp),
          isRefreshing = uiState.isRefreshing,
          currentTab = uiState.activeTab,
          onRefresh = { viewModel.refreshUIState() },
      )
      // Bottom card for selection
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

      // Transparent loading overlay (only when data or map is not ready)
      if (showLoading) {
        Box(
            Modifier.fillMaxSize()
                .align(Alignment.Center)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))) {
              LoadingScreen(modifier = Modifier.align(Alignment.Center))
            }
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
private fun RecenterFab(
    modifier: Modifier,
    isLocationGranted: Boolean,
    current: MapTab,
    onRecenter: () -> Unit,
    onAskLocation: () -> Unit,
) {
  val cs = MaterialTheme.colorScheme
  val ui = colorsForMapTab(current, cs)

  FloatingActionButton(
      modifier = modifier,
      onClick = {
        if (isLocationGranted) {
          onRecenter()
        } else {
          onAskLocation()
        }
      },
      containerColor = cs.background,
      contentColor = ui.bg,
  ) {
    if (isLocationGranted) {
      Icon(Icons.Default.LocationOn, contentDescription = "Recenter")
    } else {
      Icon(
          imageVector = Icons.Default.LocationOff, // ← add this import
          contentDescription = "Enable location")
    }
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
