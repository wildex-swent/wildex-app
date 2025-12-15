package com.android.wildex.usecase.user

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.user.UserFriendsRepository
import com.android.wildex.model.user.UserSettingsRepository
import com.android.wildex.model.user.UserTokensRepository
import com.android.wildex.model.utils.Id

/**
 * Use case: Adding a new user to the database
 *
 * Adding a new user means creating a User object but also creating a UserAnimals collection, a
 * UserSettings to store the user's preferences, a UserAchievements collection, an empty UserFriends
 * collection, and a UserTokens collection for notifications.
 */
class InitializeUserUseCase(
    private val userSettingsRepository: UserSettingsRepository =
        RepositoryProvider.userSettingsRepository,
    private val userAnimalsRepository: UserAnimalsRepository =
        RepositoryProvider.userAnimalsRepository,
    private val userAchievementsRepository: UserAchievementsRepository =
        RepositoryProvider.userAchievementsRepository,
    private val userFriendsRepository: UserFriendsRepository =
        RepositoryProvider.userFriendsRepository,
    private val userTokensRepository: UserTokensRepository =
        RepositoryProvider.userTokensRepository,
) {

  /**
   * Adds the user to the database
   *
   * @param userId user whose account we want to create
   */
  suspend operator fun invoke(userId: Id) {
    userSettingsRepository.initializeUserSettings(userId)
    userAnimalsRepository.initializeUserAnimals(userId)
    userAchievementsRepository.initializeUserAchievements(userId)
    userFriendsRepository.initializeUserFriends(userId)
    userTokensRepository.initializeUserTokens(userId)
  }
}
