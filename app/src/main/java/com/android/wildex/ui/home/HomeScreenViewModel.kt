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
import com.google.firebase.Timestamp
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
    private val currentUserId: () -> String? = {
      com.google.firebase.ktx.Firebase.auth.currentUser?.uid
    },
) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUIState())
  val uiState: StateFlow<HomeUIState> = _uiState.asStateFlow()

  private val defaultUser: User =
      User(
          userId = "defaultUserId",
          username = "defaultUsername",
          name = "Default",
          surname = "User",
          bio = "This is...",
          profilePictureURL =
              "https://cdn.expertphotography.com/wp-content/uploads/2020/08/" +
                  "social-media-profile-photos.jpg",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "Nowhere",
          friendsCount = 0)

  /** Refreshes the UI state by fetching all Post items from the repository. */
  fun refreshUIState() {
    viewModelScope.launch {
      try {
        val posts = postRepository.getAllPosts()
        val user = fetchUser()
        _uiState.value = HomeUIState(posts = posts, user = user, notif = hasNotif())
      } catch (e: Exception) {
        Log.e("HomeScreenViewModel", "Error refreshing UI state", e)
      }
    }
  }
  /** Fetches user based on login */
  private suspend fun fetchUser(): User? {
    return try {
      val id = currentUserId()
      if (id != null) userRepository.getUser(id) else defaultUser
    } catch (e: Exception) {
      Log.e("HomeScreenViewModel", "Error fetching user", e)
      defaultUser
    }
  }

  private fun hasNotif(): Boolean {
    // TODO: implement notification check
    return false
  }
}
