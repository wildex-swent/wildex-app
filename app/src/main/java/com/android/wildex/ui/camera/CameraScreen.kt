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
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.ui.LoadingScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

object CameraScreenTestTags {
    const val CAMERA_PERMISSION_SCREEN = "camera_permission_screen"
    const val CAMERA_PREVIEW_SCREEN = "camera_preview_screen"
    const val DETECTING_SCREEN = "detecting_screen"
    const val POST_CREATION_SCREEN = "post_creation_screen"
}

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

    Scaffold(bottomBar = bottomBar) { innerPadding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            when {
                uiState.currentImageUri == null ->
                    if (hasCameraPermission) {
                        CameraPreviewScreen(
                            onPhotoTaken = {
                                cameraScreenViewModel.updateImageUri(it)
                                cameraScreenViewModel.detectAnimalImage(it, context)
                            },
                            onUploadClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.testTag(CameraScreenTestTags.CAMERA_PREVIEW_SCREEN)
                        )
                    } else {
                        CameraPermissionScreen(
                            onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                            onUploadClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.testTag(CameraScreenTestTags.CAMERA_PERMISSION_SCREEN)
                        )
                    }
                uiState.isDetecting -> DetectingScreen(photoUri = uiState.currentImageUri!!,
                    modifier = Modifier.testTag(CameraScreenTestTags.DETECTING_SCREEN))
                uiState.animalDetectResponse != null ->
                    PostCreationScreen(
                        description = uiState.description,
                        onDescriptionChange = { cameraScreenViewModel.updateDescription(it) },
                        useLocation = uiState.addLocation,
                        onLocationToggle = {
                            if (!hasLocationPermission) {
                                locationPermissionState.launchPermissionRequest()
                                if (locationPermissionState.status.isGranted)
                                    cameraScreenViewModel.toggleAddLocation()
                            } else {
                                cameraScreenViewModel.toggleAddLocation()
                            }
                        },
                        photoUri = uiState.currentImageUri!!,
                        detectionResponse = uiState.animalDetectResponse!!,
                        onConfirm = {
                            cameraScreenViewModel.createPost(context = context, onPost = onPost)
                        },
                        onCancel = { cameraScreenViewModel.resetState() },
                        modifier = Modifier.testTag(CameraScreenTestTags.POST_CREATION_SCREEN),
                    )
                uiState.isLoading -> LoadingScreen()
            }
        }
    }
}
