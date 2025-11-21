package com.android.wildex.usecase.achievement

import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.utils.MainDispatcherRule
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateUserAchievementsUseCaseTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var userAchievementsRepository: UserAchievementsRepository
  private lateinit var useCase: UpdateUserAchievementsUseCase

  private val userId = "u1"

  @Before
  fun setUp() {
    userAchievementsRepository = mockk(relaxed = true)
    useCase = UpdateUserAchievementsUseCase(userAchievementsRepository)
  }

  @Test
  fun invoke_callsInitializeBeforeUpdate() {
    mainDispatcherRule.runTest {
      coEvery { userAchievementsRepository.initializeUserAchievements(userId) } returns Unit
      coEvery { userAchievementsRepository.updateUserAchievements(userId) } returns Unit

      useCase(userId)
      advanceUntilIdle()

      coVerify(ordering = Ordering.SEQUENCE) {
        userAchievementsRepository.initializeUserAchievements(userId)
        userAchievementsRepository.updateUserAchievements(userId)
      }
    }
  }
}
