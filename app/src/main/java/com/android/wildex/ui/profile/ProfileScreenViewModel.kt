package com.android.wildex.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUIState(
    val user: User? = null,
    val isUserOwner: Boolean = true,
    val achievements: List<Achievement> = emptyList(),
    val animalCount: Int = 17
)

// TODO: Add UserAnimals Repository once implemented
class ProfileScreenViewModel(
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val achievementRepository: UserAchievementsRepository =
        RepositoryProvider.userAchievementsRepository,
    private val currentUserId: () -> String? = {
      com.google.firebase.ktx.Firebase.auth.currentUser?.uid
    },
) : ViewModel() {
  private val _uiState = MutableStateFlow(ProfileUIState())
  val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()
  val defaultUser: User =
      User(
          userId = "defaultUserId",
          username = "Oscour<3",
          name = "Nuit",
          surname = "Blanche",
          bio = "This is a default user bio.",
          profilePictureURL =
              "https://paulhollandphotography.com/cdn/shop/articles" +
                  "/4713_Individual_Outdoor_f930382f-c9d6-4e5b-b17d-9fe300ae169c" +
                  ".jpg?v=1743534144&width=1500",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "Nowhere",
          friendsCount = 12,
      )

  fun refreshUIState(userId: String) {
    viewModelScope.launch {
      try {
        val user = fetchUser(userId)
        val achievements = fetchAchievements(userId)
        _uiState.value = ProfileUIState(user, checkIsUserOwner(userId), achievements)
      } catch (e: Exception) {
        Log.e("ProfileScreenViewModel", "Error refreshing UI state", e)
      }
    }
  }

  private fun checkIsUserOwner(userId: String): Boolean {
    return userId == currentUserId()
  }

  private suspend fun fetchUser(userId: String): User? {
    return try {
      userRepository.getUser(userId)
    } catch (e: Exception) {
      Log.e("ProfileScreenViewModel", "Error fetching user", e)
      defaultUser
    }
  }

  private suspend fun fetchAchievements(userId: String): List<Achievement> {
    return try {
      achievementRepository.getAllAchievementsByUser(userId)
    } catch (e: Exception) {
      Log.e("ProfileScreenViewModel", "Error fetching achievements", e)
      emptyList()
    }
  }

  // TODO: ADD fetch animals count
}
