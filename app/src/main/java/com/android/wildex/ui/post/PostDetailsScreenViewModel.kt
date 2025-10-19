package com.android.wildex.ui.post

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.social.Comment
import com.android.wildex.model.social.CommentsRepository
import com.android.wildex.model.social.Like
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.UserRepository
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
    val animalName: Id = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val authorId: Id = "",
    val authorUsername: String = "",
    val authorProfilePictureURL: URL = "",
    val commentsUI: List<CommentWithAuthorUI> = emptyList(),
    val currentUserId: Id = "",
    val currentUserProfilePictureURL: URL = "",
    val likedByCurrentUser: Boolean = false,
    val errorMsg: String? = null,
)

data class CommentWithAuthorUI(
    val authorId: Id = "",
    val authorProfilePictureUrl: String = "",
    val authorUserName: String = "",
    val text: String = "",
    val date: String = "",
)

class PostDetailsScreenViewModel(
    private val postRepository: PostsRepository = RepositoryProvider.postRepository,
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val commentRepository: CommentsRepository = RepositoryProvider.commentRepository,
    private val likeRepository: LikeRepository = RepositoryProvider.likeRepository,
    private val currentUserId: Id = Firebase.auth.uid!!,
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

  fun loadPostDetails(postId: String) {
    viewModelScope.launch {
      try {
        val post = postRepository.getPost(postId)
        val simpleAuthor = userRepository.getSimpleUser(post.authorId)
        val comments = commentRepository.getAllCommentsByPost(postId).sortedByDescending { it.date }

        var localErrorMsg: String? = null
        val commentsUI =
            try {
              commentsToCommentsUI(comments)
            } catch (e: Exception) {
              Log.e("PostDetailsViewModel", "Error loading comments for post id $postId", e)
              localErrorMsg = "Failed to load comments: ${e.message}"
              emptyList()
            }

        val currentUserProfilePictureURL =
            try {
              userRepository.getSimpleUser(currentUserId).profilePictureURL
            } catch (e: Exception) {
              Log.e("PostDetailsViewModel", "Error loading current user data", e)
              if (localErrorMsg == null) localErrorMsg = "Failed to load user data: ${e.message}"
              ""
            }

        val likedByCurrentUser =
            try {
              likeRepository.getLikeForPost(postId) != null
            } catch (e: Exception) {
              Log.e("PostDetailsViewModel", "Error loading current user like", e)
              if (localErrorMsg == null) localErrorMsg = "Failed to load user like: ${e.message}"
              false
            }

        _uiState.value =
            PostDetailsUIState(
                postId = postId,
                pictureURL = post.pictureURL,
                location = post.location?.name ?: "",
                description = post.description,
                date = formatDate(post.date),
                animalName = post.animalId, // TODO: replace with actual animal name when available
                likesCount = post.likesCount,
                commentsCount = post.commentsCount,
                authorId = post.authorId,
                authorUsername = simpleAuthor.username,
                authorProfilePictureURL = simpleAuthor.profilePictureURL,
                commentsUI = commentsUI,
                currentUserId = currentUserId,
                currentUserProfilePictureURL = currentUserProfilePictureURL,
                likedByCurrentUser = likedByCurrentUser,
                errorMsg = localErrorMsg,
            )
      } catch (e: Exception) {
        Log.e("PostDetailsViewModel", "Error loading post details by post id $postId", e)
        setErrorMsg("Failed to load post details: ${e.message}")
      }
    }
  }

  /** Optimistically update like state in UI. */
  private fun applyOptimisticLike(liked: Boolean) {
    _uiState.value =
        _uiState.value.copy(
            likedByCurrentUser = liked,
            likesCount = (_uiState.value.likesCount + if (liked) 1 else -1).coerceAtLeast(0))
  }

  /** Try to increment likes with a single repo call (recommended). */
  private suspend fun safeIncrementLikes(postId: Id, delta: Int) {
    try {
      postRepository.incrementLikes(postId, delta)
    } catch (e: IllegalArgumentException) {
      Log.w("PostDetailsViewModel", "Post not found for incrementLikes")
    } catch (e: UnsupportedOperationException) {
      // If not implemented in your repo, you can add a fallback here if needed.
      Log.w("PostDetailsViewModel", "incrementLikes not implemented in repository")
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
                likesCount = (_uiState.value.likesCount - 1).coerceAtLeast(0))
      }
      try {
        val likeId = likeRepository.getNewLikeId()
        likeRepository.addLike(Like(likeId = likeId, postId = postId, userId = currentUserId))
        safeIncrementLikes(postId, +1)
      } catch (e: Exception) {
        Log.e("PostDetailsViewModel", "addLike failed for $postId", e)
        setErrorMsg("Failed to like. Check your connection.")
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
                likedByCurrentUser = true, likesCount = _uiState.value.likesCount + 1)
      }
      try {
        val like =
            likeRepository.getLikeForPost(postId) ?: throw IllegalStateException("Like not found")
        likeRepository.deleteLike(like.likeId)
        safeIncrementLikes(postId, -1)
      } catch (e: Exception) {
        Log.e("PostDetailsViewModel", "removeLike failed for $postId", e)
        setErrorMsg("Failed to remove like. Try again.")
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
      val username =
          try {
            userRepository.getSimpleUser(currentUserId).username
          } catch (e: Exception) {
            Log.w(
                "PostDetailsViewModel", "Could not fetch current username quickly; using fallback")
            "You"
          }

      val optimistic =
          CommentWithAuthorUI(
              authorId = currentUserId,
              authorProfilePictureUrl = currentPfp,
              authorUserName = username,
              text = text,
              date = formattedNow,
          )

      // 1) Optimistically prepend comment + bump count
      val before = _uiState.value
      _uiState.value =
          before.copy(
              commentsUI = listOf(optimistic) + before.commentsUI,
              commentsCount = before.commentsCount + 1)

      try {
        // 2) Persist comment
        val commentId = commentRepository.getNewCommentId()
        commentRepository.addComment(
            Comment(
                commentId = commentId,
                postId = postId,
                authorId = currentUserId,
                text = text,
                date = now,
            ))

        // 3) Update count in the post (optional to keep server in sync)
        try {
          val post = postRepository.getPost(postId)
          postRepository.editPost(
              postId = postId,
              newValue = post.copy(commentsCount = post.commentsCount + 1),
          )
        } catch (e: Exception) {
          // Not fatal for UI; server count may lag until next refresh
          Log.w(
              "PostDetailsViewModel",
              "Failed to increment post commentsCount on server: ${e.message}")
        }
      } catch (e: Exception) {
        // Rollback UI
        Log.e("PostDetailsViewModel", "Error adding comment to post id $postId", e)
        setErrorMsg("Failed to add comment: ${e.message}")
        // remove the optimistic one and restore count
        val current = _uiState.value
        _uiState.value =
            current.copy(
                commentsUI = current.commentsUI.drop(1),
                commentsCount = (current.commentsCount - 1).coerceAtLeast(0))
      }
    }
  }

  private fun formatDate(ts: Timestamp): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return dateFormat.format(ts.toDate())
  }

  private suspend fun commentsToCommentsUI(comments: List<Comment>): List<CommentWithAuthorUI> {
    return comments.map { comment ->
      val author = userRepository.getSimpleUser(comment.authorId)
      CommentWithAuthorUI(
          authorId = author.userId,
          authorProfilePictureUrl = author.profilePictureURL,
          authorUserName = author.username,
          text = comment.text,
          date = formatDate(comment.date),
      )
    }
  }
}
