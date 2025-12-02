package com.android.wildex.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.friendRequest.FriendRequest
import com.android.wildex.model.friendRequest.FriendRequestRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.user.UserFriendsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.social.FriendStatus
import com.android.wildex.usecase.achievement.UpdateUserAchievementsUseCase
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Point
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val emptyUser =
    User("", "Username", "Name", "Surname", "", "", UserType.REGULAR, Timestamp(0, 0), "Country")

/** Represents the UI state of the Profile screen */
data class ProfileUIState(
    val user: User = emptyUser,
    val isUserOwner: Boolean = false,
    val achievements: List<Achievement> = emptyList(),
    val friendStatus: FriendStatus = FriendStatus.IS_CURRENT_USER,
    val animalCount: Int = 0,
    val friendsCount: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMsg: String? = null,
    val isError: Boolean = false,
    val recentPins: List<Point> = emptyList(),
)

/**
 * View Model for the Profile screen
 *
 * @property userRepository repository to get the user from
 * @property achievementRepository repository to get the user's achievements
 * @property postRepository repository to get the post pins from for the map preview
 * @property updateUserAchievements use case to update the user's achievements
 * @property userAnimalsRepository repository to get the user's animals count from
 * @property userFriendsRepository repository to get the user's friends count from
 * @property friendRequestRepository repository to get the possibly existing friend requests between
 *   the current user and the owner of the profile screen
 * @property currentUserId currently logged in user
 */
class ProfileScreenViewModel(
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val achievementRepository: UserAchievementsRepository =
        RepositoryProvider.userAchievementsRepository,
    private val postRepository: PostsRepository = RepositoryProvider.postRepository,
    private val updateUserAchievements: UpdateUserAchievementsUseCase =
        UpdateUserAchievementsUseCase(),
    private val userAnimalsRepository: UserAnimalsRepository =
        RepositoryProvider.userAnimalsRepository,
    private val userFriendsRepository: UserFriendsRepository =
        RepositoryProvider.userFriendsRepository,
    private val friendRequestRepository: FriendRequestRepository =
        RepositoryProvider.friendRequestRepository,
    private val currentUserId: Id? = Firebase.auth.uid,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ProfileUIState())
  val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

  /**
   * Refreshes the Profile screen's UI state for the given user
   *
   * @param userId user whose profile we want to load
   */
  fun refreshUIState(userId: Id) {
    _uiState.value = _uiState.value.copy(isRefreshing = true, errorMsg = null)
    viewModelScope.launch { updateUIState(userId) }
  }

  /**
   * Loads the Profile screen's UI state for the given user
   *
   * @param userId user whose profile we want to load
   */
  fun loadUIState(userId: Id) {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)
    viewModelScope.launch { updateUIState(userId) }
  }

  /**
   * Updates the Profile screen's UI state for the given user. This function is used by both the
   * load and the refresh functions.
   *
   * @param userId user whose profile we want to load
   */
  private suspend fun updateUIState(userId: Id) = coroutineScope {
    if (userId.isBlank()) {
      setErrorMsg("Empty user id")
      _uiState.value =
          _uiState.value.copy(
              isLoading = false,
              isRefreshing = false,
              isUserOwner = false,
              isError = true,
          )
      return@coroutineScope
    }
    try {
      // 1) Load user fast and show something
      val user = userRepository.getUser(userId)

      _uiState.value =
          _uiState.value.copy(
              user = user,
              isUserOwner = (currentUserId != null && user.userId == currentUserId),
              isLoading = false,
              isRefreshing = false,
              isError = false,
          )

      // 2) In parallel, load heavy stuff AFTER user is visible
      viewModelScope.launch {
        val achievements = fetchAchievements(userId)
        _uiState.value = _uiState.value.copy(achievements = achievements)
      }

      viewModelScope.launch {
        val currentUserId = currentUserId!!
        val sentRequests =
            friendRequestRepository.getAllFriendRequestsBySender(currentUserId).map {
              it.receiverId
            }
        val friendsIds = userFriendsRepository.getAllFriendsOfUser(userId).map { it.userId }
        val receivedRequests =
            friendRequestRepository.getAllFriendRequestsByReceiver(currentUserId).map {
              it.senderId
            }
        when {
          _uiState.value.isUserOwner ->
              _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.IS_CURRENT_USER)
          friendsIds.contains(currentUserId) ->
              _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.FRIEND)
          sentRequests.contains(_uiState.value.user.userId) ->
              _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.PENDING_SENT)
          receivedRequests.contains(_uiState.value.user.userId) ->
              _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.PENDING_RECEIVED)
          else -> _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.NOT_FRIEND)
        }
      }

      viewModelScope.launch {
        val animalsCount = userAnimalsRepository.getAnimalsCountOfUser(userId)
        _uiState.value = _uiState.value.copy(animalCount = animalsCount)
      }

      viewModelScope.launch {
        val friendsCount = userFriendsRepository.getFriendsCountOfUser(userId)
        _uiState.value = _uiState.value.copy(friendsCount = friendsCount)
      }

      viewModelScope.launch {
        val pins = fetchPostPins(userId)
        _uiState.value = _uiState.value.copy(recentPins = pins)
      }
    } catch (e: Exception) {
      Log.e("ProfileScreenViewModel", "Error refreshing UI state", e)
      setErrorMsg("Unexpected error: ${e.message ?: "unknown"}")
      _uiState.value =
          _uiState.value.copy(
              isError = true,
              isLoading = false,
              isRefreshing = false,
          )
    }
  }

  /** Clears the error message from the ui state */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets the given error message to the ui state */
  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  /** Fetches the achievements of the given user */
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

  /**
   * Fetches at most <limit> post pins from the given user to the ui state
   *
   * @param authorId user whose pins we want to load
   * @param limit maximum number of pins to load
   */
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

  /**
   * Sends a friend request from the current user to the owner of the profile screen, and updates
   * the ui state accordingly.
   */
  fun sendRequestToUser() {
    viewModelScope.launch {
      val state = _uiState.value
      if (state.isUserOwner) return@launch
      val currentUserId = currentUserId!!

      _uiState.value = state.copy(friendStatus = FriendStatus.PENDING_SENT)

      try {
        friendRequestRepository.initializeFriendRequest(currentUserId, state.user.userId)
      } catch (e: Exception) {
        _uiState.value = state
        setErrorMsg("Failed to send request to user ${state.user.userId} : ${e.message}")
      }
    }
  }

  /** Cancels a friend request sent from the current user to the user owning the profile screen */
  fun cancelSentRequestToUser() {
    viewModelScope.launch {
      val state = _uiState.value
      if (state.isUserOwner) return@launch
      val currentUserId = currentUserId!!
      val request = FriendRequest(currentUserId, state.user.userId)

      _uiState.value = state.copy(friendStatus = FriendStatus.NOT_FRIEND)

      try {
        friendRequestRepository.refuseFriendRequest(request)
      } catch (e: Exception) {
        _uiState.value = state
        setErrorMsg("Failed to cancel request to user ${state.user.userId} : ${e.message}")
      }
    }
  }

  /** Accepts a friend request from the user owning the profile to the current user */
  fun acceptReceivedRequest() {
    viewModelScope.launch {
      val state = _uiState.value
      if (state.isUserOwner) return@launch
      val currentUserId = currentUserId!!
      val request = FriendRequest(state.user.userId, currentUserId)

      _uiState.value =
          state.copy(friendStatus = FriendStatus.FRIEND, friendsCount = state.friendsCount + 1)

      try {
        friendRequestRepository.acceptFriendRequest(request)
      } catch (e: Exception) {
        _uiState.value = state
        setErrorMsg("Failed to accept request from user ${state.user.userId} : ${e.message}")
      }
    }
  }

  /** Declines a friend request from the user owning the profile to the current user */
  fun declineReceivedRequest() {
    viewModelScope.launch {
      val state = _uiState.value
      if (state.isUserOwner) return@launch
      val currentUserId = currentUserId!!
      val request = FriendRequest(state.user.userId, currentUserId)

      _uiState.value = state.copy(friendStatus = FriendStatus.NOT_FRIEND)

      try {
        friendRequestRepository.refuseFriendRequest(request)
      } catch (e: Exception) {
        _uiState.value = state
        setErrorMsg("Failed to decline request from user ${state.user.userId} : ${e.message}")
      }
    }
  }

  /** Removes the user owning the profile from the current user's friend list. */
  fun unfollowUser() {
    viewModelScope.launch {
      val state = _uiState.value
      if (state.isUserOwner) return@launch
      val currentUserId = currentUserId!!

      _uiState.value =
          state.copy(friendStatus = FriendStatus.NOT_FRIEND, friendsCount = state.friendsCount - 1)

      try {
        userFriendsRepository.deleteFriendToUserFriendsOfUser(state.user.userId, currentUserId)
        userFriendsRepository.deleteFriendToUserFriendsOfUser(currentUserId, state.user.userId)
      } catch (e: Exception) {
        _uiState.value = state
        setErrorMsg("Failed to remove friendship with user ${state.user.userId} : ${e.message}")
      }
    }
  }
}
