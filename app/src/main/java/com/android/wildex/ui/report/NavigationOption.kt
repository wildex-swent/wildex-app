package com.android.wildex.ui.report

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.android.wildex.R

object NavigationSheetTestTags {
  const val SHEET = "navigation_sheet"
  const val TITLE = "navigation_sheet_title"
  const val LOCATION = "navigation_sheet_location"
  const val BTN_GOOGLE_MAPS = "navigation_sheet_google_maps"
  const val BTN_COPY = "navigation_sheet_copy"
  const val BTN_SHARE = "navigation_sheet_share"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationOptionsBottomSheet(
    latitude: Double,
    longitude: Double,
    displayLabel: String? = null,
    onDismissRequest: () -> Unit,
) {
  val context = LocalContext.current
  val label = displayLabel ?: "%.5f, %.5f".format(latitude, longitude)

  val onBackground = MaterialTheme.colorScheme.onBackground
  val secondaryText = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
  val outline = MaterialTheme.colorScheme.outline
  val primary = MaterialTheme.colorScheme.primary

  ModalBottomSheet(
      onDismissRequest = onDismissRequest,
      dragHandle = { BottomSheetDefaults.DragHandle() },
      modifier = Modifier.testTag(NavigationSheetTestTags.SHEET),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
          text = context.getString(R.string.report_details_navigate),
          fontWeight = FontWeight.SemiBold,
          color = onBackground,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.testTag(NavigationSheetTestTags.TITLE),
      )

      Text(
          text = label,
          style = MaterialTheme.typography.bodyMedium,
          color = secondaryText,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag(NavigationSheetTestTags.LOCATION),
      )

      // Google Maps
      OutlinedButton(
          onClick = {
            openInGoogleMapsApp(context, latitude, longitude)
            onDismissRequest()
          },
          modifier = Modifier.fillMaxWidth().testTag(NavigationSheetTestTags.BTN_GOOGLE_MAPS),
          shape = RoundedCornerShape(28.dp),
          border = BorderStroke(1.dp, outline),
          colors =
              ButtonDefaults.outlinedButtonColors(
                  contentColor = onBackground,
              ),
      ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
              imageVector = Icons.Filled.Map,
              contentDescription = null,
              tint = primary,
          )
          Text(context.getString(R.string.report_details_google_maps))
        }
      }

      // Copy
      OutlinedButton(
          onClick = {
            copyLocationToClipboard(context, latitude, longitude, label)
            onDismissRequest()
          },
          modifier = Modifier.fillMaxWidth().testTag(NavigationSheetTestTags.BTN_COPY),
          shape = RoundedCornerShape(28.dp),
          border = BorderStroke(1.dp, outline),
          colors =
              ButtonDefaults.outlinedButtonColors(
                  contentColor = onBackground,
              ),
      ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
              imageVector = Icons.Filled.ContentCopy,
              contentDescription = null,
              tint = onBackground,
          )
          Text(context.getString(R.string.report_details_copy))
        }
      }

      // Share
      OutlinedButton(
          onClick = {
            shareLocation(context, latitude, longitude, label)
            onDismissRequest()
          },
          modifier = Modifier.fillMaxWidth().testTag(NavigationSheetTestTags.BTN_SHARE),
          shape = RoundedCornerShape(28.dp),
          border = BorderStroke(1.dp, outline),
          colors =
              ButtonDefaults.outlinedButtonColors(
                  contentColor = onBackground,
              ),
      ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
              imageVector = Icons.Filled.Share,
              contentDescription = null,
              tint = onBackground,
          )
          Text(context.getString(R.string.report_details_share))
        }
      }

      Spacer(Modifier.height(12.dp))
    }
  }
}

private fun openInGoogleMapsApp(context: Context, latitude: Double, longitude: Double) {
  val uri = "google.navigation:q=$latitude,$longitude&mode=d".toUri()
  val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
  try {
    if (intent.resolveActivity(context.packageManager) != null) {
      context.startActivity(intent)
    } else {
      Toast.makeText(context, "Google Maps is not installed.", Toast.LENGTH_SHORT).show()
    }
  } catch (_: ActivityNotFoundException) {
    Toast.makeText(context, "Google Maps is not available.", Toast.LENGTH_SHORT).show()
  }
}

private fun copyLocationToClipboard(
    context: Context,
    latitude: Double,
    longitude: Double,
    label: String,
) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  val clip = ClipData.newPlainText("Wildex report location", "$label\n($latitude, $longitude)")
  clipboard.setPrimaryClip(clip)
  Toast.makeText(context, "Location copied.", Toast.LENGTH_SHORT).show()
}

private fun shareLocation(
    context: Context,
    latitude: Double,
    longitude: Double,
    label: String,
) {
  val url = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
  val body = "$label\n($latitude, $longitude)\n\n$url"

  val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Wildex report location")
        putExtra(Intent.EXTRA_TEXT, body)
      }

  try {
    context.startActivity(Intent.createChooser(intent, "Share location"))
  } catch (_: ActivityNotFoundException) {
    Toast.makeText(context, "No apps available.", Toast.LENGTH_SHORT).show()
  }
}
