package com.android.wildex.ui.achievement

import androidx.lifecycle.ViewModel
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.UserAchievementsRepository

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
) : ViewModel() {}
