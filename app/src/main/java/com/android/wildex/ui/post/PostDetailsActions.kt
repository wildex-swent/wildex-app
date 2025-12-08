package com.android.wildex.ui.post

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.android.wildex.R
import com.android.wildex.model.utils.Location
import kotlinx.coroutines.launch

object PostDetailsActionsTestTags {
  const val SHEET = "action_sheet"
  const val BTN_GOOGLE_MAPS = "action_sheet_google_maps"
  const val BTN_COPY = "action_sheet_copy"
  const val BTN_SHARE = "action_sheet_share"
  const val BTN_DELETE = "action_sheet_delete"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsActions(
    postLocation: Location?,
    onDeletePressed: () -> Unit,
    onDismissRequest: () -> Unit,
    isAuthor: Boolean = false,
) {

  val locationUri =
      postLocation?.let {
        "geo:${it.latitude},${it.longitude}?q=${it.latitude},${it.longitude}(${it.name})".toUri()
      }
  val context = LocalContext.current
  val clipboard = LocalClipboard.current
  val currentScope = rememberCoroutineScope()

  ModalBottomSheet(
      onDismissRequest = onDismissRequest,
      modifier = Modifier.testTag(PostDetailsActionsTestTags.SHEET),
      containerColor = colorScheme.background,
  ) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = LocalWindowInfo.current.containerSize.height.dp * 0.8f)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

      // Google Maps
      item {
        OutlinedButton(
            onClick = {
              locationUri?.let {
                val intent = Intent(Intent.ACTION_VIEW, it)
                intent.setPackage("com.google.android.apps.maps")
                if (intent.resolveActivity(context.packageManager) != null) {
                  context.startActivity(intent)
                } else {
                  Toast.makeText(context, "No Google Maps app found", Toast.LENGTH_SHORT).show()
                }
              }
              onDismissRequest()
            },
            enabled = locationUri != null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PostDetailsActionsTestTags.BTN_GOOGLE_MAPS),
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
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
                imageVector = Icons.Filled.Map,
                contentDescription = "Google Maps",
                tint = colorScheme.primary,
            )
            Text(context.getString(R.string.post_details_actions_google_maps))
          }
        }
      }

      // Copy
      item {
        OutlinedButton(
            onClick = {
              postLocation?.name.let {
                currentScope.launch {
                  clipboard.setClipEntry(
                      ClipEntry(clipData = ClipData.newPlainText("location", it))
                  )
                }
                Toast.makeText(context, "Location copied", Toast.LENGTH_SHORT).show()
              }
              onDismissRequest()
            },
            enabled = locationUri != null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PostDetailsActionsTestTags.BTN_COPY),
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
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = "Copy location",
                tint = colorScheme.primary,
            )
            Text(context.getString(R.string.post_details_actions_copy))
          }
        }
      }

      // Share
      item {
        OutlinedButton(
            onClick = {
              postLocation?.let {
                val url =
                    "https://www.google.com/maps/search/?api=1&query=${it.latitude},${it.longitude}"
                val intent =
                    Intent(Intent.ACTION_SEND).apply {
                      type = "text/plain"
                      putExtra(
                          Intent.EXTRA_TEXT,
                          context.getString(R.string.post_details_share_body, url),
                      )
                    }
                context.startActivity(Intent.createChooser(intent, "Share location"))
              }
              onDismissRequest()
            },
            enabled = locationUri != null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PostDetailsActionsTestTags.BTN_SHARE),
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
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = "Share location",
                tint = colorScheme.primary,
            )
            Text(context.getString(R.string.post_details_actions_share))
          }
        }
      }

      // Post actions for author
      if (isAuthor) {
        item {
          OutlinedButton(
              onClick = onDeletePressed,
              modifier = Modifier
                  .fillMaxWidth()
                  .testTag(PostDetailsActionsTestTags.BTN_DELETE),
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
                verticalAlignment = Alignment.CenterVertically,
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
      }

      item { Spacer(Modifier.height(12.dp)) }
    }
  }
}
