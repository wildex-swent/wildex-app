package com.android.wildex.utils

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

  val postsRepository: PostsRepository =
      object : PostsRepository, ClearableRepository {
        val listOfPosts = mutableListOf<Post>()

        init {
          listOfPosts.clear()
        }

        override fun getNewPostId(): String = "newPostId"

        override suspend fun getAllPosts(): List<Post> = listOfPosts

        override suspend fun getAllPostsByAuthor(): List<Post> = listOfPosts

        override suspend fun getAllPostsByGivenAuthor(authorId: Id): List<Post> =
            listOfPosts.filter { it.authorId == authorId }

        override suspend fun getPost(postId: Id): Post = listOfPosts.first()

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

  val likeRepository: LikeRepository =
      object : LikeRepository, ClearableRepository {
        val listOfLikes = mutableListOf<Like>()

        init {
          listOfLikes.clear()
        }

        override fun getNewLikeId(): String = "newLikeId"

        override suspend fun getAllLikesByCurrentUser(): List<Like> {
          return listOfLikes
        }

        override suspend fun getLikesForPost(postId: String): List<Like> =
            listOfLikes.filter { it.postId == postId }

        override suspend fun getLikeForPost(postId: String): Like? =
            listOfLikes.find { it.postId == postId }

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

  val userRepository: UserRepository =
      object : UserRepository, ClearableRepository {
        val listOfUsers = mutableListOf<User>()

        init {
          listOfUsers.clear()
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

  fun clearAll() {
    (postsRepository as ClearableRepository).clear()
    (likeRepository as ClearableRepository).clear()
    (userRepository as ClearableRepository).clear()
  }
}
