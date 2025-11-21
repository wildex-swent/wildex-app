package com.android.wildex.ui.achievement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Default user used when no user data is available. */
private val defaultUser: SimpleUser =
    SimpleUser(
        userId = "defaultUserId",
        username = "defaultUsername",
        profilePictureURL = "",
        userType = UserType.REGULAR,
    )

/**
 * UI state for the Achievements screen.
 *
 * @property unlocked List of achievements that the user has unlocked.
 * @property locked List of achievements that the user has not yet unlocked.
 * @property isLoading Indicates whether the achievements are currently being loaded.
 * @property errorMsg An optional error message if loading failed.
 * @property isError Indicates whether there was an error during loading.
 */
data class AchievementsUIState(
    val unlocked: List<Achievement> = emptyList(),
    val locked: List<Achievement> = emptyList(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val isError: Boolean = false,
    val user: SimpleUser = defaultUser,
    val isUserOwner: Boolean = false,
)

/**
 * ViewModel for the Achievements screen.
 *
 * Responsible for loading the user's achievements and managing the UI state.
 */
class AchievementsScreenViewModel(
    private val userAchievementsRepository: UserAchievementsRepository =
        RepositoryProvider.userAchievementsRepository,
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val currentUserId: Id = Firebase.auth.uid ?: "",
) : ViewModel() {
  private val _uiState = MutableStateFlow(AchievementsUIState(isLoading = true))
  val uiState: StateFlow<AchievementsUIState> = _uiState.asStateFlow()

  /**
   * Loads the UI state for the specified user.
   *
   * @param userId The UID of the user whose achievements are to be loaded.
   */
  fun loadUIState(userId: String) {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null, isError = false)
    viewModelScope.launch {
      try {
        val user = userRepository.getSimpleUser(userId)
        val unlockedAchievements = userAchievementsRepository.getAllAchievementsByUser(userId)
        val lockedAchievements =
            userAchievementsRepository.getAllAchievements().filter { it !in unlockedAchievements }

        _uiState.value =
            _uiState.value.copy(
                unlocked = unlockedAchievements,
                locked = lockedAchievements,
                isLoading = false,
                isError = false,
                errorMsg = null,
                user = user,
                isUserOwner = userId == currentUserId,
            )
      } catch (e: Exception) {
        setErrorMsg(e.localizedMessage ?: "Failed to load achievements.")
        _uiState.value = _uiState.value.copy(isLoading = false, isError = true)
      }
    }
  }

  /** Clears any existing error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Sets an error message in the UI state.
   *
   * @param msg The error message to be set.
   */
  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }
}
