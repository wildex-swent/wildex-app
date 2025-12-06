package com.android.wildex.ui.locationpicker

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.android.wildex.R
import com.android.wildex.ui.report.SubmitReportFormScreenTestTags

/**
 * Top bar for the Submit Report Screen.
 *
 * @param context The context of the current state of the application.
 * @param onGoBack Callback invoked when the user wants to go back to the previous screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerTopBar(context: Context, onGoBack: () -> Unit) {
  CenterAlignedTopAppBar(
      modifier = Modifier.testTag(SubmitReportFormScreenTestTags.TOP_APP_BAR),
      title = {
        Text(
            text = context.getString(R.string.location_picker_title),
            modifier = Modifier.testTag(LocationPickerTestTags.TOP_APP_BAR_TEXT),
        )
      },
      navigationIcon = {
        IconButton(
            onClick = onGoBack,
            modifier = Modifier.testTag(LocationPickerTestTags.BACK_BUTTON),
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
      },
  )
}
