package com.android.wildex.ui.home

/**
 * HomeScreenViewModel.kt
 *
 * Provides data and state management for the Wildex Home Screen. Fetches posts, user information,
 * and manages like interactions.
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.AppTheme
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.Like
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserSettingsRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the UI state of the Home Screen.
 *
 * @property postStates List of post states currently displayed on screen.
 * @property currentUser The currently authenticated user.
 * @property isLoading Indicates whether the screen is currently refreshing data.
 * @property errorMsg Optional error message to display if refresh fails.
 */
data class HomeUIState(
    val postStates: List<PostState> = emptyList(),
    val currentUser: SimpleUser = defaultUser,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMsg: String? = null,
    val isError: Boolean = false,
)

/** Default placeholder user used when no valid user is loaded. */
private val defaultUser: SimpleUser =
    SimpleUser(
        userId = "defaultUserId",
        username = "defaultUsername",
        profilePictureURL = "",
        userType = UserType.REGULAR,
    )

/**
 * Combines post data with associated metadata for display.
 *
 * @property post The base Post object.
 * @property isLiked Indicates whether the current user has liked this post.
 * @property author Simplified user information for the post author.
 * @property animalName The animal referenced in the post.
 */
data class PostState(
    val post: Post,
    val isLiked: Boolean,
    val author: SimpleUser,
    val animalName: String,
    val likeCount: Int,
    val commentsCount: Int,
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
    private val commentRepository: CommentRepository = RepositoryProvider.commentRepository,
    private val animalRepository: AnimalRepository = RepositoryProvider.animalRepository,
    private val userSettingsRepository: UserSettingsRepository =
        RepositoryProvider.userSettingsRepository,
    private val currentUserId: Id = Firebase.auth.uid ?: "",
) : ViewModel() {

  /** Backing property for the home screen state. */
  private val _uiState = MutableStateFlow(HomeUIState())

  /** Public immutable state exposed to the UI layer. */
  val uiState: StateFlow<HomeUIState> = _uiState.asStateFlow()

  /**
   * Loads the UI state by fetching posts and the current user. Updates [_uiState] with new values.
   *
   * Also manages [HomeUIState.isLoading], [HomeUIState.isRefreshing], [HomeUIState.isError] and
   * [HomeUIState.errorMsg] to allow UI feedback.
   */
  private suspend fun updateUIState() {
    try {
      AppTheme.appearanceMode = userSettingsRepository.getAppearanceMode(currentUserId)
      val postStates = fetchPosts()
      val user = userRepository.getSimpleUser(currentUserId)
      _uiState.value =
          _uiState.value.copy(
              currentUser = user,
              postStates = postStates,
              isRefreshing = false,
              isLoading = false,
              errorMsg = null,
              isError = false,
          )
    } catch (e: Exception) {
      setErrorMsg(e.localizedMessage ?: "Failed to load posts.")
      _uiState.value = _uiState.value.copy(isRefreshing = false, isLoading = false, isError = true)
    }
  }

  fun loadUIState() {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState() }
  }

  fun refreshUIState() {
    _uiState.value = _uiState.value.copy(isRefreshing = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState() }
  }

  /**
   * Retrieves posts and converts them to [PostState] objects including like status and author data.
   */
  private suspend fun fetchPosts(): List<PostState> = coroutineScope {
    postRepository
        .getAllPosts()
        .map { post ->
          async {
            try {
              val author = userRepository.getSimpleUser(post.authorId)
              val isLiked = likeRepository.getLikeForPost(post.postId) != null
              val animalName = animalRepository.getAnimal(post.animalId).name
              val likeCount = likeRepository.getLikesForPost(post.postId).size
              val commentCount = commentRepository.getAllCommentsByPost(post.postId).size

              PostState(
                  post = post,
                  author = author,
                  isLiked = isLiked,
                  animalName = animalName,
                  likeCount = likeCount,
                  commentsCount = commentCount,
              )
            } catch (_: Exception) {
              null
            }
          }
        }
        .mapNotNull { it.await() }
  }

  /**
   * Handles like/unlike logic for a specific post.
   * - If the post is already liked by the current user, it removes the like.
   * - Otherwise, it creates and adds a new like entry.
   *
   * @param postId The unique identifier of the post to toggle like status.
   */
  fun toggleLike(postId: Id) {
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

  /** Clears any existing error message from the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets a new error message in the UI state. */
  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }
}
