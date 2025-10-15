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
    val achievements: List<Achievement> = emptyList()
)

class ProfileScreenViewModel(
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val achievementRepository: UserAchievementsRepository =
        RepositoryProvider.userAchievementsRepository,
    private val currentUserId: () -> String? = {
      com.google.firebase.ktx.Firebase.auth.currentUser?.uid
    },
    private val uid: String = "",
) : ViewModel() {
  private val _uiState = MutableStateFlow(ProfileUIState())
  val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()
  private val defaultUser: User =
      User(
          userId = "defaultUserId",
          username = "defaultUsername",
          name = "Default",
          surname = "User",
          bio = "This is...",
          profilePictureURL =
              "https://paulhollandphotography.com/cdn/shop/articles" +
                  "/4713_Individual_Outdoor_f930382f-c9d6-4e5b-b17d-9fe300ae169c" +
                  ".jpg?v=1743534144&width=1500",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "Nowhere",
          friendsCount = 0)

  fun refreshUIState() {
    viewModelScope.launch {
      try {
        val user = fetchUser()
        val achievements = fetchAchievements()
        _uiState.value = ProfileUIState(user, checkIsUserOwner(), achievements)
      } catch (e: Exception) {
        Log.e("ProfileScreenViewModel", "Error refreshing UI state", e)
      }
    }
  }

  private fun checkIsUserOwner(): Boolean {
    return uid == currentUserId()
  }

  private suspend fun fetchUser(): User? {
    return try {
      userRepository.getUser(uid)
    } catch (e: Exception) {
      Log.e("ProfileScreenViewModel", "Error fetching user", e)
      defaultUser
    }
  }

  private suspend fun fetchAchievements(): List<Achievement> {
    return try {
      achievementRepository.getAllAchievementsByUser(uid)
    } catch (e: Exception) {
      Log.e("ProfileScreenViewModel", "Error fetching achievements", e)
      emptyList()
    }
  }
}
