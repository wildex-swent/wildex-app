package com.android.wildex.ui.map

import android.Manifest
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.AppTheme
import com.android.wildex.R
import com.android.wildex.model.map.PinDetails
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.navigation.NavigationTestTags
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location

/* ---------- Test tags ---------- */
object MapContentTestTags {
  const val ROOT = "MapScreen/Root"
  const val TAB_SWITCHER = "MapScreen/TabSwitcher"
  const val MAP_PINS = "MapPins"
  const val MAP_CANVAS = "MapCanvas"
  const val MAIN_TAB_SWITCHER = "MapTabSwitcher-Main"
  const val REFRESH = "MapScreen/Refresh"
  const val REFRESH_SPINNER = "MapScreen/Refresh/Spinner"
  const val FAB_RECENTER = "MapScreen/RecenterFab"
  const val SELECTION_CARD = "MapScreen/SelectionCard"
  const val SELECTION_POST_IMAGE = "MapScreen/SelectionCard/PostImage"
  const val SELECTION_REPORT_IMAGE = "MapScreen/SelectionCard/ReportImage"
  const val SELECTION_CLOSE = "MapScreen/SelectionCard/Close"
  const val SELECTION_AUTHOR_IMAGE = "MapScreen/SelectionCard/AuthorImage"
  const val SELECTION_LIKE_BUTTON = "MapScreen/SelectionCard/LikeButton"
  const val SELECTION_COMMENT_ICON = "MapScreen/SelectionCard/CommentIcon"
  const val SELECTION_OPEN_BUTTON = "MapScreen/SelectionCard/OpenButton"
  const val SELECTION_REPORT_DESCRIPTION = "MapScreen/SelectionCard/ReportDescription"
  const val SELECTION_LOCATION = "MapScreen/SelectionCard/Location"
  const val REPORT_ASSIGNED_ROW = "MapScreen/SelectionCard/ReportAssignedRow"

  const val BACK_BUTTON = "MapScreen/BackButton"

  fun getPinTag(tab: MapTab): String = "MapTabSwitcher-${tab.name}"
}

/* -------- Local to skip Mapbox in tests ---- */
val LocalSkipWorkerThread = staticCompositionLocalOf { false }

/**
 * Main Map Screen Composable.
 *
 * @param userId ID of the current user.
 * @param bottomBar Composable for the bottom bar.
 * @param viewModel ViewModel for the Map Screen.
 * @param onPost Callback when a post is selected.
 * @param onReport Callback when a report is selected.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    userId: Id,
    bottomBar: @Composable () -> Unit,
    viewModel: MapScreenViewModel = viewModel(),
    onPost: (Id) -> Unit = {},
    onReport: (Id) -> Unit = {},
    onGoBack: () -> Unit = {},
    onProfile: (Id) -> Unit = {},
    isCurrentUser: Boolean = true,
) {
  LaunchedEffect(Unit) { viewModel.loadUIState(userId) }
  Scaffold(bottomBar = bottomBar, modifier = Modifier.testTag(NavigationTestTags.MAP_SCREEN)) {
      inner ->
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val render by viewModel.renderState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val isDark =
        when (AppTheme.appearanceMode) {
          AppearanceMode.DARK -> true
          AppearanceMode.LIGHT -> false
          AppearanceMode.AUTOMATIC -> isSystemInDarkTheme()
        }

    val styleUri = context.getString(R.string.map_style)
    val standardImportId = context.getString(R.string.map_standard_import)

    // location permissions
    val locationPermissions =
        rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ))

    // run the permission flow only if it actually exists
    locationPermissions.let { perms ->
      LaunchedEffect(Unit) { perms.launchMultiplePermissionRequest() }
      LaunchedEffect(perms.allPermissionsGranted) {
        viewModel.onLocationPermissionResult(perms.allPermissionsGranted)
      }
    }

    val isLocationGranted = locationPermissions.allPermissionsGranted

    // Error toasts
    LaunchedEffect(uiState.errorMsg) {
      uiState.errorMsg?.let {
        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        viewModel.clearErrorMsg()
      }
    }
    LaunchedEffect(render.renderError) {
      render.renderError?.let {
        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        viewModel.clearRenderError()
      }
    }

    // Map state
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var lastPosition by remember { mutableStateOf<Point?>(null) }
    var isMapReady by remember { mutableStateOf(false) }
    // I added this to avoid the old pins taking the new tab color before they disappear
    var styleTab by remember { mutableStateOf(uiState.activeTab) }

    LaunchedEffect(uiState.centerCoordinates, isMapReady, uiState.selected, uiState.activeTab) {
      if (!isMapReady) return@LaunchedEffect
      val mv = mapView ?: return@LaunchedEffect
      val loc = uiState.centerCoordinates
      val zoom =
          if (uiState.selected != null) {
            18.0
          } else {
            12.0
          }
      val dest =
          CameraOptions.Builder()
              .center(Point.fromLngLat(loc.longitude, loc.latitude))
              .zoom(zoom)
              .build()
      mv.mapboxMap.flyTo(
          dest,
          mapAnimationOptions { duration(800L) },
      )
    }

    val indicatorListener = remember {
      OnIndicatorPositionChangedListener { p: Point -> lastPosition = p }
    }
    DisposableEffect(mapView) {
      val cancelable = mapView?.mapboxMap?.subscribeMapLoaded { isMapReady = true }
      onDispose {
        cancelable?.cancel()
        mapView?.location?.removeOnIndicatorPositionChangedListener(indicatorListener)
      }
    }
    LaunchedEffect(uiState.pins) { styleTab = uiState.activeTab }
    val showLoading = uiState.isLoading || !isMapReady

    Box(Modifier.fillMaxSize().padding(inner).testTag(MapContentTestTags.ROOT)) {
      // 1) map
      MapCanvas(
          modifier = Modifier.fillMaxSize().testTag(MapContentTestTags.MAP_CANVAS),
          mapViewRef = { mapView = it },
          styleUri = styleUri,
          styleImportId = standardImportId,
          isDark = isDark,
          showUserLocation = render.showUserLocation,
          indicatorListener = indicatorListener,
          centerCoordinates = uiState.centerCoordinates,
      )

      if (!LocalSkipWorkerThread.current) {
        // 2) pins
        PinsOverlay(
            modifier = Modifier.fillMaxSize().testTag(MapContentTestTags.MAP_PINS),
            mapView = mapView,
            pins = uiState.pins,
            currentTab = styleTab,
            selectedId =
                when (val s = uiState.selected) {
                  is PinDetails.PostDetails -> s.post.postId
                  is PinDetails.ReportDetails -> s.report.reportId
                  else -> null
                },
            onPinClick = { id -> viewModel.onPinSelected(id) },
        )
      }
      // 3) tap to clear
      MapTapToClearSelection(mapView = mapView) { viewModel.clearSelection() }

      // 4) tabs
      MapTabSwitcher(
          modifier =
              Modifier.align(Alignment.TopEnd)
                  .padding(top = 110.dp, end = 8.dp)
                  .testTag(MapContentTestTags.TAB_SWITCHER),
          activeTab = uiState.activeTab,
          availableTabs = uiState.availableTabs,
          onTabSelected = { viewModel.onTabSelected(it, userId) },
      )
      // 5) recenter
      RecenterFab(
          modifier =
              Modifier.align(Alignment.BottomEnd)
                  .padding(16.dp)
                  .testTag(MapContentTestTags.FAB_RECENTER),
          isLocationGranted = isLocationGranted,
          current = uiState.activeTab,
          onRecenter = { viewModel.requestRecenter() },
          onAskLocation = { locationPermissions.launchMultiplePermissionRequest() },
      )
      // 6) bottom card
      SelectionBottomCard(
          modifier =
              Modifier.align(Alignment.BottomEnd)
                  .padding(12.dp)
                  .fillMaxWidth()
                  .testTag(MapContentTestTags.SELECTION_CARD),
          selection = uiState.selected,
          activeTab = uiState.activeTab,
          onPost = onPost,
          onReport = onReport,
          onDismiss = { viewModel.clearSelection() },
          onToggleLike = viewModel::toggleLike,
          onProfile = onProfile,
          isCurrentUser = isCurrentUser,
      )

      // 7) back button
      if (!isCurrentUser) {
        BackButton(
            modifier =
                Modifier.padding(16.dp, vertical = 36.dp)
                    .align(Alignment.TopStart)
                    .testTag(MapContentTestTags.BACK_BUTTON),
            onGoBack = onGoBack,
            currentTab = uiState.activeTab,
        )
      }

      // I know it's a weird placement mais the idea is to have the error overlay above the refresh
      // button and to keep the map visible below
      if (uiState.isError) {
        LoadingFail(
            modifier =
                Modifier.align(Alignment.Center).background(colorScheme.surface.copy(alpha = 0.7f)))
      }
      // 8) refresh
      MapRefreshButton(
          modifier =
              Modifier.align(Alignment.TopEnd)
                  .padding(top = 56.dp, end = 8.dp)
                  .testTag(MapContentTestTags.REFRESH),
          isRefreshing = uiState.isRefreshing,
          currentTab = uiState.activeTab,
          onRefresh = { viewModel.refreshUIState(userId) },
      )
      // 9) loading overlay
      if (showLoading) {
        LoadingScreen(
            modifier =
                Modifier.align(Alignment.Center).background(colorScheme.surface.copy(alpha = 0.7f)))
      }

      // 10) camera recenter
      LaunchedEffect(render.recenterNonce) {
        if (render.recenterNonce != null) {
          val target = lastPosition
          val longitude = uiState.centerCoordinates.longitude
          val latitude = uiState.centerCoordinates.latitude
          val fallback = Point.fromLngLat(longitude, latitude)
          val center = target ?: fallback
          val zoom = if (target != null) 14.0 else 12.0
          mapView!!
              .mapboxMap
              .flyTo(
                  CameraOptions.Builder().center(center).zoom(zoom).build(),
                  mapAnimationOptions { duration(800L) },
              )
          viewModel.consumeRecenter()
        }
      }
    }
  }
}

/**
 * Recenter Floating Action Button Composable.
 *
 * @param modifier Modifier to be applied to the FAB.
 * @param isLocationGranted Boolean indicating if location permission is granted.
 * @param current Current MapTab.
 * @param onRecenter Callback when recentering is requested.
 * @param onAskLocation Callback when location permission is requested.
 */
@Composable
fun RecenterFab(
    modifier: Modifier,
    isLocationGranted: Boolean,
    current: MapTab,
    onRecenter: () -> Unit,
    onAskLocation: () -> Unit,
) {
  val cs = colorScheme
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
          imageVector = Icons.Default.LocationOff,
          contentDescription = "Enable location",
      )
    }
  }
}

/**
 * Composable that adds a tap listener to the MapView to clear selection.
 *
 * @param mapView The MapView to add the listener to.
 * @param onDismiss Callback when the map is tapped.
 */
@Composable
fun MapTapToClearSelection(mapView: MapView?, onDismiss: () -> Unit) {
  DisposableEffect(mapView) {
    val listener = OnMapClickListener {
      onDismiss()
      true
    }
    mapView?.gestures?.addOnMapClickListener(listener)
    onDispose { mapView?.gestures?.removeOnMapClickListener(listener) }
  }
}

/**
 * Refresh Button Composable with rotation animation when refreshing.
 *
 * @param modifier Modifier to be applied to the button.
 * @param isRefreshing Boolean indicating if a refresh is in progress.
 * @param currentTab Current MapTab.
 * @param onRefresh Callback when the button is clicked.
 */
@Composable
fun MapRefreshButton(
    modifier: Modifier = Modifier,
    isRefreshing: Boolean,
    currentTab: MapTab,
    onRefresh: () -> Unit,
) {
  val cs = colorScheme
  val mapUi = colorsForMapTab(currentTab, cs)

  // Rotation anim
  val rotation =
      if (isRefreshing) {
        val t = rememberInfiniteTransition(label = "refreshRotation")
        t.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(animation = tween(800, easing = LinearEasing)),
                label = "rotationAnim",
            )
            .value
      } else 0f

  IconButton(
      onClick = onRefresh,
      enabled = !isRefreshing,
      modifier = modifier.clip(CircleShape).background(mapUi.bg),
  ) {
    Icon(
        imageVector = Icons.Default.Refresh,
        contentDescription = "Refresh",
        tint = cs.background,
        modifier =
            Modifier.size(26.dp)
                .graphicsLayer(rotationZ = rotation)
                .testTag(MapContentTestTags.REFRESH_SPINNER),
    )
  }
}

/**
 * Back Button Composable.
 *
 * @param modifier Modifier to be applied to the button.
 * @param onGoBack Callback when the button is clicked.
 */
@Composable
fun BackButton(
    modifier: Modifier = Modifier,
    onGoBack: () -> Unit = {},
    currentTab: MapTab,
) {
  val cs = colorScheme
  val mapUi = colorsForMapTab(currentTab, cs)

  Box(modifier = modifier) {
    Row(
        modifier =
            Modifier.clickable(onClick = onGoBack)
                .clip(RoundedCornerShape(20.dp))
                .background(colorScheme.background)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back to Profile",
          tint = mapUi.bg,
      )

      Text(
          text = "Back",
          color = mapUi.bg,
      )
    }
  }
}
