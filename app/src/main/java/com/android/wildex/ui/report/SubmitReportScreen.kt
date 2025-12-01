package com.android.wildex.ui.report

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.R
import com.android.wildex.model.DefaultConnectivityObserver
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.camera.CameraPermissionScreen
import com.android.wildex.ui.camera.CameraPreviewScreen
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.utils.offline.OfflineScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

/**
 * Screen displaying the Submit Report Screen.
 *
 * @param viewModel The ViewModel managing the state of the Submit Report Screen.
 * @param onSubmitted Callback invoked when the report has been successfully submitted.
 * @param onGoBack Callback invoked when the user wants to go back to the previous screen.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SubmitReportScreen(
    viewModel: SubmitReportScreenViewModel = viewModel(),
    onSubmitted: () -> Unit = {},
    onGoBack: () -> Unit = {},
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  val connectivityObserver = remember { DefaultConnectivityObserver(context) }
  val isOnlineObs by connectivityObserver.isOnline.collectAsState()
  val isOnline = isOnlineObs && LocalConnectivityObserver.current

  val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

  // Display error messages as Toasts
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      viewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(NavigationTestTags.SUBMIT_REPORT_SCREEN),
      topBar = { SubmitReportTopBar(context = context, onGoBack = onGoBack) },
  ) { innerPadding ->
    if (isOnline) {
      SubmitReportScreenContent(
          uiState = uiState,
          viewModel = viewModel,
          locationClient = locationClient,
          context = context,
          onSubmitted = onSubmitted,
          onGoBack = onGoBack,
          innerPadding = innerPadding,
      )
    } else {
      OfflineScreen(innerPadding = innerPadding)
    }
  }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SubmitReportScreenContent(
    uiState: SubmitReportUiState,
    viewModel: SubmitReportScreenViewModel,
    locationClient: FusedLocationProviderClient,
    context: Context,
    onSubmitted: () -> Unit,
    onGoBack: () -> Unit,
    innerPadding: PaddingValues,
) {
  var showCamera by remember { mutableStateOf(false) }
  var locationRequested by remember { mutableStateOf(false) }

  val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
  val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
  val hasCameraPermission = cameraPermissionState.status.isGranted
  val hasLocationPermission = locationPermissionState.status.isGranted

  val imagePickerLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.GetContent(),
      ) {
        viewModel.updateImage(it)
        showCamera = false
      }

  // Fetch location when permission is granted
  LaunchedEffect(hasLocationPermission, locationRequested) {
    if (hasLocationPermission && locationRequested) {
      viewModel.fetchUserLocation(locationClient)
    } else if (!hasLocationPermission && locationRequested) {
      Toast.makeText(
              context,
              "Location permission is required to submit a report",
              Toast.LENGTH_SHORT,
          )
          .show()
    }
  }

  Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
    when {
      showCamera && hasCameraPermission -> {
        CameraPreviewScreen(
            onPhotoTaken = { uri ->
              viewModel.updateImage(uri)
              showCamera = false
            },
            onUploadClick = {
              showCamera = false
              imagePickerLauncher.launch("image/*")
            },
            modifier = Modifier,
        )
      }
      showCamera && !hasCameraPermission -> {
        CameraPermissionScreen(
            onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
            onUploadClick = { imagePickerLauncher.launch("image/*") },
            permissionRequestMsg = context.getString(R.string.camera_request_msg_1),
            extraRequestMsg = context.getString(R.string.camera_request_msg_2),
        )
      }
      uiState.isSubmitting -> {
        LoadingScreen()
      }
      else -> {
        SubmitReportFormScreen(
            uiState = uiState,
            onCameraClick = { showCamera = true },
            onDescriptionChange = viewModel::updateDescription,
            onSubmitClick = {
              if (!hasLocationPermission) {
                locationRequested = true
                locationPermissionState.launchPermissionRequest()
              } else {
                viewModel.fetchUserLocation(locationClient)
                viewModel.submitReport(onSubmitted)
              }
            },
            context = context,
            onGoBack = onGoBack,
        )
      }
    }
  }
}

/**
 * Top bar for the Submit Report Screen.
 *
 * @param context The context of the current state of the application.
 * @param onGoBack Callback invoked when the user wants to go back to the previous screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitReportTopBar(context: Context, onGoBack: () -> Unit) {
  CenterAlignedTopAppBar(
      modifier = Modifier.testTag(SubmitReportFormScreenTestTags.TOP_APP_BAR),
      title = {
        Text(
            text = context.getString(R.string.report),
            modifier = Modifier.testTag(SubmitReportFormScreenTestTags.TOP_APP_BAR_TEXT),
        )
      },
      navigationIcon = {
        IconButton(
            onClick = onGoBack,
            modifier = Modifier.testTag(SubmitReportFormScreenTestTags.BACK_BUTTON),
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
      },
  )
}
