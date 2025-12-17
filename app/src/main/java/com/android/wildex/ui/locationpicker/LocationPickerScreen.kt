package com.android.wildex.ui.locationpicker

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.android.wildex.AppTheme
import com.android.wildex.R
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.map.MapCanvas
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.utils.offline.OfflineScreen
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
  const val BACK_BUTTON = "LocationPicker/BackButton"
  const val SEARCH_FIELD = "LocationPicker/SearchField"
  const val CLEAR_BUTTON = "LocationPicker/ClearButton"
  const val GPS_BUTTON = "LocationPicker/GpsButton"
  const val SEARCH_BUTTON = "LocationPicker/SearchButton"
  const val TOP_APP_BAR_TEXT = "LocationPicker/TopBar"
}

/**
 * Screen for picking a location on a map.
 *
 * @param onBack called when the user wants to go back to the previous screen.
 * @param onLocationPicked called when a location has been picked.
 * @param viewModel the location picker viewModel.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPickerScreen(
    onBack: () -> Unit,
    onLocationPicked: (Location) -> Unit,
    viewModel: LocationPickerViewModel = viewModel(),
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  // Offline detection
  val connectivityObserver = LocalConnectivityObserver.current
  val isOnline by connectivityObserver.isOnline.collectAsState()

  val isDark =
      when (AppTheme.appearanceMode) {
        AppearanceMode.DARK -> true
        AppearanceMode.LIGHT -> false
        AppearanceMode.AUTOMATIC -> isSystemInDarkTheme()
      }

  val styleUri = context.getString(R.string.map_style)
  val standardImportId = context.getString(R.string.map_standard_import)
  val focusManager = LocalFocusManager.current

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

  LocationPickerEffects(
      viewModel = viewModel,
      uiState = uiState,
      context = context,
      isLocationGranted = isLocationGranted,
      lastPosition = lastPosition,
      mapView = mapView,
      onLocationPicked = onLocationPicked,
  )

  // If Offline, doesn't make sense to display the map
  if (!isOnline) {
    OfflineScreenPicker(context, onBack)
    return
  }

  // Online
  Scaffold { inner ->
    Box(
        modifier =
            Modifier.fillMaxSize()
                .padding(inner)
                .testTag(LocationPickerTestTags.ROOT)
                .testTag(NavigationTestTags.LOCATION_PICKER_SCREEN),
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
                  latitude = uiState.center.latitude,
                  longitude = uiState.center.longitude,
              ),
      )

      Column(
          modifier =
              Modifier.align(Alignment.TopCenter)
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 16.dp),
      ) {
        // Top row: back button + search bar
        LocationPickerTopBar(
            modifier = Modifier.fillMaxWidth().testTag(LocationPickerTestTags.SEARCH_BAR),
            query = uiState.searchQuery,
            isLocationGranted = isLocationGranted,
            isSearching = uiState.isSearching,
            isLoading = uiState.isLoading,
            onQueryChange = { viewModel.onSearchQueryChanged(it) },
            onBack = onBack,
            onSearch = { query -> viewModel.onSearchSubmitted(query) },
            onUseCurrentLocationName = { viewModel.useCurrentLocationNameAsQuery() },
        )

        SuggestionsDropdown(
            suggestions = uiState.suggestions,
            query = uiState.searchQuery,
            onSuggestionClick = { feature -> viewModel.onSuggestionClicked(feature) },
            modifier = Modifier.padding(top = 8.dp, start = 56.dp),
        )
      }

      // Tap to pick a point
      LocationPickerTapListener(
          mapView = mapView,
          onTap = { lat, lon ->
            focusManager.clearFocus()
            viewModel.onMapClicked(lat, lon)
          },
      )

      // Marker at selected point
      LocationPickerMarkerOverlay(
          mapView = mapView,
          selectedLat = uiState.selected?.latitude,
          selectedLon = uiState.selected?.longitude,
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
              val fallback = Point.fromLngLat(uiState.center.longitude, uiState.center.latitude)
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
            placeName = uiState.selected?.name.orEmpty(),
            onConfirm = { viewModel.onConfirmDialogYes() },
            onDismiss = { viewModel.onConfirmDialogNo() },
        )
      }

      // Loading
      if (uiState.isLoading) {
        Box(
            modifier =
                Modifier.align(Alignment.Center).background(colorScheme.surface.copy(alpha = 0.7f)),
        )
      }
    }
  }
}

/* ---------- Side effects extracted from LocationPickerScreen ---------- */

/**
 * Handles side effects and view-model event collection for the Location Picker.
 * - Reacts to user location availability and forwards it to the viewModel.
 * - Collects confirmation events and forwards the chosen location to the caller.
 * - Shows error toasts when the viewModel reports an error.
 *
 * @param viewModel The LocationPickerViewModel instance providing uiState and events.
 * @param uiState The current UI state snapshot for the location picker.
 * @param context Android context used for toasts and resources.
 * @param isLocationGranted Whether location permissions are currently granted.
 * @param lastPosition Last known user position on the map, if available.
 * @param mapView Reference to the MapView used to animate when a selection is shown.
 * @param onLocationPicked Callback invoked when a location has been confirmed.
 */
@Composable
private fun LocationPickerEffects(
    viewModel: LocationPickerViewModel,
    uiState: LocationPickerUiState,
    context: Context,
    isLocationGranted: Boolean,
    lastPosition: Point?,
    mapView: MapView?,
    onLocationPicked: (Location) -> Unit,
) {
  LaunchedEffect(lastPosition, isLocationGranted) {
    // When the user location becomes available and permissions are granted,
    // notify the viewModel to possibly center the map.
    if (lastPosition != null && isLocationGranted && !uiState.hasCenteredOnUserLocation) {
      viewModel.onUserLocationAvailable(
          latitude = lastPosition.latitude(),
          longitude = lastPosition.longitude(),
      )
    }
  }

  LaunchedEffect(Unit) {
    // Collect one-shot events from the viewModel and forward to caller.
    viewModel.events.collect { event ->
      when (event) {
        is LocationPickerEvent.Confirmed -> onLocationPicked(event.location)
      }
    }
  }

  LaunchedEffect(uiState.error) {
    uiState.error?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      viewModel.clearError()
    }
  }

  LaunchedEffect(uiState.selected, uiState.showConfirmDialog, mapView) {
    val mv = mapView ?: return@LaunchedEffect
    val lat = uiState.selected?.latitude
    val lon = uiState.selected?.longitude

    if (uiState.showConfirmDialog && lat != null && lon != null) {
      val center = Point.fromLngLat(lon, lat)
      mv.mapboxMap.flyTo(
          CameraOptions.Builder().center(center).zoom(17.5).pitch(60.0).bearing(40.0).build(),
          mapAnimationOptions { duration(800L) },
      )
    }
  }
}

/* ---------- Top bar: back + search + suggestions ---------- */

@Composable
private fun LocationPickerTopBar(
    modifier: Modifier = Modifier,
    query: String,
    isLocationGranted: Boolean,
    isSearching: Boolean,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onUseCurrentLocationName: () -> Unit,
) {
  val focusManager = LocalFocusManager.current
  val focusRequester = remember { FocusRequester() }
  val isBusy = isSearching || isLoading
  val context = LocalContext.current

  Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButton(
        onClick = onBack,
        modifier = Modifier.testTag(LocationPickerTestTags.BACK_BUTTON),
    ) {
      Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
      )
    }

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            Modifier.weight(1f)
                .padding(start = 8.dp)
                .focusRequester(focusRequester)
                .testTag(LocationPickerTestTags.SEARCH_FIELD),
        placeholder = {
          Text(
              text = context.getString(R.string.location_search_box),
              overflow = TextOverflow.Ellipsis,
              maxLines = 1)
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        enabled = !isLoading,
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = colorScheme.surface,
                unfocusedContainerColor = colorScheme.surface,
                disabledContainerColor = colorScheme.surface,
                cursorColor = colorScheme.primary,
                focusedIndicatorColor = colorScheme.surface,
                unfocusedIndicatorColor = colorScheme.surface,
                disabledIndicatorColor = colorScheme.surface,
            ),
        trailingIcon = {
          LocationPickerTrailingIcons(
              query = query,
              isBusy = isBusy,
              isLocationGranted = isLocationGranted,
              onQueryChange = onQueryChange,
              onUseCurrentLocationName = onUseCurrentLocationName,
              onSearch = onSearch,
              focusRequester = focusRequester,
              focusManager = focusManager,
          )
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions =
            locationPickerKeyboardActions(
                query = query,
                onSearch = onSearch,
                focusManager = focusManager,
            ),
    )
  }
}

/**
 * Trailing icons for the search text field in the location picker.
 *
 * @param query Current search query string.
 * @param isBusy True if a search is in progress or loading UI is active.
 * @param isLocationGranted Whether location permissions are granted.
 * @param onQueryChange Callback when the query is changed (e.g. clear).
 * @param onUseCurrentLocationName Callback to use the device location name as query.
 * @param onSearch Callback to trigger a search.
 * @param focusRequester FocusRequester used to request focus on the text field.
 * @param focusManager FocusManager used to clear focus when appropriate.
 */
@Composable
private fun LocationPickerTrailingIcons(
    query: String,
    isBusy: Boolean,
    isLocationGranted: Boolean,
    onQueryChange: (String) -> Unit,
    onUseCurrentLocationName: () -> Unit,
    onSearch: (String) -> Unit,
    focusRequester: FocusRequester,
    focusManager: FocusManager,
) {
  if (isBusy) {
    CircularProgressIndicator(
        strokeWidth = 2.dp,
        modifier = Modifier.size(20.dp),
    )
    return
  }

  Row(verticalAlignment = Alignment.CenterVertically) {
    if (query.isNotEmpty()) {
      IconButton(
          onClick = {
            onQueryChange("")
            focusRequester.requestFocus()
          },
          modifier = Modifier.testTag(LocationPickerTestTags.CLEAR_BUTTON),
      ) {
        Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = "Clear search",
        )
      }
    }

    IconButton(
        onClick = { onUseCurrentLocationName() },
        modifier = Modifier.testTag(LocationPickerTestTags.GPS_BUTTON),
        enabled = isLocationGranted,
    ) {
      IconLocation(isLocationGranted)
    }

    IconButton(
        onClick = {
          val trimmed = query.trim()
          if (trimmed.isEmpty()) {
            focusRequester.requestFocus()
          } else {
            onSearch(trimmed)
            focusManager.clearFocus()
          }
        },
        modifier = Modifier.testTag(LocationPickerTestTags.SEARCH_BUTTON),
    ) {
      Icon(
          Icons.Default.Search,
          contentDescription = "Search",
          tint = colorScheme.primary,
          modifier = Modifier.size(28.dp),
      )
    }
  }
}

/**
 * Provides keyboard actions for the search field.
 *
 * Triggers the given onSearch lambda when the IME search action is pressed.
 */
private fun locationPickerKeyboardActions(
    query: String,
    onSearch: (String) -> Unit,
    focusManager: FocusManager,
): KeyboardActions {
  return KeyboardActions(
      onSearch = {
        val q = query.trim()
        if (q.isNotEmpty()) {
          onSearch(q)
          focusManager.clearFocus()
        }
      },
  )
}

/**
 * Shows a dropdown of suggested locations matching the user's query.
 *
 * @param suggestions List of Location suggestions to display.
 * @param query Current query string used for highlighting.
 * @param onSuggestionClick Callback invoked when a suggestion is selected.
 * @param modifier Optional modifier for the dropdown card.
 */
@Composable
private fun SuggestionsDropdown(
    suggestions: List<Location>,
    query: String,
    onSuggestionClick: (Location) -> Unit,
    modifier: Modifier = Modifier,
) {
  if (suggestions.isEmpty()) return
  val focusManager = LocalFocusManager.current
  val queryLower = query.trim().lowercase()

  Card(
      modifier = modifier,
      shape = RoundedCornerShape(16.dp),
      colors =
          cardColors(
              containerColor = colorScheme.surface,
          ),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
      suggestions.forEachIndexed { index, feature ->
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable {
                      focusManager.clearFocus()
                      onSuggestionClick(feature)
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              tint = colorScheme.primary,
              modifier = Modifier.size(18.dp),
          )

          val annotated =
              buildHighlightedName(
                  name = feature.name,
                  queryLower = queryLower,
                  highlightColor = colorScheme.primary,
              )

          Box(
              modifier = Modifier.padding(start = 8.dp),
          ) {
            Text(
                text = annotated,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
          }
        }

        if (index < suggestions.lastIndex) {
          HorizontalDivider(
              thickness = 0.5.dp,
              color = colorScheme.outlineVariant.copy(alpha = 0.4f),
          )
        }
      }
    }
  }
}

/**
 * Floating action button to recenter the map or request location permission.
 *
 * @param modifier Modifier to apply to the FAB.
 * @param isLocationGranted Whether location permission is currently granted.
 * @param onRecenter Callback to center the map on the user.
 * @param onAskLocation Callback to start permission request flow.
 */
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
      contentColor = cs.onBackground,
  ) {
    IconLocation(isLocationGranted)
  }
}

/**
 * Adds a tap listener to the MapView to allow picking coordinates.
 *
 * @param mapView The MapView instance to attach the listener to.
 * @param onTap Callback invoked with the tapped latitude and longitude.
 */
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

/**
 * Confirmation dialog shown when the user selects a location on the map.
 *
 * @param placeName Name of the place to confirm.
 * @param onConfirm Callback invoked when the user confirms.
 * @param onDismiss Callback invoked when the dialog is dismissed.
 */
@Composable
private fun LocationPickerConfirmDialog(
    placeName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {

  val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.location_pin))
  val progress by
      animateLottieCompositionAsState(
          composition = composition,
          iterations = LottieConstants.IterateForever,
      )

  AlertDialog(
      modifier = Modifier.testTag(LocationPickerTestTags.CONFIRM_DIALOG),
      onDismissRequest = onDismiss,
      containerColor = colorScheme.background,
      titleContentColor = colorScheme.primary,
      textContentColor = colorScheme.onBackground,
      title = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          LottieAnimation(
              composition = composition,
              progress = { progress },
              modifier = Modifier.size(160.dp),
          )
          Text(
              text = "Confirm location",
              fontWeight = FontWeight.Bold,
              style = typography.titleMedium,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      text = {
        Text(
            text = stringResource(R.string.location_picker_confirm_text, placeName),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
      },
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

/**
 * Draws a pin overlay on the MapView at the selected coordinates using a vector drawable.
 *
 * @param mapView The MapView to add the annotation to.
 * @param selectedLat Selected latitude or null to clear.
 * @param selectedLon Selected longitude or null to clear.
 */
@Composable
private fun LocationPickerMarkerOverlay(
    mapView: MapView?,
    selectedLat: Double?,
    selectedLon: Double?,
) {
  val context = LocalContext.current

  DisposableEffect(mapView, selectedLat, selectedLon) {
    val mv = mapView ?: return@DisposableEffect onDispose {}

    val annotationManager = mv.annotations.createPointAnnotationManager().apply { deleteAll() }

    if (selectedLat != null && selectedLon != null) {
      val point = Point.fromLngLat(selectedLon, selectedLat)
      val pinBitmap = loadVectorAsBitmap(context, R.drawable.ic_map_pin)
      val options = PointAnnotationOptions().withPoint(point).withIconImage(pinBitmap)
      annotationManager.create(options)
    }

    onDispose { annotationManager.deleteAll() }
  }
}

/**
 * Icon composable that switches between enabled/disabled location icons.
 *
 * @param isLocationGranted True to show the active location icon, false to show the disabled icon.
 */
@Composable
private fun IconLocation(isLocationGranted: Boolean) {
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

/**
 * Offline placeholder wrapper for the location picker.
 *
 * @param context Android context used to build the top bar.
 * @param onBack Callback invoked when the user wants to go back.
 */
@Composable
private fun OfflineScreenPicker(context: Context, onBack: () -> Unit) {
  Scaffold(topBar = { LocationPickerTopBar(context, onBack) }) { inner ->
    OfflineScreen(innerPadding = inner)
  }
}

/**
 * Loads a vector drawable as a square bitmap of the requested size.
 *
 * @param context Android context used to load the drawable resource.
 * @param resId Resource id of the drawable.
 * @param sizePx Desired bitmap size in pixels (square).
 * @return A Bitmap representation of the vector drawable.
 */
private fun loadVectorAsBitmap(
    context: Context,
    resId: Int,
    sizePx: Int = 96,
): Bitmap {
  // Create bitmap and canvas and draw the vector into it so Mapbox annotations can use it.
  val d = AppCompatResources.getDrawable(context, resId) ?: return createBitmap(1, 1)
  val bmp = createBitmap(sizePx, sizePx)
  val canvas = Canvas(bmp)
  d.setBounds(0, 0, sizePx, sizePx)
  d.draw(canvas)
  return bmp
}

/**
 * Builds an AnnotatedString where the matching substring (query) is highlighted.
 *
 * @param name The full name to display.
 * @param queryLower The lowercase query to highlight.
 * @param highlightColor Color used for the highlighted span.
 * @return AnnotatedString with the highlighted part if found.
 */
private fun buildHighlightedName(
    name: String,
    queryLower: String,
    highlightColor: Color,
): AnnotatedString {
  if (queryLower.isEmpty()) return AnnotatedString(name)

  val nameLower = name.lowercase()
  val start = nameLower.indexOf(queryLower)
  if (start < 0) return AnnotatedString(name)

  val end = start + queryLower.length
  return buildAnnotatedString {
    append(name.substring(0, start))
    withStyle(
        SpanStyle(
            fontWeight = FontWeight.SemiBold,
            color = highlightColor,
        ),
    ) {
      append(name.substring(start, end))
    }
    append(name.substring(end))
  }
}
