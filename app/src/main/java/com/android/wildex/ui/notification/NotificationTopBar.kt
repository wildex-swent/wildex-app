package com.android.wildex.ui.notification

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
import androidx.compose.ui.res.stringResource
import com.android.wildex.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationTopBar(onGoBack: () -> Unit) {
  val cs = colorScheme
  val context = LocalContext.current
  CenterAlignedTopAppBar(
      title = {
        Text(
            text = stringResource(R.string.notifications_title),
            style = typography.titleLarge,
            color = cs.onBackground,
            modifier = Modifier.testTag(NotificationScreenTestTags.TOP_BAR_TITLE),
        )
      },
      navigationIcon = {
        IconButton(
            modifier = Modifier.testTag(NotificationScreenTestTags.BACK_BUTTON),
            onClick = { onGoBack() },
        ) {
          Icon(
              imageVector = Icons.Default.ChevronLeft,
              contentDescription = context.getString(R.string.back),
              tint = cs.onBackground,
          )
        }
      },
      modifier = Modifier.testTag(NotificationScreenTestTags.TOP_BAR),
  )
}
