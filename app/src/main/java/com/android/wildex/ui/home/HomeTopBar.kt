package com.android.wildex.ui.home

/**
 * WildexHomeTopAppBar.kt
 *
 * Defines the top app bar for the Wildex home screen. Displays the app title, a notification icon,
 * and the user's profile picture. Provides callbacks for notification and profile actions.
 */
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.home.HomeScreenTestTags.NOTIFICATION_BELL
import com.android.wildex.ui.home.HomeScreenTestTags.PROFILE_PICTURE

/**
 * Composable that renders the top app bar in the Wildex home screen.
 *
 * @param user The currently logged-in user, used to display their profile picture.
 * @param onNotificationClick Callback invoked when the notification icon is pressed.
 * @param onProfilePictureClick Callback invoked when the profile picture is pressed, passing the
 *   user's ID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    user: SimpleUser,
    onNotificationClick: () -> Unit,
    onProfilePictureClick: (userId: Id) -> Unit,
) {
  TopAppBar(
      title = {
        Box(
            modifier = Modifier.fillMaxWidth().testTag(HomeScreenTestTags.TITLE),
            contentAlignment = Alignment.Center,
        ) {
          Text(text = "Wildex", style = MaterialTheme.typography.titleLarge)
        }
      },
      navigationIcon = {
        IconButton(
            onClick = { onNotificationClick() },
            modifier = Modifier.testTag(NOTIFICATION_BELL),
        ) {
          Icon(
              painter = painterResource(R.drawable.notification_bell),
              contentDescription = "Notifications",
              modifier = Modifier.size(30.dp),
          )
        }
      },
      actions = {
        IconButton(
            onClick = { onProfilePictureClick(user.userId) },
            modifier = Modifier.testTag(PROFILE_PICTURE),
        ) {
          AsyncImage(
              model = user.profilePictureURL,
              contentDescription = "Profile picture",
              modifier =
                  Modifier.size(40.dp)
                      .clip(CircleShape)
                      .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
              contentScale = ContentScale.Crop,
          )
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              titleContentColor = MaterialTheme.colorScheme.primary,
              navigationIconContentColor = MaterialTheme.colorScheme.primary,
          ),
  )
}
