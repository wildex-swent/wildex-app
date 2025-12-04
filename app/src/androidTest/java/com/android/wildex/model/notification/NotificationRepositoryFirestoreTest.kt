package com.android.wildex.model.notification

import com.android.wildex.utils.FirebaseEmulator
import com.android.wildex.utils.FirestoreTest
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

const val NOTIFICATION_COLLECTION_PATH = "notifications"

class NotificationRepositoryFirestoreTest : FirestoreTest(NOTIFICATION_COLLECTION_PATH) {

  private val repository = NotificationRepositoryFirestore(FirebaseEmulator.firestore)
  val notification1 =
      Notification(
          notificationId = "notification1",
          targetId = "target1",
          authorId = "author1",
          isRead = false,
          title = "title1",
          body = "body1",
          route = "route1",
          date = Timestamp(1, 0),
      )

  val notification2 =
      Notification(
          notificationId = "notification2",
          targetId = "target2",
          authorId = "author2",
          isRead = false,
          title = "title2",
          body = "body2",
          route = "route2",
          date = Timestamp(2, 0),
      )

  val notification3 =
      Notification(
          notificationId = "notification3",
          targetId = "target1",
          authorId = "author3",
          isRead = true,
          title = "title3",
          body = "body3",
          route = "route3",
          date = Timestamp(3, 0),
      )

  @Before
  fun setup() {
    super.setUp()
    // Populate the repository with some test data
    runBlocking {
      FirebaseEmulator.firestore
          .collection(NOTIFICATION_COLLECTION_PATH)
          .document(notification1.notificationId)
          .set(notification1)
          .await()
      FirebaseEmulator.firestore
          .collection(NOTIFICATION_COLLECTION_PATH)
          .document(notification2.notificationId)
          .set(notification2)
          .await()
      FirebaseEmulator.firestore
          .collection(NOTIFICATION_COLLECTION_PATH)
          .document(notification3.notificationId)
          .set(notification3)
          .await()
    }
  }

  @Test
  fun testGetAllNotificationsForUser() = runTest {
    val notifications = repository.getAllNotificationsForUser("target1")
    assertEquals(listOf(notification1, notification3), notifications)
  }

  @Test(expected = IllegalArgumentException::class)
  fun testGetAllNotificationsForUser_MissingAuthorId() = runTest {
    FirebaseEmulator.firestore
        .collection(NOTIFICATION_COLLECTION_PATH)
        .document("notification4")
        .set(
            mapOf(
                "targetId" to "target4",
                "isRead" to true,
                "title" to "title4",
                "body" to "body4",
                "route" to "route4",
                "date" to Timestamp(4, 0),
            ))
        .await()
    repository.getAllNotificationsForUser("target4")
  }

  @Test(expected = IllegalArgumentException::class)
  fun testGetAllNotificationsForUser_MissingTitle() = runTest {
    FirebaseEmulator.firestore
        .collection(NOTIFICATION_COLLECTION_PATH)
        .document("notification4")
        .set(
            mapOf(
                "targetId" to "target4",
                "authorId" to "author4",
                "isRead" to true,
                "body" to "body4",
                "route" to "route4",
                "date" to Timestamp(4, 0),
            ))
        .await()
    repository.getAllNotificationsForUser("target4")
  }

  @Test(expected = IllegalArgumentException::class)
  fun testGetAllNotificationsForUser_MissingBody() = runTest {
    FirebaseEmulator.firestore
        .collection(NOTIFICATION_COLLECTION_PATH)
        .document("notification4")
        .set(
            mapOf(
                "targetId" to "target4",
                "authorId" to "author4",
                "isRead" to true,
                "title" to "title4",
                "route" to "route4",
                "date" to Timestamp(4, 0),
            ))
        .await()
    repository.getAllNotificationsForUser("target4")
  }

  @Test(expected = IllegalArgumentException::class)
  fun testGetAllNotificationsForUser_MissingRoute() = runTest {
    FirebaseEmulator.firestore
        .collection(NOTIFICATION_COLLECTION_PATH)
        .document("notification4")
        .set(
            mapOf(
                "targetId" to "target4",
                "authorId" to "author4",
                "isRead" to true,
                "title" to "title4",
                "body" to "body4",
                "date" to Timestamp(4, 0),
            ))
        .await()
    repository.getAllNotificationsForUser("target4")
  }

  @Test(expected = IllegalArgumentException::class)
  fun testGetAllNotificationsForUser_MissingIsRead() = runTest {
    FirebaseEmulator.firestore
        .collection(NOTIFICATION_COLLECTION_PATH)
        .document("notification4")
        .set(
            mapOf(
                "targetId" to "target4",
                "authorId" to "author4",
                "title" to "title4",
                "body" to "body4",
                "route" to "route4",
                "date" to Timestamp(4, 0),
            ))
        .await()
    repository.getAllNotificationsForUser("target4")
  }

  @Test(expected = IllegalArgumentException::class)
  fun testGetAllNotificationsForUser_MissingDate() = runTest {
    FirebaseEmulator.firestore
        .collection(NOTIFICATION_COLLECTION_PATH)
        .document("notification4")
        .set(
            mapOf(
                "targetId" to "target4",
                "authorId" to "author4",
                "isRead" to true,
                "title" to "title4",
                "body" to "body4",
                "route" to "route4",
            ))
        .await()
    repository.getAllNotificationsForUser("target4")
  }

  @Test
  fun testMarkNotificationAsRead() = runTest {
    repository.markNotificationAsRead("notification1")
    val updatedNotification =
        FirebaseEmulator.firestore
            .collection(NOTIFICATION_COLLECTION_PATH)
            .document("notification1")
            .get()
            .await()
            .toNotification()

    assert(updatedNotification.isRead)
  }

  @Test(expected = IllegalArgumentException::class)
  fun testMarkNotificationAsRead_NonExistentNotification() = runTest {
    repository.markNotificationAsRead("nonExistentNotification")
  }

  @Test
  fun testMarkAllNotificationsForUserAsRead() = runTest {
    repository.markAllNotificationsForUserAsRead("target1")
    val updatedNotification1 =
        FirebaseEmulator.firestore
            .collection(NOTIFICATION_COLLECTION_PATH)
            .document("notification1")
            .get()
            .await()
            .toNotification()
    val updatedNotification2 =
        FirebaseEmulator.firestore
            .collection(NOTIFICATION_COLLECTION_PATH)
            .document("notification2")
            .get()
            .await()
            .toNotification()
    val updatedNotification3 =
        FirebaseEmulator.firestore
            .collection(NOTIFICATION_COLLECTION_PATH)
            .document("notification3")
            .get()
            .await()
            .toNotification()

    assertTrue(updatedNotification1.isRead)
    assertFalse(updatedNotification2.isRead)
    assertTrue(updatedNotification3.isRead)
  }

  @Test
  fun testDeleteNotification() = runTest {
    repository.deleteNotification("notification2")
    assertFalse(
        FirebaseEmulator.firestore
            .collection(NOTIFICATION_COLLECTION_PATH)
            .document("notification2")
            .get()
            .await()
            .exists())
  }

  @Test(expected = IllegalArgumentException::class)
  fun testDeleteNonExistentNotification() = runTest {
    repository.deleteNotification("nonExistentNotification")
  }

  @Test
  fun testDeleteAllNotificationsForUser() = runTest {
    repository.deleteAllNotificationsForUser("target1")
    assertTrue(
        FirebaseEmulator.firestore
            .collection(NOTIFICATION_COLLECTION_PATH)
            .whereEqualTo("targetId", "target1")
            .get()
            .await()
            .documents
            .isEmpty())
  }

  @Test
  fun testDeleteAllNotificationsByUser() = runTest {
    repository.deleteAllNotificationsByUser("author1")
    assertTrue(
        FirebaseEmulator.firestore
            .collection(NOTIFICATION_COLLECTION_PATH)
            .whereEqualTo("authorId", "author1")
            .get()
            .await()
            .documents
            .isEmpty())
  }

  private fun DocumentSnapshot.toNotification(): Notification {
    val notificationId = id
    val targetId = getString("targetId") ?: throwMissingFieldException("targetId")
    val authorId = getString("authorId") ?: throwMissingFieldException("authorId")
    val title = getString("title") ?: throwMissingFieldException("title")
    val body = getString("body") ?: throwMissingFieldException("body")
    val route = getString("route") ?: throwMissingFieldException("route")
    val isRead = getBoolean("read") ?: throwMissingFieldException("read")
    val date = getTimestamp("date") ?: throwMissingFieldException("date")

    return Notification(notificationId, targetId, authorId, isRead, title, body, route, date)
  }

  private fun throwMissingFieldException(field: String): Nothing {
    throw IllegalArgumentException("Missing required field in Notification: $field")
  }
}
