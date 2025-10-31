package com.android.wildex.ui.achievement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.UserAchievementsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Achievements screen.
 *
 * @property unlocked List of achievements that the user has unlocked.
 * @property locked List of achievements that the user has not yet unlocked.
 * @property isLoading Indicates whether the achievements are currently being loaded.
 * @property errorMsg An optional error message if loading failed.
 * @property isError Indicates whether there was an error during loading.
 * @property isRefreshing Indicates whether a refresh operation is in progress.
 */
data class AchievementsUIState(
    val unlocked: List<Achievement> = emptyList(),
    val locked: List<Achievement> = emptyList(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val isError: Boolean = false,
    val isRefreshing: Boolean = false
)

/**
 * ViewModel for the Achievements screen.
 *
 * @property repository Repository to fetch user achievements.
 */
class AchievementsScreenViewModel(
    private val repository: UserAchievementsRepository =
        RepositoryProvider.userAchievementsRepository
) : ViewModel() {
  private val _uiState = MutableStateFlow(AchievementsUIState(isLoading = true))
  val uiState: StateFlow<AchievementsUIState> = _uiState.asStateFlow()

  fun loadAchievements(refresh: Boolean = false) {
    viewModelScope.launch {
      _uiState.value =
          _uiState.value.copy(
              isLoading = !refresh, isRefreshing = refresh, isError = false, errorMsg = null)

      try {
        val unlocked = repository.getAllAchievementsByCurrentUser()
        val locked = repository.getAllAchievements().filter { it !in unlocked }

        _uiState.value =
            _uiState.value.copy(
                unlocked = unlocked,
                locked = locked,
                isLoading = false,
                isRefreshing = false,
                isError = false,
                errorMsg = null)
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                isError = true,
                errorMsg = "Failed to load achievements: ${e.message}")
      }
    }
  }
}
