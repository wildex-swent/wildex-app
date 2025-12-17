package com.android.wildex.ui.achievement

import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.UserAchievementsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.collections.listOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementsScreenViewModelTest {
  private lateinit var userAchievementsRepository: UserAchievementsRepository
  private lateinit var currentUserId: String

  private lateinit var viewModel: AchievementsScreenViewModel
  private val testDispatcher = StandardTestDispatcher()

  private val postMaster =
      Achievement(
          achievementId = "achievement_1",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/2583/2583343.png",
          description = "Reach 10 posts",
          name = "Post Master",
      )

  private val communityBuilder =
      Achievement(
          achievementId = "achievement_3",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/1077/1077012.png",
          description = "Write 20 comments",
          name = "Community Builder",
      )

  private val firstPost =
      Achievement(
          achievementId = "achievement_5",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/1828/1828961.png",
          description = "Create your first post",
          name = "First Post",
      )

  private val conversationalist =
      Achievement(
          achievementId = "achievement_7",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/2462/2462719.png",
          description = "Write 50 comments overall",
          name = "Conversationalist",
      )

  private val mockAchievement1 =
      Achievement(
          achievementId = "mockPostId",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/616/616408.png",
          description = "This is a mock achievement for testing purposes",
          name = "Mock Achievement",
      )

  private val mockAchievement2 =
      Achievement(
          achievementId = "mockPostId2",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/4339/4339544.png",
          description = "This is another mock achievement for testing purposes",
          name = "Mock Achievement 2",
      )
  private val dummyUnlocked = listOf(postMaster, communityBuilder, firstPost)
  private val dummyLocked = listOf(conversationalist, mockAchievement1, mockAchievement2)
  private val achievements = dummyUnlocked + dummyLocked

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    currentUserId = "currentUserId"
    userAchievementsRepository = mockk()
    viewModel =
        AchievementsScreenViewModel(
            userAchievementsRepository,
            testDispatcher,
            testDispatcher,
        )
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initialStateIsLoading() {
    runTest {
      val state = viewModel.uiState.value
      assertTrue(state.isLoading)
      assertTrue(state.unlocked.isEmpty())
      assertTrue(state.locked.isEmpty())
      assertFalse(state.isError)
      assertNull(state.errorMsg)
    }
  }

  @Test
  fun loadAchievementsFinalState() {
    runTest {
      coEvery { userAchievementsRepository.getAllAchievementsByUser(any()) } returns dummyUnlocked
      coEvery { userAchievementsRepository.getAllAchievements() } returns achievements

      viewModel.loadUIState(currentUserId)
      advanceUntilIdle()

      val loadedState = viewModel.uiState.value
      assertFalse(loadedState.isLoading)
      val unlockedAchievementUIStates =
          dummyUnlocked.map { achievement ->
            AchievementUIState(
                id = achievement.achievementId,
                name = achievement.name,
                description = achievement.description,
                pictureURL = achievement.pictureURL,
                progress = achievement.progress(currentUserId),
            )
          }

      val lockedAchievementUIStates =
          dummyLocked.map { achievement ->
            AchievementUIState(
                id = achievement.achievementId,
                name = achievement.name,
                description = achievement.description,
                pictureURL = achievement.pictureURL,
                progress = achievement.progress(currentUserId),
            )
          }
      assertEquals(unlockedAchievementUIStates, loadedState.unlocked)
      assertEquals(lockedAchievementUIStates, loadedState.locked)
      assertFalse(loadedState.isError)
      assertNull(loadedState.errorMsg)
    }
  }

  @Test
  fun loadAchievementsEmitsLoadingThenLoaded() {
    runTest {
      coEvery { userAchievementsRepository.getAllAchievementsByUser(any()) } returns dummyUnlocked
      coEvery { userAchievementsRepository.getAllAchievements() } returns achievements

      // Read the current (initial) state synchronously.
      val initialState = viewModel.uiState.value
      assertTrue(initialState.isLoading)

      // Trigger the load and advance the scheduler to let emissions happen.
      viewModel.loadUIState(currentUserId)
      advanceUntilIdle()

      val loadedState = viewModel.uiState.value
      assertFalse(loadedState.isLoading)
      val unlockedAchievementUIStates =
          dummyUnlocked.map { achievement ->
            AchievementUIState(
                id = achievement.achievementId,
                name = achievement.name,
                description = achievement.description,
                pictureURL = achievement.pictureURL,
                progress = achievement.progress(currentUserId),
            )
          }

      val lockedAchievementUIStates =
          dummyLocked.map { achievement ->
            AchievementUIState(
                id = achievement.achievementId,
                name = achievement.name,
                description = achievement.description,
                pictureURL = achievement.pictureURL,
                progress = achievement.progress(currentUserId),
            )
          }
      assertEquals(unlockedAchievementUIStates, loadedState.unlocked)
      assertEquals(lockedAchievementUIStates, loadedState.locked)
      assertFalse(loadedState.isError)
      assertNull(loadedState.errorMsg)
    }
  }

  @Test
  fun loadAchievementsHandlesError() {
    runTest {
      coEvery { userAchievementsRepository.getAllAchievementsByUser(any()) } throws
          Exception("Network error")

      viewModel.loadUIState(currentUserId)
      advanceUntilIdle()

      val errorState = viewModel.uiState.value
      assertFalse(errorState.isLoading)
      assertTrue(errorState.unlocked.isEmpty())
      assertTrue(errorState.locked.isEmpty())
      assertTrue(errorState.isError)
    }
  }

  @Test
  fun loadAchievementsWithRefresh() {
    runTest {
      coEvery { userAchievementsRepository.getAllAchievementsByUser(any()) } returns dummyUnlocked
      coEvery { userAchievementsRepository.getAllAchievements() } returns achievements

      viewModel.loadUIState(currentUserId)
      advanceUntilIdle()

      val loadedState = viewModel.uiState.value
      assertFalse(loadedState.isLoading)
      val unlockedAchievementUIStates =
          dummyUnlocked.map { achievement ->
            AchievementUIState(
                id = achievement.achievementId,
                name = achievement.name,
                description = achievement.description,
                pictureURL = achievement.pictureURL,
                progress = achievement.progress(currentUserId),
            )
          }

      val lockedAchievementUIStates =
          dummyLocked.map { achievement ->
            AchievementUIState(
                id = achievement.achievementId,
                name = achievement.name,
                description = achievement.description,
                pictureURL = achievement.pictureURL,
                progress = achievement.progress(currentUserId),
            )
          }
      assertEquals(unlockedAchievementUIStates, loadedState.unlocked)
      assertEquals(lockedAchievementUIStates, loadedState.locked)
      assertFalse(loadedState.isError)
      assertNull(loadedState.errorMsg)
    }
  }
}
