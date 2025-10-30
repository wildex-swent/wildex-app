package com.android.wildex.ui.camera

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

  val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)
  val hasLocationPermission = locationPermissionState.status.isGranted

  val imagePickerLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.GetContent(),
      ) { uri ->
        uri?.let {
          cameraScreenViewModel.updateImageUri(it)
          cameraScreenViewModel.detectAnimalImage(it, context)
        }
      }

  Scaffold(bottomBar = { bottomBar() }) { innerPadding ->
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(innerPadding).fillMaxSize(),
    ) {
      when {
        !hasCameraPermission ->
            CameraPermissionScreen(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                onUploadClick = { imagePickerLauncher.launch("image/*") },
            )
        uiState.isLoading -> LoadingScreen()
        uiState.isDetecting -> DetectingScreen(uiState.currentImageUri!!)
        uiState.animalDetectResponse == null ->
            CameraPreviewScreen(
                onPhotoTaken = {
                  cameraScreenViewModel.updateImageUri(it)
                  cameraScreenViewModel.detectAnimalImage(it, context)
                },
                onUploadClick = { imagePickerLauncher.launch("image/*") },
            )
        uiState.animalDetectResponse != null ->
            PostCreationScreen(
                description = uiState.description,
                onDescriptionChange = { cameraScreenViewModel.updateDescription(it) },
                useLocation = uiState.addLocation,
                onLocationToggle = {
                  if (!hasLocationPermission) {
                    locationPermissionState.launchPermissionRequest()
                  } else {
                    cameraScreenViewModel.toggleAddLocation()
                  }
                },
                photoUri = uiState.currentImageUri!!,
                boundingBox = uiState.animalDetectResponse!!.boundingBox,
                animalName = uiState.animalDetectResponse!!.animalType,
                speciesName = uiState.animalDetectResponse!!.taxonomy.species,
                onConfirm = {
                  cameraScreenViewModel.createPost(context = context, onPost = onPost)
                },
                onCancel = { cameraScreenViewModel.resetState() },
            )
      }
    }
  }
}
