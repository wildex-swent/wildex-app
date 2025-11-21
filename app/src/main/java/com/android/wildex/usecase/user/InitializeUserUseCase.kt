package com.android.wildex.usecase.user

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserSettingsRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.google.firebase.Timestamp

/**
 * Use case: Adding a new user to the database
 *
 * Adding a new user means creating a User object but also creating a UserAnimals collection, a
 * UserSettings to store the user's preferences and a UserAchievements collection
 */
class InitializeUserUseCase(
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val userSettingsRepository: UserSettingsRepository =
        RepositoryProvider.userSettingsRepository,
    private val userAnimalsRepository: UserAnimalsRepository =
        RepositoryProvider.userAnimalsRepository,
    private val userAchievementsRepository: UserAchievementsRepository =
        RepositoryProvider.userAchievementsRepository
) {

  /**
   * Adds the user to the database
   *
   * @param userId user whose account we want to create
   */
  suspend operator fun invoke(userId: Id) {
    val user = User(userId, "", "", "", "", "", UserType.REGULAR, Timestamp.now(), "")
    userRepository.addUser(user)
    userSettingsRepository.initializeUserSettings(userId)
    userAnimalsRepository.initializeUserAnimals(userId)
    userAchievementsRepository.initializeUserAchievements(userId)
  }
}
