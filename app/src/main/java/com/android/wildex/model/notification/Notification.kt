package com.android.wildex.model.notification

import android.app.NotificationManager
import com.android.wildex.model.utils.Id
import com.google.firebase.Timestamp

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

enum class NotificationGroupType(val groupId: Id, val groupName: String) {
  SOCIAL("social_group", "Social"),
  REPORT("report_group", "Report"),
}
