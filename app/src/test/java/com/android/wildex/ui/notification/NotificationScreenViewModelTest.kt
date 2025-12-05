package com.android.wildex.ui.notification

import com.android.wildex.model.notification.Notification
import com.android.wildex.model.notification.NotificationRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert
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
          isRead = false,
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
    Assert.assertTrue(s.notifications.isEmpty())
    Assert.assertFalse(s.isLoading)
    Assert.assertFalse(s.isRefreshing)
    Assert.assertFalse(s.isError)
    Assert.assertNull(s.errorMsg)
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
        Assert.assertFalse(s.isLoading)
        Assert.assertFalse(s.isError)
        Assert.assertNull(s.errorMsg)
        Assert.assertEquals(1, s.notifications.size)
        val ui = s.notifications.first()
        Assert.assertEquals(sampleNotification.notificationId, ui.notificationId)
        Assert.assertEquals(sampleNotification.title, ui.notificationTitle)
        Assert.assertEquals(sampleNotification.body, ui.notificationDescription)
        Assert.assertEquals(sampleAuthor.username, ui.simpleUser.username)

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
        Assert.assertTrue(s.isError)
        Assert.assertFalse(s.isLoading)
        Assert.assertNotNull(s.errorMsg)
        Assert.assertTrue(s.errorMsg!!.contains("Error loading notifications: boom"))
        coVerify(exactly = 1) { notificationRepository.getAllNotificationsForUser("uid-1") }
        confirmVerified(notificationRepository, userRepository)
      }

  @Test
  fun refresh_updatesState() =
      mainDispatcherRule.runTest {
        coEvery { notificationRepository.getAllNotificationsForUser("uid-1") } returns
            listOf(sampleNotification)
        coEvery { userRepository.getSimpleUser(sampleAuthor.userId) } returns sampleAuthor

        viewModel.refreshUIState()
        advanceUntilIdle()

        val s = viewModel.uiState.value
        Assert.assertEquals(1, s.notifications.size)
        coVerify(exactly = 1) { notificationRepository.getAllNotificationsForUser("uid-1") }
        coVerify(exactly = 1) { userRepository.getSimpleUser(sampleAuthor.userId) }
        confirmVerified(notificationRepository, userRepository)
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
    }
  }
}
