package com.android.wildex.model.notification

import android.app.NotificationManager
import com.android.wildex.model.utils.Id
import com.google.firebase.Timestamp

/**
 * Data class representing a notification.
 *
 * @property notificationId The unique identifier for the notification.
 * @property targetId The ID of the target user related to the notification.
 * @property authorId The ID of the user who triggered the notification.
 * @property read Indicates whether the notification has been read.
 * @property title The title of the notification.
 * @property body The body content of the notification.
 * @property route The navigation route associated with the notification.
 * @property date The timestamp when the notification was created.
 */
data class Notification(
    val notificationId: Id,
    val targetId: Id,
    val authorId: Id,
    val read: Boolean,
    val title: String,
    val body: String,
    val route: String,
    val date: Timestamp,
)

/**
 * Enum class representing different types of notification channels.
 *
 * @property channelId The unique identifier for the notification channel.
 * @property channelName The name of the notification channel.
 * @property channelDesc The description of the notification channel.
 * @property importance The importance level of the notification channel.
 * @property group The group to which the notification channel belongs.
 */
enum class NotificationChannelType(
    val channelId: Id,
    val channelName: String,
    val channelDesc: String,
    val importance: Int,
    val group: NotificationGroupType,
) {
  POSTS(
      "post_channel",
      "Posts",
      "Post notifications",
      NotificationManager.IMPORTANCE_DEFAULT,
      NotificationGroupType.SOCIAL,
  ),
  LIKES(
      "like_channel",
      "Likes",
      "Like notifications",
      NotificationManager.IMPORTANCE_DEFAULT,
      NotificationGroupType.SOCIAL,
  ),
  COMMENTS(
      "comment_channel",
      "Comments",
      "Comment notifications",
      NotificationManager.IMPORTANCE_DEFAULT,
      NotificationGroupType.SOCIAL,
  ),
  FRIEND_REQUESTS(
      "friend_request_channel",
      "Friend Requests",
      "Friend request notifications",
      NotificationManager.IMPORTANCE_DEFAULT,
      NotificationGroupType.SOCIAL,
  ),
  REPORTS(
      "report_channel",
      "Reports",
      "Report notifications",
      NotificationManager.IMPORTANCE_HIGH,
      NotificationGroupType.REPORT,
  ),
}

/**
 * Enum class representing different groups of notification channels.
 *
 * @property groupId The unique identifier for the notification group.
 * @property groupName The name of the notification group.
 */
enum class NotificationGroupType(val groupId: Id, val groupName: String) {
  SOCIAL("social_group", "Social"),
  REPORT("report_group", "Report"),
}
