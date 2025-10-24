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
          friendsCount = 1,
      )

  private val u2 = u1.copy(username = "user_one_2", friendsCount = 42)

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
            currentUserId = "uid-1",
        )
  }

  @Test
  fun viewModel_initializes_default_UI_state_isEmptyLoading() {
    val s = viewModel.uiState.value
    Assert.assertNull(s.user)
    Assert.assertFalse(s.isUserOwner)
    Assert.assertTrue(s.isLoading)
    Assert.assertTrue(s.achievements.isEmpty())
    Assert.assertNull(s.errorMsg)
  }

  @Test
  fun refreshUIState_owner_true_and_false_paths() {
    mainDispatcherRule.runTest {
      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns listOf(a1, a2)

      viewModel.refreshUIState("uid-1")
      advanceUntilIdle()
      val s1 = viewModel.uiState.value
      Assert.assertEquals(u1, s1.user)
      Assert.assertTrue(s1.isUserOwner)
      Assert.assertEquals(listOf(a1, a2), s1.achievements)

      viewModel =
          ProfileScreenViewModel(
              userRepository = userRepository,
              achievementRepository = achievementsRepository,
              currentUserId = "someone-else",
          )
      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns emptyList()

      viewModel.refreshUIState("uid-1")
      advanceUntilIdle()
      val s2 = viewModel.uiState.value
      Assert.assertEquals(u1, s2.user)
      Assert.assertFalse(s2.isUserOwner)
      Assert.assertTrue(s2.achievements.isEmpty())
    }
  }

  @Test
  fun refreshUIState_error_paths_userRepo_then_achievementsRepo() {
    mainDispatcherRule.runTest {
      coEvery { userRepository.getUser("uid-1") } throws RuntimeException("boom")
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns listOf(a1)

      viewModel.refreshUIState("uid-1")
      advanceUntilIdle()
      val e1 = viewModel.uiState.value
      Assert.assertNull(e1.user)
      Assert.assertEquals(false, e1.isUserOwner)
      Assert.assertEquals("Unexpected error: boom", e1.errorMsg)

      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } throws
          RuntimeException("x")

      viewModel.refreshUIState("uid-1")
      advanceUntilIdle()
      val e2 = viewModel.uiState.value
      Assert.assertEquals(u1, e2.user)
      Assert.assertTrue(e2.achievements.isEmpty())
      Assert.assertEquals("x", e2.errorMsg)
    }
  }

  @Test
  fun refreshUIState_multipleCalls_updatesWithLatestData() {
    mainDispatcherRule.runTest {
      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns listOf(a1)
      viewModel.refreshUIState("uid-1")
      advanceUntilIdle()

      coEvery { userRepository.getUser("uid-1") } returns u2
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns listOf(a2)
      viewModel.refreshUIState("uid-1")
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals(u2, s.user)
      Assert.assertEquals(listOf(a2), s.achievements)
      Assert.assertTrue(s.isUserOwner)
    }
  }

  @Test
  fun refreshUIState_withBlankUserId_setsErrorAndStopsLoading_and_clearErrorMsg() {
    mainDispatcherRule.runTest {
      viewModel.refreshUIState("")
      advanceUntilIdle()
      val s = viewModel.uiState.value
      Assert.assertEquals("Empty user id", s.errorMsg)
      Assert.assertFalse(s.isLoading)
      Assert.assertNull(s.user)
      Assert.assertFalse(s.isUserOwner)

      viewModel.clearErrorMsg()
      Assert.assertNull(viewModel.uiState.value.errorMsg)
    }
  }

  @Test
  fun refreshUIState_whenUnexpectedErrorAfterFetch_hitsOuterCatch() {
    mainDispatcherRule.runTest {
      val badUser = mockk<User>(relaxed = true)
      io.mockk.every { badUser.userId } throws RuntimeException("kaboom")
      coEvery { userRepository.getUser("uid-1") } returns badUser
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns emptyList()

      viewModel.refreshUIState("uid-1")
      advanceUntilIdle()
      val s = viewModel.uiState.value
      Assert.assertTrue(s.errorMsg?.startsWith("Unexpected error: kaboom") == true)
      Assert.assertFalse(s.isLoading)
    }
  }
}
