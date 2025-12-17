package com.android.wildex.ui.camera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.wildex.R

object CameraPermissionScreenTestTags {
  const val CAMERA_PERMISSION_CAMERA_ICON = "camera_permission_camera_icon"
  const val CAMERA_PERMISSION_MESSAGE_1 = "camera_permission_message_1"
  const val CAMERA_PERMISSION_MESSAGE_2 = "camera_permission_message_2"
  const val CAMERA_PERMISSION_BUTTON = "camera_permission_button"
  const val CAMERA_PERMISSION_UPLOAD_BUTTON = "camera_permission_upload_button"
}

/**
 * UI shown when the app lacks camera permission.
 *
 * Displays an explanatory message and provides actions to request permission or upload a photo from
 * device storage.
 *
 * @param onRequestPermission Callback invoked when user requests the camera permission.
 * @param onUploadClick Callback invoked when user chooses to upload an image instead.
 * @param modifier Modifier applied to the root container.
 * @param permissionRequestMsg Primary message explaining the permission need.
 * @param extraRequestMsg Optional extra message with more details.
 */
@Composable
fun CameraPermissionScreen(
    onRequestPermission: () -> Unit,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier,
    permissionRequestMsg: String = "",
    extraRequestMsg: String = ""
) {
  Box(
      modifier = modifier.fillMaxSize().background(colorScheme.background),
      contentAlignment = Alignment.Center,
  ) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp),
    ) {
      // Sad camera icon
      Icon(
          imageVector = Icons.Default.CameraAlt,
          contentDescription = null,
          tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
          modifier =
              Modifier.size(120.dp)
                  .testTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_CAMERA_ICON),
      )

      Text(
          text = permissionRequestMsg,
          style = typography.headlineMedium,
          color = colorScheme.onBackground,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_MESSAGE_1),
      )

      if (extraRequestMsg.isNotEmpty()) {
        Text(
            text = extraRequestMsg,
            style = typography.bodyLarge,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_MESSAGE_2),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Request permission button
      Button(
          onClick = onRequestPermission,
          modifier =
              Modifier.fillMaxWidth()
                  .height(56.dp)
                  .testTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_BUTTON),
          colors =
              ButtonDefaults.buttonColors(
                  containerColor = colorScheme.primary,
                  contentColor = colorScheme.onPrimary,
              ),
          shape = RoundedCornerShape(16.dp),
      ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.request_permission_button),
            style = typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
      }

      // Upload button
      OutlinedButton(
          onClick = onUploadClick,
          modifier =
              Modifier.fillMaxWidth()
                  .height(56.dp)
                  .testTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_UPLOAD_BUTTON),
          border = BorderStroke(2.dp, colorScheme.outline),
          shape = RoundedCornerShape(16.dp),
      ) {
        Icon(
            imageVector = Icons.Default.Upload,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.upload_button),
            style = typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}
