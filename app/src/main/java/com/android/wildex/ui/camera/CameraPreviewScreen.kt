package com.android.wildex.ui.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

@Composable
fun CameraPreviewScreen(
    onPhotoTaken: (Uri) -> Unit,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val previewUseCase = remember { Preview.Builder().build() }
  val imageCaptureUseCase = remember { ImageCapture.Builder().build() }
  val surfaceRequest = remember { mutableStateOf<SurfaceRequest?>(null) }

  // Initialize camera
  LaunchedEffect(Unit) {
    val cameraProvider = ProcessCameraProvider.awaitInstance(context)
    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        CameraSelector.DEFAULT_BACK_CAMERA,
        previewUseCase,
        imageCaptureUseCase,
    )
    previewUseCase.setSurfaceProvider { surfaceRequest.value = it }
  }

  Box(modifier = modifier.fillMaxSize()) {
    // Camera viewfinder
    CameraViewfinder(surfaceRequest = surfaceRequest.value)

    // Gradient overlay
    CameraOverlay()

    // Bottom controls
    CameraControls(
        onCaptureClick = { capturePhoto(context, imageCaptureUseCase, onPhotoTaken) },
        onUploadClick = onUploadClick,
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .padding(start = 60.dp, end = 60.dp, bottom = 40.dp),
    )
  }
}

@Composable
private fun CameraViewfinder(surfaceRequest: SurfaceRequest?) {
  surfaceRequest?.let {
    CameraXViewfinder(
        modifier = Modifier.fillMaxSize(),
        surfaceRequest = it,
    )
  }
}

@Composable
private fun CameraOverlay() {
  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(
                  Brush.verticalGradient(
                      colors =
                          listOf(
                              colorScheme.surface.copy(alpha = 0.2f),
                              Color.Transparent,
                              colorScheme.surface.copy(alpha = 0.4f),
                          ))))
}

@Composable
private fun CameraControls(
    onCaptureClick: () -> Unit,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Box(
      modifier = modifier.fillMaxWidth(),
  ) {
    // Upload button - left side
    UploadButton(onClick = onUploadClick, modifier = Modifier.align(Alignment.CenterStart))

    // Capture button - main action (larger)
    CaptureButton(onClick = onCaptureClick, modifier = Modifier.align(Alignment.Center))
  }
}

@Composable
private fun CaptureButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  IconButton(
      onClick = onClick,
      modifier =
          modifier
              .size(80.dp)
              .background(colorScheme.primary, CircleShape)
              .border(2.dp, colorScheme.surface, CircleShape),
  ) {
    Icon(
        imageVector = Icons.Default.CameraAlt,
        contentDescription = "Capture photo",
        tint = colorScheme.onPrimary,
    )
  }
}

@Composable
private fun UploadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  IconButton(
      onClick = onClick,
      modifier =
          modifier
              .size(60.dp)
              .background(colorScheme.surfaceVariant, CircleShape)
              .border(2.dp, colorScheme.surface, CircleShape),
  ) {
    Icon(
        imageVector = Icons.Default.Upload,
        contentDescription = "Upload photo",
        tint = colorScheme.onSurfaceVariant,
    )
  }
}

// Helper function for capture logic
private fun capturePhoto(
    context: Context,
    imageCaptureUseCase: ImageCapture,
    onPhotoTaken: (Uri) -> Unit,
) {
  val photoFile = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
  val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

  imageCaptureUseCase.takePicture(
      outputOptions,
      ContextCompat.getMainExecutor(context),
      object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
          output.savedUri?.let { onPhotoTaken(it) }
          Log.d("CameraPreview", "Image captured: ${output.savedUri}")
        }

        override fun onError(exception: ImageCaptureException) {
          Log.e("CameraPreview", "Capture failed", exception)
        }
      },
  )
}
