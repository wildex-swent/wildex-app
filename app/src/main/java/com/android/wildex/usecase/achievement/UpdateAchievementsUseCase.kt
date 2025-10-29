package com.android.wildex.usecase.achievement

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.achievement.InputKey
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.utils.Id

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
    val likePostIds: List<Id> =
        try {
          likeRepository.getAllLikesByUser(userId).map { it.postId }
        } catch (_: Exception) {
          emptyList()
        }

    // 3) COMMENT_IDS: comments AUTHORED by the target user
    val authoredCommentIds: List<Id> =
        try {
          commentRepository.getCommentsByUser(userId).map { it.commentId }
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
}
