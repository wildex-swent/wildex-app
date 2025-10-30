package com.android.wildex.ui.achievement

import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.InputKey
import com.android.wildex.model.achievement.UserAchievementsRepository
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlin.collections.listOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementsScreenViewModelTest {
  private lateinit var repository: UserAchievementsRepository
  private lateinit var viewModel: AchievementsScreenViewModel
  private val testDispatcher = StandardTestDispatcher()

  private val postMaster =
      Achievement(
          achievementId = "achievement_1",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/2583/2583343.png",
          description = "Reach 10 posts",
          name = "Post Master",
          expects = setOf(InputKey.POST_IDS),
          condition = { inputs ->
            val postIds = inputs[InputKey.POST_IDS].orEmpty()
            postIds.size >= 10
          },
      )

  private val communityBuilder =
      Achievement(
          achievementId = "achievement_3",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/1077/1077012.png",
          description = "Write 20 comments",
          name = "Community Builder",
          expects = setOf(InputKey.COMMENT_IDS),
          condition = { inputs ->
            val commentIds = inputs[InputKey.COMMENT_IDS].orEmpty()
            commentIds.size >= 20
          },
      )

  private val firstPost =
      Achievement(
          achievementId = "achievement_5",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/1828/1828961.png",
          description = "Create your first post",
          name = "First Post",
          expects = setOf(InputKey.POST_IDS),
          condition = { inputs ->
            val postIds = inputs[InputKey.POST_IDS].orEmpty()
            postIds.isNotEmpty()
          },
      )

  private val conversationalist =
      Achievement(
          achievementId = "achievement_7",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/2462/2462719.png",
          description = "Write 50 comments overall",
          name = "Conversationalist",
          expects = setOf(InputKey.COMMENT_IDS),
          condition = { inputs ->
            val commentIds = inputs[InputKey.COMMENT_IDS].orEmpty()
            commentIds.size >= 50
          },
      )

  private val mockAchievement1 =
      Achievement(
          achievementId = "mockPostId",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/616/616408.png",
          description = "This is a mock achievement for testing purposes",
          name = "Mock Achievement",
          expects = setOf(InputKey.TEST_IDS),
          condition = { inputs ->
            val testIds = inputs[InputKey.TEST_IDS].orEmpty()
            testIds.size == 1 && testIds[0] == "mockPostId"
          },
      )

  private val mockAchievement2 =
      Achievement(
          achievementId = "mockPostId2",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/4339/4339544.png",
          description = "This is another mock achievement for testing purposes",
          name = "Mock Achievement 2",
          expects = setOf(InputKey.TEST_IDS),
          condition = { inputs ->
            val testIds = inputs[InputKey.TEST_IDS].orEmpty()
            testIds.size == 2
          },
      )

  private val dummyUnlocked = listOf(postMaster, communityBuilder, firstPost)
  private val dummyLocked = listOf(conversationalist, mockAchievement1, mockAchievement2)
  private val achievements = dummyUnlocked + dummyLocked

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = mockk()
    viewModel = AchievementsScreenViewModel(repository)
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
      coEvery { repository.getAllAchievementsByCurrentUser() } returns dummyUnlocked
      coEvery { repository.getAllAchievements() } returns achievements

      viewModel.loadAchievements()
      advanceUntilIdle()

      val loadedState = viewModel.uiState.value
      assertFalse(loadedState.isLoading)
      assertEquals(dummyUnlocked, loadedState.unlocked)
      assertEquals(dummyLocked, loadedState.locked)
      assertFalse(loadedState.isError)
      assertNull(loadedState.errorMsg)
    }
  }

  @Test
  fun loadAchievementsEmitsLoadingThenLoaded() {
    runTest {
      coEvery { repository.getAllAchievementsByCurrentUser() } returns dummyUnlocked
      coEvery { repository.getAllAchievements() } returns achievements

      // Read the current (initial) state synchronously.
      val initialState = viewModel.uiState.value
      assertTrue(initialState.isLoading)

      // Trigger the load and advance the scheduler to let emissions happen.
      viewModel.loadAchievements()
      advanceUntilIdle()

      val loadedState = viewModel.uiState.value
      assertFalse(loadedState.isLoading)
      assertEquals(dummyUnlocked, loadedState.unlocked)
      assertEquals(dummyLocked, loadedState.locked)
      assertFalse(loadedState.isError)
      assertNull(loadedState.errorMsg)
    }
  }

  @Test
  fun loadAchievementsHandlesError() {
    runTest {
      coEvery { repository.getAllAchievementsByCurrentUser() } throws Exception("Network error")

      viewModel.loadAchievements()
      advanceUntilIdle()

      val errorState = viewModel.uiState.value
      assertFalse(errorState.isLoading)
      assertTrue(errorState.unlocked.isEmpty())
      assertTrue(errorState.locked.isEmpty())
      assertTrue(errorState.isError)
      assertEquals("Failed to load achievements: Network error", errorState.errorMsg)
    }
  }

  @Test
  fun loadAchievementsWithRefresh() {
    runTest {
      coEvery { repository.getAllAchievementsByCurrentUser() } returns dummyUnlocked
      coEvery { repository.getAllAchievements() } returns achievements

      viewModel.loadAchievements(refresh = true)
      advanceUntilIdle()

      val loadedState = viewModel.uiState.value
      assertFalse(loadedState.isLoading)
      assertFalse(loadedState.isRefreshing)
      assertEquals(dummyUnlocked, loadedState.unlocked)
      assertEquals(dummyLocked, loadedState.locked)
      assertFalse(loadedState.isError)
      assertNull(loadedState.errorMsg)
    }
  }
}
