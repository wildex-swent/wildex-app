package com.android.wildex.ui.post

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.social.Comment
import com.android.wildex.model.social.CommentsRepository
import com.android.wildex.model.social.Like
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
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
    val animalId: Id = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val authorId: Id = "",
    val authorUsername: String = "",
    val authorProfilePictureURL: URL = "",
    val commentsUI: List<CommentWithAuthorUI> = emptyList(),
    val currentUserId: Id = "",
    val currentUserProfilePictureURL: URL = "",
    val likedByCurrentUser: Boolean = false,
    val errorMsg: String? = null
)

data class CommentWithAuthorUI(
    val authorId: Id = "",
    val authorProfilePictureUrl: String = "",
    val authorUserName: String = "",
    val text: String = "",
    val date: String = ""
)

class PostDetailsScreenViewModel(
    private val postRepository: PostsRepository = RepositoryProvider.postRepository,
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val commentRepository: CommentsRepository,
    private val likeRepository: LikeRepository,
    private val currentUserId: () -> String? = { Firebase.auth.currentUser?.uid ?: "" }
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

        //        val post = testPost // To test UI
        //        val simpleAuthor = testPostSimpleAuthor // To test UI
        //        val comments = testComments // Temporary until commentRepository is implemented
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
                animalId = post.animalId,
                likesCount = post.likesCount,
                commentsCount = post.commentsCount,
                authorId = post.authorId,
                authorUsername = simpleAuthor.username,
                authorProfilePictureURL = simpleAuthor.profilePictureURL,
                commentsUI =
                    try {
                      commentsToCommentsUI(comments)
                    } catch (e: Exception) {
                      Log.e("PostDetailsViewModel", "Error loading comments for post id $postId", e)
                      setErrorMsg("Failed to load comments: ${e.message}")
                      emptyList()
                    },
                currentUserId = currentUserId() ?: "",
                currentUserProfilePictureURL =
                    try {
                      userRepository.getSimpleUser(currentUserId() ?: "").profilePictureURL
                    } catch (e: Exception) {
                      Log.e("PostDetailsViewModel", "Error loading current user data", e)
                      setErrorMsg("Failed to load user data: ${e.message}")
                      ""
                    },
                likedByCurrentUser =
                    try {
                      likeRepository.getLikeForPost(postId) != null
                    } catch (e: Exception) {
                      Log.e("PostDetailsViewModel", "Error loading current user like", e)
                      setErrorMsg("Failed to load user like: ${e.message}")
                      false
                    },
                errorMsg = null)
      } catch (e: Exception) {
        Log.e("PostDetailsViewModel", "Error loading post details by post id $postId", e)
        setErrorMsg("Failed to load post details: ${e.message}")
      }
    }
  }

  fun addLike() {
    viewModelScope.launch {
      val postId = _uiState.value.postId
      try {
        val post = postRepository.getPost(postId)
        postRepository.editPost(
            postId = postId, newValue = post.copy(likesCount = post.likesCount + 1))
      } catch (e: Exception) {
        Log.e("PostDetailsViewModel", "Error loading post by post id $postId", e)
        setErrorMsg("Failed to load post : ${e.message}")
      }
      try {
        val likeId = likeRepository.getNewLikeId()
        likeRepository.addLike(
            Like(likeId = likeId, postId = postId, userId = currentUserId() ?: ""))
      } catch (e: Exception) {
        Log.e("PostDetailsViewModel", "Error handling post likes by post id $postId", e)
        setErrorMsg("Failed to handle post likes: ${e.message}")
      }
    }
  }

  fun removeLike() {
    viewModelScope.launch {
      val postId = _uiState.value.postId
      try {
        val post = postRepository.getPost(postId)
        postRepository.editPost(
            postId = postId, newValue = post.copy(likesCount = post.likesCount - 1))
      } catch (e: Exception) {
        Log.e("PostDetailsViewModel", "Error loading post by post id $postId", e)
        setErrorMsg("Failed to load post : ${e.message}")
      }
      try {
        val likeId =
            likeRepository.getLikeForPost(postId = postId)?.likeId
                ?: throw Exception("Like not found")
        likeRepository.deleteLike(likeId)
      } catch (e: Exception) {
        Log.e("PostDetailsViewModel", "Error handling post likes by post id $postId", e)
        setErrorMsg("Failed to handle post likes: ${e.message}")
      }
    }
  }

  fun addComment(text: String = "") {
    viewModelScope.launch {
      val postId = _uiState.value.postId
      try {
        val post = postRepository.getPost(postId)
        postRepository.editPost(
            postId = postId, newValue = post.copy(commentsCount = post.commentsCount + 1))
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
                authorId = currentUserId() ?: "",
                text = text,
                date = Timestamp.now()))
      } catch (e: Exception) {
        Log.e("PostDetailsViewModel", "Error adding comment to post id $postId", e)
        setErrorMsg("Failed to add comment: ${e.message}")
      }
    }
  }

  private suspend fun commentsToCommentsUI(comments: List<Comment>): List<CommentWithAuthorUI> {
    return comments.map { comment ->
      val author = userRepository.getSimpleUser(comment.authorId)
      // val author = testCommentsAuthor
      CommentWithAuthorUI(
          authorId = author.userId,
          authorProfilePictureUrl = author.profilePictureURL,
          authorUserName = author.username,
          text = comment.text,
          date =
              comment.date.let {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                return@let dateFormat.format(comment.date.toDate())
              })
    }
  }

  val testPost =
      Post(
          postId = "post1",
          authorId = "poster1",
          pictureURL = "https://upload.wikimedia.org/wikipedia/commons/5/56/Tiger.50.jpg",
          location = Location(37.7749, -122.4194, "India"),
          description = "Saw this beautiful tiger during my trip!",
          date = Timestamp.now(),
          animalId = "Tiger",
          likesCount = 42,
          commentsCount = 2)

  val testPostSimpleAuthor =
      SimpleUser(
          userId = "poster1",
          username = "tiger_lover",
          profilePictureURL =
              "https://vectorportal.com/storage/d5YN3OWWLMAJMqMZZJsITZT6bUniD0mbd2HGVNkB.jpg")
  val testCommentsAuthor =
      SimpleUser(
          userId = "commentAuthor1",
          username = "joe34",
          profilePictureURL =
              "https://vectorportal.com/storage/KIygRdXXMVXBs09f42hJ4VWOYVZIX9WdhOJP7Rf4.jpg")
  val testComments =
      listOf(
          Comment(
              commentId = "comment1",
              postId = "post1",
              authorId = "commentAuthor1",
              text = "Great post!",
              date = Timestamp.now()),
          Comment(
              commentId = "comment2",
              postId = "post1",
              authorId = "commentAuthor1",
              text = "Thanks for sharing!",
              date = Timestamp.now()),
          Comment(
              commentId = "comment3",
              postId = "post1",
              authorId = "commentAuthor1",
              text = "It's beautiful!",
              date = Timestamp.now()),
          Comment(
              commentId = "comment4",
              postId = "post1",
              authorId = "commentAuthor1",
              text = "Would love to see it in person.",
              date = Timestamp.now()),
      )
}
