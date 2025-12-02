package com.android.wildex.ui.settings

import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.authentication.AuthRepository
import com.android.wildex.model.friendRequest.FriendRequestRepository
import com.android.wildex.model.notification.NotificationRepository
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.user.UserFriendsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserSettingsRepository
import com.android.wildex.model.user.UserTokensRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.usecase.user.DeleteUserUseCase
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert
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
    coEvery { userSettingsRepository.getEnableNotification("currentUserId") } returns false
    coEvery { userSettingsRepository.getAppearanceMode("currentUserId") } returns
        AppearanceMode.DARK
    coEvery { userSettingsRepository.getEnableNotification("otherUserId") } returns true
    coEvery { userSettingsRepository.getAppearanceMode("otherUserId") } returns AppearanceMode.LIGHT
    coEvery { userAnimalsRepository.deleteUserAnimals("currentUserId") } just Runs
    coEvery { userAchievementsRepository.deleteUserAchievements("currentUserId") } just Runs
  }

  @Test
  fun viewModel_initializes_default_UI_state_isEmptyLoading() {
    val s = viewModel.uiState.value
    Assert.assertFalse(s.isError)
    Assert.assertEquals(AppearanceMode.AUTOMATIC, s.appearanceMode)
    Assert.assertTrue(s.notificationsEnabled)
    Assert.assertEquals(UserType.REGULAR, s.userType)
    Assert.assertFalse(s.isLoading)
    Assert.assertNull(s.errorMsg)
  }

  @Test
  fun loadUIState_updates_UI_state_success() {
    mainDispatcherRule.runTest {
      val deferred = CompletableDeferred<AppearanceMode>()
      coEvery { userSettingsRepository.getAppearanceMode("currentUserId") } coAnswers
          {
            deferred.await()
          }
      viewModel.loadUIState()
      Assert.assertTrue(viewModel.uiState.value.isLoading)
      deferred.complete(AppearanceMode.DARK)
      advanceUntilIdle()
      val expectedAppearanceMode = AppearanceMode.DARK
      val expectedNotificationsEnabled = false
      val expectedUserType = UserType.REGULAR
      val updatedState = viewModel.uiState.value
      Assert.assertFalse(updatedState.isLoading)
      Assert.assertFalse(updatedState.isError)
      Assert.assertNull(updatedState.errorMsg)
      Assert.assertEquals(expectedAppearanceMode, updatedState.appearanceMode)
      Assert.assertEquals(expectedNotificationsEnabled, updatedState.notificationsEnabled)
      Assert.assertEquals(expectedUserType, updatedState.userType)
    }
  }

  @Test
  fun clearErrorMsg_resets_errorMsg_to_null() {
    mainDispatcherRule.runTest {
      coEvery { userSettingsRepository.getAppearanceMode("currentUserId") } throws
          RuntimeException("boom")
      viewModel.loadUIState()
      advanceUntilIdle()
      Assert.assertNotNull(viewModel.uiState.value.errorMsg)

      viewModel.clearErrorMsg()
      Assert.assertNull(viewModel.uiState.value.errorMsg)
    }
  }

  @Test
  fun setNotificationsEnabled_updates_notificationsEnabled_in_UI_state() {
    mainDispatcherRule.runTest {
      coEvery { userSettingsRepository.setEnableNotification("currentUserId", true) } coAnswers {}
      viewModel.loadUIState()
      viewModel.setNotificationsEnabled(true)
      advanceUntilIdle()
      val updatedState = viewModel.uiState.value
      Assert.assertTrue(updatedState.notificationsEnabled)
    }
  }

  @Test
  fun setAppearanceMode_updates_appearanceMode_in_UI_state() {
    mainDispatcherRule.runTest {
      coEvery {
        userSettingsRepository.setAppearanceMode("currentUserId", AppearanceMode.LIGHT)
      } coAnswers {}
      viewModel.loadUIState()
      viewModel.setAppearanceMode(AppearanceMode.LIGHT)
      advanceUntilIdle()
      val updatedState = viewModel.uiState.value
      Assert.assertEquals(AppearanceMode.LIGHT, updatedState.appearanceMode)
    }
  }

  @Test
  fun setUserType_updates_userType_in_UI_state() {
    mainDispatcherRule.runTest {
      val updatedUser = u1.copy(userType = UserType.PROFESSIONAL)
      coEvery { userRepository.editUser("currentUserId", updatedUser) } coAnswers {}
      viewModel.loadUIState()
      viewModel.setUserType(UserType.PROFESSIONAL)
      advanceUntilIdle()

      val updatedState = viewModel.uiState.value
      Assert.assertEquals(UserType.PROFESSIONAL, updatedState.userType)
    }
  }
}
