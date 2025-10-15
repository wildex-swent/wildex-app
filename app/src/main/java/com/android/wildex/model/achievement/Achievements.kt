package com.android.wildex.model.achievement

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.utils.Id

object Achievements {
  private val postsRepository: PostsRepository = RepositoryProvider.postRepository

  // Achievement 1: "Post Master" - Achieved when user has 10 or more posts
  val postMaster =
      Achievement(
          achievementId = "achievement_1",
          pictureURL = "image_placeholder", // Replace with actual image URL
          description = "Reach 10 posts",
          name = "Post Master",
          condition = { postIds -> postIds.size >= 10 },
      )

  // Achievement 2: "Social Butterfly" - Achieved when user has 50 liked posts
  val socialButterfly =
      Achievement(
          achievementId = "achievement_2",
          pictureURL = "image_placeholder", // Replace with actual image URL
          description = "Reach 50 liked posts",
          name = "Social Butterfly",
          condition = { postIds ->
            val likedPosts = postsRepository.getLikedPosts(postIds)
            likedPosts.size >= 50
          },
      )

  // Achievement 3: "Community Builder" - Achieved when user has 20 commented Posts
  val communityBuilder =
      Achievement(
          achievementId = "achievement_3",
          pictureURL = "image_placeholder", // Replace with actual image URL
          description = "Reach 20 commented posts",
          name = "Community Builder",
          condition = { postIds ->
            val commentedPosts = postsRepository.getCommentedPosts(postIds)
            commentedPosts.size >= 20
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
            val totalLikes = postsRepository.getLikesCount(postIds)
            totalLikes >= 1000
          },
      )

  // Mock Achievement for testing purposes, DO NOT DELETE OTHERWISE THE TESTS WILL FAIL
  val mockAchievement1 =
      Achievement(
          achievementId = "mockPostId",
          pictureURL = "image_placeholder",
          description = "This is a mock achievement for testing purposes",
          name = "Mock Achievement",
          condition = { postIds -> postIds.size == 1 && postIds[0] == "mockPostId" },
      )

  val mockAchievement2 =
      Achievement(
          achievementId = "mockPostId2",
          pictureURL = "image_placeholder",
          description = "This is another mock achievement for testing purposes",
          name = "Mock Achievement 2",
          condition = { postIds -> postIds.size == 2 },
      )

  // ------------------------------------------------------------

  // List of all achievements
  val ALL =
      listOf(
          mockAchievement1,
          mockAchievement2,
          postMaster,
          socialButterfly,
          communityBuilder,
          influencer,
      )
  val achievement_by_id: Map<Id, Achievement> = ALL.associateBy { it.achievementId }

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
