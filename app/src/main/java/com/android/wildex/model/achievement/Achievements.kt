package com.android.wildex.model.achievement

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.utils.Id

object Achievements {
  private val likeRepository: LikeRepository = RepositoryProvider.likeRepository

  // ─────────────────────────────────────────────────────────────────────────────
  // INPUT CONVENTIONS
  // - POST_IDS: list of the current user's post IDs.
  // - LIKE_IDS: list of post IDs that the current user has liked.
  // - COMMENT_IDS: list of comment IDs authored by the current user.
  // ALWAYS PASS A MAP CONTAINING ALL THESE THREE INPUT KEYS TO THE ACHIEVEMENT UPDATE METHOD
  // OTHERWISE SOME ACHIEVEMENTS MAY GET DROPPED UNINTENTIONALLY.
  // ─────────────────────────────────────────────────────────────────────────────

  /** Post Master — awarded for publishing at least 10 posts. */
  val postMaster =
      Achievement(
          achievementId = "achievement_1",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/2583/2583343.png",
          description = "Reach 10 posts",
          name = "Post Master",
          expects = setOf(InputKey.POST_IDS),
          condition = { postIds -> postIds.size >= 10 },
      )

  /**
   * Social Butterfly — awarded for liking 50 different posts. Verifies likes by checking the
   * LikeRepository for each provided post ID.
   */
  val socialButterfly =
      Achievement(
          achievementId = "achievement_2",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/616/616408.png",
          description = "Like 50 posts",
          name = "Social Butterfly",
          expects = setOf(InputKey.LIKE_IDS),
          condition = { likedPostIds ->
            countDistinctLikedPostsVerified(likedPostIds, likeRepository) >= 50
          },
      )

  /** Community Builder — awarded for writing at least 20 comments. */
  val communityBuilder =
      Achievement(
          achievementId = "achievement_3",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/1077/1077012.png",
          description = "Write 20 comments",
          name = "Community Builder",
          expects = setOf(InputKey.COMMENT_IDS),
          condition = { commentIds -> commentIds.size >= 20 },
      )

  /** Influencer — awarded for receiving a total of 1000 likes across all posts. */
  val influencer =
      Achievement(
          achievementId = "achievement_4",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/4339/4339544.png",
          description = "Get 1000 likes across all your posts",
          name = "Influencer",
          expects = setOf(InputKey.POST_IDS),
          condition = { postIds -> sumLikesAcrossPosts(postIds, likeRepository) >= 1000 },
      )

  /** First Post — awarded for creating the first post. */
  val firstPost =
      Achievement(
          achievementId = "achievement_5",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/1828/1828961.png",
          description = "Create your first post",
          name = "First Post",
          expects = setOf(InputKey.POST_IDS),
          condition = { postIds -> postIds.isNotEmpty() },
      )

  /** Rising Star — awarded for a post that reaches at least 100 likes. */
  val risingStar =
      Achievement(
          achievementId = "achievement_6",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/616/616490.png",
          description = "Get 100 likes on a single post",
          name = "Rising Star",
          expects = setOf(InputKey.POST_IDS),
          condition = { postIds -> hasPostWithAtLeastLikes(postIds, likeRepository, 100) },
      )

  /** Conversationalist — awarded for writing at least 50 comments overall. */
  val conversationalist =
      Achievement(
          achievementId = "achievement_7",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/2462/2462719.png",
          description = "Write 50 comments overall",
          name = "Conversationalist",
          expects = setOf(InputKey.COMMENT_IDS),
          condition = { commentIds -> commentIds.size >= 50 },
      )

  /**
   * Engaged Creator — awarded for being active across multiple areas:
   * - At least 5 posts created
   * - At least 10 distinct posts liked
   * - At least 10 comments written
   */
  val engagedCreator =
      Achievement(
          achievementId = "achievement_8",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/4144/4144723.png",
          description = "Be active across Wildex: post, like, and comment regularly",
          name = "Engaged Creator",
          expects = setOf(InputKey.POST_IDS, InputKey.LIKE_IDS, InputKey.COMMENT_IDS),
          multiCondition = { inputs ->
            val postIds = inputs[InputKey.POST_IDS].orEmpty()
            val likedPostIds = inputs[InputKey.LIKE_IDS].orEmpty()
            val commentIds = inputs[InputKey.COMMENT_IDS].orEmpty()

            val postsOk = postIds.size >= 5
            val likesOk = countDistinctLikedPostsVerified(likedPostIds, likeRepository) >= 10
            val commentsOk = commentIds.size >= 10

            postsOk && likesOk && commentsOk
          },
      )

  /** Mock Achievement 1 — used for repository testing. */
  val mockAchievement1 =
      Achievement(
          achievementId = "mockPostId",
          pictureURL = "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/svg/1f41b.svg",
          description = "This is a mock achievement for testing purposes",
          name = "Mock Achievement",
          expects = setOf(InputKey.TEST_IDS),
          condition = { postIds -> postIds.size == 1 && postIds[0] == "mockPostId" },
      )

  /** Mock Achievement 2 — used for repository testing. */
  val mockAchievement2 =
      Achievement(
          achievementId = "mockPostId2",
          pictureURL = "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/svg/1fab2.svg",
          description = "This is another mock achievement for testing purposes",
          name = "Mock Achievement 2",
          expects = setOf(InputKey.TEST_IDS),
          condition = { postIds -> postIds.size == 2 },
      )

  // ---------- Collections ----------

  val ALL =
      listOf(
          mockAchievement1,
          mockAchievement2,
          postMaster,
          socialButterfly,
          communityBuilder,
          influencer,
          firstPost,
          risingStar,
          conversationalist,
          engagedCreator,
      )

  val achievement_by_id: Map<Id, Achievement> = ALL.associateBy { it.achievementId }

  // ---------- Focused helpers ----------

  /** Sums likes received across all posts. */
  private suspend fun sumLikesAcrossPosts(postIds: List<Id>, likeRepo: LikeRepository): Int {
    var total = 0
    for (pid in postIds) {
      total += likeRepo.getLikesForPost(pid).size
      if (total >= 1000) return total
    }
    return total
  }

  /** Returns true if any post has at least [threshold] likes. */
  private suspend fun hasPostWithAtLeastLikes(
      postIds: List<Id>,
      likeRepo: LikeRepository,
      threshold: Int
  ): Boolean {
    for (pid in postIds) {
      if (likeRepo.getLikesForPost(pid).size >= threshold) return true
    }
    return false
  }

  /**
   * Verifies that each post ID provided in LIKE_IDS was actually liked by the current user, based
   * on LikeRepository.getLikeForPost(postId). Counts distinct verified post IDs.
   */
  private suspend fun countDistinctLikedPostsVerified(
      likedPostIds: List<Id>,
      likeRepo: LikeRepository
  ): Int {
    val verified = HashSet<Id>()
    for (pid in likedPostIds) {
      val like = likeRepo.getLikeForPost(pid)
      if (like != null) verified += pid
      if (verified.size >= 50) return verified.size
    }
    return verified.size
  }
}
