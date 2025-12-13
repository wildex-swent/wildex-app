package com.android.wildex.ui.camera

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.wildex.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi

object SaveToGalleryScreenTestTags {
  const val DISCARD_BUTTON = "save_to_gallery_discard_button"
  const val DISCARD_CANCEL = "save_to_gallery_discard_cancel"
  const val DISCARD_TEXT_FIRST = "save_to_gallery_discard_text_first"
  const val DISCARD_TEXT_SECOND = "save_to_gallery_discard_text_second"
  const val DISCARD_TEXT_THIRD = "save_to_gallery_discard_text_third"
  const val PICTURE = "save_to_gallery_picture"
  const val SAVE_BUTTON = "save_to_gallery_save_button"
  const val SAVE_CANCEL = "save_to_gallery_save_cancel"
  const val SAVE_TEXT_FIRST = "save_to_gallery_save_text_first"
  const val SAVE_TEXT_SECOND = "save_to_gallery_save_text_second"
  const val SAVE_TEXT_THIRD = "save_to_gallery_save_text_third"
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SaveToGalleryScreen(
    photoUri: Uri,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var showSaveDialog by remember { mutableStateOf(false) }
  var showDiscardDialog by remember { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize()) {
    AsyncImage(
        model = photoUri,
        contentDescription = "photoToSave",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().testTag(SaveToGalleryScreenTestTags.PICTURE),
    )

    DiscardButton(
        onClick = { showDiscardDialog = true },
        modifier =
            Modifier.padding(top = 20.dp, start = 20.dp)
                .testTag(SaveToGalleryScreenTestTags.DISCARD_BUTTON),
    )

    SaveButton(
        onClick = { showSaveDialog = true },
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .padding(40.dp)
                .testTag(SaveToGalleryScreenTestTags.SAVE_BUTTON),
    )
  }

  if (showSaveDialog) {
    AlertDialog(
        onDismissRequest = { showSaveDialog = false },
        title = {
          Text(
              text = context.getString(R.string.save_to_gallery_first),
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag(SaveToGalleryScreenTestTags.SAVE_TEXT_FIRST),
          )
        },
        text = {
          Text(
              text = context.getString(R.string.save_to_gallery_second),
              modifier = Modifier.testTag(SaveToGalleryScreenTestTags.SAVE_TEXT_SECOND),
          )
        },
        confirmButton = {
          TextButton(
              onClick = {
                showSaveDialog = false
                onSave()
              },
              modifier = Modifier.testTag(SaveToGalleryScreenTestTags.SAVE_TEXT_THIRD),
          ) {
            Text(text = context.getString(R.string.save_to_gallery_third))
          }
        },
        dismissButton = {
          TextButton(
              onClick = { showSaveDialog = false },
              modifier = Modifier.testTag(SaveToGalleryScreenTestTags.SAVE_CANCEL),
          ) {
            Text(text = context.getString(R.string.cancel))
          }
        },
    )
  }

  if (showDiscardDialog) {
    AlertDialog(
        onDismissRequest = { showDiscardDialog = false },
        title = {
          Text(
              text = context.getString(R.string.discard_picture_first),
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag(SaveToGalleryScreenTestTags.DISCARD_TEXT_FIRST),
          )
        },
        text = {
          Text(
              text = context.getString(R.string.discard_picture_second),
              modifier = Modifier.testTag(SaveToGalleryScreenTestTags.DISCARD_TEXT_SECOND),
          )
        },
        confirmButton = {
          TextButton(
              onClick = {
                showDiscardDialog = false
                onDiscard()
              },
              modifier = Modifier.testTag(SaveToGalleryScreenTestTags.DISCARD_TEXT_THIRD),
          ) {
            Text(context.getString(R.string.discard_picture_third))
          }
        },
        dismissButton = {
          TextButton(
              onClick = { showDiscardDialog = false },
              modifier = Modifier.testTag(SaveToGalleryScreenTestTags.DISCARD_CANCEL),
          ) {
            Text(context.getString(R.string.cancel))
          }
        },
    )
  }
}

@Composable
private fun DiscardButton(onClick: () -> Unit, modifier: Modifier) {
  IconButton(
      onClick = onClick,
      modifier =
          modifier
              .size(30.dp)
              .background(colorScheme.surfaceVariant, CircleShape)
              .border(2.dp, colorScheme.surface, CircleShape),
  ) {
    Icon(
        imageVector = Icons.Outlined.Close,
        contentDescription = "Discard photo",
        tint = colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxSize(0.6f),
    )
  }
}

@Composable
private fun SaveButton(onClick: () -> Unit, modifier: Modifier) {
  IconButton(
      onClick = onClick,
      modifier =
          modifier
              .size(80.dp)
              .background(colorScheme.primary, CircleShape)
              .border(2.dp, colorScheme.surface, CircleShape),
  ) {
    Icon(
        imageVector = Icons.Default.Download,
        contentDescription = "Save photo",
        tint = colorScheme.onPrimary,
        modifier = Modifier.fillMaxSize(0.6f),
    )
  }
}
