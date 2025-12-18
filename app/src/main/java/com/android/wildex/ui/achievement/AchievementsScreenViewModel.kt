package com.android.wildex.ui.achievement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.ProgressInfo
import com.android.wildex.model.utils.URL
import com.android.wildex.usecase.achievement.UpdateUserAchievementsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val unlocked: List<AchievementUIState> = emptyList(),
    val locked: List<AchievementUIState> = emptyList(),
    val overallProgress: Pair<Int, Int> = Pair(0, 0),
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val isError: Boolean = false,
)

/**
 * UI state for a single achievement.
 *
 * @property id The unique identifier of the achievement.
 * @property name The name of the achievement.
 * @property description A brief description of the achievement.
 * @property pictureURL The URL of the achievement's picture.
 * @property progress A list of progress information related to the achievement.
 */
data class AchievementUIState(
    val id: Id,
    val name: String,
    val description: String,
    val pictureURL: URL,
    val progress: List<ProgressInfo>,
)

/**
 * ViewModel for the Achievements screen.
 *
 * Responsible for loading the user's achievements and managing the UI state.
 */
class AchievementsScreenViewModel(
    private val userAchievementsRepository: UserAchievementsRepository =
        RepositoryProvider.userAchievementsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
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
    viewModelScope.launch { updateUIState(userId) }
    viewModelScope.launch(ioDispatcher) {
      runCatching { UpdateUserAchievementsUseCase()(userId) }.onSuccess { updateUIState(userId) }
    }
  }

  private suspend fun updateUIState(userId: String) {
    try {
      // 1) Repo calls off-main, in parallel
      val (allAchievements, unlockedAchievements) =
          withContext(ioDispatcher) {
            val allDeferred = async { userAchievementsRepository.getAllAchievements() }
            val unlockedDeferred = async {
              userAchievementsRepository.getAllAchievementsByUser(userId)
            }
            allDeferred.await() to unlockedDeferred.await()
          }

      // 2) Compute locked + build UI models off-main
      val unlockedIds = unlockedAchievements.asSequence().map { it.achievementId }.toHashSet()
      val lockedAchievements =
          allAchievements.asSequence().filter { it.achievementId !in unlockedIds }.toList()

      val (unlockedUI, lockedUI) =
          withContext(computeDispatcher) {
            coroutineScope {
              val unlockedUIDeferred =
                  unlockedAchievements.map { ach ->
                    async {
                      AchievementUIState(
                          id = ach.achievementId,
                          name = ach.name,
                          description = ach.description,
                          pictureURL = ach.pictureURL,
                          progress = ach.progress(userId),
                      )
                    }
                  }

              val lockedUIDeferred =
                  lockedAchievements.map { ach ->
                    async {
                      AchievementUIState(
                          id = ach.achievementId,
                          name = ach.name,
                          description = ach.description,
                          pictureURL = ach.pictureURL,
                          progress = ach.progress(userId),
                      )
                    }
                  }

              unlockedUIDeferred.awaitAll() to lockedUIDeferred.awaitAll()
            }
          }

      _uiState.value =
          _uiState.value.copy(
              unlocked = unlockedUI,
              locked = lockedUI,
              overallProgress = Pair(unlockedUI.size, allAchievements.size),
              isLoading = false,
              isError = false,
              errorMsg = null,
          )
    } catch (e: Exception) {
      setErrorMsg(e.localizedMessage ?: "Failed to load achievements.")
      _uiState.value = _uiState.value.copy(isLoading = false, isError = true)
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
