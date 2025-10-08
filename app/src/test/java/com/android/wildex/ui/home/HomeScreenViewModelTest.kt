package com.android.wildex.ui.home

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

class HomeScreenViewModelTest {
    private lateinit var viewModel: HomeScreenViewModel

    @Before
    fun setUp() {
        viewModel = HomeScreenViewModel()
    }
    @After
    fun tearDown() {}
    @Test
    fun testExample() {
        assertNotNull(viewModel.uiState.value)
    }
    @Test
    fun `viewModel initializes default UI state`(){
        val initialState = viewModel.uiState.value
        assertEquals(emptyList<com.android.wildex.model.social.Post>(), initialState.posts)
        assertNull(initialState.user)
        assertEquals(false, initialState.notif)
        assertNull(initialState.errorMsg)
        assertEquals(false, initialState.signedOut)
    }
}