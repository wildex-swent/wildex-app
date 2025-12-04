package com.android.wildex.model.notification

import com.android.wildex.model.utils.Id

/** Represents a repository that manages Notification items. */
interface NotificationRepository {

  /** Retrieves all Notification items from the repository for a given user. */
  suspend fun getAllNotificationsForUser(userId: Id): List<Notification>

  /** Marks a Notification item as read. */
  suspend fun markNotificationAsRead(notificationId: Id)

  /** Marks all Notification items for a given user as read. */
  suspend fun markAllNotificationsForUserAsRead(userId: Id)

  /** Deletes a Notification item from the repository. */
  suspend fun deleteNotification(notificationId: Id)

  /** Deletes all Notification items made for a specific user. */
  suspend fun deleteAllNotificationsForUser(userId: Id)

  /** Deletes all Notification items made by a specific user. */
  suspend fun deleteAllNotificationsByUser(userId: Id)
}
