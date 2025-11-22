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

/**
 * A bottom sheet that provides navigation options for a given location.
 *
 * @param latitude The latitude of the location.
 * @param longitude The longitude of the location.
 * @param displayLabel An optional label to display for the location. If null, the coordinates will
 *   be used.
 * @param onDismissRequest A callback invoked when the bottom sheet is dismissed.
 */
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
  val secondaryText = onBackground.copy(alpha = 0.7f)
  val outline = MaterialTheme.colorScheme.outline
  val primary = MaterialTheme.colorScheme.primary

  ModalBottomSheet(
      onDismissRequest = onDismissRequest,
      dragHandle = { BottomSheetDefaults.DragHandle() },
      modifier = Modifier.testTag(NavigationSheetTestTags.SHEET),
      contentColor = MaterialTheme.colorScheme.background,
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

/**
 * Opens the specified latitude and longitude in the Google Maps app for navigation. If the Google
 * Maps app is not installed, a toast message is shown to inform the user.
 *
 * @param context The context used to start the activity and show toast messages.
 * @param latitude The latitude of the location to navigate to.
 * @param longitude The longitude of the location to navigate to.
 */
private fun openInGoogleMapsApp(context: Context, latitude: Double, longitude: Double) {
  val uri = "google.navigation:q=$latitude,$longitude&mode=d".toUri()
  val intent =
      Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage(context.getString(R.string.report_details_maps_package))
      }
  try {
    if (intent.resolveActivity(context.packageManager) != null) {
      context.startActivity(intent)
    } else {
      Toast.makeText(
              context,
              context.getString(R.string.report_details_maps_not_installed),
              Toast.LENGTH_SHORT,
          )
          .show()
    }
  } catch (_: ActivityNotFoundException) {
    Toast.makeText(
            context,
            context.getString(R.string.report_details_maps_not_available),
            Toast.LENGTH_SHORT,
        )
        .show()
  }
}

/**
 * Generates a shareable text for the given location.
 *
 * @param context The context used to access string resources.
 * @param latitude The latitude of the location.
 * @param longitude The longitude of the location.
 * @param label A label for the location.
 * @return A formatted string containing the location details and a Google Maps link.
 */
private fun getTextForLocation(
    context: Context,
    latitude: Double,
    longitude: Double,
    label: String,
): String {
  val url = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
  return context.getString(
      R.string.report_details_share_body,
      label,
      latitude.toString(),
      longitude.toString(),
      url,
  )
}

/**
 * Copies the location details to the clipboard and shows a toast message.
 *
 * @param context The context used to access the clipboard service and show toast messages.
 * @param latitude The latitude of the location.
 * @param longitude The longitude of the location.
 * @param label A label for the location.
 */
private fun copyLocationToClipboard(
    context: Context,
    latitude: Double,
    longitude: Double,
    label: String,
) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  val copiedText = getTextForLocation(context, latitude, longitude, label)
  val clip =
      ClipData.newPlainText(context.getString(R.string.report_details_location_report), copiedText)
  clipboard.setPrimaryClip(clip)
  Toast.makeText(
          context,
          context.getString(R.string.report_details_location_copied),
          Toast.LENGTH_SHORT,
      )
      .show()
}

/**
 * Shares the location details using an implicit intent.
 *
 * @param context The context used to start the share activity and show toast messages.
 * @param latitude The latitude of the location.
 * @param longitude The longitude of the location.
 * @param label A label for the location.
 */
private fun shareLocation(
    context: Context,
    latitude: Double,
    longitude: Double,
    label: String,
) {
  val body = getTextForLocation(context, latitude, longitude, label)
  val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.report_details_location_report))
        putExtra(Intent.EXTRA_TEXT, body)
      }

  try {
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.report_details_share_location)))
  } catch (_: ActivityNotFoundException) {
    Toast.makeText(context, context.getString(R.string.report_details_no_apps), Toast.LENGTH_SHORT)
        .show()
  }
}
