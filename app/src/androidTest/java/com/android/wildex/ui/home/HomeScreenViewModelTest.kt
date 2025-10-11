package com.android.wildex.ui.home

import com.android.wildex.model.social.Post
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenViewModelTest {
  private lateinit var viewModel: HomeScreenViewModel

  @Before
  fun setUp() {
    viewModel = HomeScreenViewModel()
  }

  @After fun tearDown() {}

  @Test
  fun viewModel_initializes_default_UI_state() {
    val initialState = viewModel.uiState.value
    Assert.assertEquals(emptyList<Post>(), initialState.posts)
    Assert.assertNull(initialState.user)
    Assert.assertEquals(false, initialState.notif)
    Assert.assertNull(initialState.errorMsg)
    Assert.assertEquals(false, initialState.signedOut)
  }

  @Test
  fun refreshUIState_updates_UI_state() {
    viewModel.refreshUIState()
    val updatedState = viewModel.uiState.value
    Assert.assertEquals(emptyList<Post>(), updatedState.posts)
    Assert.assertNotNull(updatedState.user)
    Assert.assertEquals(false, updatedState.notif)
    Assert.assertNull(updatedState.errorMsg)
    Assert.assertEquals(false, updatedState.signedOut)
  }
}
