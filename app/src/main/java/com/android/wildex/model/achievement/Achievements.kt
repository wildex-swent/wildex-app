package com.android.wildex.model.achievement

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.utils.Id
import kotlinx.coroutines.runBlocking

object Achievements {
  val postsRepository: PostsRepository = RepositoryProvider.postRepository

  // Achievement 1: "Post Master" - Achieved when user has 10 or more posts
  val postMaster =
      Achievement(
          achievementId = "achievement_1",
          pictureURL = "image_placeholder", // Replace with actual image URL
          description = "Reach 10 posts",
          name = "Post Master",
          condition = { postIds -> postIds.size >= 10 },
      )

  // Achievement 2: "Social Butterfly" - Achieved when user has liked 50 posts
  val socialButterfly =
      Achievement(
          achievementId = "achievement_2",
          pictureURL = "image_placeholder", // Replace with actual image URL
          description = "Like 50 posts",
          name = "Social Butterfly",
          condition = { postIds ->
            runBlocking {
              val likedPosts = postsRepository.getLikedPosts(postIds)
              likedPosts.size >= 50
            }
          },
      )
  // Achievement 3: "Commentator" - Achieved when user has commented on 20 different posts
  val commentator =
      Achievement(
          achievementId = "achievement_3",
          pictureURL = "image_placeholder", // Replace with actual image URL
          description = "Comment on 20 different posts",
          name = "Commentator",
          condition = { postIds ->
            runBlocking {
              val commentedPosts = postsRepository.getCommentedPosts(postIds)
              commentedPosts.size >= 20
            }
          },
      )

  // Achievement 4: "Influencer" - Achieved when user has 1000 likes across all their posts
  val influencer =
      Achievement(
          achievementId = "achievement_4",
          pictureURL = "image_placeholder", // Replace with actual image URL
          description = "Get 1000 likes across all your posts",
          name = "Influencer",
          condition = { postIds ->
            runBlocking {
              val totalLikes = postsRepository.getLikesCount(postIds)
              totalLikes >= 1000
            }
          },
      )

  // List of all achievements
  val ALL = listOf(postMaster, socialButterfly, commentator, influencer)

  // ----------- Helpers ----------------
  private suspend fun PostsRepository.getLikedPosts(postIds: List<Id>): List<Post> {
    val posts = postIds.map { getPost(it) }
    return posts.filter { it.likesCount > 0 }
  }

  private suspend fun PostsRepository.getCommentedPosts(postIds: List<Id>): List<Post> {
    val posts = postIds.map { getPost(it) }
    return posts.filter { it.commentsCount > 0 }
  }

  private suspend fun PostsRepository.getLikesCount(postIds: List<Id>): Int {
    val posts = postIds.map { getPost(it) }
    return posts.sumOf { it.likesCount }
  }
}
