package com.android.wildex.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.String

data class HomeUIState(
    val posts: List<Post> = emptyList(),
    val user: User? = null,
    val notif: Boolean = false,
    val errorMsg: String? = null,
    val signedOut: Boolean = false
)
class HomeScreenViewModel(
    //private val todoRepository: ToDosRepository = ToDosRepositoryProvider.repository,
    //private val authRepository: AuthRepository = AuthRepositoryFirebase(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUIState())
    val uiState: StateFlow<HomeUIState> = _uiState.asStateFlow()

    /** Refreshes the UI state by fetching all Post items from the repository. */
    fun refreshUIState() {
        getAllPosts()
    }
    /** Fetches user based on login */
    private fun fetchUser(): User? {
        var user: User? = null
        //viewModelScope.launch {
            try {
                //TODO: implement fetching user
                /** _uiState.user = "fetchUserFromUserId(...)" */
            } catch (e: Exception) {
                Log.e("HomeScreenViewModel", "Error fetching user", e)
                //setErrorMsg("Failed to load user: ${e.message}")
            }
        //}
        return user
    }
    /** Fetches all Posts from the repository and updates the UI state. */
    private fun getAllPosts() {
        //viewModelScope.launch {
            try {
                //TODO: implement fetching posts
                /** Pull posts from repository and update UI state */
                //val posts = postRepository.getAllPosts()
                //_uiState.value = OverviewUIState(todos = todos)
                _uiState.value = HomeUIState(
                    posts = emptyList(),
                    user = fetchUser(),
                    notif = hasNotif()
                )
            } catch (e: Exception) {
                Log.e("HomeScreenViewModel", "Error fetching posts", e)
                //setErrorMsg("Failed to load todos: ${e.message}")
            }
        //}
    }
    private fun hasNotif() : Boolean {
        //TODO: implement notification check
        return false
    }
}