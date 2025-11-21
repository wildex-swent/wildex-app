package com.android.wildex.usecase.achievement

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.achievement.UserAchievementsRepository
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
    userAchievementsRepository.updateUserAchievements(userId)
  }
}
