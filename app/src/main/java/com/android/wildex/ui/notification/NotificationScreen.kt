package com.android.wildex.ui.notification

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.unit.sp
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
    onNotificationClick: (Id, NotificationType) -> Unit = { _, _ -> },
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
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        state = pullState,
        isRefreshing = uiState.isRefreshing,
        onRefresh = { notificationScreenViewModel.refreshUIState() },
    ) {
      when {
        uiState.isError -> LoadingFail()
        uiState.isLoading -> LoadingScreen()
        uiState.notifications.isEmpty() -> NoNotificationView()
        else -> {
          NotificationView(
              notifications = uiState.notifications,
              pd = pd,
              onNotificationClick = onNotificationClick,
              onProfileClick = onProfileClick,
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
    pd: PaddingValues,
    onProfileClick: (Id) -> Unit = {},
    onNotificationClick: (Id, NotificationType) -> Unit = { _, _ -> },
) {
  val cs = colorScheme
  LazyColumn(
      modifier = Modifier.fillMaxWidth().padding(pd).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
  ) {
    items(notifications.size) { index ->
      val notification = notifications[index]
      NotificationItem(
          notificationContentId = notification.notificationId,
          simpleUser = notification.simpleUser,
          notificationType = notification.notificationType,
          notificationTitle = notification.notificationTitle,
          notificationDescription = notification.notificationDescription,
          onNotificationClick = onNotificationClick,
          onProfileClick = onProfileClick,
          cs = cs,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationItem(
    notificationContentId: Id = "",
    notificationType: NotificationType = NotificationType.LIKE,
    simpleUser: SimpleUser,
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
          ClickableProfilePicture(
              modifier =
                  Modifier.size(48.dp)
                      .testTag(
                          NotificationScreenTestTags.testTagForProfilePicture(simpleUser.userId)),
              profileId = simpleUser.userId,
              profilePictureURL = simpleUser.profilePictureURL,
              profileUserType = simpleUser.userType,
              onProfile = onProfileClick,
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
              modifier =
                  Modifier.size(48.dp)
                      .testTag(
                          NotificationScreenTestTags.testTagForNotification(
                              notificationContentId))) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open notification",
                    tint = cs.onBackground,
                    modifier = Modifier.size(20.dp))
              }
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
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
  }
}
