package com.android.wildex.usecase.user

import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.authentication.AuthRepository
import com.android.wildex.model.friendRequest.FriendRequestRepository
import com.android.wildex.model.notification.NotificationRepository
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.user.UserFriendsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserSettingsRepository
import com.android.wildex.model.user.UserTokensRepository
import com.android.wildex.utils.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteUserUseCaseTest {
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
  private lateinit var authRepository: AuthRepository
  private lateinit var notificationRepository: NotificationRepository
  private lateinit var useCase: DeleteUserUseCase

  private val userId = "userId"

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

    useCase =
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
        )
  }

  @Test
  fun invokeWhenNoUserExistThrows() {
    mainDispatcherRule.runTest {
      var exceptionThrown = false

      coEvery { userRepository.deleteUser(userId) } throws RuntimeException("bim-boom")
      coEvery { userSettingsRepository.deleteUserSettings(userId) } throws
          RuntimeException("bim-boom")
      coEvery { userAnimalsRepository.deleteUserAnimals(userId) } throws
          RuntimeException("bim-boom")
      coEvery { userAchievementsRepository.deleteUserAchievements(userId) } throws
          RuntimeException("bim-boom")
      coEvery { userFriendsRepository.deleteUserFriendsOfUser(userId) } throws
          RuntimeException("bim-boom")
      coEvery { friendRequestRepository.deleteAllFriendRequestsOfUser(userId) } throws
          RuntimeException("bim-boom")
      coEvery { userTokensRepository.deleteUserTokens(userId) } throws RuntimeException("bim-boom")

      try {
        useCase(userId)
      } catch (e: RuntimeException) {
        exceptionThrown = true
        Assert.assertEquals("bim-boom", e.message)
      }

      Assert.assertTrue(exceptionThrown)
    }
  }

  @Test
  fun invokeWhenUserExistsIsSuccess() {
    mainDispatcherRule.runTest {
      var exceptionThrown = false

      coEvery { userRepository.deleteUser(userId) } just Runs
      coEvery { userSettingsRepository.deleteUserSettings(userId) } just Runs
      coEvery { userAnimalsRepository.deleteUserAnimals(userId) } just Runs
      coEvery { userAchievementsRepository.deleteUserAchievements(userId) } just Runs
      coEvery { userFriendsRepository.deleteUserFriendsOfUser(userId) } just Runs
      coEvery { friendRequestRepository.deleteAllFriendRequestsOfUser(userId) } just Runs
      coEvery { postsRepository.deletePostsByUser(userId) } just Runs
      coEvery { reportRepository.deleteReportsByUser(userId) } just Runs
      coEvery { likeRepository.deleteLikesByUser(userId) } just Runs
      coEvery { commentRepository.deleteCommentsByUser(userId) } just Runs
      coEvery { userTokensRepository.deleteUserTokens(userId) } just Runs
      coEvery { notificationRepository.deleteAllNotificationsByUser(userId) } just Runs
      coEvery { notificationRepository.deleteAllNotificationsForUser(userId) } just Runs
      coEvery { authRepository.deleteUserAuth() } returns Result.success(Unit)

      try {
        useCase(userId)
      } catch (e: Exception) {
        exceptionThrown = true
      }

      Assert.assertFalse(exceptionThrown)
    }
  }
}
