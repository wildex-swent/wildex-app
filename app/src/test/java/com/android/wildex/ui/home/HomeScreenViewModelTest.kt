// kotlin
package com.android.wildex.ui.home
/*
import android.util.Log
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Location
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.Calendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenViewModelTest {
  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var postsRepository: PostsRepository
  private lateinit var userRepository: UserRepository
  private lateinit var likeRepository: LikeRepository
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
          commentsCount = 0,
      )

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
          commentsCount = 1,
      )

  private val u1 =
      SimpleUser(
          userId = "uid-1",
          username = "user_one",
          profilePictureURL = "u",
      )

  @Before
  fun setUp() {
    postsRepository = mockk()
    userRepository = mockk()
    likeRepository = mockk()
    coEvery { likeRepository.getLikeForPost(any()) } returns null
    viewModel = HomeScreenViewModel(postsRepository, userRepository, likeRepository, "uid-1")
  }

  @Test
  fun viewModel_initializes_default_UI_state() {
    val initialState = viewModel.uiState.value
    Assert.assertTrue(initialState.postStates.isEmpty())
    Assert.assertEquals(initialState.currentUser, defaultUser)
  }

  @Test
  fun refreshUIState_updates_UI_state_success() {
    mainDispatcherRule.runTest {
      coEvery { postsRepository.getAllPosts() } returns listOf(p1, p2)
      coEvery { userRepository.getSimpleUser("uid-1") } returns u1

      viewModel.refreshUIState()
      advanceUntilIdle()

      val updatedState = viewModel.uiState.value
      Assert.assertEquals(listOf(p1, p2), updatedState.postStates.map { it.post })
      Assert.assertEquals(u1, updatedState.currentUser)
    }
  }

  @Test
  fun refreshUIState_whenCurrentUserNull_usesDefaultUser_and_doesNotCallUserRepo() {
    mainDispatcherRule.runTest {
      coEvery { postsRepository.getAllPosts() } returns listOf(p1)

      viewModel = HomeScreenViewModel(postsRepository, userRepository, likeRepository)

      viewModel.refreshUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals(listOf(p1), s.postStates.map { it.post })
      Assert.assertNotNull(s.currentUser)
      Assert.assertEquals("defaultUserId", s.currentUser.userId)
      Assert.assertEquals("defaultUsername", s.currentUser.username)
      coVerify(exactly = 0) { userRepository.getSimpleUser(any()) }
    }
  }

  @Test
  fun refreshUIState_whenUserRepoThrows_fallsBackToDefaultUser() {
    mainDispatcherRule.runTest {
      coEvery { postsRepository.getAllPosts() } returns listOf(p1, p2)
      coEvery { userRepository.getSimpleUser("uid-1") } throws RuntimeException("boom")

      viewModel.refreshUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals(listOf(p1, p2), s.postStates.map { it.post })
      Assert.assertNotNull(s.currentUser)
      Assert.assertEquals("defaultUserId", s.currentUser.userId)
    }
  }

  @Test
  fun refreshUIState_whenPostsRepoThrows_keepsPreviousState() {
    mainDispatcherRule.runTest {
      coEvery { postsRepository.getAllPosts() } returns listOf(p1)
      coEvery { userRepository.getSimpleUser("uid-1") } returns u1
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
      coEvery { userRepository.getSimpleUser("uid-1") } returns u1
      viewModel.refreshUIState()
      advanceUntilIdle()

      val u2 = u1.copy(username = "user_one_2")
      coEvery { postsRepository.getAllPosts() } returns listOf(p2)
      coEvery { userRepository.getSimpleUser("uid-1") } returns u2
      viewModel.refreshUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals(listOf(p2), s.postStates.map { it.post })
      Assert.assertEquals(u2, s.currentUser)
    }
  }

  @Test
  fun homeUIState_defaultValues_areCorrect() {
    val s = HomeUIState()
    Assert.assertTrue(s.postStates.isEmpty())
    Assert.assertEquals(s.currentUser, defaultUser)
  }
}
 */
