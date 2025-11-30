package com.android.wildex.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.friendRequest.FriendRequest
import com.android.wildex.model.friendRequest.FriendRequestRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserFriendsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Id
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Represents the UI state of the Friend Screen */
data class FriendsScreenUIState(
    val friends: List<FriendState> = emptyList(),
    val suggestions: List<RecommendationResult> = emptyList(),
    val receivedRequests: List<RequestState> = emptyList(),
    val sentRequests: List<RequestState> = emptyList(),
    val isCurrentUser: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMsg: String? = null,
)

data class RequestState(val user: SimpleUser, val request: FriendRequest)

/**
 * Represents the state of a friend list element
 *
 * @property friend friend of the screen's subject (current user or any other user)
 * @property status the state of the friend relative to the current user in order to display the
 *   right interactable
 */
data class FriendState(val friend: SimpleUser, val status: FriendStatus)

enum class FriendStatus {
  FRIEND,
  NOT_FRIEND,
  PENDING_RECEIVED,
  PENDING_SENT,
  IS_CURRENT_USER
}

/**
 * ViewModel for the Friend Screen
 *
 * @param currentUserId the user id of the currently logged in user
 * @param userRepository Repository used to get Simple users from
 * @param userFriendsRepository Repository to get users' friends lists from
 * @param friendRequestRepository Repository to get ongoing friend requests from
 * @param userRecommender Recommendation algorithm to get suggested users from
 */
class FriendScreenViewModel(
    private val currentUserId: Id = Firebase.auth.uid ?: "",
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val userFriendsRepository: UserFriendsRepository =
        RepositoryProvider.userFriendsRepository,
    private val friendRequestRepository: FriendRequestRepository =
        RepositoryProvider.friendRequestRepository,
    private val userRecommender: UserRecommender = UserRecommender(currentUserId = currentUserId)
) : ViewModel() {
  /** Backing property for the friends screen state. */
  private val _uiState = MutableStateFlow(FriendsScreenUIState())

  /** Public immutable state exposed to the UI layer. */
  val uiState: StateFlow<FriendsScreenUIState> = _uiState.asStateFlow()

  private val suggestions = MutableStateFlow(listOf<RecommendationResult>())

  /**
   * Updates the UI state of the Friend Screen by fetching all needed data from the repositories.
   *
   * @param userId the user whose friend list we want to fetch
   */
  private suspend fun updateUIState(userId: Id) {
    try {
      val isCurrentUser = userId == currentUserId
      val userFriends = userFriendsRepository.getAllFriendsOfUser(userId)
      val currentUserFriends = userFriendsRepository.getAllFriendsOfUser(currentUserId)
      val currentUserSentRequests =
          friendRequestRepository.getAllFriendRequestsBySender(currentUserId)
      val currentUserReceivedRequests =
          friendRequestRepository.getAllFriendRequestsByReceiver(currentUserId)
      val friendsStates =
          userFriends.map {
            FriendState(
                friend = userRepository.getSimpleUser(it.userId),
                status =
                    when {
                      it.userId == currentUserId -> FriendStatus.IS_CURRENT_USER
                      currentUserFriends.contains(it) -> FriendStatus.FRIEND
                      currentUserSentRequests.any { request -> request.receiverId == it.userId } ->
                          FriendStatus.PENDING_SENT
                      currentUserReceivedRequests.any { request ->
                        request.senderId == it.userId
                      } -> FriendStatus.PENDING_RECEIVED
                      else -> FriendStatus.NOT_FRIEND
                    })
          }
      val receivedRequests =
          if (isCurrentUser) {
            currentUserReceivedRequests.map {
              RequestState(userRepository.getSimpleUser(it.senderId), it)
            }
          } else emptyList()
      val sentRequests =
          if (isCurrentUser) {
            currentUserSentRequests.map {
              RequestState(userRepository.getSimpleUser(it.receiverId), it)
            }
          } else emptyList()
      _uiState.value =
          _uiState.value.copy(
              friends = friendsStates,
              receivedRequests = receivedRequests,
              sentRequests = sentRequests,
              isCurrentUser = isCurrentUser,
              isRefreshing = false,
              isLoading = false,
              errorMsg = null,
              isError = false,
          )
      viewModelScope.launch {
        suggestions.value =
            if (isCurrentUser) userRecommender.getRecommendedUsers() else emptyList()
        _uiState.value = _uiState.value.copy(suggestions = suggestions.value.take(5))
      }
    } catch (e: Exception) {
      setErrorMsg(e.localizedMessage ?: "Failed to load friends and requests.")
      _uiState.value = _uiState.value.copy(isRefreshing = false, isLoading = false, isError = true)
    }
  }

  /**
   * Loads the UI state corresponding to the given user
   *
   * @param userId the user whose state we want to load
   */
  fun loadUIState(userId: Id) {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState(userId) }
  }

  /**
   * Refreshes the UI state corresponding to the given user
   *
   * @param userId the user whose friendship state is loaded
   */
  fun refreshUIState(userId: Id) {
    _uiState.value = _uiState.value.copy(isRefreshing = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState(userId) }
  }

  /**
   * Sends a friend request to the given user. If the screen is the current user's friend screen,
   * sending a friend request means that a new request appears in the sent requests section, and the
   * user is removed from the friend list in case the follow was initiated there (possible if the
   * current user unfollows one of his friends but then resends a friend request). If the screen is
   * another user's friend screen, sending a friend request means updating the friend states to
   * notify that a friend request to the given user was initiated and allowing to cancel the friend
   * request.
   *
   * @param userId the user who the current user sends a friend request to
   */
  fun sendRequestToUser(userId: Id) {
    viewModelScope.launch {
      val state = _uiState.value
      val request = FriendRequest(senderId = currentUserId, receiverId = userId)
      val requestState = RequestState(userRepository.getSimpleUser(request.receiverId), request)
      val sentRequests = state.sentRequests
      val friends = state.friends
      var newSentRequests: List<RequestState>
      var newFriends: List<FriendState>

      if (state.isCurrentUser) {
        newSentRequests = sentRequests + listOf(requestState)
        newFriends = friends.filter { it.friend.userId != userId }
        suggestions.value = suggestions.value.filter { it.user.userId != userId }
      } else {
        newSentRequests = sentRequests
        newFriends =
            friends.map {
              if (it.friend.userId == userId) {
                it.copy(status = FriendStatus.PENDING_SENT)
              } else it
            }
      }

      _uiState.value =
          state.copy(
              friends = newFriends,
              sentRequests = newSentRequests,
              suggestions = suggestions.value.take(5))

      try {
        friendRequestRepository.initializeFriendRequest(currentUserId, userId)
        // to always display some suggestions to the current user, we get new ones when there are
        // not enough left
        if (state.isCurrentUser && suggestions.value.size < 5) {
          suggestions.value = userRecommender.getRecommendedUsers()
        }
      } catch (e: Exception) {
        _uiState.value = state
        setErrorMsg("Failed to send request to user $userId : ${e.message}")
        // if the request was done in the suggestions, we need to retrieve the suggestion
        if (state.isCurrentUser) suggestions.value = state.suggestions
      }
    }
  }

  /**
   * Unfollows the given user. Unfollowing is always possible whether we are viewing the current
   * user's friend screen or that of another user and it results in the same effect in both views.
   * Unfollowing a user means updating the friends states to reflect the change in the screen, i.e.
   * notify that the given user was unfollowed and allowing the current user to send a new friend
   * request if the action was accidental.
   *
   * @param userId the user who the current user wishes to unfollow
   */
  fun unfollowUser(userId: Id) {
    viewModelScope.launch {
      val state = _uiState.value

      val friends = state.friends
      val newFriends =
          friends.map {
            if (it.friend.userId == userId) it.copy(status = FriendStatus.NOT_FRIEND) else it
          }

      _uiState.value = state.copy(friends = newFriends)

      try {
        userFriendsRepository.deleteFriendToUserFriendsOfUser(userId, currentUserId)
        userFriendsRepository.deleteFriendToUserFriendsOfUser(currentUserId, userId)
      } catch (e: Exception) {
        _uiState.value = state
        setErrorMsg("Failed to unfollow user $userId : ${e.message}")
      }
    }
  }

  /**
   * Accepts a request received by the current user. If the screen is the current user's screen,
   * accepting a received friend request results in the friend request disappearing from the
   * received requests section and a new friend appearing in the current user's friend list.
   * Otherwise, it results in the friend appearing as not friend relative to the current user and
   * allowing the current user to send a friend request.
   *
   * @param userId the user whose friend request is accepted
   */
  fun acceptReceivedRequest(userId: Id) {
    viewModelScope.launch {
      val state = _uiState.value

      val receivedRequests = state.receivedRequests
      val newReceivedRequests = receivedRequests.filter { it.request.senderId != userId }

      val friends = state.friends
      val newFriends =
          if (state.isCurrentUser) {
            friends +
                listOf(
                    FriendState(
                        friend = userRepository.getSimpleUser(userId),
                        status = FriendStatus.FRIEND))
          } else {
            friends.map {
              if (it.friend.userId == userId) it.copy(status = FriendStatus.FRIEND) else it
            }
          }

      _uiState.value = state.copy(friends = newFriends, receivedRequests = newReceivedRequests)

      try {
        friendRequestRepository.acceptFriendRequest(FriendRequest(userId, currentUserId))
      } catch (e: Exception) {
        _uiState.value = state
        setErrorMsg("Failed to accept request from user $userId : ${e.message}")
      }
    }
  }

  /**
   * Declines a friend request received by the current user. If the screen is the current user's
   * screen, declining the request always results in the request simply disappearing from view.
   * Otherwise, it results in the friend appearing as not friend and allowing the current user to
   * send a friend request
   *
   * @param userId the user whose request is declined
   */
  fun declineReceivedRequest(userId: Id) {
    viewModelScope.launch {
      val state = _uiState.value

      val receivedRequests = state.receivedRequests
      val newReceivedRequests = receivedRequests.filter { it.request.senderId != userId }

      val friends = state.friends
      val newFriendStates =
          if (!state.isCurrentUser) {
            friends.map {
              if (it.friend.userId == userId) it.copy(status = FriendStatus.NOT_FRIEND) else it
            }
          } else friends

      _uiState.value = state.copy(receivedRequests = newReceivedRequests, friends = newFriendStates)

      try {
        friendRequestRepository.refuseFriendRequest(FriendRequest(userId, currentUserId))
      } catch (e: Exception) {
        _uiState.value = state
        setErrorMsg("Failed to decline request from user $userId : ${e.message}")
      }
    }
  }

  /**
   * Cancels a friend request sent by the current user. If the screen is the current user's friend
   * screen, cancelling a sent friend request simply means removing it from the sent request. If the
   * screen is another user's friend screen, cancelling a sent friend request means updating the
   * friend state of the receiver so that the screen can display the fact that there are no pending
   * request anymore and can allow the current user to send a new friend request.
   *
   * @param userId the user the friend request to cancel is destined to
   */
  fun cancelSentRequest(userId: Id) {
    viewModelScope.launch {
      val state = _uiState.value

      val sentRequests = state.sentRequests
      // if the screen is the current user's, we remove the cancelled sent request
      // otherwise we don't need to do it since we don't show requests on other users' friend
      // screens
      val newSentRequests =
          if (state.isCurrentUser) {
            sentRequests.filter { it.request.receiverId != userId }
          } else sentRequests

      val friends = state.friends
      // if the screen is another user's, we still show the friend in the other user's list but
      // change the status
      // of this friend relative to the current user.
      // Otherwise, we don't need to touch the friends list
      val newFriends =
          if (!state.isCurrentUser) {
            friends.map {
              if (it.friend.userId == userId) it.copy(status = FriendStatus.NOT_FRIEND) else it
            }
          } else friends

      _uiState.value = state.copy(friends = newFriends, sentRequests = newSentRequests)

      try {
        friendRequestRepository.refuseFriendRequest(FriendRequest(currentUserId, userId))
        viewModelScope.launch {
          if (state.isCurrentUser) {
            suggestions.value = userRecommender.getRecommendedUsers()
          }
          _uiState.value = _uiState.value.copy(suggestions = suggestions.value.take(5))
        }
      } catch (e: Exception) {
        _uiState.value = state
        setErrorMsg("Failed to cancel request to user $userId : ${e.message}")
      }
    }
  }

  /** Clears any existing error message from the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Shows an offline error message when trying to refresh while offline. */
  fun refreshOffline() {
    setErrorMsg("You are currently offline\nYou can not refresh for now :/")
  }

  /**
   * Sets a new error message in the UI state.
   *
   * @param msg the error message
   */
  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }
}
