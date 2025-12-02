package com.android.wildex.ui.notification

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.R
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.utils.ClickableProfilePicture

object NotificationScreenTestTags {
  const val GO_BACK = "notification_screen_go_back"
  const val NO_NOTIFICATION_TEXT = "no_notification_text"

  fun testTagForNotification(notificationId: Id): String =
      "NotificationScreen_notification_$notificationId"

  fun testTagForProfilePicture(profileId: String): String {
    return "ProfilePicture_$profileId"
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onGoBack: () -> Unit = {},
    notificationScreenViewModel: NotificationScreenViewModel = viewModel(),
    onProfileClick: (Id) -> Unit = {},
    onNotificationClick: (String) -> Unit = {},
) {
  val uiState by notificationScreenViewModel.uiState.collectAsState()
  val context = LocalContext.current
  LaunchedEffect(Unit) { notificationScreenViewModel.loadUIState() }
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      notificationScreenViewModel.clearErrorMsg()
    }
  }
  Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        NotificationTopBar(onGoBack = onGoBack, goBackTag = NotificationScreenTestTags.GO_BACK)
      },
  ) { pd ->
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { notificationScreenViewModel.refreshUIState() },
        modifier = Modifier.padding(pd),
    ) {
      when {
        uiState.isError -> LoadingFail()
        uiState.isLoading -> LoadingScreen()
        uiState.notifications.isEmpty() -> NoNotificationView()
        else -> {
          NotificationView(
              notifications = uiState.notifications,
              onProfileClick = onProfileClick,
              onNotificationClick = onNotificationClick,
              markAsRead = { notificationScreenViewModel.markAsRead(it) },
              markAllAsRead = { notificationScreenViewModel.markAllAsRead() },
              clearNotification = { notificationScreenViewModel.clearNotification(it) },
              clearAllNotifications = { notificationScreenViewModel.clearAllNotifications() },
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationView(
    notifications: List<NotificationUIState> = emptyList(),
    onProfileClick: (Id) -> Unit = {},
    onNotificationClick: (String) -> Unit = {},
    markAsRead: (Id) -> Unit = {},
    markAllAsRead: () -> Unit = {},
    clearNotification: (Id) -> Unit = {},
    clearAllNotifications: () -> Unit = {},
) {
  Row(modifier = Modifier.fillMaxWidth()) {
    TextButton(onClick = { markAllAsRead() }, content = { Text(text = "Mark all as read") })
    TextButton(onClick = { clearAllNotifications() }, content = { Text(text = "Clear all") })
  }
  LazyColumn(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    items(notifications) { notification ->
      NotificationItem(
          notificationContentId = notification.notificationId,
          simpleUser = notification.author,
          notificationRoute = notification.notificationRoute,
          notificationTitle = notification.notificationTitle,
          notificationDescription = notification.notificationDescription,
          onNotificationClick = onNotificationClick,
          onProfileClick = onProfileClick,
          markAsRead = markAsRead,
          clearNotification = clearNotification,
      )
      HorizontalDivider(
          color = colorScheme.onSurface.copy(alpha = .6f),
          modifier = Modifier.fillMaxWidth(.95f),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationItem(
    notificationContentId: Id = "",
    notificationRoute: String = "",
    simpleUser: SimpleUser,
    notificationTitle: String = "DEFAULT TITLE",
    notificationDescription: String = "DEFAULT DESCRIPTION",
    onProfileClick: (Id) -> Unit = {},
    onNotificationClick: (String) -> Unit = {},
    markAsRead: (Id) -> Unit = {},
    clearNotification: (Id) -> Unit = {},
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    ClickableProfilePicture(
        modifier =
            Modifier.size(48.dp)
                .testTag(NotificationScreenTestTags.testTagForProfilePicture(simpleUser.userId)),
        profileId = simpleUser.userId,
        profilePictureURL = simpleUser.profilePictureURL,
        profileUserType = simpleUser.userType,
        onProfile = onProfileClick,
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
          text = notificationTitle,
          color = colorScheme.onBackground,
          style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
      Text(
          text = notificationDescription,
          color = colorScheme.onBackground,
          style = typography.bodyMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
    }
    IconButton(
        onClick = { onNotificationClick(notificationRoute) },
        modifier =
            Modifier.size(48.dp)
                .testTag(NotificationScreenTestTags.testTagForNotification(notificationContentId)),
    ) {
      Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = "Open notification",
          tint = colorScheme.onBackground,
      )
    }
  }
}

@Composable
fun NoNotificationView() {
  Column(
      modifier =
          Modifier.fillMaxSize()
              .padding(24.dp)
              .testTag(NotificationScreenTestTags.NO_NOTIFICATION_TEXT),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Icon(
        painter = painterResource(R.drawable.nothing_found),
        contentDescription = "Nothing Found",
        tint = colorScheme.primary,
        modifier = Modifier.size(96.dp),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = LocalContext.current.getString(R.string.no_notifications),
        color = colorScheme.primary,
        style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
  }
}
