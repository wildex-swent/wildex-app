package com.android.wildex.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileUIState(
    val user: User? = null,
    val errorMsg: String? = null,
    val signedOut: Boolean = false
)

class ProfileScreenViewModel() : ViewModel() {
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
          friendsCount = 0,
          animalsId = emptyList(),
          animalsCount = 0,
          achievementsId = emptyList(),
          achievementsCount = 0,
      )

  fun refreshUIState() {
    _uiState.value = ProfileUIState(user = fetchUser())
  }

  private fun fetchUser(): User? {
    var user: User? = null
    // viewModelScope.launch {
    try {
      // TODO: implement fetching user
      /** _uiState.user = "fetchUserFromUserId(...)" */
      // user = "to User."Firebase.auth.currentUser
    } catch (e: Exception) {
      Log.e("ProfileScreenViewModel", "Error fetching user", e)
      // setErrorMsg("Failed to load user: ${e.message}")
    }
    // }
    return defaultUser
  }
}
