package com.android.wildex.ui.home

import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.utils.FirestoreTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenViewModelTest {
  private lateinit var postsRepository: PostsRepository
  private lateinit var userRepository: UserRepository
  private lateinit var viewModel: HomeScreenViewModel

  @Before
  fun setUp() {
    postsRepository = mockk()
    userRepository = mockk()
    viewModel = HomeScreenViewModel(postsRepository, userRepository)
  }

  @After fun tearDown() {}

  @Test
  fun viewModel_initializes_default_UI_state() {
    val initialState = viewModel.uiState.value
    assertEquals(emptyList<Post>(), initialState.posts)
    assertNull(initialState.user)
    assertEquals(false, initialState.notif)
  }

  @Test
  fun refreshUIState_updates_UI_state() {
    val testData = FirestoreTest("testCollection")
    coEvery { postsRepository.getAllPostsByAuthor() } returns listOf(testData.post1, testData.post2)
    coEvery { userRepository.getUser(any()) } returns testData.user1

    viewModel.refreshUIState()
    val updatedState = viewModel.uiState.value

    assertEquals(listOf(testData.post1, testData.post2), updatedState.posts)
    assertEquals(testData.user1, updatedState.user)
    assertEquals(false, updatedState.notif)
  }
}
