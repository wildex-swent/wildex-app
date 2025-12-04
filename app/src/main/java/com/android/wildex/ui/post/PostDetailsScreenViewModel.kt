package com.android.wildex.ui.post

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.social.Comment
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.CommentTag
import com.android.wildex.model.social.Like
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PostDetailsUIState(
    val postId: Id = "",
    val pictureURL: URL = "",
    val location: String = "",
    val description: String = "",
    val date: String = "",
    val animalName: String = "",
    val animalSpecies: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val authorId: Id = "",
    val authorUsername: String = "",
    val authorProfilePictureURL: URL = "",
    val authorUserType: UserType = UserType.REGULAR,
    val commentsUI: List<CommentWithAuthorUI> = emptyList(),
    val currentUserId: Id = "",
    val currentUserProfilePictureURL: URL = "",
    val currentUserUsername: String = "",
    val currentUserUserType: UserType = UserType.REGULAR,
    val likedByCurrentUser: Boolean = false,
    val errorMsg: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isError: Boolean = false,
)

data class CommentWithAuthorUI(
    val commentId: Id = "",
    val authorId: Id = "",
    val authorProfilePictureUrl: String = "",
    val authorUserName: String = "",
    val authorUserType: UserType = UserType.REGULAR,
    val text: String = "",
    val date: String = "",
)

class PostDetailsScreenViewModel(
    private val postRepository: PostsRepository = RepositoryProvider.postRepository,
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val commentRepository: CommentRepository = RepositoryProvider.commentRepository,
    private val animalRepository: AnimalRepository = RepositoryProvider.animalRepository,
    private val likeRepository: LikeRepository = RepositoryProvider.likeRepository,
    private val userAnimalsRepository: UserAnimalsRepository =
        RepositoryProvider.userAnimalsRepository,
    private val currentUserId: Id = Firebase.auth.uid ?: "",
) : ViewModel() {

  private val _uiState = MutableStateFlow(PostDetailsUIState())
  val uiState: StateFlow<PostDetailsUIState> = _uiState.asStateFlow()

  /** In-flight guard to avoid double-tap spamming like/unlike. */
  @Volatile private var likeInFlight = false

  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  fun refreshPostDetails(postId: Id) {
    _uiState.value = _uiState.value.copy(isRefreshing = true, errorMsg = null, isError = false)
    viewModelScope.launch { updatePostDetails(postId) }
  }

  fun loadPostDetails(postId: Id) {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null, isError = false)
    viewModelScope.launch { updatePostDetails(postId) }
  }

  private suspend fun updatePostDetails(postId: Id) {
    try {
      val post = postRepository.getPost(postId)
      val simpleAuthor = userRepository.getSimpleUser(post.authorId)
      val comments = commentRepository.getAllCommentsByPost(postId).sortedByDescending { it.date }
      val animal = animalRepository.getAnimal(post.animalId)
      val currentUser = userRepository.getSimpleUser(currentUserId)

      var localErrorMsg: String? = null
      val commentsUI =
          try {
            commentsToCommentsUI(comments)
          } catch (e: Exception) {
            Log.e("PostDetailsViewModel", "Error loading comments for post id $postId", e)
            localErrorMsg = "Failed to load comments: ${e.message}"
            emptyList()
          }

      val likedByCurrentUser =
          try {
            likeRepository.getLikeForPost(postId) != null
          } catch (e: Exception) {
            Log.e("PostDetailsViewModel", "Error loading current user like", e)
            if (localErrorMsg == null) localErrorMsg = "Failed to load user like: ${e.message}"
            false
          }
      val likeCount = likeRepository.getLikesForPost(postId).size
      val commentCount = comments.size

      _uiState.value =
          PostDetailsUIState(
              postId = postId,
              pictureURL = post.pictureURL,
              location = post.location?.name ?: "",
              description = post.description,
              date = formatDate(post.date),
              likesCount = likeCount,
              commentsCount = commentCount,
              authorId = post.authorId,
              authorUsername = simpleAuthor.username,
              authorProfilePictureURL = simpleAuthor.profilePictureURL,
              authorUserType = simpleAuthor.userType,
              animalName = animal.name,
              animalSpecies = animal.species,
              commentsUI = commentsUI,
              currentUserId = currentUserId,
              currentUserProfilePictureURL = currentUser.profilePictureURL,
              currentUserUsername = currentUser.username,
              currentUserUserType = currentUser.userType,
              likedByCurrentUser = likedByCurrentUser,
              errorMsg = localErrorMsg,
              isLoading = false,
              isRefreshing = false,
              isError = false,
          )
    } catch (e: Exception) {
      Log.e("PostDetailsViewModel", "Error loading post details by post id $postId", e)
      setErrorMsg("Failed to load post details: ${e.message}")
      _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false, isError = true)
    }
  }

  fun addLike() {
    if (likeInFlight || _uiState.value.likedByCurrentUser) return
    likeInFlight = true

    // Optimistic UI
    applyOptimisticLike(liked = true)

    viewModelScope.launch {
      val postId = _uiState.value.postId
      val rollback = {
        _uiState.value =
            _uiState.value.copy(
                likedByCurrentUser = false,
                likesCount = (_uiState.value.likesCount - 1).coerceAtLeast(0),
            )
      }
      try {
        val likeId = likeRepository.getNewLikeId()
        likeRepository.addLike(Like(likeId = likeId, postId = postId, userId = currentUserId))
      } catch (e: Exception) {
        handleException("Add Like failed for $postId", e)
        rollback()
      } finally {
        likeInFlight = false
      }
    }
  }

  fun removeLike() {
    if (likeInFlight || !_uiState.value.likedByCurrentUser) return
    likeInFlight = true

    // Optimistic UI
    applyOptimisticLike(liked = false)

    viewModelScope.launch {
      val postId = _uiState.value.postId
      val rollback = {
        _uiState.value =
            _uiState.value.copy(
                likedByCurrentUser = true,
                likesCount = _uiState.value.likesCount + 1,
            )
      }
      try {
        val like =
            likeRepository.getLikeForPost(postId) ?: throw IllegalStateException("Like not found")
        likeRepository.deleteLike(like.likeId)
      } catch (e: Exception) {
        handleException("Remove Like failed for $postId", e)
        rollback()
      } finally {
        likeInFlight = false
      }
    }
  }

  /** Optimistic comment add with rollback on failure. */
  fun addComment(text: String = "") {
    if (text.isBlank()) return

    viewModelScope.launch {
      val postId = _uiState.value.postId
      val now = Timestamp.now()
      val formattedNow = formatDate(now)

      // Build optimistic UI comment
      val currentPfp = _uiState.value.currentUserProfilePictureURL
      val username = _uiState.value.currentUserUsername
      val userType = _uiState.value.currentUserUserType

      val optimistic =
          CommentWithAuthorUI(
              authorId = currentUserId,
              authorProfilePictureUrl = currentPfp,
              authorUserName = username,
              authorUserType = userType,
              text = text,
              date = formattedNow,
          )

      // 1) Optimistically prepend comment + bump count
      val before = _uiState.value
      _uiState.value =
          before.copy(
              commentsUI = listOf(optimistic) + before.commentsUI,
              commentsCount = before.commentsCount + 1,
          )

      try {
        // 2) Persist comment
        val commentId = commentRepository.getNewCommentId()
        commentRepository.addComment(
            Comment(
                commentId = commentId,
                parentId = postId,
                authorId = currentUserId,
                text = text,
                date = now,
                tag = CommentTag.POST_COMMENT))
        updatePostDetails(postId)
      } catch (e: Exception) {
        // Rollback UI
        val current = _uiState.value
        _uiState.value =
            current.copy(
                commentsUI = current.commentsUI.drop(1),
                commentsCount = (current.commentsCount - 1).coerceAtLeast(0),
            )
        handleException("Error adding comment to post id $postId", e)
      }
    }
  }

  /** Optimistic comment remove with rollback on failure. */
  fun removeComment(commentId: Id) {
    viewModelScope.launch {
      val postId = _uiState.value.postId

      // 1) Optimistically remove comment + decrease count
      val before = _uiState.value
      val newCommentsUI = before.commentsUI.filterNot { it.commentId == commentId }
      _uiState.value =
          before.copy(
              commentsUI = newCommentsUI,
              commentsCount = before.commentsCount - 1,
          )

      try {
        // 2) Remove comment
        commentRepository.deleteComment(commentId)
      } catch (e: Exception) {
        // Rollback UI
        val current = _uiState.value
        _uiState.value =
            current.copy(
                commentsUI = before.commentsUI,
                commentsCount = before.commentsCount,
            )
        handleException("Error deleting comment id $commentId to post id $postId", e)
      }
    }
  }

  /** Deletes a post and associated items */
  fun removePost(postId: Id) {
    viewModelScope.launch {
      try {
        val animalId = postRepository.getPost(postId).animalId
        postRepository.deletePost(postId)
        commentRepository.deleteAllCommentsOfPost(postId)
        likeRepository.deleteAllLikesOfPost(postId)
        userAnimalsRepository.deleteAnimalToUserAnimals(_uiState.value.authorId, animalId)
      } catch (e: Exception) {
        handleException("Failed to delete post $postId", e)
      }
    }
  }

  /** Optimistically update like state in UI. */
  private fun applyOptimisticLike(liked: Boolean) {
    _uiState.value =
        _uiState.value.copy(
            likedByCurrentUser = liked,
            likesCount = (_uiState.value.likesCount + if (liked) 1 else -1).coerceAtLeast(0),
        )
  }

  private fun formatDate(ts: Timestamp): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return dateFormat.format(ts.toDate())
  }

  private suspend fun commentsToCommentsUI(comments: List<Comment>): List<CommentWithAuthorUI> {
    return comments.map { comment ->
      val author = userRepository.getSimpleUser(comment.authorId)
      CommentWithAuthorUI(
          commentId = comment.commentId,
          authorId = author.userId,
          authorProfilePictureUrl = author.profilePictureURL,
          authorUserName = author.username,
          authorUserType = author.userType,
          text = comment.text,
          date = formatDate(comment.date),
      )
    }
  }

  /**
   * Centralized error handler for fatal errors.
   *
   * @param message High-level description of where the error happened.
   * @param e The exception that was thrown.
   */
  private fun handleException(message: String, e: Exception) {
    Log.e("PostDetailsScreenViewModel", message, e)
    setErrorMsg("$message: ${e.message}")
    _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false, isError = true)
  }
}
