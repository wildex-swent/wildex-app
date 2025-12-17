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
import com.android.wildex.model.user.OnBoardingStage
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val emptyUser =
    User(
        "",
        "Username",
        "Name",
        "Surname",
        "",
        "",
        UserType.REGULAR,
        Timestamp(0, 0),
        "Country",
        OnBoardingStage.NAMING,
    )

/**
 * UI state for the Profile screen.
 *
 * Contains the displayed user, ownership flag, achievements list, friend status, counts for animals
 * and friends, loading/refreshing flags and recent map pins.
 */
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val currentUserId: Id? = Firebase.auth.uid,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ProfileUIState())
  val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

  /**
   * Refreshes the Profile screen UI state for [userId].
   *
   * This triggers a forced refresh (used for pull-to-refresh): it sets the refreshing flag and
   * launches a coroutine that performs the heavy update logic.
   *
   * @param userId id of the user whose profile should be refreshed.
   */
  fun refreshUIState(userId: Id) {
    _uiState.value = _uiState.value.copy(isRefreshing = true, errorMsg = null)
    viewModelScope.launch { updateUIState(userId, calledFromRefresh = true) }
  }

  /**
   * Loads the Profile screen UI state for [userId].
   *
   * Sets loading flag and launches the asynchronous update flow.
   *
   * @param userId id of the user to load.
   */
  fun loadUIState(userId: Id) {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)
    viewModelScope.launch { updateUIState(userId) }
  }

  /**
   * Suspends and updates the complete UI state for the given [userId].
   *
   * This function performs a fast initial load of the user and then launches parallel background
   * operations for heavier data (achievements recompute, friends, counts, pins).
   *
   * Inline comments: this method coordinates multiple repository calls and parallel child
   * coroutines; error handling is split between fatal (stop) and non-fatal (accumulate/set).
   *
   * @param userId id of the user to load.
   * @param calledFromRefresh if true, forces repository cache refreshes before loading.
   */
  private suspend fun updateUIState(userId: Id, calledFromRefresh: Boolean = false) =
      coroutineScope {
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
        // Refresh the cache when called from a pull-to-refresh action
        if (calledFromRefresh) {
          userRepository.refreshCache()
        }
        try {
          // 1) Fast path: show user immediately so UI is responsive
          val user = userRepository.getUser(userId)

          _uiState.value =
              _uiState.value.copy(
                  user = user,
                  isUserOwner = (currentUserId != null && user.userId == currentUserId),
                  isLoading = false,
                  isRefreshing = false,
                  isError = false,
              )

          // 2) Launch heavier loads in parallel after the user is visible to avoid blocking UI
          viewModelScope.launch {
            val achievements = fetchAchievements(userId)
            _uiState.value = _uiState.value.copy(achievements = achievements)
          }

          // 3) Determine friendship status (potentially multiple repository calls)
          viewModelScope.launch {
            val currentUserId = currentUserId ?: return@launch
            val sentRequests =
                runCatching {
                      friendRequestRepository.getAllFriendRequestsBySender(currentUserId).map {
                        it.receiverId
                      }
                    }
                    .getOrElse {
                      setErrorMsg(it.message ?: "Failed to load sent requests")
                      emptyList()
                    }

            val friendsIds =
                runCatching { userFriendsRepository.getAllFriendsOfUser(userId).map { it.userId } }
                    .getOrElse {
                      setErrorMsg(it.message ?: "Failed to load friends list")
                      emptyList()
                    }

            val receivedRequests =
                runCatching {
                      friendRequestRepository.getAllFriendRequestsByReceiver(currentUserId).map {
                        it.senderId
                      }
                    }
                    .getOrElse {
                      setErrorMsg(it.message ?: "Failed to load received requests")
                      emptyList()
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

          // 4) Load animals count
          viewModelScope.launch {
            val animalsCount =
                runCatching { userAnimalsRepository.getAnimalsCountOfUser(userId) }
                    .getOrElse {
                      setErrorMsg(it.message ?: "Failed to load animal count")
                      0
                    }
            _uiState.value = _uiState.value.copy(animalCount = animalsCount)
          }

          // 5) Load friends count
          viewModelScope.launch {
            val friendsCount =
                runCatching { userFriendsRepository.getFriendsCountOfUser(userId) }
                    .getOrElse {
                      setErrorMsg(it.message ?: "Failed to load friend count")
                      0
                    }
            _uiState.value = _uiState.value.copy(friendsCount = friendsCount)
          }

          // 6) Load recent post pins for map preview
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

  /** Shows an offline error message when trying to refresh while offline. */
  fun refreshOffline() {
    setErrorMsg("You are currently offline\nYou can not refresh for now :/")
  }

  /** Sets the given error message to the ui state */
  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  /**
   * Fetches the achievements for [userId].
   *
   * Fast path returns cached or remote list quickly; in background a heavy recompute use-case may
   * be executed to refresh results and update UI once available.
   *
   * Inline comment: the background recompute is intentionally launched on [ioDispatcher] because it
   * is CPU / IO heavy and should not block the main flow.
   *
   * @param userId id of the user whose achievements are requested.
   * @return a possibly cached list of achievements to display immediately.
   */
  private suspend fun fetchAchievements(userId: Id): List<Achievement> {
    // 1) FAST PATH: just read what we have
    val cachedOrRemote =
        runCatching { achievementRepository.getAllAchievementsByUser(userId) }
            .getOrElse {
              setErrorMsg(it.message ?: "Failed to load achievements")
              emptyList()
            }
    // 2) SLOW PATH: recompute in background
    viewModelScope.launch(ioDispatcher) {
      runCatching {
            updateUserAchievements(userId) // Very heavy
          }
          .onSuccess {
            // after recompute, refresh achievements in UI
            runCatching { achievementRepository.getAllAchievementsByUser(userId) }
                .onSuccess { fresh -> _uiState.value = _uiState.value.copy(achievements = fresh) }
          }
    }
    return cachedOrRemote
  }

  /**
   * Fetches up to [limit] post pins authored by [authorId].
   *
   * Maps post locations to geojson Points and returns at most [limit] results ordered by date.
   *
   * @param authorId id of the posts' author.
   * @param limit maximum number of pins to return.
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
   * Sends a friend request from the current user to the profile owner.
   *
   * Updates the UI optimistically and reverts on failure.
   */
  fun sendRequestToUser() {
    viewModelScope.launch {
      val state = _uiState.value
      if (state.isUserOwner) return@launch
      val currentUserId = currentUserId ?: return@launch

      _uiState.value = state.copy(friendStatus = FriendStatus.PENDING_SENT)

      try {
        friendRequestRepository.initializeFriendRequest(currentUserId, state.user.userId)
      } catch (e: Exception) {
        _uiState.value = state
        setErrorMsg("Failed to send request to user ${state.user.userId} : ${e.message}")
      }
    }
  }

  /**
   * Cancels a friend request previously sent by the current user.
   *
   * Reverts UI on failure.
   */
  fun cancelSentRequestToUser() {
    viewModelScope.launch {
      val state = _uiState.value
      if (state.isUserOwner) return@launch
      val currentUserId = currentUserId ?: return@launch
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

  /**
   * Accepts a friend request received from the profile owner.
   *
   * Updates counts and UI optimistically; reverts on failure.
   */
  fun acceptReceivedRequest() {
    viewModelScope.launch {
      val state = _uiState.value
      if (state.isUserOwner) return@launch
      val currentUserId = currentUserId ?: return@launch
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

  /**
   * Declines a friend request received from the profile owner.
   *
   * Reverts UI on failure.
   */
  fun declineReceivedRequest() {
    viewModelScope.launch {
      val state = _uiState.value
      if (state.isUserOwner) return@launch
      val currentUserId = currentUserId ?: return@launch
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

  /**
   * Removes an existing friendship between the current user and the profile owner.
   *
   * Updates UI optimistically and reverts on failure.
   */
  fun unfollowUser() {
    viewModelScope.launch {
      val state = _uiState.value
      if (state.isUserOwner) return@launch
      val currentUserId = currentUserId ?: return@launch

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
