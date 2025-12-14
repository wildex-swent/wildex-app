package com.android.wildex.ui.report

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.android.wildex.R

/**
 * Top bar for the report details screen with a back button and title.
 *
 * @param onGoBack Callback when the back button is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailsTopBar(onGoBack: () -> Unit = {}) {
  val context = LocalContext.current
  CenterAlignedTopAppBar(
      navigationIcon = {
        IconButton(
            modifier = Modifier.testTag(ReportDetailsScreenTestTags.BACK_BUTTON),
            onClick = onGoBack,
        ) {
          Icon(
              imageVector = Icons.Default.ChevronLeft,
              contentDescription = context.getString(R.string.back),
              tint = colorScheme.onBackground,
          )
        }
      },
      title = {}
  )
}
