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

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  fun loadPostDetails(postId: String) {
    viewModelScope.launch {
      try {

        val post = postRepository.getPost(postId)
        val simpleAuthor = userRepository.getSimpleUser(post.authorId)
        val comments = commentRepository.getAllCommentsByPost(postId)

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
                date =
                    post.date.let {
                      val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                      return@let dateFormat.format(post.date.toDate())
                    },
                animalName = post.animalId, // TODO replace with animal name once repository is up
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

  fun addLike() {
    viewModelScope
        .launch {
          val postId = _uiState.value.postId
          try {
            val post = postRepository.getPost(postId)
            postRepository.editPost(
                postId = postId,
                newValue = post.copy(likesCount = post.likesCount + 1),
            )
          } catch (e: Exception) {
            Log.e("PostDetailsViewModel", "Error loading post by post id $postId", e)
            setErrorMsg("Failed to load post : ${e.message}")
          }
          try {
            val likeId = likeRepository.getNewLikeId()
            likeRepository.addLike(Like(likeId = likeId, postId = postId, userId = currentUserId))
          } catch (e: Exception) {
            Log.e("PostDetailsViewModel", "Error handling post likes by post id $postId", e)
            setErrorMsg("Failed to handle post likes: ${e.message}")
          }
        }
        .invokeOnCompletion { loadPostDetails(_uiState.value.postId) }
  }

  fun removeLike() {
    viewModelScope
        .launch {
          val postId = _uiState.value.postId

          try {
            val likeId =
                likeRepository.getLikeForPost(postId = postId)?.likeId
                    ?: throw Exception("Like not found")
            likeRepository.deleteLike(likeId)
          } catch (e: Exception) {
            Log.e("PostDetailsViewModel", "Error handling post likes by post id $postId", e)
            setErrorMsg("Failed to handle post likes: ${e.message}")
            return@launch
          }
          try {
            val post = postRepository.getPost(postId)
            postRepository.editPost(
                postId = postId,
                newValue = post.copy(likesCount = post.likesCount - 1),
            )
          } catch (e: Exception) {
            Log.e("PostDetailsViewModel", "Error loading post by post id $postId", e)
            setErrorMsg("Failed to load post : ${e.message}")
          }
        }
        .invokeOnCompletion { loadPostDetails(_uiState.value.postId) }
  }

  fun addComment(text: String = "") {
    viewModelScope
        .launch {
          val postId = _uiState.value.postId
          try {
            val post = postRepository.getPost(postId)
            postRepository.editPost(
                postId = postId,
                newValue = post.copy(commentsCount = post.commentsCount + 1),
            )
          } catch (e: Exception) {
            Log.e("PostDetailsViewModel", "Error loading post by post id $postId", e)
            setErrorMsg("Failed to load post : ${e.message}")
          }
          try {
            val commentId = commentRepository.getNewCommentId()
            commentRepository.addComment(
                Comment(
                    commentId = commentId,
                    postId = postId,
                    authorId = currentUserId,
                    text = text,
                    date = Timestamp.now(),
                )
            )
          } catch (e: Exception) {
            Log.e("PostDetailsViewModel", "Error adding comment to post id $postId", e)
            setErrorMsg("Failed to add comment: ${e.message}")
          }
        }
        .invokeOnCompletion { loadPostDetails(_uiState.value.postId) }
  }

  private suspend fun commentsToCommentsUI(comments: List<Comment>): List<CommentWithAuthorUI> {
    return comments.map { comment ->
      val author = userRepository.getSimpleUser(comment.authorId)
      CommentWithAuthorUI(
          authorId = author.userId,
          authorProfilePictureUrl = author.profilePictureURL,
          authorUserName = author.username,
          text = comment.text,
          date =
              comment.date.let {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                return@let dateFormat.format(comment.date.toDate())
              },
      )
    }
  }
}
