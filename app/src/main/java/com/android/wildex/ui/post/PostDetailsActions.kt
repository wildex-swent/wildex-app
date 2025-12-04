package com.android.wildex.ui.post

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.wildex.R

object PostDetailsActionsTestTags {
  const val SHEET = "action_sheet"
  const val TITLE = "action_sheet_title"
  const val LOCATION = "action_sheet_location"
  const val BTN_GOOGLE_MAPS = "action_sheet_google_maps"
  const val BTN_COPY = "action_sheet_copy"
  const val BTN_SHARE = "action_sheet_share"
  const val POST = "action_sheet_post"
  const val BTN_DELETE = "action_sheet_delete"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsActions(
    onDeletePressed: () -> Unit,
    onDismissRequest: () -> Unit,
    isAuthor: Boolean = false,
) {

  val context = LocalContext.current
  val secondaryText = colorScheme.onBackground.copy(alpha = 0.7f)

  ModalBottomSheet(
      onDismissRequest = onDismissRequest,
      dragHandle = { BottomSheetDefaults.DragHandle() },
      modifier = Modifier.testTag(PostDetailsActionsTestTags.SHEET),
      contentColor = colorScheme.background,
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
          text = context.getString(R.string.post_details_actions),
          color = colorScheme.onBackground,
          style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          modifier = Modifier.testTag(PostDetailsActionsTestTags.TITLE),
      )

      Text(
          text = context.getString(R.string.post_details_actions_location),
          color = secondaryText,
          style = typography.bodyMedium,
          modifier = Modifier.testTag(PostDetailsActionsTestTags.LOCATION),
      )

      // Google Maps
      OutlinedButton(
          onClick = {
            // TODO open in Google Maps (copy report details or refactor for both)
            onDismissRequest()
          },
          Modifier.fillMaxWidth().testTag(PostDetailsActionsTestTags.BTN_GOOGLE_MAPS),
          shape = RoundedCornerShape(28.dp),
          border = BorderStroke(1.dp, colorScheme.outline),
          colors =
              ButtonDefaults.outlinedButtonColors(
                  contentColor = colorScheme.onBackground,
              ),
      ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
              imageVector = Icons.Filled.Map,
              contentDescription = "Google Maps",
              tint = colorScheme.primary,
          )
          Text(context.getString(R.string.post_details_actions_google_maps))
        }
      }

      // Copy
      OutlinedButton(
          onClick = {
            // TODO copy location to clipboard (copy report details or refactor for both)
            onDismissRequest()
          },
          modifier = Modifier.fillMaxWidth().testTag(PostDetailsActionsTestTags.BTN_COPY),
          shape = RoundedCornerShape(28.dp),
          border = BorderStroke(1.dp, colorScheme.outline),
          colors =
              ButtonDefaults.outlinedButtonColors(
                  contentColor = colorScheme.onBackground,
              ),
      ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
              imageVector = Icons.Filled.ContentCopy,
              contentDescription = "Copy location",
              tint = colorScheme.onBackground,
          )
          Text(context.getString(R.string.post_details_actions_copy))
        }
      }

      // Share
      OutlinedButton(
          onClick = {
            // TODO share location (copy report details or refactor for both)
            onDismissRequest()
          },
          modifier = Modifier.fillMaxWidth().testTag(PostDetailsActionsTestTags.BTN_SHARE),
          shape = RoundedCornerShape(28.dp),
          border = BorderStroke(1.dp, colorScheme.outline),
          colors =
              ButtonDefaults.outlinedButtonColors(
                  contentColor = colorScheme.onBackground,
              ),
      ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
              imageVector = Icons.Filled.Share,
              contentDescription = "Share location",
              tint = colorScheme.onBackground,
          )
          Text(context.getString(R.string.post_details_actions_share))
        }
      }

      // Post actions for author
      if (isAuthor) {
        Text(
            text = context.getString(R.string.post_details_actions_post),
            color = secondaryText,
            style = typography.bodyMedium,
            modifier = Modifier.testTag(PostDetailsActionsTestTags.POST),
        )

        OutlinedButton(
            onClick = onDeletePressed,
            modifier = Modifier.fillMaxWidth().testTag(PostDetailsActionsTestTags.BTN_DELETE),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, colorScheme.outline),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = colorScheme.onBackground,
                ),
        ) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete Post",
                tint = colorScheme.error,
            )
            Text(
                text = context.getString(R.string.post_details_actions_delete_post),
                color = colorScheme.error,
            )
          }
        }
      }

      Spacer(Modifier.height(12.dp))
    }
  }
}
