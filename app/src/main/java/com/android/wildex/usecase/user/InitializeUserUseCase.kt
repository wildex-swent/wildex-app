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

class InitializeUserUseCase(
  private val userRepository: UserRepository = RepositoryProvider.userRepository,
  private val userSettingsRepository: UserSettingsRepository = RepositoryProvider.userSettingsRepository,
  private val userAnimalsRepository: UserAnimalsRepository = RepositoryProvider.userAnimalsRepository,
  private val userAchievementsRepository: UserAchievementsRepository = RepositoryProvider.userAchievementsRepository
) {
  suspend operator fun invoke(userId: Id) {
    val user = User(
      userId,
      "",
      "",
      "",
      "",
      "",
      UserType.REGULAR,
      Timestamp.now(),
      "",
      0)
    userRepository.addUser(user)
    userSettingsRepository.initializeUserSettings(userId)
    userAnimalsRepository.initializeUserAnimals(userId)
    userAchievementsRepository.initializeUserAchievements(userId)
  }
}