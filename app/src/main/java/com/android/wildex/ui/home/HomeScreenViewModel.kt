package com.android.wildex.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUIState(
    val posts: List<Post> = emptyList(),
    val user: User? = null,
    val notif: Boolean = false,
)

class HomeScreenViewModel(
    private val postRepository: PostsRepository = RepositoryProvider.postRepository,
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUIState())

  private val defaultUser: User =
      User(
          userId = "defaultUserId",
          username = "defaultUsername",
          name = "Default",
          surname = "User",
          bio = "This is...",
          profilePictureURL = "https://example.com/default-profile-pic.png",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "Nowhere",
          friendsCount = 0)

  val uiState: StateFlow<HomeUIState> = _uiState.asStateFlow()
    private var authorId: String? = ""

  /** Refreshes the UI state by fetching all Post items from the repository. */
  fun refreshUIState() {
    getAllPosts()
  }
  /** Fetches user based on login */
  private fun fetchUser(): User? {
    var user: User? = null
    authorId = com.google.firebase.ktx.Firebase.auth.currentUser?.uid

    viewModelScope.launch {
      try {
        user = if (authorId != null) userRepository.getUser(authorId!!) else defaultUser
      } catch (e: Exception) {
        Log.e("HomeScreenViewModel", "Error fetching user", e)
      }
    }
    return user
  }
  /** Fetches all Posts from the repository and updates the UI state. */
  private fun getAllPosts() {
    viewModelScope.launch {
      try {
        /** Pull posts from repository and update UI state */
        _uiState.value =
            HomeUIState(
                posts = postRepository.getAllPosts(),
                user = fetchUser(),
                notif = hasNotif())
      } catch (e: Exception) {
        Log.e("HomeScreenViewModel", "Error fetching posts", e)
      }
    }
  }

  private fun hasNotif(): Boolean {
    // TODO: implement notification check
    return false
  }
}
