package com.android.wildex.usecase.user

import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserSettingsRepository
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
  private lateinit var postsRepository: PostsRepository
  private lateinit var reportRepository: ReportRepository
  private lateinit var likeRepository: LikeRepository
  private lateinit var commentRepository: CommentRepository
  private lateinit var useCase: DeleteUserUseCase

  private val userId = "userId"

  @Before
  fun setUp() {
    userRepository = mockk()
    userSettingsRepository = mockk()
    userAnimalsRepository = mockk()
    userAchievementsRepository = mockk()
    postsRepository = mockk()
    reportRepository = mockk()
    likeRepository = mockk()
    commentRepository = mockk()

    useCase =
        DeleteUserUseCase(
            userRepository,
            userSettingsRepository,
            userAnimalsRepository,
            userAchievementsRepository,
            postsRepository,
            reportRepository,
            likeRepository,
            commentRepository)
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
      coEvery { postsRepository.deletePostsByUser(userId) } just Runs
      coEvery { reportRepository.deleteReportsByUser(userId) } just Runs
      coEvery { likeRepository.deleteLikesByUser(userId) } just Runs
      coEvery { commentRepository.deleteCommentsByUser(userId) } just Runs

      try {
        useCase(userId)
      } catch (e: Exception) {
        exceptionThrown = true
      }

      Assert.assertFalse(exceptionThrown)
    }
  }
}
