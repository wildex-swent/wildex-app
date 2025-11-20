package com.android.wildex.ui.camera

import android.content.Context
import android.net.Uri
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CameraPreviewScreenTestTags {
  const val CAMERA_PREVIEW_CAMERA_VIEWFINDER = "camera_preview_camera_viewfinder"
  const val CAMERA_PREVIEW_CAPTURE_BUTTON = "camera_preview_capture_button"
  const val CAMERA_PREVIEW_UPLOAD_BUTTON = "camera_preview_upload_button"
  const val CAMERA_PREVIEW_SWITCH_BUTTON = "camera_preview_switch_button"
  const val CAMERA_PREVIEW_ZOOM_VALUE = "camera_preview_zoom_value"
}

@Composable
fun CameraPreviewScreen(
    onPhotoTaken: (Uri) -> Unit,
    onUploadClick: () -> Unit,
    modifier: Modifier,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val previewUseCase = remember { Preview.Builder().build() }
  val imageCaptureUseCase = remember { ImageCapture.Builder().build() }
  val surfaceRequest = remember { mutableStateOf<SurfaceRequest?>(null) }
  val cameraRef = remember { mutableStateOf<Camera?>(null) }
  val cameraSelector = remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
  val zoomRatio = remember { mutableFloatStateOf(1f) }

  // Initialize camera
  LaunchedEffect(cameraSelector.value) {
    val cameraProvider = ProcessCameraProvider.awaitInstance(context)
    withContext(mainDispatcher) {
      cameraProvider.unbindAll()
      val camera =
          cameraProvider.bindToLifecycle(
              lifecycleOwner,
              cameraSelector.value,
              previewUseCase,
              imageCaptureUseCase,
          )
      cameraRef.value = camera
      previewUseCase.setSurfaceProvider { surfaceRequest.value = it }
      camera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomRatio.floatValue = it.zoomRatio }
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    // Camera viewfinder
    CameraPreview(surfaceRequest.value, cameraRef.value)

    // Bottom controls
    CameraControls(
        onSwitchClick = {
          if (cameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA)
              cameraSelector.value = CameraSelector.DEFAULT_FRONT_CAMERA
          else cameraSelector.value = CameraSelector.DEFAULT_BACK_CAMERA
        },
        onCaptureClick = {
          imageCaptureUseCase.capturePhoto(context, onPhotoTaken, cameraSelector.value)
        },
        onUploadClick = onUploadClick,
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .padding(start = 60.dp, end = 60.dp, bottom = 40.dp),
        zoomValue = zoomRatio.floatValue,
    )
  }
}

@Composable
private fun CameraPreview(surfaceRequest: SurfaceRequest?, cameraRef: Camera?) {
  Box(
      modifier =
          Modifier.fillMaxSize()
              .testTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_CAMERA_VIEWFINDER)) {
        surfaceRequest?.let {
          CameraXViewfinder(
              modifier =
                  Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                      cameraRef
                          ?.cameraControl
                          ?.setZoomRatio(cameraRef.cameraInfo.zoomState.value!!.zoomRatio * zoom)
                    }
                  },
              surfaceRequest = surfaceRequest,
          )
        }
      }
}

@Composable
private fun CameraControls(
    onSwitchClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onUploadClick: () -> Unit,
    zoomValue: Float = 1.0f,
    modifier: Modifier,
) {
  Column(
      modifier = modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    ZoomText(
        zoomValue = zoomValue,
        modifier = Modifier.testTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_ZOOM_VALUE),
    )

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      // Upload button - left side
      UploadButton(
          onClick = onUploadClick,
          modifier = Modifier.testTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_UPLOAD_BUTTON),
      )

      // Capture button - main action (larger)
      CaptureButton(
          onClick = onCaptureClick,
          modifier = Modifier.testTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_CAPTURE_BUTTON),
      )

      // Switch button - right side
      SwitchButton(
          onClick = onSwitchClick,
          modifier = Modifier.testTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_SWITCH_BUTTON),
      )
    }
  }
}

@Composable
private fun ZoomText(zoomValue: Float, modifier: Modifier) {
  Surface(
      modifier = modifier,
      shape = RoundedCornerShape(20.dp),
      color = colorScheme.surface.copy(alpha = 0.25f),
  ) {
    Text(
        text = "Zoom: ${"%.2f".format(zoomValue)}",
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        style = typography.bodySmall,
    )
  }
}

@Composable
private fun CaptureButton(
    onClick: () -> Unit,
    modifier: Modifier,
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
        modifier = Modifier.fillMaxSize(0.6f),
    )
  }
}

@Composable
private fun UploadButton(
    onClick: () -> Unit,
    modifier: Modifier,
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
        modifier = Modifier.fillMaxSize(0.5f),
    )
  }
}

@Composable
private fun SwitchButton(
    onClick: () -> Unit,
    modifier: Modifier,
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
        imageVector = Icons.Default.Cameraswitch,
        contentDescription = "Switch camera",
        tint = colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxSize(0.5f),
    )
  }
}

// Helper function for capture logic
private fun ImageCapture.capturePhoto(
    context: Context,
    onPhotoTaken: (Uri) -> Unit,
    cameraSelector: CameraSelector,
) {
  val photoFile = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
  val metaData = ImageCapture.Metadata()
  if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) metaData.isReversedHorizontal = true
  val outputOptions =
      ImageCapture.OutputFileOptions.Builder(photoFile).setMetadata(metaData).build()

  takePicture(
      outputOptions,
      ContextCompat.getMainExecutor(context),
      object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
          output.savedUri?.let { onPhotoTaken(it) }
        }

        override fun onError(exception: ImageCaptureException) {
          // Error
        }
      },
  )
}
