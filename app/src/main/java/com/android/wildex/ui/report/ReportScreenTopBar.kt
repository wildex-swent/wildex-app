package com.android.wildex.ui.report

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.wildex.R
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL

/**
 * A composable that displays the top bar of the ReportScreen.
 *
 * @param userId The ID of the user.
 * @param userType The type of the user.
 * @param userProfilePictureURL The URL of the user's profile picture.
 * @param onProfileClick The function to be called when the profile is clicked.
 * @param onNotificationClick The function to be called when the notification button is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreenTopBar(
    userId: Id = "",
    userType: UserType = UserType.REGULAR,
    userProfilePictureURL: URL = "",
    onProfileClick: (Id) -> Unit = {},
    onNotificationClick: () -> Unit
) {
  TopAppBar(
      title = {
        Text(
            modifier = Modifier.fillMaxWidth().testTag(ReportScreenTestTags.SCREEN_TITLE),
            text =
                when (userType) {
                  UserType.REGULAR -> LocalContext.current.getString(R.string.report_title_regular)
                  UserType.PROFESSIONAL ->
                      LocalContext.current.getString(R.string.report_title_professional)
                },
            textAlign = TextAlign.Center,
            color = colorScheme.primary,
            style = MaterialTheme.typography.titleLarge)
      },
      navigationIcon = {
        IconButton(
            modifier = Modifier.testTag(ReportScreenTestTags.NOTIFICATION_BUTTON),
            onClick = onNotificationClick) {
              Icon(
                  imageVector = Icons.Outlined.Notifications,
                  contentDescription = "Notifications",
                  tint = colorScheme.tertiary,
                  modifier = Modifier.size(30.dp))
            }
      },
      actions = {
        ClickableProfilePicture(
            profileId = userId,
            profilePictureURL = userProfilePictureURL,
            role = "user",
            onProfile = onProfileClick)
      })
}
