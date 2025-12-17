package com.android.wildex.ui.notification

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.R
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.utils.ClickableProfilePicture
import com.android.wildex.ui.utils.offline.OfflineScreen
import com.android.wildex.ui.utils.refresh.WildexPullToRefreshIndicator

object NotificationScreenTestTags {
  const val NO_NOTIFICATION_TEXT = "no_notification_text"

  const val MARK_ALL_AS_READ_BUTTON = "mark_all_as_read_button"
  const val CLEAR_ALL_BUTTON = "delete_all_button"

  const val PULL_TO_REFRESH = "pull_to_refresh"
  const val BACK_BUTTON = "back_button"
  const val TOP_BAR = "top_bar"
  const val TOP_BAR_TITLE = "top_bar_title"
  const val NOTIFICATION_LIST = "notification_list"

  fun testTagForNotification(notificationId: Id): String = "notification_$notificationId"

  fun testTagForProfilePicture(notificationId: Id): String =
      "notification_profile_picture_$notificationId"

  fun testTagForNotificationDate(notificationId: Id): String = "notification_date_$notificationId"

  fun testTagForNotificationTitle(notificationId: Id): String = "notification_title_$notificationId"

  fun testTagForNotificationDescription(notificationId: Id): String =
      "notification_description_$notificationId"

  fun testTagForNotificationReadState(notificationId: Id): String =
      "notification_read_state_$notificationId"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onGoBack: () -> Unit = {},
    notificationScreenViewModel: NotificationScreenViewModel = viewModel(),
    onProfileClick: (Id) -> Unit = {},
    onNotificationClick: (String) -> Unit = {},
) {
  /**
   * Main notifications screen composable.
   *
   * Shows loading/error/offline states and the list of notifications when online.
   *
   * @param onGoBack Navigate back callback.
   * @param notificationScreenViewModel ViewModel that provides screen state.
   * @param onProfileClick Called when a profile picture is clicked.
   * @param onNotificationClick Called when a notification is tapped (provides route).
   */
  val context = LocalContext.current
  val uiState by notificationScreenViewModel.uiState.collectAsState()
  val connectivityObserver = LocalConnectivityObserver.current
  val isOnline by connectivityObserver.isOnline.collectAsState()

  LaunchedEffect(Unit) { notificationScreenViewModel.loadUIState() }
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      notificationScreenViewModel.clearErrorMsg()
    }
  }
  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(NavigationTestTags.NOTIFICATION_SCREEN),
      topBar = { NotificationTopBar(onGoBack = onGoBack) },
  ) { pd ->
    val pullState = rememberPullToRefreshState()

    PullToRefreshBox(
        state = pullState,
        isRefreshing = uiState.isRefreshing,
        onRefresh = {
          if (isOnline) notificationScreenViewModel.refreshUIState()
          else notificationScreenViewModel.refreshOffline()
        },
        modifier = Modifier.padding(pd).testTag(NotificationScreenTestTags.PULL_TO_REFRESH),
        indicator = { WildexPullToRefreshIndicator(pullState, uiState.isRefreshing) },
    ) {
      if (isOnline) {
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
      } else {
        OfflineScreen(innerPadding = pd)
      }
    }
  }
}

/**
 * Renders the list of notifications with actions row and swipe-to-delete support.
 *
 * @param notifications List of notifications to display.
 * @param onProfileClick Callback when profile image is clicked.
 * @param onNotificationClick Callback when a notification is clicked (route).
 * @param markAsRead Callback to mark a single notification as read.
 * @param markAllAsRead Callback to mark all notifications as read.
 * @param clearNotification Callback to delete a single notification.
 * @param clearAllNotifications Callback to delete all notifications.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationView(
    notifications: List<NotificationUIState>,
    onProfileClick: (Id) -> Unit,
    onNotificationClick: (String) -> Unit,
    markAsRead: (Id) -> Unit,
    markAllAsRead: () -> Unit,
    clearNotification: (Id) -> Unit,
    clearAllNotifications: () -> Unit,
) {

  LazyColumn(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 4.dp)
              .testTag(NotificationScreenTestTags.NOTIFICATION_LIST),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item {
      ActionsRow(onMarkAllRead = markAllAsRead, onDeleteAll = clearAllNotifications)
      Spacer(Modifier.height(16.dp))
      HorizontalDivider(
          color = colorScheme.onSurface.copy(alpha = .3f),
          modifier = Modifier.fillMaxWidth(),
      )
    }
    items(notifications, key = { it.notificationId }) {
      SwipeToDeleteNotification(
          itemId = it.notificationId,
          onDelete = { clearNotification(it.notificationId) },
          modifier =
              Modifier.clickable {
                    markAsRead(it.notificationId)
                    onNotificationClick(it.notificationRoute)
                  }
                  .testTag(NotificationScreenTestTags.testTagForNotification(it.notificationId)),
      ) {
        Column {
          NotificationItem(
              simpleUser = it.author,
              notificationId = it.notificationId,
              notificationTitle = it.notificationTitle,
              notificationDescription = it.notificationDescription,
              notificationRelativeTime = it.notificationRelativeTime,
              notificationReadState = it.notificationReadState,
              onProfileClick = onProfileClick,
          )

          HorizontalDivider(
              color = colorScheme.onSurface.copy(alpha = .1f),
              modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}

/**
 * Renders a single notification row with author picture, title, description and timestamp.
 *
 * @param simpleUser The author information to display.
 * @param notificationId Unique id of the notification.
 * @param notificationTitle Title text.
 * @param notificationDescription Description text.
 * @param notificationRelativeTime Human readable time string.
 * @param notificationReadState True if notification was already read.
 * @param onProfileClick Callback when the author's profile picture is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationItem(
    simpleUser: SimpleUser,
    notificationId: Id,
    notificationTitle: String,
    notificationDescription: String,
    notificationRelativeTime: String,
    notificationReadState: Boolean,
    onProfileClick: (Id) -> Unit,
) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    // Apply weight to BadgedBox, not the inner content
    BadgedBox(
        modifier = Modifier.weight(.2f).aspectRatio(1f, matchHeightConstraintsFirst = true),
        badge = {
          if (!notificationReadState) {
            Badge(
                modifier =
                    Modifier.offset(x = 1.dp, y = (-1).dp)
                        .size(10.dp)
                        .testTag(
                            NotificationScreenTestTags.testTagForNotificationReadState(
                                notificationId)),
                containerColor = colorScheme.error.copy(.8f),
            )
          }
        },
    ) {
      ClickableProfilePicture(
          modifier =
              Modifier.fillMaxSize()
                  .testTag(NotificationScreenTestTags.testTagForProfilePicture(notificationId)),
          profileId = simpleUser.userId,
          profilePictureURL = simpleUser.profilePictureURL,
          profileUserType = simpleUser.userType,
          onProfile = onProfileClick,
      )
    }

    Spacer(modifier = Modifier.width(10.dp))
    Column(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
          text = notificationRelativeTime,
          color = colorScheme.onBackground.copy(alpha = .6f),
          style = typography.bodySmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.End,
          modifier =
              Modifier.fillMaxWidth()
                  .testTag(NotificationScreenTestTags.testTagForNotificationDate(notificationId)),
      )
      Spacer(modifier = Modifier.height(4.dp))
      Column(verticalArrangement = Arrangement.Center) {
        Text(
            text = notificationTitle,
            color = colorScheme.onBackground,
            style = typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier.testTag(
                    NotificationScreenTestTags.testTagForNotificationTitle(notificationId)),
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (notificationDescription.isNotEmpty()) {
          Text(
              text = notificationDescription,
              color = colorScheme.onBackground,
              style = typography.bodyMedium,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier =
                  Modifier.testTag(
                      NotificationScreenTestTags.testTagForNotificationDescription(notificationId)),
          )
        }
      }
    }
  }
}

/** Shown when the user has no notifications. */
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
        text = LocalContext.current.getString(R.string.notifications_no_notifications),
        color = colorScheme.primary,
        style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
  }
}
