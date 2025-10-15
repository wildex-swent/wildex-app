package com.android.wildex.ui.profile

import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileScreenViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var userRepository: UserRepository
  private lateinit var achievementsRepository: UserAchievementsRepository
  private lateinit var viewModel: ProfileScreenViewModel

  private val u1 =
      User(
          userId = "uid-1",
          username = "user_one",
          name = "First",
          surname = "User",
          bio = "bio",
          profilePictureURL = "pic",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "X",
          friendsCount = 1)

  private val u2 = u1.copy(userId = "uid-1", username = "user_one_2", friendsCount = 42)

  private val a1: Achievement = mockk()
  private val a2: Achievement = mockk()

  @Before
  fun setUp() {
    userRepository = mockk()
    achievementsRepository = mockk()
    viewModel =
        ProfileScreenViewModel(
            userRepository = userRepository,
            achievementRepository = achievementsRepository,
            currentUserId = { "uid-1" },
            uid = "uid-1")
  }

  @After fun tearDown() {}

  @Test
  fun viewModel_initializes_default_UI_state() {
    val s = viewModel.uiState.value
    Assert.assertNull(s.user)
    Assert.assertTrue(s.isUserOwner)
    Assert.assertTrue(s.achievements.isEmpty())
  }

  @Test
  fun refreshUIState_updates_UI_state_success_ownerTrue() {
    mainDispatcherRule.runTest {
      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns listOf(a1, a2)

      viewModel.refreshUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals(u1, s.user)
      Assert.assertTrue(s.isUserOwner)
      Assert.assertEquals(listOf(a1, a2), s.achievements)
    }
  }

  @Test
  fun refreshUIState_ownerFalse_whenUidDiffersFromCurrent() {
    mainDispatcherRule.runTest {
      viewModel =
          ProfileScreenViewModel(
              userRepository = userRepository,
              achievementRepository = achievementsRepository,
              currentUserId = { "someone-else" },
              uid = "uid-1")

      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns emptyList()

      viewModel.refreshUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals(u1, s.user)
      Assert.assertFalse(s.isUserOwner)
      Assert.assertTrue(s.achievements.isEmpty())
    }
  }

  @Test
  fun refreshUIState_whenUserRepoThrows_fallsBackToDefaultUser() {
    mainDispatcherRule.runTest {
      coEvery { userRepository.getUser("uid-1") } throws RuntimeException("boom")
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns listOf(a1)

      viewModel.refreshUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertNotNull(s.user)
      Assert.assertEquals("defaultUserId", s.user!!.userId)
      Assert.assertEquals(listOf(a1), s.achievements)
    }
  }

  @Test
  fun refreshUIState_whenAchievementsRepoThrows_fallsBackToEmptyList() {
    mainDispatcherRule.runTest {
      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } throws
          RuntimeException("x")

      viewModel.refreshUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals(u1, s.user)
      Assert.assertTrue(s.achievements.isEmpty())
    }
  }

  @Test
  fun refreshUIState_multipleCalls_updatesWithLatestData() {
    mainDispatcherRule.runTest {
      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns listOf(a1)
      viewModel.refreshUIState()
      advanceUntilIdle()

      coEvery { userRepository.getUser("uid-1") } returns u2
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns listOf(a2)
      viewModel.refreshUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals(u2, s.user)
      Assert.assertEquals(listOf(a2), s.achievements)
      Assert.assertTrue(s.isUserOwner)
    }
  }
}
