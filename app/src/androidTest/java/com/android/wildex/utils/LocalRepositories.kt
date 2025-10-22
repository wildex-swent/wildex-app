package com.android.wildex.utils

import com.android.wildex.model.social.Comment
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.Like
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Id

interface ClearableRepository {
  fun clear()
}

object LocalRepositories {

  class PostsRepositoryImpl(private val currentUserId: Id = "currentUserId-1") :
    PostsRepository, ClearableRepository {
    val listOfPosts = mutableListOf<Post>()

    init {
      clear()
    }

    override fun getNewPostId(): String = "newPostId"

    override suspend fun getAllPosts(): List<Post> = listOfPosts

    override suspend fun getAllPostsByAuthor(): List<Post> =
      listOfPosts.filter { it.authorId == currentUserId }

    override suspend fun getAllPostsByGivenAuthor(authorId: Id): List<Post> =
      listOfPosts.filter { it.authorId == authorId }

    override suspend fun getPost(postId: Id): Post = listOfPosts.find { it.postId == postId }!!

    override suspend fun addPost(post: Post) {
      listOfPosts.add(post)
    }

    override suspend fun editPost(postId: Id, newValue: Post) {
      listOfPosts.removeIf { it.postId == postId }
      listOfPosts.add(newValue)
    }

    override suspend fun deletePost(postId: Id) {
      listOfPosts.removeIf { it.postId == postId }
    }

    override fun clear() {
      listOfPosts.clear()
    }
  }

  class LikeRepositoryImpl(private val currentUserId: Id = "currentUserId-1") :
    LikeRepository, ClearableRepository {
    val listOfLikes = mutableListOf<Like>()

    init {
      clear()
    }

    override fun getNewLikeId(): String = "newLikeId"

    override suspend fun getAllLikesByCurrentUser(): List<Like> =
      listOfLikes.filter { it.userId == currentUserId }

    override suspend fun getLikesForPost(postId: String): List<Like> =
      listOfLikes.filter { it.postId == postId }

    override suspend fun getLikeForPost(postId: String): Like? =
      listOfLikes.find { it.postId == postId && it.userId == currentUserId }

    override suspend fun addLike(like: Like) {
      listOfLikes.add(like)
    }

    override suspend fun deleteLike(likeId: String) {
      listOfLikes.removeIf { it.likeId == likeId }
    }

    override fun clear() {
      listOfLikes.clear()
    }
  }

  class UserRepositoryImpl(private val currentUserId: Id = "currentUserId-1") :
    UserRepository, ClearableRepository {
    val listOfUsers = mutableListOf<User>()

    init {
      clear()
    }

    override fun getNewUid(): String = "newUserId"

    override suspend fun getUser(userId: Id): User = listOfUsers.find { it.userId == userId }!!

    override suspend fun getSimpleUser(userId: Id): SimpleUser {
      val user = listOfUsers.find { it.userId == userId }!!
      return SimpleUser(
        userId = user.userId,
        username = user.username,
        profilePictureURL = user.profilePictureURL,
      )
    }

    override suspend fun addUser(user: User) {
      listOfUsers.add(user)
    }

    override suspend fun editUser(userId: Id, newUser: User) {
      listOfUsers.removeIf { it.userId == userId }
      listOfUsers.add(newUser)
    }

    override suspend fun deleteUser(userId: Id) {
      listOfUsers.removeIf { it.userId == userId }
    }

    override fun clear() {
      listOfUsers.clear()
    }
  }

  class CommentRepositoryImpl(private val currentUserId: Id = "currentUserId-1") :
    CommentRepository, ClearableRepository {
    val listOfComments = mutableListOf<Comment>()

    init {
      clear()
    }

    override fun getNewCommentId(): String = "newCommentId"

    override suspend fun getAllCommentsByPost(postId: String): List<Comment> =
      listOfComments.filter { it.postId == postId }

    override suspend fun addComment(comment: Comment) {
      listOfComments.add(comment)
    }

    override suspend fun editComment(commentId: String, newValue: Comment) {
      listOfComments.removeIf { it.commentId == commentId }
      listOfComments.add(newValue)
    }

    override suspend fun deleteComment(commentId: String) {
      listOfComments.removeIf { it.commentId == commentId }
    }

    override fun clear() {
      listOfComments.clear()
    }
  }

  val postsRepository: PostsRepository = PostsRepositoryImpl()
  val likeRepository: LikeRepository = LikeRepositoryImpl()
  val userRepository: UserRepository = UserRepositoryImpl()
  val commentRepository: CommentRepository = CommentRepositoryImpl()

  fun clearAll() {
    (postsRepository as ClearableRepository).clear()
    (likeRepository as ClearableRepository).clear()
    (userRepository as ClearableRepository).clear()
    (commentRepository as ClearableRepository).clear()
  }
}
