package com.android.wildex.ui.camera

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.LoadingScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    cameraScreenViewModel: CameraScreenViewModel = viewModel(),
    onPost: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
) {
  val uiState by cameraScreenViewModel.uiState.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      cameraScreenViewModel.clearErrorMsg()
    }
  }

  val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
  val hasCameraPermission = cameraPermissionState.status.isGranted

  Scaffold(bottomBar = { bottomBar() }) { innerPadding ->
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(innerPadding).fillMaxSize(),
    ) {
      when {
        !hasCameraPermission ->
            CameraPermissionRequest(
                requestPermission = { cameraPermissionState.launchPermissionRequest() },
            )
        uiState.animalDetectResponse == null ->
            CameraPreview(onPhotoTaken = { cameraScreenViewModel.detectAnimalImage(it) })
        uiState.isDetecting -> DetectionLoadingScreen()
        uiState.postInConstruction != null ->
            PostCreationPrompt(
                onDescUpdate = { cameraScreenViewModel.updateDescription(it) },
                onPost = {
                  cameraScreenViewModel.createPost(
                      location = Location(0.0, 0.0), /* placeholder */
                      onPost = { onPost() },
                  )
                },
                onCancel = { cameraScreenViewModel.resetState() },
            )
        uiState.isLoading -> LoadingScreen()
      }
    }
  }
}

@Composable
fun CameraPermissionRequest(requestPermission: () -> Unit) {
    LaunchedEffect(Unit) { requestPermission() }
  /* Camera permission request UI, eg. Request Button */
}

@Composable
fun CameraPreview(onPhotoTaken: (Uri) -> Unit) {
  /* Camera preview with an import photo button */
}

@Composable
fun DetectionLoadingScreen() {
  /* Custom loading screen for detection */
}

@Composable
fun PostCreationPrompt(onDescUpdate: (String) -> Unit, onPost: () -> Unit, onCancel: () -> Unit) {}
