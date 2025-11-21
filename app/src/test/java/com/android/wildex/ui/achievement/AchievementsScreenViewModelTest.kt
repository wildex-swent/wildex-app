package com.android.wildex.ui.achievement

import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.google.firebase.Timestamp
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
  private lateinit var userRepository: UserRepository

  private lateinit var repository: UserAchievementsRepository
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
          friendsCount = 3)

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
          friendsCount = 3)

  private val su1 =
      SimpleUser(
          userId = "currentUserId",
          username = "currentUsername",
          profilePictureURL =
              "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
          userType = UserType.REGULAR,
      )

  private val su2 =
      SimpleUser(
          userId = "otherUserId",
          username = "otherUsername",
          profilePictureURL =
              "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
          userType = UserType.REGULAR)

  private val dummyUnlocked = listOf(postMaster, communityBuilder, firstPost)
  private val dummyLocked = listOf(conversationalist, mockAchievement1, mockAchievement2)
  private val achievements = dummyUnlocked + dummyLocked

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = mockk()
    userRepository = mockk()
    viewModel = AchievementsScreenViewModel(repository)
    coEvery { userRepository.getUser("currentUserId") } returns u1
    coEvery { userRepository.getUser("otherUserId") } returns u2
    coEvery { userRepository.getSimpleUser("currentUserId") } returns su1
    coEvery { userRepository.getSimpleUser("otherUserId") } returns su2
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
      coEvery { repository.getAllAchievementsByUser(any()) } returns dummyUnlocked
      coEvery { repository.getAllAchievements() } returns achievements

      viewModel.loadUIState("currentUserId")
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
      coEvery { repository.getAllAchievementsByUser(any()) } returns dummyUnlocked
      coEvery { repository.getAllAchievements() } returns achievements

      // Read the current (initial) state synchronously.
      val initialState = viewModel.uiState.value
      assertTrue(initialState.isLoading)

      // Trigger the load and advance the scheduler to let emissions happen.
      viewModel.loadUIState("currentUserId")
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
      coEvery { repository.getAllAchievementsByUser(any()) } throws Exception("Network error")

      viewModel.loadUIState("currentUserId")
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
      coEvery { repository.getAllAchievementsByUser(any()) } returns dummyUnlocked
      coEvery { repository.getAllAchievements() } returns achievements

      viewModel.loadUIState("currentUserId")
      advanceUntilIdle()

      val loadedState = viewModel.uiState.value
      assertFalse(loadedState.isLoading)
      assertEquals(dummyUnlocked, loadedState.unlocked)
      assertEquals(dummyLocked, loadedState.locked)
      assertFalse(loadedState.isError)
      assertNull(loadedState.errorMsg)
    }
  }
}
