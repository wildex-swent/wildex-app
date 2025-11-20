package com.android.wildex.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.usecase.achievement.UpdateUserAchievementsUseCase
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val emptyUser = User("", "", "", "", "", "", UserType.REGULAR, Timestamp(0, 0), "")

data class ProfileUIState(
    val user: User = emptyUser,
    val isUserOwner: Boolean = false,
    val achievements: List<Achievement> = emptyList(),
    val animalCount: Int = 17,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMsg: String? = null,
    val isError: Boolean = false,
    val recentPins: List<Point> = emptyList(),
)

class ProfileScreenViewModel(
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val achievementRepository: UserAchievementsRepository =
        RepositoryProvider.userAchievementsRepository,
    private val postRepository: PostsRepository = RepositoryProvider.postRepository,
    private val updateUserAchievements: UpdateUserAchievementsUseCase =
        UpdateUserAchievementsUseCase(),
    private val currentUserId: Id? = Firebase.auth.uid,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ProfileUIState())
  val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

  fun refreshUIState(userId: Id) {
    _uiState.value = _uiState.value.copy(isRefreshing = true, errorMsg = null)
    viewModelScope.launch { updateUIState(userId) }
  }

  fun loadUIState(userId: Id) {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)
    viewModelScope.launch { updateUIState(userId) }
  }

  private suspend fun updateUIState(userId: Id) {
    if (userId.isBlank()) {
      setErrorMsg("Empty user id")
      _uiState.value =
          _uiState.value.copy(
              isLoading = false,
              isRefreshing = false,
              isUserOwner = false,
              isError = true,
          )
      return
    }

    try {
      val user = userRepository.getUser(userId)
      val achievements = fetchAchievements(userId)
      val pins = fetchPostPins(userId)
      _uiState.value =
          _uiState.value.copy(
              user = user,
              isUserOwner = (currentUserId != null && user.userId == currentUserId),
              achievements = achievements,
              isLoading = false,
              recentPins = pins,
              isRefreshing = false,
              errorMsg = _uiState.value.errorMsg,
              isError = false,
          )
    } catch (e: Exception) {
      Log.e("ProfileScreenViewModel", "Error refreshing UI state", e)
      setErrorMsg("Unexpected error: ${e.message ?: "unknown"}")
      _uiState.value = _uiState.value.copy(isError = true, isLoading = false, isRefreshing = false)
    }
  }

  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  private suspend fun fetchAchievements(userId: String): List<Achievement> {
    return runCatching {
          updateUserAchievements(userId)
          achievementRepository.getAllAchievementsByUser(userId)
        }
        .getOrElse {
          setErrorMsg(it.message ?: "Failed to load achievements")
          emptyList()
        }
  }

  private suspend fun fetchPostPins(authorId: Id, limit: Int = 30): List<Point> {
    val posts =
        try {
          postRepository.getAllPostsByGivenAuthor(authorId)
        } catch (e: Exception) {
          setErrorMsg(e.message ?: "Failed to load user's posts")
          emptyList()
        }
    val sorted = posts.sortedByDescending { it.date }
    return sorted
        .asSequence()
        .mapNotNull { p ->
          val loc = p.location ?: return@mapNotNull null
          Point.fromLngLat(loc.longitude, loc.latitude)
        }
        .take(limit)
        .toList()
  }
}
