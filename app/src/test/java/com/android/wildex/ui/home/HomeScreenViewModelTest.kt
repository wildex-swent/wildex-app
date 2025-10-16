// kotlin
package com.android.wildex.ui.home

import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Location
import com.android.wildex.utils.MainDispatcherRule // <- ajouter cet import
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.Calendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenViewModelTest {
  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var postsRepository: PostsRepository
  private lateinit var userRepository: UserRepository
  private lateinit var viewModel: HomeScreenViewModel

  private val p1 =
      Post(
          postId = "p1",
          authorId = "author1",
          pictureURL = "url1",
          location = Location(0.0, 0.0),
          description = "d1",
          date = Timestamp(Calendar.getInstance().time),
          animalId = "a1",
          likesCount = 1,
          commentsCount = 0)

  private val p2 =
      Post(
          postId = "p2",
          authorId = "author2",
          pictureURL = "url2",
          location = Location(1.0, 1.0),
          description = "d2",
          date = Timestamp(Calendar.getInstance().time),
          animalId = "a2",
          likesCount = 2,
          commentsCount = 1)

  private val u1 =
      User(
          userId = "uid-1",
          username = "user_one",
          name = "First",
          surname = "User",
          bio = "bio",
          profilePictureURL = "u",
          userType = UserType.REGULAR,
          creationDate = Timestamp(Calendar.getInstance().time),
          country = "X",
          friendsCount = 0)

  @Before
  fun setUp() {
    postsRepository = mockk()
    userRepository = mockk()
    viewModel = HomeScreenViewModel(postsRepository, userRepository) { "uid-1" }
  }

  @After fun tearDown() {}

  @Test
  fun viewModel_initializes_default_UI_state() {
    val initialState = viewModel.uiState.value
    Assert.assertTrue(initialState.posts.isEmpty())
    Assert.assertNull(initialState.user)
    Assert.assertFalse(initialState.notif)
  }

  @Test
  fun refreshUIState_updates_UI_state_success() {
    mainDispatcherRule.runTest {
      coEvery { postsRepository.getAllPosts() } returns listOf(p1, p2)
      coEvery { userRepository.getUser("uid-1") } returns u1

      viewModel.refreshUIState()
      advanceUntilIdle()

      val updatedState = viewModel.uiState.value
      Assert.assertEquals(listOf(p1, p2), updatedState.posts)
      Assert.assertEquals(u1, updatedState.user)
      Assert.assertFalse(updatedState.notif)
    }
  }

  @Test
  fun refreshUIState_whenCurrentUserNull_usesDefaultUser_and_doesNotCallUserRepo() {
    mainDispatcherRule.runTest {
      coEvery { postsRepository.getAllPosts() } returns listOf(p1)

      viewModel = HomeScreenViewModel(postsRepository, userRepository) { null }

      viewModel.refreshUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals(listOf(p1), s.posts)
      Assert.assertNotNull(s.user)
      Assert.assertEquals("defaultUserId", s.user!!.userId)
      Assert.assertEquals("defaultUsername", s.user!!.username)
      Assert.assertFalse(s.notif)
      coVerify(exactly = 0) { userRepository.getUser(any()) }
    }
  }

  @Test
  fun refreshUIState_whenUserRepoThrows_fallsBackToDefaultUser() {
    mainDispatcherRule.runTest {
      coEvery { postsRepository.getAllPosts() } returns listOf(p1, p2)
      coEvery { userRepository.getUser("uid-1") } throws RuntimeException("boom")

      viewModel.refreshUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals(listOf(p1, p2), s.posts)
      Assert.assertNotNull(s.user)
      Assert.assertEquals("defaultUserId", s.user!!.userId)
      Assert.assertFalse(s.notif)
    }
  }

  @Test
  fun refreshUIState_whenPostsRepoThrows_keepsPreviousState() {
    mainDispatcherRule.runTest {
      coEvery { postsRepository.getAllPosts() } returns listOf(p1)
      coEvery { userRepository.getUser("uid-1") } returns u1
      viewModel.refreshUIState()
      advanceUntilIdle()
      val s1 = viewModel.uiState.value

      coEvery { postsRepository.getAllPosts() } throws RuntimeException("fail")
      viewModel.refreshUIState()
      advanceUntilIdle()
      val s2 = viewModel.uiState.value

      Assert.assertEquals(s1, s2)
    }
  }

  @Test
  fun refreshUIState_multipleCalls_updatesWithLatestData() {
    mainDispatcherRule.runTest {
      coEvery { postsRepository.getAllPosts() } returns listOf(p1)
      coEvery { userRepository.getUser("uid-1") } returns u1
      viewModel.refreshUIState()
      advanceUntilIdle()

      val u2 = u1.copy(username = "user_one_2", friendsCount = 99)
      coEvery { postsRepository.getAllPosts() } returns listOf(p2)
      coEvery { userRepository.getUser("uid-1") } returns u2
      viewModel.refreshUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals(listOf(p2), s.posts)
      Assert.assertEquals(u2, s.user)
      Assert.assertFalse(s.notif)
    }
  }

  @Test
  fun homeUIState_defaultValues_areCorrect() {
    val s = HomeUIState()
    Assert.assertTrue(s.posts.isEmpty())
    Assert.assertNull(s.user)
    Assert.assertFalse(s.notif)
  }
}
