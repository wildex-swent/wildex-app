package com.android.wildex.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.ui.utils.ClickableProfilePicture

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopLevelTopBar(
    user: SimpleUser,
    title: String,
    onNotificationClick: () -> Unit = {},
    onProfilePictureClick: () -> Unit = {}
) {
  TopAppBar(
      title = {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
          Text(
              text = title,
              style = typography.titleLarge,
              modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
        }
      },
      navigationIcon = {
        IconButton(
            onClick = { onNotificationClick() },
            modifier = Modifier.testTag(NavigationTestTags.NOTIFICATION_BELL),
        ) {
          Icon(
              imageVector = Icons.Outlined.Notifications,
              contentDescription = "Notifications",
              tint = colorScheme.onBackground,
              modifier = Modifier.size(30.dp))
        }
      },
      actions = {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
          ClickableProfilePicture(
              modifier = Modifier.size(40.dp).testTag(NavigationTestTags.TOP_BAR_PROFILE_PICTURE),
              profileId = user.userId,
              profilePictureURL = user.profilePictureURL,
              profileUserType = user.userType,
              onProfile = { id -> onProfilePictureClick() },
          )
        }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              titleContentColor = colorScheme.onBackground,
              navigationIconContentColor = colorScheme.onBackground,
          ),
  )
}
