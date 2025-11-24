package com.android.wildex.ui.locationpicker

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.AppTheme
import com.android.wildex.R
import com.android.wildex.model.location.PickedLocation
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.map.MapCanvas
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener

/* ---------- Test tags ---------- */
object LocationPickerTestTags {
  const val ROOT = "LocationPicker/Root"
  const val MAP_CANVAS = "LocationPicker/MapCanvas"
  const val FAB_RECENTER = "LocationPicker/RecenterFab"
  const val SEARCH_BAR = "LocationPicker/SearchBar"
  const val CONFIRM_DIALOG = "LocationPicker/ConfirmDialog"
  const val CONFIRM_YES = "LocationPicker/ConfirmYes"
  const val CONFIRM_NO = "LocationPicker/ConfirmNo"
}

/**
 * Screen for picking a location on a map.
 *
 * Has:
 * - Top bar with back button and search field
 * - MapCanvas
 * - Recenter FAB
 * - Tap-to-select + confirmation dialog
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPickerScreen(
    onBack: () -> Unit,
    onLocationPicked: (PickedLocation) -> Unit,
    viewModel: LocationPickerViewModel = viewModel(),
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  val isDark =
      when (AppTheme.appearanceMode) {
        AppearanceMode.DARK -> true
        AppearanceMode.LIGHT -> false
        AppearanceMode.AUTOMATIC -> isSystemInDarkTheme()
      }

  val styleUri = context.getString(R.string.map_style)
  val standardImportId = context.getString(R.string.map_standard_import)

  // Location permissions
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

  // Map state
  var mapView by remember { mutableStateOf<MapView?>(null) }
  var lastPosition by remember { mutableStateOf<Point?>(null) }

  val indicatorListener = remember {
    OnIndicatorPositionChangedListener { p: Point -> lastPosition = p }
  }

  LaunchedEffect(lastPosition, isLocationGranted) {
    if (lastPosition != null && isLocationGranted) {
      viewModel.onUserLocationAvailable(
          latitude = lastPosition!!.latitude(),
          longitude = lastPosition!!.longitude(),
      )
    }
  }

  // Listen to confirmed location
  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is LocationPickerEvent.Confirmed -> onLocationPicked(event.location)
      }
    }
  }

  // Toast for errors
  LaunchedEffect(uiState.error) {
    uiState.error?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      viewModel.clearError()
    }
  }

  LaunchedEffect(uiState.selectedLat, uiState.selectedLon, uiState.showConfirmDialog, mapView) {
    val mv = mapView ?: return@LaunchedEffect
    val lat = uiState.selectedLat
    val lon = uiState.selectedLon

    if (uiState.showConfirmDialog && lat != null && lon != null) {
      val offsetLat = lat - 0.0015
      val center = Point.fromLngLat(lon, offsetLat)
      mv.mapboxMap.flyTo(
          CameraOptions.Builder().center(center).zoom(17.5).pitch(60.0).bearing(40.0).build(),
          mapAnimationOptions { duration(800L) },
      )
    }
  }

  Scaffold(
      topBar = {
        LocationPickerTopBar(
            modifier = Modifier.testTag(LocationPickerTestTags.SEARCH_BAR),
            onBack = onBack,
            onSearch = { query -> viewModel.onSearchSubmitted(query) },
        )
      },
  ) { inner ->
    Box(
        modifier = Modifier.fillMaxSize().padding(inner).testTag(LocationPickerTestTags.ROOT),
    ) {
      // Map
      MapCanvas(
          modifier = Modifier.fillMaxSize().testTag(LocationPickerTestTags.MAP_CANVAS),
          mapViewRef = { mv -> mapView = mv },
          styleUri = styleUri,
          styleImportId = standardImportId,
          isDark = isDark,
          showUserLocation = isLocationGranted,
          indicatorListener = indicatorListener,
          centerCoordinates =
              Location(
                  latitude = uiState.centerLat,
                  longitude = uiState.centerLon,
              ),
      )

      // Tap to pick a point
      LocationPickerTapListener(
          mapView = mapView,
          onTap = { lat, lon -> viewModel.onMapClicked(lat, lon) },
      )

      // Marker at selected point
      LocationPickerMarkerOverlay(
          mapView = mapView,
          selectedLat = uiState.selectedLat,
          selectedLon = uiState.selectedLon,
      )

      // Recenter
      LocationPickerRecenterFab(
          modifier =
              Modifier.align(Alignment.BottomEnd)
                  .padding(16.dp)
                  .testTag(LocationPickerTestTags.FAB_RECENTER),
          isLocationGranted = isLocationGranted,
          onRecenter = {
            val mv = mapView ?: return@LocationPickerRecenterFab
            val target = lastPosition
            if (target != null) {
              mv.mapboxMap.flyTo(
                  CameraOptions.Builder().center(target).zoom(14.0).build(),
                  mapAnimationOptions { duration(800L) },
              )
            } else {
              val fallback = Point.fromLngLat(uiState.centerLon, uiState.centerLat)
              mv.mapboxMap.flyTo(
                  CameraOptions.Builder().center(fallback).zoom(12.0).build(),
                  mapAnimationOptions { duration(800L) },
              )
            }
          },
          onAskLocation = { locationPermissions.launchMultiplePermissionRequest() },
      )

      // Confirmation dialog
      if (uiState.showConfirmDialog) {
        LocationPickerConfirmDialog(
            placeName = uiState.selectedName.orEmpty(),
            onConfirm = { viewModel.onConfirmDialogYes() },
            onDismiss = { viewModel.onConfirmDialogNo() },
        )
      }

      // Loading overlay
      if (uiState.isLoading) {
        Box(
            modifier =
                Modifier.align(Alignment.Center).background(colorScheme.surface.copy(alpha = 0.7f)),
        )
      }
    }
  }
}

/* ---------- Top bar: back + search ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationPickerTopBar(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
) {
  var searchValue by remember { mutableStateOf(TextFieldValue("")) }
  val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

  TopAppBar(
      modifier = modifier,
      title = {
        Box(modifier = Modifier.fillMaxWidth().padding(end = 8.dp)) {
          TextField(
              value = searchValue,
              onValueChange = { searchValue = it },
              modifier = Modifier.fillMaxWidth(),
              placeholder = { Text("Search address") },
              singleLine = true,
              colors =
                  TextFieldDefaults.colors(
                      focusedContainerColor = colorScheme.background,
                      unfocusedContainerColor = colorScheme.background,
                      disabledContainerColor = colorScheme.background,
                      cursorColor = colorScheme.primary,
                      focusedIndicatorColor = colorScheme.background,
                      unfocusedIndicatorColor = colorScheme.background,
                      disabledIndicatorColor = colorScheme.background,
                  ),
              trailingIcon = {
                IconButton(
                    onClick = {
                      val q = searchValue.text.trim()
                      if (q.isNotEmpty()) {
                        onSearch(q)
                        focusManager.clearFocus()
                      }
                    },
                ) {
                  Icon(
                      Icons.Default.Search,
                      contentDescription = "Search",
                      tint = colorScheme.primary,
                      modifier = Modifier.size(28.dp),
                  )
                }
              },
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
              keyboardActions =
                  KeyboardActions(
                      onSearch = {
                        val q = searchValue.text.trim()
                        if (q.isNotEmpty()) {
                          onSearch(q)
                          focusManager.clearFocus()
                        }
                      },
                  ),
          )
        }
      },
      navigationIcon = {
        IconButton(onClick = onBack) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
          )
        }
      },
  )
}

/* ---------- Recenter FAB ---------- */

@Composable
private fun LocationPickerRecenterFab(
    modifier: Modifier = Modifier,
    isLocationGranted: Boolean,
    onRecenter: () -> Unit,
    onAskLocation: () -> Unit,
) {
  val cs = colorScheme

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
      contentColor = cs.primary,
  ) {
    if (isLocationGranted) {
      Icon(
          imageVector = Icons.Default.LocationOn,
          contentDescription = "Recenter on my location",
      )
    } else {
      Icon(
          imageVector = Icons.Default.LocationOff,
          contentDescription = "Enable location",
      )
    }
  }
}

/* ---------- Tap listener to pick a point ---------- */

@Composable
private fun LocationPickerTapListener(
    mapView: MapView?,
    onTap: (latitude: Double, longitude: Double) -> Unit,
) {
  DisposableEffect(mapView) {
    val listener = OnMapClickListener { point ->
      onTap(point.latitude(), point.longitude())
      true
    }
    mapView?.gestures?.addOnMapClickListener(listener)
    onDispose { mapView?.gestures?.removeOnMapClickListener(listener) }
  }
}

/* ---------- Confirm dialog ---------- */

@Composable
private fun LocationPickerConfirmDialog(
    placeName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
  AlertDialog(
      modifier = Modifier.testTag(LocationPickerTestTags.CONFIRM_DIALOG),
      onDismissRequest = onDismiss,
      title = { Text("Confirm location") },
      text = { Text("Is \"$placeName\" the location you want to pick?") },
      confirmButton = {
        TextButton(
            modifier = Modifier.testTag(LocationPickerTestTags.CONFIRM_YES),
            onClick = onConfirm,
        ) {
          Text("Yes")
        }
      },
      dismissButton = {
        TextButton(
            modifier = Modifier.testTag(LocationPickerTestTags.CONFIRM_NO),
            onClick = onDismiss,
        ) {
          Text("No")
        }
      },
  )
}

@Composable
private fun LocationPickerMarkerOverlay(
    mapView: MapView?,
    selectedLat: Double?,
    selectedLon: Double?,
) {
  val context = LocalContext.current

  DisposableEffect(mapView, selectedLat, selectedLon) {
    val mv = mapView ?: return@DisposableEffect onDispose {}

    // Create a manager and clear previous annotations
    val annotationManager = mv.annotations.createPointAnnotationManager().apply { deleteAll() }

    if (selectedLat != null && selectedLon != null) {
      val point = Point.fromLngLat(selectedLon, selectedLat)

      // Reuse the same pin style as the profile mini-map
      val pinBitmap = loadVectorAsBitmap(context, R.drawable.ic_map_pin)

      val options = PointAnnotationOptions().withPoint(point).withIconImage(pinBitmap)

      annotationManager.create(options)
    }

    onDispose { annotationManager.deleteAll() }
  }
}

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
