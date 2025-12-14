package com.android.wildex.ui.settings

import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.authentication.AuthRepository
import com.android.wildex.model.friendRequest.FriendRequestRepository
import com.android.wildex.model.notification.NotificationRepository
import com.android.wildex.model.report.Report
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.user.OnBoardingStage
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.user.UserFriendsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserSettingsRepository
import com.android.wildex.model.user.UserTokensRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Location
import com.android.wildex.usecase.user.DeleteUserUseCase
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsScreenViewModelTest {
  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var userRepository: UserRepository
  private lateinit var userSettingsRepository: UserSettingsRepository
  private lateinit var userAnimalsRepository: UserAnimalsRepository
  private lateinit var userAchievementsRepository: UserAchievementsRepository
  private lateinit var userFriendsRepository: UserFriendsRepository
  private lateinit var userTokensRepository: UserTokensRepository
  private lateinit var friendRequestRepository: FriendRequestRepository
  private lateinit var postsRepository: PostsRepository
  private lateinit var reportRepository: ReportRepository
  private lateinit var likeRepository: LikeRepository
  private lateinit var commentRepository: CommentRepository
  private lateinit var notificationRepository: NotificationRepository
  private lateinit var authRepository: AuthRepository
  private lateinit var viewModel: SettingsScreenViewModel

  private val u1 =
      User(
          userId = "currentUserId",
          username = "currentUsername",
          name = "John",
          surname = "Doe",
          bio = "This is a bio",
          profilePictureURL =
              "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "France",
          onBoardingStage = OnBoardingStage.COMPLETE
      )

  private val u2 =
      User(
          userId = "otherUserId",
          username = "otherUsername",
          name = "Bob",
          surname = "Smith",
          bio = "This is my bob bio",
          profilePictureURL =
              "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "France",
          onBoardingStage = OnBoardingStage.COMPLETE
      )

  private val u3 =
      User(
          userId = "userId3",
          username = "otherUsername3",
          name = "Alan",
          surname = "Monkey",
          bio = "This is my Alaa bio",
          profilePictureURL =
              "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
          userType = UserType.PROFESSIONAL,
          creationDate = Timestamp.now(),
          country = "Spain",
      )

  private val report1 =
      Report(
          reportId = "reportId1",
          imageURL = "fakeURL",
          location = Location(latitude = 1.0, longitude = 2.0, name = "fakeName"),
          date = Timestamp.now(),
          description = "fakeDescription",
          authorId = "otherUserId",
          assigneeId = "userId3",
      )

  @Before
  fun setUp() {
    userRepository = mockk()
    userSettingsRepository = mockk()
    userAnimalsRepository = mockk()
    userAchievementsRepository = mockk()
    userFriendsRepository = mockk()
    userTokensRepository = mockk()
    friendRequestRepository = mockk()
    postsRepository = mockk()
    reportRepository = mockk()
    likeRepository = mockk()
    commentRepository = mockk()
    authRepository = mockk()
    notificationRepository = mockk()

    viewModel =
        SettingsScreenViewModel(
            authRepository = authRepository,
            userSettingsRepository = userSettingsRepository,
            userRepository = userRepository,
            userTokensRepository = userTokensRepository,
            reportRepository = reportRepository,
            currentUserId = "currentUserId",
            deleteUserUseCase =
                DeleteUserUseCase(
                    userRepository,
                    userSettingsRepository,
                    userAnimalsRepository,
                    userAchievementsRepository,
                    userFriendsRepository,
                    friendRequestRepository,
                    postsRepository,
                    reportRepository,
                    likeRepository,
                    commentRepository,
                    authRepository,
                    userTokensRepository,
                    notificationRepository,
                ),
        )

    coEvery { userRepository.getUser("currentUserId") } returns u1
    coEvery { userRepository.getUser("otherUserId") } returns u2
    coEvery { userRepository.getUser("userId3") } returns u3
    coEvery { userSettingsRepository.getEnableNotification("currentUserId") } returns false
    coEvery { userSettingsRepository.getAppearanceMode("currentUserId") } returns
        AppearanceMode.DARK
    coEvery { userSettingsRepository.getEnableNotification("otherUserId") } returns true
    coEvery { userSettingsRepository.setEnableNotification(any(), any()) } just Runs
    coEvery { userSettingsRepository.getAppearanceMode("otherUserId") } returns AppearanceMode.LIGHT
    coEvery { userAnimalsRepository.deleteUserAnimals("currentUserId") } just Runs
    coEvery { userAchievementsRepository.deleteUserAchievements("currentUserId") } just Runs
  }

  @Test
  fun viewModel_initializes_default_UI_state_isEmptyLoading() {
    val s = viewModel.uiState.value
    assertFalse(s.isError)
    assertEquals(AppearanceMode.AUTOMATIC, s.appearanceMode)
    assertTrue(s.notificationsEnabled)
    assertEquals(UserType.REGULAR, s.userType)
    assertFalse(s.isLoading)
    assertNull(s.errorMsg)
  }

  @Test
  fun loadUIState_updates_UI_state_success() {
    mainDispatcherRule.runTest {
      val deferred = CompletableDeferred<AppearanceMode>()
      coEvery { userSettingsRepository.getAppearanceMode("currentUserId") } coAnswers
          {
            deferred.await()
          }
      viewModel.loadUIState(true)
      assertTrue(viewModel.uiState.value.isLoading)
      deferred.complete(AppearanceMode.DARK)
      advanceUntilIdle()
      val expectedAppearanceMode = AppearanceMode.DARK
      val expectedNotificationsEnabled = false
      val expectedUserType = UserType.REGULAR
      val updatedState = viewModel.uiState.value
      assertFalse(updatedState.isLoading)
      assertFalse(updatedState.isError)
      assertNull(updatedState.errorMsg)
      assertEquals(expectedAppearanceMode, updatedState.appearanceMode)
      assertEquals(expectedNotificationsEnabled, updatedState.notificationsEnabled)
      assertEquals(expectedUserType, updatedState.userType)
    }
  }

  @Test
  fun clearErrorMsg_resets_errorMsg_to_null() {
    mainDispatcherRule.runTest {
      coEvery { userSettingsRepository.getAppearanceMode("currentUserId") } throws
          RuntimeException("boom")
      viewModel.loadUIState(true)
      advanceUntilIdle()
      assertNotNull(viewModel.uiState.value.errorMsg)

      viewModel.clearErrorMsg()
      assertNull(viewModel.uiState.value.errorMsg)
    }
  }

  @Test
  fun setNotificationsEnabled_updates_notificationsEnabled_in_UI_state() {
    mainDispatcherRule.runTest {
      coEvery { userSettingsRepository.setEnableNotification("currentUserId", true) } coAnswers {}
      viewModel.loadUIState(true)
      viewModel.setNotificationsEnabled(true)
      advanceUntilIdle()
      val updatedState = viewModel.uiState.value
      assertTrue(updatedState.notificationsEnabled)
    }
  }

  @Test
  fun setAppearanceMode_updates_appearanceMode_in_UI_state() {
    mainDispatcherRule.runTest {
      coEvery {
        userSettingsRepository.setAppearanceMode("currentUserId", AppearanceMode.LIGHT)
      } coAnswers {}
      viewModel.loadUIState(true)
      viewModel.setAppearanceMode(AppearanceMode.LIGHT)
      advanceUntilIdle()
      val updatedState = viewModel.uiState.value
      assertEquals(AppearanceMode.LIGHT, updatedState.appearanceMode)
    }
  }

  @Test
  fun setUserType_updates_userType_in_UI_state() {
    mainDispatcherRule.runTest {
      val updatedUser = u1.copy(userType = UserType.PROFESSIONAL)
      coEvery { userRepository.editUser("currentUserId", updatedUser) } coAnswers {}
      viewModel.loadUIState(true)
      viewModel.setUserType(UserType.PROFESSIONAL)
      advanceUntilIdle()

      val updatedState = viewModel.uiState.value
      assertEquals(UserType.PROFESSIONAL, updatedState.userType)
    }
  }

  @Test
  fun setUserType_from_professional_to_regular_unassigns_reports() {
    mainDispatcherRule.runTest {
      val updatedUser = u3.copy(userType = UserType.REGULAR)
      coEvery { userRepository.editUser("userId3", updatedUser) } coAnswers {}
      coEvery { reportRepository.getAllReportsByAssignee("userId3") } returns listOf(report1)
      coEvery {
        reportRepository.editReport("reportId1", report1.copy(assigneeId = null))
      } coAnswers {}
      viewModel.loadUIState(true)
      viewModel.setUserType(UserType.REGULAR)
      advanceUntilIdle()

      val updatedState = viewModel.uiState.value
      assertEquals(UserType.REGULAR, updatedState.userType)
    }
  }

  @Test
  fun onOfflineClickSetsError() {
    mainDispatcherRule.runTest {
      viewModel.clearErrorMsg()
      assertNull(viewModel.uiState.value.errorMsg)
      viewModel.onOfflineClick()
      assertNotNull(viewModel.uiState.value.errorMsg)
      assertEquals(
          "This action is not supported offline. Check your connection and try again.",
          viewModel.uiState.value.errorMsg)
    }
  }

  @Test
  fun notificationEnabled_setsToFalse_whenNoPermission() {
    mainDispatcherRule.runTest {
      viewModel.loadUIState(false)
      advanceUntilIdle()

      assertFalse(viewModel.uiState.value.notificationsEnabled)
      coVerify { userSettingsRepository.setEnableNotification("currentUserId", false) }
    }
  }
}
