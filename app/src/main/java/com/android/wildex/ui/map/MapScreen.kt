package com.android.wildex.ui.map

import android.Manifest
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location

object MapScreenTestTags {
  const val MAP = "MapScreen"
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    userId: Id,
    bottomBar: @Composable () -> Unit,
    viewModel: MapScreenViewModel = viewModel(),
    onPost: (Id) -> Unit = {},
    onReport: (Id) -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsState()
  val render by viewModel.renderState.collectAsState()

  val isDark = isSystemInDarkTheme()
  val context = LocalContext.current
  val style = context.getString(R.string.map_style)
  val standardImportId = context.getString(R.string.map_standard_import)

  // LaunchedEffect(currentUserId) { viewModel.loadUIState() }

  // Ask location permission, then notify VM
  val locationPermissions =
      rememberMultiplePermissionsState(
          listOf(
              Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))

  LaunchedEffect(Unit) { locationPermissions.launchMultiplePermissionRequest() }

  LaunchedEffect(locationPermissions.allPermissionsGranted) {
    viewModel.onLocationPermissionResult(locationPermissions.allPermissionsGranted)
  }

  // Map reference + latest position from the puck
  var mapView by remember { mutableStateOf<MapView?>(null) }
  var lastPosition by remember { mutableStateOf<Point?>(null) }

  // listener that updates lastPosition whenever the puck moves
  val indicatorListener = remember {
    OnIndicatorPositionChangedListener { point: Point -> lastPosition = point }
  }

  Scaffold(bottomBar = { bottomBar() }) { inner ->
    Box(Modifier.padding(inner).fillMaxSize()) {
      MapboxMap(Modifier.fillMaxSize()) {
        MapEffect(isDark, render.showUserLocation) { mv ->
          mapView = mv
          val mapboxMap = mv.mapboxMap
          mapboxMap.loadStyle(style) { style ->
            // Initial camera (Lausanne)
            mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(6.6323, 46.5197))
                    .zoom(12.0)
                    .build())
            // Apply day/night preset right after load
            style.setStyleImportConfigProperty(
                standardImportId, "lightPreset", (if (isDark) "night" else "day").toValue())
          }
          // apply location puck settings
          mv.location.updateSettings {
            enabled = render.showUserLocation
            pulsingEnabled = render.showUserLocation
          }
          // (re)attach listener only if location is enabled
          mv.location.removeOnIndicatorPositionChangedListener(indicatorListener)
          if (render.showUserLocation) {
            mv.location.addOnIndicatorPositionChangedListener(indicatorListener)
          }
        }

        // Recenter when VM asks
        LaunchedEffect(render.recenterNonce) {
          if (render.recenterNonce != null) {
            val target = lastPosition
            val fallback = Point.fromLngLat(6.6323, 46.5197)
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

      FloatingActionButton(
          onClick = { viewModel.requestRecenter() },
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Default.LocationOn, contentDescription = "Recenter")
          }
    }
  }
}
