package com.android.wildex.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenViewModelTest {
    private lateinit var viewModel: HomeScreenViewModel

    @Before
    fun setUp() {
        viewModel = HomeScreenViewModel()
    }
    @After
    fun tearDown() {}
    @Test
    fun `viewModel initializes default UI state`(){
        val initialState = viewModel.uiState.value
        assertEquals(emptyList<com.android.wildex.model.social.Post>(), initialState.posts)
        assertNull(initialState.user)
        assertEquals(false, initialState.notif)
        assertNull(initialState.errorMsg)
        assertEquals(false, initialState.signedOut)
    }
    @Test
    fun `refreshUIState updates UI state`(){
        viewModel.refreshUIState()
        val updatedState = viewModel.uiState.value
        assertEquals(emptyList<com.android.wildex.model.social.Post>(), updatedState.posts)
        assertNull(updatedState.user)
        assertEquals(false, updatedState.notif)
        assertNull(updatedState.errorMsg)
        assertEquals(false, updatedState.signedOut)
    }
}