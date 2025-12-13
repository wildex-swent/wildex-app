package com.android.wildex.ui.notification

import com.android.wildex.model.notification.Notification
import com.android.wildex.model.notification.NotificationRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationScreenViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var notificationRepository: NotificationRepository
  private lateinit var userRepository: UserRepository
  private lateinit var viewModel: NotificationScreenViewModel

  private val sampleAuthor =
      SimpleUser(
          userId = "author-1",
          username = "authorName",
          profilePictureURL = "",
          userType = UserType.REGULAR,
      )

  private val sampleNotification =
      Notification(
          notificationId = "n1",
          targetId = "t1",
          authorId = sampleAuthor.userId,
          read = false,
          title = "Titre test",
          body = "Corps test",
          route = "route/test",
          date = Timestamp.now(),
      )

  @Before
  fun setUp() {
    notificationRepository = mockk()
    userRepository = mockk()
    viewModel =
        NotificationScreenViewModel(
            notificationRepository = notificationRepository,
            userRepository = userRepository,
            currentUserId = "uid-1",
        )
  }

  @Test
  fun initial_UI_state_defaults() {
    val s = viewModel.uiState.value
    assertTrue(s.notifications.isEmpty())
    assertFalse(s.isLoading)
    assertFalse(s.isRefreshing)
    assertFalse(s.isError)
    assertNull(s.errorMsg)
  }

  @Test
  fun loadUIState_success_populatesNotifications() =
      mainDispatcherRule.runTest {
        coEvery { notificationRepository.getAllNotificationsForUser("uid-1") } returns
            listOf(sampleNotification)
        coEvery { userRepository.getSimpleUser(sampleAuthor.userId) } returns sampleAuthor

        viewModel.loadUIState()
        advanceUntilIdle()

        val s = viewModel.uiState.value
        assertFalse(s.isLoading)
        assertFalse(s.isError)
        assertNull(s.errorMsg)
        assertEquals(1, s.notifications.size)
        val ui = s.notifications.first()
        assertEquals(sampleNotification.notificationId, ui.notificationId)
        assertEquals(sampleNotification.title, ui.notificationTitle)
        assertEquals(sampleNotification.body, ui.notificationDescription)
        assertEquals(sampleAuthor.username, ui.author.username)

        coVerify(exactly = 1) { notificationRepository.getAllNotificationsForUser("uid-1") }
        coVerify(exactly = 1) { userRepository.getSimpleUser(sampleAuthor.userId) }
        confirmVerified(notificationRepository, userRepository)
      }

  @Test
  fun loadUIState_error_setsError() =
      mainDispatcherRule.runTest {
        coEvery { notificationRepository.getAllNotificationsForUser("uid-1") } throws
            RuntimeException("boom")

        viewModel.loadUIState()
        advanceUntilIdle()

        val s = viewModel.uiState.value
        assertTrue(s.isError)
        assertFalse(s.isLoading)
        assertNotNull(s.errorMsg)
        assertTrue(s.errorMsg!!.contains("Error loading notifications: boom"))
        coVerify(exactly = 1) { notificationRepository.getAllNotificationsForUser("uid-1") }
        confirmVerified(notificationRepository, userRepository)
      }

  @Test
  fun refresh_updatesState() =
      mainDispatcherRule.runTest {
        coEvery { notificationRepository.getAllNotificationsForUser("uid-1") } returns
            listOf(sampleNotification)
        coEvery { userRepository.getSimpleUser(sampleAuthor.userId) } returns sampleAuthor
        coEvery { userRepository.refreshCache() } just Runs

        viewModel.refreshUIState()
        advanceUntilIdle()

        val s = viewModel.uiState.value
        assertEquals(1, s.notifications.size)

        coVerify(exactly = 1) { notificationRepository.getAllNotificationsForUser("uid-1") }
        coVerify(exactly = 1) { userRepository.getSimpleUser(sampleAuthor.userId) }
        confirmVerified(notificationRepository)
      }

  @Test
  fun refreshOffline_sets_error_notifications() {
    mainDispatcherRule.runTest {
      val before = viewModel.uiState.value
      viewModel.refreshOffline()
      val after = viewModel.uiState.value
      assertNull(before.errorMsg)
      assertNotNull(after.errorMsg)
      assertTrue(
          after.errorMsg!!.contains("You are currently offline\nYou can not refresh for now :/"))
      viewModel.clearErrorMsg()
      assertNull(viewModel.uiState.value.errorMsg)
    }
  }

  @Test
  fun markAllAsRead_updatesState() =
      mainDispatcherRule.runTest {
        coEvery { notificationRepository.markAllNotificationsForUserAsRead("uid-1") } just Runs
        coEvery { notificationRepository.getAllNotificationsForUser("uid-1") } returns
            listOf(sampleNotification.copy(read = true))
        coEvery { userRepository.getSimpleUser(sampleAuthor.userId) } returns sampleAuthor

        viewModel.markAllAsRead()
        advanceUntilIdle()

        val s = viewModel.uiState.value
        assertFalse(s.isLoading)
        assertFalse(s.isError)
        assertNull(s.errorMsg)
        assertTrue(s.notifications.first().notificationReadState)

        coVerify(exactly = 1) { notificationRepository.markAllNotificationsForUserAsRead("uid-1") }
        coVerify(exactly = 1) { notificationRepository.getAllNotificationsForUser("uid-1") }
        coVerify(exactly = 1) { userRepository.getSimpleUser(sampleAuthor.userId) }
        confirmVerified(notificationRepository, userRepository)
      }

  @Test
  fun markAllAsRead_error_setsError() =
      mainDispatcherRule.runTest {
        coEvery { notificationRepository.markAllNotificationsForUserAsRead("uid-1") } throws
            RuntimeException("Failed to mark all as read")

        viewModel.markAllAsRead()
        advanceUntilIdle()

        val s = viewModel.uiState.value
        assertTrue(s.isError)
        assertFalse(s.isLoading)
        assertNotNull(s.errorMsg)
        assertTrue(s.errorMsg!!.contains("Error marking all notifications as read"))

        coVerify(exactly = 1) { notificationRepository.markAllNotificationsForUserAsRead("uid-1") }
        confirmVerified(notificationRepository, userRepository)
      }

  @Test
  fun markAsRead_updatesState() =
      mainDispatcherRule.runTest {
        coEvery { notificationRepository.markNotificationAsRead("n1") } just Runs
        coEvery { notificationRepository.getAllNotificationsForUser("uid-1") } returns
            listOf(sampleNotification.copy(read = true))
        coEvery { userRepository.getSimpleUser(sampleAuthor.userId) } returns sampleAuthor

        viewModel.markAsRead("n1")
        advanceUntilIdle()

        val s = viewModel.uiState.value
        assertFalse(s.isLoading)
        assertFalse(s.isError)
        assertNull(s.errorMsg)
        assertTrue(s.notifications.first().notificationReadState)

        coVerify(exactly = 1) { notificationRepository.markNotificationAsRead("n1") }
        coVerify(exactly = 1) { notificationRepository.getAllNotificationsForUser("uid-1") }
        coVerify(exactly = 1) { userRepository.getSimpleUser(sampleAuthor.userId) }
        confirmVerified(notificationRepository, userRepository)
      }

  @Test
  fun markAsRead_error_setsError() =
      mainDispatcherRule.runTest {
        coEvery { notificationRepository.markNotificationAsRead("n1") } throws
            RuntimeException("Failed to mark as read")

        viewModel.markAsRead("n1")
        advanceUntilIdle()

        val s = viewModel.uiState.value
        assertTrue(s.isError)
        assertFalse(s.isLoading)
        assertNotNull(s.errorMsg)
        assertTrue(s.errorMsg!!.contains("Error marking notification as read"))

        coVerify(exactly = 1) { notificationRepository.markNotificationAsRead("n1") }
        confirmVerified(notificationRepository, userRepository)
      }

  @Test
  fun clearNotification_success() =
      mainDispatcherRule.runTest {
        coEvery { notificationRepository.deleteNotification("n1") } just Runs
        coEvery { notificationRepository.getAllNotificationsForUser("uid-1") } returns emptyList()

        viewModel.clearNotification("n1")
        advanceUntilIdle()

        val s = viewModel.uiState.value
        assertFalse(s.isLoading)
        assertFalse(s.isError)
        assertNull(s.errorMsg)
        assertTrue(s.notifications.isEmpty())

        coVerify(exactly = 1) { notificationRepository.deleteNotification("n1") }
        coVerify(exactly = 1) { notificationRepository.getAllNotificationsForUser("uid-1") }
        confirmVerified(notificationRepository, userRepository)
      }

  @Test
  fun clearNotification_error_setsError() =
      mainDispatcherRule.runTest {
        coEvery { notificationRepository.deleteNotification("n1") } throws
            RuntimeException("Failed to delete")

        viewModel.clearNotification("n1")
        advanceUntilIdle()

        val s = viewModel.uiState.value
        assertTrue(s.isError)
        assertFalse(s.isLoading)
        assertNotNull(s.errorMsg)
        assertTrue(s.errorMsg!!.contains("Error clearing notification"))

        coVerify(exactly = 1) { notificationRepository.deleteNotification("n1") }
        confirmVerified(notificationRepository, userRepository)
      }

  @Test
  fun clearAllNotifications_success() =
      mainDispatcherRule.runTest {
        coEvery { notificationRepository.deleteAllNotificationsForUser("uid-1") } just Runs
        coEvery { notificationRepository.getAllNotificationsForUser("uid-1") } returns emptyList()

        viewModel.clearAllNotifications()
        advanceUntilIdle()

        val s = viewModel.uiState.value
        assertFalse(s.isLoading)
        assertFalse(s.isError)
        assertNull(s.errorMsg)
        assertTrue(s.notifications.isEmpty())

        coVerify(exactly = 1) { notificationRepository.deleteAllNotificationsForUser("uid-1") }
        coVerify(exactly = 1) { notificationRepository.getAllNotificationsForUser("uid-1") }
        confirmVerified(notificationRepository, userRepository)
      }

  @Test
  fun clearAllNotifications_error_setsError() =
      mainDispatcherRule.runTest {
        coEvery { notificationRepository.deleteAllNotificationsForUser("uid-1") } throws
            RuntimeException("Failed to delete all")

        viewModel.clearAllNotifications()
        advanceUntilIdle()

        val s = viewModel.uiState.value
        assertTrue(s.isError)
        assertFalse(s.isLoading)
        assertNotNull(s.errorMsg)
        assertTrue(s.errorMsg!!.contains("Error clearing all notifications"))

        coVerify(exactly = 1) { notificationRepository.deleteAllNotificationsForUser("uid-1") }
        confirmVerified(notificationRepository, userRepository)
      }

  @Test
  fun fetchNotifications_filtersOutFailedUserFetch() =
      mainDispatcherRule.runTest {
        val notification2 = sampleNotification.copy(notificationId = "n2", authorId = "author-2")
        coEvery { notificationRepository.getAllNotificationsForUser("uid-1") } returns
            listOf(sampleNotification, notification2)
        coEvery { userRepository.getSimpleUser(sampleAuthor.userId) } returns sampleAuthor
        coEvery { userRepository.getSimpleUser("author-2") } throws
            RuntimeException("User not found")

        viewModel.loadUIState()
        advanceUntilIdle()

        val s = viewModel.uiState.value
        assertFalse(s.isError)
        assertEquals(1, s.notifications.size)
        assertEquals("n1", s.notifications.first().notificationId)

        coVerify(exactly = 1) { notificationRepository.getAllNotificationsForUser("uid-1") }
        coVerify(exactly = 1) { userRepository.getSimpleUser(sampleAuthor.userId) }
        coVerify(exactly = 1) { userRepository.getSimpleUser("author-2") }
        confirmVerified(notificationRepository, userRepository)
      }

  @Test
  fun notificationsSortedByDate() =
      mainDispatcherRule.runTest {
        val olderNotification =
            sampleNotification.copy(
                notificationId = "n2",
                date = Timestamp(sampleNotification.date.seconds - 1000, 0),
            )
        coEvery { notificationRepository.getAllNotificationsForUser("uid-1") } returns
            listOf(olderNotification, sampleNotification)
        coEvery { userRepository.getSimpleUser(sampleAuthor.userId) } returns sampleAuthor

        viewModel.loadUIState()
        advanceUntilIdle()

        val s = viewModel.uiState.value
        assertEquals(2, s.notifications.size)
        assertEquals("n1", s.notifications[0].notificationId)
        assertEquals("n2", s.notifications[1].notificationId)

        coVerify(exactly = 1) { notificationRepository.getAllNotificationsForUser("uid-1") }
        coVerify(exactly = 2) { userRepository.getSimpleUser(sampleAuthor.userId) }
        confirmVerified(notificationRepository, userRepository)
      }

  @Test
  fun getRelativeTime_allTimePeriods() =
      mainDispatcherRule.runTest {
        val now = Timestamp.now()
        val nowSeconds = now.seconds
        // Create notifications for each time period
        val secondsAgo =
            sampleNotification.copy(notificationId = "n1", date = Timestamp(nowSeconds - 10, 0))
        val minutesAgo =
            sampleNotification.copy(notificationId = "n2", date = Timestamp(nowSeconds - 300, 0))
        val hoursAgo =
            sampleNotification.copy(notificationId = "n3", date = Timestamp(nowSeconds - 7200, 0))
        val daysAgo =
            sampleNotification.copy(notificationId = "n4", date = Timestamp(nowSeconds - 172800, 0))
        val weeksAgo =
            sampleNotification.copy(
                notificationId = "n5",
                date = Timestamp(nowSeconds - 1209600, 0),
            )
        val monthsAgo =
            sampleNotification.copy(
                notificationId = "n6",
                date = Timestamp(nowSeconds - 5184000, 0),
            )
        val yearsAgo =
            sampleNotification.copy(
                notificationId = "n7",
                date = Timestamp(nowSeconds - 63072000, 0),
            )
        val justNow =
            sampleNotification.copy(
                notificationId = "n8",
                date = Timestamp(nowSeconds - 2, 0),
            )
        coEvery { notificationRepository.getAllNotificationsForUser("uid-1") } returns
            listOf(
                secondsAgo,
                minutesAgo,
                hoursAgo,
                daysAgo,
                weeksAgo,
                monthsAgo,
                yearsAgo,
                justNow,
            )
        coEvery { userRepository.getSimpleUser(sampleAuthor.userId) } returns sampleAuthor

        viewModel.loadUIState()
        advanceUntilIdle()

        val s = viewModel.uiState.value
        assertFalse(s.isError)
        assertEquals(8, s.notifications.size)

        val notificationMap = s.notifications.associateBy { it.notificationId }
        assertEquals("now", notificationMap["n8"]?.notificationRelativeTime)
        assertTrue(notificationMap["n1"]?.notificationRelativeTime?.contains("seconds ago") == true)
        assertTrue(notificationMap["n2"]?.notificationRelativeTime?.contains("minutes ago") == true)
        assertTrue(notificationMap["n3"]?.notificationRelativeTime?.contains("hours ago") == true)
        assertTrue(notificationMap["n4"]?.notificationRelativeTime?.contains("days ago") == true)
        assertTrue(notificationMap["n5"]?.notificationRelativeTime?.contains("weeks ago") == true)
        assertTrue(notificationMap["n6"]?.notificationRelativeTime?.contains("months ago") == true)
        assertTrue(notificationMap["n7"]?.notificationRelativeTime?.contains("years ago") == true)
        coVerify(exactly = 1) { notificationRepository.getAllNotificationsForUser("uid-1") }
        coVerify(exactly = 8) { userRepository.getSimpleUser(sampleAuthor.userId) }
        confirmVerified(notificationRepository, userRepository)
      }
}
