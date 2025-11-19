package com.android.wildex.ui.notification

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL

object NotificationScreenTestTags {
  const val GO_BACK = "notification_screen_go_back"

  fun testTagForNotification(notificationId: Id, element: String): String =
      "NotificationScreen_notification_${notificationId}_$element"

  fun testTagForProfilePicture(profileId: String, role: String = ""): String {
    return if (role.isEmpty()) "ProfilePicture_$profileId" else "ProfilePicture_${role}_$profileId"
  }
}

@Composable
fun NotificationScreen(
    onGoBack: () -> Unit = {},
    notificationScreenViewModel: NotificationScreenViewModel = viewModel(),
) {
  val uiState by notificationScreenViewModel.uiState.collectAsState()
  val context = LocalContext.current
  LaunchedEffect(Unit) { notificationScreenViewModel.loadUIState() }
  Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = { NotificationTopBar(onGoBack = onGoBack) },
  ) { pd ->
    NotificationView(notifications = uiState.notifications, pd = pd)
  }
}

@Composable
fun NotificationView(notifications: List<NotificationUIState> = emptyList(), pd: PaddingValues) {
  val cs = MaterialTheme.colorScheme
  LazyColumn(
      modifier = Modifier.fillMaxWidth().padding(pd).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
  ) {
    items(notifications.size) { index ->
      val notification = notifications[index]
      NotificationItem(
          profilePictureUrl = notification.profilePictureUrl,
          notificationType = notification.notificationType,
          notificationTitle = notification.notificationTitle,
          notificationDescription = notification.notificationDescription,
          cs = cs,
      )
    }
  }
}

@Composable
fun NotificationItem(
    authorId: Id = "",
    notificationContentId: Id = "",
    notificationType: NotificationType = NotificationType.LIKE,
    profilePictureUrl: URL = LocalContext.current.getString(R.string.default_profile_picture_link),
    notificationTitle: String = "DEFAULT TITLE",
    notificationDescription: String = "DEFAULT DESCRIPTION",
    onProfileClick: (Id) -> Unit = {},
    onNotificationClick: (Id, NotificationType) -> Unit = { _, _ -> },
    cs: ColorScheme,
) {
  Box(
      modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
          AsyncImage(
              model = profilePictureUrl,
              contentDescription = "Profile picture",
              modifier =
                  Modifier.width(48.dp)
                      .aspectRatio(1f)
                      .clip(CircleShape)
                      .border(1.dp, cs.outline, CircleShape)
                      .clickable(onClick = { onProfileClick(authorId) }),
              contentScale = ContentScale.Crop,
          )
          Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = notificationTitle,
                color = cs.onBackground,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = notificationDescription,
                color = cs.onBackground,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
          }
          IconButton(
              onClick = { onNotificationClick(notificationContentId, notificationType) },
              modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open notification",
                    tint = cs.onBackground,
                    modifier = Modifier.size(20.dp))
              }
        }
  }
}
