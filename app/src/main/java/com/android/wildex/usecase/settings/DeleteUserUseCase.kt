package com.android.wildex.usecase.settings

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserSettingsRepository
import com.android.wildex.model.utils.Id


/**
 * Use case: Delete a user's account
 *
 * Deleting a user's account means deleting the User object itself but also all other objects linked
 * to this User. This includes a UserSettings, a UserAnimals and a UserAchievements.
 */
class DeleteUserUseCase(
  private val userRepository: UserRepository = RepositoryProvider.userRepository,
  private val userSettingsRepository: UserSettingsRepository = RepositoryProvider.userSettingsRepository,
  private val userAnimalsRepository: UserAnimalsRepository = RepositoryProvider.userAnimalsRepository,
  private val userAchievementsRepository: UserAchievementsRepository = RepositoryProvider.userAchievementsRepository
) {

  /**
   * Deletes the account of the given user
   *
   * @param userId user whose account we want to delete
   */
  suspend operator fun invoke(userId: Id) {
    userRepository.deleteUser(userId)
    userSettingsRepository.deleteUserSettings(userId)
    userAnimalsRepository.deleteUserAnimals(userId)
    userAchievementsRepository.deleteUserAchievements(userId)
  }
}