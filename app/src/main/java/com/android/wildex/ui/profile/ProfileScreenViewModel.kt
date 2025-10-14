package com.android.wildex.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUIState(
    val user: User? = null,
    val achievements: List<Achievement> = emptyList(),
    val signedOut: Boolean = false
)

class ProfileScreenViewModel(
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    // private val achievementRepository: UserAchievementsRepository =
    // RepositoryProvider.achievementRepository,
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
          profilePictureURL = "https://example.com/default-profile-pic.png",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "Nowhere",
          friendsCount = 0)

  fun refreshUIState(
      userUid: String,
  ) {
    _uiState.value =
        ProfileUIState(user = fetchUser(userUid), achievements = fetchAchievements(userUid))
  }

  private fun fetchUser(
      userUid: String,
  ): User? {
    var user: User? = null
    viewModelScope.launch {
      try {
        // TODO: implement fetching user
        /** _uiState.user = "fetchUserFromUserId(...)" */
        // user = "to User."Firebase.auth.currentUser
        user = userRepository.getUser(userUid)
      } catch (e: Exception) {
        Log.e("ProfileScreenViewModel", "Error fetching user", e)
      }
    }
    return defaultUser
  }

  private fun fetchAchievements(userId: String): List<Achievement> {
    // getAllAchievementsByUser(userId: String)
    return emptyList()
  }
}
