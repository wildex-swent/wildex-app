package com.android.wildex.model.notification

import com.android.wildex.model.utils.Id
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NotificationRepositoryFirestore(db: FirebaseFirestore) : NotificationRepository {
  companion object {
    private const val NOTIFICATIONS_COLLECTION_PATH = "notifications"
    private const val TARGET_ID = "targetId"
    private const val AUTHOR_ID = "authorId"
    private const val TITLE = "title"
    private const val BODY = "body"
    private const val ROUTE = "route"
    private const val READ = "read"
    private const val DATE = "date"
  }

  private val collection = db.collection(NOTIFICATIONS_COLLECTION_PATH)

  override suspend fun getAllNotificationsForUser(userId: Id): List<Notification> {
    return collection.whereEqualTo(TARGET_ID, userId).get().await().documents.map {
      documentToNotification(it)
    }
  }

  override suspend fun markNotificationAsRead(notificationId: Id) {
    val docRef = collection.document(notificationId)
    require(docRef.get().await().exists()) {
      "A notification with id '$notificationId' does not exist."
    }
    docRef.update(READ, true).await()
  }

  override suspend fun markAllNotificationsForUserAsRead(userId: Id) {
    collection.whereEqualTo(TARGET_ID, userId).get().await().documents.forEach {
      it.reference.update(READ, true).await()
    }
  }

  override suspend fun deleteNotification(notificationId: Id) {
    val docRef = collection.document(notificationId)
    require(docRef.get().await().exists()) {
      "A notification with id '$notificationId' does not exist."
    }
    docRef.delete().await()
  }

  override suspend fun deleteAllNotificationsForUser(userId: Id) {
    collection.whereEqualTo(TARGET_ID, userId).get().await().documents.forEach {
      it.reference.delete().await()
    }
  }

  override suspend fun deleteAllNotificationsByUser(userId: Id) {
    collection.whereEqualTo(AUTHOR_ID, userId).get().await().documents.forEach {
      it.reference.delete().await()
    }
  }

  private fun documentToNotification(document: DocumentSnapshot): Notification {
    val notificationId = document.id
    val targetId = document.getString(TARGET_ID) ?: throwMissingFieldException(TARGET_ID)
    val authorId = document.getString(AUTHOR_ID) ?: throwMissingFieldException(AUTHOR_ID)
    val title = document.getString(TITLE) ?: throwMissingFieldException(TITLE)
    val body = document.getString(BODY) ?: throwMissingFieldException(BODY)
    val route = document.getString(ROUTE) ?: throwMissingFieldException(ROUTE)
    val isRead = document.getBoolean(READ) ?: throwMissingFieldException(READ)
    val date = document.getTimestamp(DATE) ?: throwMissingFieldException(DATE)

    return Notification(notificationId, targetId, authorId, isRead, title, body, route, date)
  }

  private fun throwMissingFieldException(field: String): Nothing {
    throw IllegalArgumentException("Missing required field in Notification: $field")
  }
}
