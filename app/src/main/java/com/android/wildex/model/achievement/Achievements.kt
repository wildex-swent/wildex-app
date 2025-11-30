package com.android.wildex.model.achievement

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.utils.Id

object Achievements {
  private val likeRepository: LikeRepository = RepositoryProvider.likeRepository
  private val postRepository: PostsRepository = RepositoryProvider.postRepository
  private val commentRepository: CommentRepository = RepositoryProvider.commentRepository

  // ---------- Achievements ----------

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
          progress = { userId ->
            listOf(Pair(postRepository.getAllPostsByGivenAuthor(userId).size, 10))
          },
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
          progress = { userId -> listOf(Pair(likeRepository.getAllLikesByUser(userId).size, 50)) },
      )

  /** Community Builder — awarded for writing at least 20 comments. */
  val communityBuilder =
      Achievement(
          achievementId = "achievement_3",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/1077/1077012.png",
          description = "Write 20 comments",
          name = "Community Builder",
          progress = { userId ->
            listOf(Pair(commentRepository.getCommentsByUser(userId).size, 20))
          },
      )

  /** Influencer — awarded for receiving a total of 1000 likes across all posts. */
  val influencer =
      Achievement(
          achievementId = "achievement_4",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/4339/4339544.png",
          description = "Get 1000 likes across all your posts",
          name = "Influencer",
          progress = { userId ->
            listOf(Pair(likeRepository.getAllLikesByUser(userId).size, 1000))
          },
      )

  /** First Post — awarded for creating the first post. */
  val firstPost =
      Achievement(
          achievementId = "achievement_5",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/1828/1828961.png",
          description = "Create your first post",
          name = "First Post",
          progress = { userId ->
            listOf(Pair(postRepository.getAllPostsByGivenAuthor(userId).size, 1))
          },
      )

  /** Rising Star — awarded for a post that reaches at least 100 likes. */
  val risingStar =
      Achievement(
          achievementId = "achievement_6",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/616/616490.png",
          description = "Get 100 likes on a single post",
          name = "Rising Star",
          progress = { userId ->
            listOf(Pair(likeRepository.getAllLikesByUser(userId).size, 100))
          },
      )

  /** Conversationalist — awarded for writing at least 50 comments overall. */
  val conversationalist =
      Achievement(
          achievementId = "achievement_7",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/2462/2462719.png",
          description = "Write 50 comments overall",
          name = "Conversationalist",
          progress = { userId ->
            listOf(Pair(commentRepository.getCommentsByUser(userId).size, 50))
          },
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
          progress = { userId ->
            listOf(
                Pair(postRepository.getAllPostsByGivenAuthor(userId).size, 5),
                Pair(likeRepository.getAllLikesByUser(userId).size, 10),
                Pair(commentRepository.getCommentsByUser(userId).size, 10),
            )
          },
      )

  // ---------- Collections ----------

  val ALL =
      listOf(
          postMaster,
          socialButterfly,
          communityBuilder,
          influencer,
          firstPost,
          risingStar,
          conversationalist,
          engagedCreator,
      )

  val achievementById: Map<Id, Achievement> = ALL.associateBy { it.achievementId }
}
