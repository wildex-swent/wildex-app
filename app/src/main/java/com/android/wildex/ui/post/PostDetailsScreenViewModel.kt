package com.android.wildex.ui.post

import androidx.lifecycle.ViewModel
import com.android.wildex.model.social.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PostDetailsUIState(
    val post: Post? = null,
    val errorMsg: String? = null
)

class PostDetailsScreenViewModel() : ViewModel() {
    private val _uiState = MutableStateFlow(PostDetailsUIState())
    val uiState: StateFlow<PostDetailsUIState> = _uiState.asStateFlow()


}