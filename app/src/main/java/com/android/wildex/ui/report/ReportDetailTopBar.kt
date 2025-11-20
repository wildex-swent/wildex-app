package com.android.wildex.ui.report

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.android.wildex.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailsTopBar(onGoBack: () -> Unit = {}) {
  TopAppBar(
      navigationIcon = {
        IconButton(
            modifier = Modifier.testTag(ReportDetailsScreenTestTags.BACK_BUTTON),
            onClick = onGoBack,
        ) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = colorScheme.onBackground,
          )
        }
      },
      title = { Text(LocalContext.current.getString(R.string.report_details_bar_title)) },
  )
}
