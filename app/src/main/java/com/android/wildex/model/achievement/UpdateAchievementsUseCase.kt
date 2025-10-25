package com.android.wildex.model.achievement

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.social.Comment
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.utils.Id
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * Use case: Refresh a user's achievements.
 *
 * REMINDER INPUT CONVENTIONS
 * - POST_IDS: list of the current user's post IDs.
 * - LIKE_IDS: list of post IDs that the current user has liked.
 * - COMMENT_IDS: list of comment IDs authored by the current user.
 *
 * Always passes a map containing all three keys to the repository update method. Best not to call
 * on Dispatchers.Main, as it may be computationally heavy. Run on Dispatchers.IO instead.
 */
class UpdateUserAchievementsUseCase(
    private val postsRepository: PostsRepository = RepositoryProvider.postRepository,
    private val likeRepository: LikeRepository = RepositoryProvider.likeRepository,
    private val commentRepository: CommentRepository = RepositoryProvider.commentRepository,
    private val userAchievementsRepository: UserAchievementsRepository =
        RepositoryProvider.userAchievementsRepository
) {

  /**
   * Recomputes and updates achievements for [userId]. Safe to call repeatedly; underlying repo
   * writes only if set changes.
   */
  suspend operator fun invoke(userId: Id) {
    // Just in case the achievements repo was never initialized for this user.
    userAchievementsRepository.initializeUserAchievements(userId)

    // 1) POST_IDS: posts authored by the target user
    val postIds: List<Id> =
        try {
          postsRepository.getAllPostsByGivenAuthor(userId).map { it.postId }
        } catch (_: Exception) {
          emptyList()
        }

    // 2) LIKE_IDS: posts liked BY the target user
    val currentUid = Firebase.auth.currentUser?.uid
    val likePostIds: List<Id> =
        try {
          if (currentUid == userId) {
            likeRepository.getAllLikesByCurrentUser().map { it.postId }
          } else {
            emptyList()
          }
        } catch (_: Exception) {
          emptyList()
        }

    // 3) COMMENT_IDS: comments AUTHORED by the target user
    // TODO: This is veery inefficient; consider adding a getCommentByUser to CommentRepository.
    val authoredCommentIds: List<Id> =
        try {
          val userPosts = postsRepository.getAllPostsByGivenAuthor(userId)
          val ids = mutableListOf<Id>()
          for (p in userPosts) {
            val comments = commentRepository.getAllCommentsByPost(p.postId)
            for (c in comments) {
              if (isAuthoredByUser(c, userId)) ids += c.commentId
            }
          }
          ids
        } catch (_: Exception) {
          emptyList()
        }

    val inputs =
        mapOf(
            InputKey.POST_IDS to postIds,
            InputKey.LIKE_IDS to likePostIds,
            InputKey.COMMENT_IDS to authoredCommentIds)

    userAchievementsRepository.updateUserAchievements(userId, inputs)
  }

  private fun isAuthoredByUser(comment: Comment, userId: Id): Boolean {
    // Adjust if your Comment model uses a different property name.
    return try {
      comment.authorId == userId
    } catch (_: Throwable) {
      false
    }
  }
}
