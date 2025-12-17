package com.android.wildex.ui.post

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Top bar used on the Post Details screen.
 *
 * Shows a back button and an actions button.
 *
 * @param onGoBack Called when the back button is pressed.
 * @param onOpenActions Called when the actions (more) button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsTopBar(
    onGoBack: () -> Unit,
    onOpenActions: () -> Unit,
) {
  CenterAlignedTopAppBar(
      title = {},
      navigationIcon = {
        IconButton(
            onClick = onGoBack,
            modifier = Modifier.testTag(PostDetailsScreenTestTags.BACK_BUTTON)) {
              Icon(
                  imageVector = Icons.Default.ChevronLeft,
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
