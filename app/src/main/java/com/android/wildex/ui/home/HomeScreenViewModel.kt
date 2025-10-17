package com.android.wildex.ui.home

/**
 * HomeScreenViewModel.kt
 *
 * Provides data and state management for the Wildex Home Screen. Fetches posts, user information,
 * and manages like interactions.
 */
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.social.Like
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Id
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the UI state of the Home Screen.
 *
 * @property posts List of post states currently displayed on screen.
 * @property currentUser The currently authenticated user.
 */
data class HomeUIState(
    val posts: List<PostState> = emptyList(),
    val currentUser: SimpleUser = defaultUser,
)

/** Default placeholder user used when no valid user is loaded. */
private val defaultUser: SimpleUser =
    SimpleUser(
        userId = "defaultUserId",
        username = "defaultUsername",
        profilePictureURL =
            "https://cdn.expertphotography.com/wp-content/uploads/2020/08/" +
                "social-media-profile-photos.jpg",
    )

/** Default placeholder animal used when no valid animal is associated with a post. */
private val defaultAnimal: Animal =
    Animal(
        animalId = "defaultAnimalId",
        name = "Default Animal",
        species = "Unknown",
        description = "This is a default animal.",
        pictureURL =
            "https://www.publicdomainpictures.net/pictures/320000/velka/background-image.png",
    )

/**
 * Combines post data with associated metadata for display.
 *
 * @property post The base Post object.
 * @property isLiked Indicates whether the current user has liked this post.
 * @property author Simplified user information for the post author.
 * @property animal The animal referenced in the post.
 */
data class PostState(
    val post: Post,
    val isLiked: Boolean = false,
    val author: SimpleUser = defaultUser,
    val animal: Animal = defaultAnimal,
)

/**
 * ViewModel for managing the home screen UI state and user interactions.
 *
 * Responsible for:
 * - Loading posts and user data.
 * - Tracking like states.
 * - Handling like/unlike operations.
 *
 * @param postRepository Repository for accessing posts.
 * @param userRepository Repository for accessing user data.
 * @param likeRepository Repository for managing likes.
 * @param currentUserId ID of the currently authenticated user.
 */
class HomeScreenViewModel(
    private val postRepository: PostsRepository = RepositoryProvider.postRepository,
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val likeRepository: LikeRepository = RepositoryProvider.likeRepository,
    private val currentUserId: Id = Firebase.auth.uid!!,
) : ViewModel() {

  /** Backing property for the home screen state. */
  private val _uiState = MutableStateFlow(HomeUIState())

  /** Public immutable state exposed to the UI layer. */
  val uiState: StateFlow<HomeUIState> = _uiState.asStateFlow()

  /**
   * Refreshes the UI state by fetching posts and the current user. Updates [_uiState] with new
   * values.
   */
  fun refreshUIState() {
    viewModelScope.launch {
      try {
        val posts = fetchPosts()
        val user = fetchUser()
        _uiState.value = HomeUIState(currentUser = user, posts = posts)
        Log.d("HomeScreenViewModel", "UI state refreshed with ${posts.size} posts.")
      } catch (e: Exception) {
        Log.e("HomeScreenViewModel", "Error refreshing UI state", e)
      }
    }
  }

  /**
   * Retrieves posts and converts them to [PostState] objects including like status and author data.
   */
  private suspend fun fetchPosts(): List<PostState> =
      postRepository.getAllPosts().map { post ->
        PostState(
            post = post,
            isLiked = likeRepository.getLikeForPost(post.postId) != null,
            author = userRepository.getSimpleUser(post.authorId),
        )
      }

  /** Fetches the current authenticated user’s information. */
  private suspend fun fetchUser(): SimpleUser = userRepository.getSimpleUser(currentUserId)

  /**
   * Handles like/unlike logic for a specific post.
   * - If the post is already liked by the current user, it removes the like.
   * - Otherwise, it creates and adds a new like entry.
   *
   * @param postId The unique identifier of the post to toggle like status.
   */
  fun handleLike(postId: Id) {
    viewModelScope.launch {
      val like = likeRepository.getLikeForPost(postId)
      if (like != null) {
        likeRepository.deleteLike(like.likeId)
      } else {
        val newLike =
            Like(
                likeId = likeRepository.getNewLikeId(),
                postId = postId,
                userId = currentUserId,
            )
        likeRepository.addLike(newLike)
      }
    }
  }
}
