package com.android.wildex.ui.report

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.wildex.R

/** Test tags for the Submit Report Form Screen components. */
object SubmitReportFormScreenTestTags {
  const val TOP_APP_BAR = "submit_report_top_app_bar"
  const val TOP_APP_BAR_TEXT = "submit_report_top_app_bar_text"
  const val BACK_BUTTON = "submit_report_back_button"
  const val IMAGE_BOX = "image_box"
  const val SELECTED_IMAGE = "selected_image"
  const val CAMERA_ICON = "camera_icon"
  const val DESCRIPTION_FIELD = "description_field"
  const val SUBMIT_BUTTON = "submit_button"
}

/**
 * Screen displaying the Submit Report Form Screen.
 *
 * @param uiState The current UI state of the submit report form.
 * @param onCameraClick Callback invoked when the camera button is clicked.
 * @param onDescriptionChange Callback invoked when the description text changes.
 * @param onSubmitClick Callback invoked when the submit button is clicked.
 * @param context The context of the current state of the application.
 * @param onGoBack Callback invoked when the user wants to go back to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitReportFormScreen(
    uiState: SubmitReportUiState,
    onCameraClick: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
    context: Context,
    onGoBack: () -> Unit,
) {

  Column(
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = context.getString(R.string.submit_rescue_alert),
            style = typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(64.dp))

        Box(
            modifier =
                Modifier.fillMaxWidth(0.9f)
                    .height(200.dp)
                    .clickable { onCameraClick() }
                    .testTag(SubmitReportFormScreenTestTags.IMAGE_BOX),
            contentAlignment = Alignment.Center,
        ) {
          if (uiState.imageUri != null) {
            AsyncImage(
                model = uiState.imageUri,
                contentDescription = "Selected Image",
                modifier =
                    Modifier.fillMaxSize().testTag(SubmitReportFormScreenTestTags.SELECTED_IMAGE),
                contentScale = ContentScale.Crop,
            )
          } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier =
                        Modifier.size(100.dp).testTag(SubmitReportFormScreenTestTags.CAMERA_ICON),
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = uiState.description,
            onValueChange = onDescriptionChange,
            label = {
              Text(
                  text = context.getString(R.string.description),
                  style = typography.bodyMedium,
                  color = colorScheme.onBackground)
            },
            modifier =
                Modifier.fillMaxWidth(0.9f)
                    .height(100.dp)
                    .testTag(SubmitReportFormScreenTestTags.DESCRIPTION_FIELD),
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSubmitClick,
            enabled =
                !uiState.isSubmitting &&
                    uiState.imageUri != null &&
                    uiState.description.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.onBackground),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.testTag(SubmitReportFormScreenTestTags.SUBMIT_BUTTON),
        ) {
          Text(
              text =
                  if (uiState.isSubmitting) context.getString(R.string.submitting)
                  else context.getString(R.string.submitted),
              color = colorScheme.background,
              style = typography.bodyMedium,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
          )
        }
      }
}
