package com.android.wildex.ui.notification

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.notification.Notification
import com.android.wildex.model.notification.NotificationRepository
import com.android.wildex.model.user.OnBoardingStage
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingScreenTestTags
import com.android.wildex.utils.LocalRepositories
import com.android.wildex.utils.offline.FakeConnectivityObserver
import com.google.firebase.Timestamp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val userRepository = LocalRepositories.userRepository
  private val fakeObserver = FakeConnectivityObserver(initial = true)

  private val testNotification =
      Notification(
          notificationId = "notif1",
          targetId = "currentUserId-1",
          authorId = "author1",
          title = "New Like",
          body = "Someone liked your post",
          date = Timestamp.now(),
          route = "post/123",
          read = true,
      )

  open class LocalNotificationRepository : NotificationRepository {
    val sampleNotifications: MutableList<Notification> = mutableListOf()

    override suspend fun getAllNotificationsForUser(userId: Id): List<Notification> {
      return sampleNotifications
    }

    override suspend fun markNotificationAsRead(notificationId: Id) {
      sampleNotifications.replaceAll {
        if (it.notificationId == notificationId) it.copy(read = true) else it
      }
    }

    override suspend fun markAllNotificationsForUserAsRead(userId: Id) {
      sampleNotifications.replaceAll { if (it.targetId == userId) it.copy(read = true) else it }
    }

    override suspend fun deleteNotification(notificationId: Id) {
      sampleNotifications.removeIf { it.notificationId == notificationId }
    }

    override suspend fun deleteAllNotificationsForUser(userId: Id) {
      sampleNotifications.removeIf { it.targetId == userId }
    }

    override suspend fun deleteAllNotificationsByUser(userId: Id) {
      sampleNotifications.removeIf { it.authorId == userId }
    }
  }

  private val notificationRepository = LocalNotificationRepository()

  private lateinit var notificationScreenVM: NotificationScreenViewModel

  @Before
  fun setup() = runBlocking {
    // Add test users
    userRepository.addUser(
        User(
            userId = "currentUserId-1",
            username = "testuser",
            name = "Test",
            surname = "User",
            bio = "This is a test user.",
            profilePictureURL =
                "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "Testland",
            onBoardingStage = OnBoardingStage.COMPLETE
        ))

    userRepository.addUser(
        User(
            userId = "author1",
            username = "author_1",
            name = "Author1",
            surname = "One",
            bio = "Author 1 user.",
            profilePictureURL =
                "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "Testland",
            onBoardingStage = OnBoardingStage.COMPLETE
        ))
    userRepository.addUser(
        User(
            userId = "author2",
            username = "author_2",
            name = "Author2",
            surname = "Two",
            bio = "Author 2 user.",
            profilePictureURL =
                "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "Testland",
            onBoardingStage = OnBoardingStage.COMPLETE
        ))

    notificationScreenVM =
        NotificationScreenViewModel(
            notificationRepository,
            userRepository,
            "currentUserId-1",
        )
  }

  @After
  fun tearDown() {
    notificationRepository.sampleNotifications.clear()
    LocalRepositories.clearAll()
  }

  @Test
  fun swipeToDeleteWorks() {
    fakeObserver.setOnline(true)
    val notif2 = testNotification.copy(notificationId = "notif2", title = "New Comment", body = "")
    val notif3 =
        testNotification.copy(
            notificationId = "notif3",
            title = "New Follow",
            body = "",
            read = false,
        )

    runBlocking {
      notificationRepository.sampleNotifications.add(testNotification)
      notificationRepository.sampleNotifications.add(notif2)
      notificationRepository.sampleNotifications.add(notif3)
      notificationScreenVM.refreshUIState()
    }

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        NotificationScreen(notificationScreenViewModel = notificationScreenVM)
      }
    }

    composeTestRule
        .onNodeWithTag(NotificationScreenTestTags.testTagForNotification("notif1"))
        .performScrollTo()
        .assertIsDisplayed()
        .performTouchInput { swipeRight() }
    composeTestRule.waitForIdle()
    notificationScreenVM.refreshUIState()

    assertTrue(
        notificationScreenVM.uiState.value.notifications.none { it.notificationId == "notif1" })
  }

  @Test
  fun testTagsAreCorrectlySetWhenNoNotifications() {
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        NotificationScreen(notificationScreenViewModel = notificationScreenVM)
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.assertTopBarIsDisplayed()
    composeTestRule
        .onNodeWithTag(NotificationScreenTestTags.NO_NOTIFICATION_TEXT)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(NotificationScreenTestTags.testTagForNotification("notif1"))
        .assertIsNotDisplayed()
  }

  @Test
  fun notificationsShowCorrectly() {
    fakeObserver.setOnline(true)
    val notif2 = testNotification.copy(notificationId = "notif2", title = "New Comment", body = "")
    val notif3 =
        testNotification.copy(
            notificationId = "notif3",
            title = "New Follow",
            body = "",
            read = false,
        )

    runBlocking {
      notificationRepository.sampleNotifications.add(testNotification)
      notificationRepository.sampleNotifications.add(notif2)
      notificationRepository.sampleNotifications.add(notif3)
      notificationScreenVM.refreshUIState()
    }

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        NotificationScreen(notificationScreenViewModel = notificationScreenVM)
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.assertTopBarIsDisplayed()
    composeTestRule
        .onNodeWithTag(NotificationScreenTestTags.NO_NOTIFICATION_TEXT)
        .assertIsNotDisplayed()

    composeTestRule.onNodeWithTag(NotificationScreenTestTags.NOTIFICATION_LIST).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(NotificationScreenTestTags.MARK_ALL_AS_READ_BUTTON)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(NotificationScreenTestTags.CLEAR_ALL_BUTTON).assertIsDisplayed()
    composeTestRule.assertNotificationIsDisplayed("notif1", withDesc = true, isRead = true)
    composeTestRule.assertNotificationIsDisplayed("notif2", withDesc = false, isRead = true)
    composeTestRule.assertNotificationIsDisplayed("notif3", withDesc = false, isRead = false)
  }

  @Test
  fun goBackButtonClick_invokesCallback() {
    fakeObserver.setOnline(true)
    var backClicked = false

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        NotificationScreen(
            notificationScreenViewModel = notificationScreenVM,
            onGoBack = { backClicked = true },
        )
      }
    }

    composeTestRule.assertTopBarIsDisplayed()
    composeTestRule.onNodeWithTag(NotificationScreenTestTags.BACK_BUTTON).performClick()
    assertTrue(backClicked)
  }

  @Test
  fun profilePictureClick_invokesCallback() {
    fakeObserver.setOnline(true)
    var profileClickedId: String? = null

    runBlocking {
      notificationRepository.sampleNotifications.add(testNotification)
      notificationScreenVM.refreshUIState()
    }

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        NotificationScreen(
            notificationScreenViewModel = notificationScreenVM,
            onProfileClick = { profileClickedId = it },
        )
      }
    }

    composeTestRule.assertNotificationIsDisplayed(
        notifId = "notif1",
        withDesc = true,
        isRead = true,
    )
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(
            NotificationScreenTestTags.testTagForProfilePicture("notif1"),
            useUnmergedTree = true,
        )
        .performScrollTo()
        .performClick()
    assertTrue(profileClickedId == "author1")
  }

  @Test
  fun notificationClick_invokesCallbackAndMarksAsRead() {
    fakeObserver.setOnline(true)
    var notificationRoute: String? = null

    runBlocking {
      notificationRepository.sampleNotifications.add(testNotification)
      notificationScreenVM.refreshUIState()
    }

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        NotificationScreen(
            notificationScreenViewModel = notificationScreenVM,
            onNotificationClick = { notificationRoute = it },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(
            NotificationScreenTestTags.testTagForNotification("notif1"),
            useUnmergedTree = true,
        )
        .performScrollTo()
        .performClick()

    assertTrue(notificationRoute == "post/123")
  }

  @Test
  fun unreadNotification_displaysBadge() {
    fakeObserver.setOnline(true)

    runBlocking {
      notificationRepository.sampleNotifications.add(testNotification)
      notificationRepository.sampleNotifications.add(
          testNotification.copy(notificationId = "notif2", read = false))
      notificationScreenVM.refreshUIState()
    }

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        NotificationScreen(notificationScreenViewModel = notificationScreenVM)
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(
            NotificationScreenTestTags.testTagForNotificationReadState("notif1"),
            useUnmergedTree = true,
        )
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(
            NotificationScreenTestTags.testTagForNotificationReadState("notif2"),
            useUnmergedTree = true,
        )
        .assertIsDisplayed()
  }

  @Test
  fun loadingFailed_whenError() {
    fakeObserver.setOnline(true)
    val errorRepository =
        object : LocalNotificationRepository() {
          override suspend fun getAllNotificationsForUser(userId: Id): List<Notification> {
            throw Exception("Boom")
          }
        }

    val errorVm = NotificationScreenViewModel(errorRepository, userRepository, "currentUserId-1")

    runBlocking {
      errorRepository.sampleNotifications.add(testNotification)
      errorRepository.sampleNotifications.add(testNotification.copy(notificationId = "notif2"))
      errorRepository.sampleNotifications.add(testNotification.copy(notificationId = "notif3"))
    }
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        NotificationScreen(notificationScreenViewModel = errorVm)
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(LoadingScreenTestTags.LOADING_FAIL).assertIsDisplayed()
  }

  fun markAllAsReadButton_works() {
    fakeObserver.setOnline(true)
    runBlocking {
      notificationRepository.sampleNotifications.add(testNotification.copy(read = false))
      notificationRepository.sampleNotifications.add(
          testNotification.copy(notificationId = "notif2", read = false))
      notificationRepository.sampleNotifications.add(
          testNotification.copy(notificationId = "notif3", read = false))
      notificationScreenVM.refreshUIState()
    }
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        NotificationScreen(notificationScreenViewModel = notificationScreenVM)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NotificationScreenTestTags.NOTIFICATION_LIST).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(NotificationScreenTestTags.MARK_ALL_AS_READ_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()
    assertTrue(notificationScreenVM.uiState.value.notifications.all { it.notificationReadState })
  }

  fun clearAllButton_works() {
    fakeObserver.setOnline(true)
    runBlocking {
      notificationRepository.sampleNotifications.add(testNotification)
      notificationRepository.sampleNotifications.add(
          testNotification.copy(notificationId = "notif2"))
      notificationRepository.sampleNotifications.add(
          testNotification.copy(notificationId = "notif3"))
      notificationScreenVM.refreshUIState()
    }
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        NotificationScreen(notificationScreenViewModel = notificationScreenVM)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NotificationScreenTestTags.NOTIFICATION_LIST).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(NotificationScreenTestTags.CLEAR_ALL_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()
    assertTrue(notificationScreenVM.uiState.value.notifications.isEmpty())
    composeTestRule
        .onNodeWithTag(NotificationScreenTestTags.NO_NOTIFICATION_TEXT)
        .assertIsDisplayed()
  }

  @Test
  fun loadingScreen_showsWhileFetchingNotifications() {
    fakeObserver.setOnline(true)
    val fetchSignal = CompletableDeferred<Unit>()

    val delayedNotificationRepo =
        object : LocalRepositories.NotificationRepositoryImpl() {
          override suspend fun getAllNotificationsForUser(userId: String): List<Notification> {
            fetchSignal.await()
            return super.getAllNotificationsForUser(userId)
          }
        }

    runBlocking {
      val vm =
          NotificationScreenViewModel(
              delayedNotificationRepo,
              LocalRepositories.userRepository,
              "currentUserId-1",
          )
      vm.loadUIState()

      composeTestRule.setContent {
        CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
          NotificationScreen(notificationScreenViewModel = vm)
        }
      }

      composeTestRule
          .onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN, useUnmergedTree = true)
          .assertIsDisplayed()

      fetchSignal.complete(Unit)
      composeTestRule.waitForIdle()

      composeTestRule
          .onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN, useUnmergedTree = true)
          .assertIsNotDisplayed()
    }
  }

  @Test
  fun refreshDisabledWhenOfflineWithNotifications() {
    fakeObserver.setOnline(false)

    runBlocking {
      notificationRepository.sampleNotifications.add(testNotification)
      notificationRepository.sampleNotifications.add(
          testNotification.copy(notificationId = "notif2", title = "Second notification"))
      notificationScreenVM.refreshUIState()
    }

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        NotificationScreen(notificationScreenViewModel = notificationScreenVM)
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NotificationScreenTestTags.PULL_TO_REFRESH).performTouchInput {
      swipeDown()
    }

    assertFalse(notificationScreenVM.uiState.value.isRefreshing)
  }

  @Test
  fun notificationGetsSkipped_whenAuthorLookupFails() {
    fakeObserver.setOnline(true)
    val badNotification =
        testNotification.copy(notificationId = "bad_notif", authorId = "unknown-author")

    notificationRepository.sampleNotifications.add(testNotification)
    notificationRepository.sampleNotifications.add(badNotification)

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        NotificationScreen(notificationScreenViewModel = notificationScreenVM)
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(NotificationScreenTestTags.testTagForNotification("bad_notif"))
        .assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(NotificationScreenTestTags.NOTIFICATION_LIST).assertIsDisplayed()
    composeTestRule.assertNotificationIsDisplayed(
        testNotification.notificationId,
        testNotification.body.isNotEmpty(),
        testNotification.read,
    )
  }

  private fun ComposeTestRule.assertTopBarIsDisplayed() {
    onNodeWithTag(NotificationScreenTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    onNodeWithTag(NotificationScreenTestTags.TOP_BAR).assertIsDisplayed()
    onNodeWithTag(NotificationScreenTestTags.BACK_BUTTON).assertIsDisplayed()
  }

  private fun ComposeTestRule.assertNotificationIsDisplayed(
      notifId: Id,
      withDesc: Boolean,
      isRead: Boolean,
  ) {
    onNodeWithTag(
            NotificationScreenTestTags.testTagForNotification(notifId),
            useUnmergedTree = true,
        )
        .performScrollTo()
    onNodeWithTag(
            NotificationScreenTestTags.testTagForProfilePicture(notifId),
            useUnmergedTree = true,
        )
        .performScrollTo()
    onNodeWithTag(
            NotificationScreenTestTags.testTagForNotificationTitle(notifId),
            useUnmergedTree = true,
        )
        .performScrollTo()
    onNodeWithTag(
            NotificationScreenTestTags.testTagForNotificationDate(notifId),
            useUnmergedTree = true,
        )
        .performScrollTo()
    if (!isRead)
        onNodeWithTag(
                NotificationScreenTestTags.testTagForNotificationReadState(notifId),
                useUnmergedTree = true,
            )
            .performScrollTo()
    if (withDesc)
        onNodeWithTag(
                NotificationScreenTestTags.testTagForNotificationDescription(notifId),
                useUnmergedTree = true,
            )
            .performScrollTo()
  }
}
