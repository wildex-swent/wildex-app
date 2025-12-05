package com.android.wildex.ui.post

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsTopBar(
    onGoBack: () -> Unit,
    onOpenActions: () -> Unit,
) {
  TopAppBar(
      title = {},
      navigationIcon = {
        IconButton(
            onClick = onGoBack,
            modifier = Modifier.testTag(PostDetailsScreenTestTags.BACK_BUTTON)) {
              Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back to Homepage",
                  tint = colorScheme.onBackground,
              )
            }
      },
      actions = {
        IconButton(onClick = onOpenActions) {
          Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More actions")
        }
      })
}
