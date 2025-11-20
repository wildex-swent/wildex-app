package com.android.wildex.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.relationship.Relationship
import com.android.wildex.model.relationship.RelationshipRepository
import com.android.wildex.model.relationship.StatusEnum
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserFriendsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Id
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch

data class FriendsScreenUIState(
  val friends: List<FriendState> = emptyList(),
  val suggestions: List<RecommendationResult> = emptyList(),
  val receivedRequests: List<Relationship> = emptyList(),
  val sentRequests: List<Relationship> = emptyList(),
  val isCurrentUser: Boolean = false,
  val isRefreshing: Boolean = false,
  val isLoading: Boolean = false,
  val isError: Boolean = false,
  val errorMsg: String? = null,
)

data class FriendState(
  val friend: SimpleUser,
  val isFriend: Boolean,
  val isPending: Boolean
)

class FriendScreenViewModel(
  private val currentUserId: Id = Firebase.auth.uid ?: "",
  private val userRepository: UserRepository = RepositoryProvider.userRepository,
  private val userFriendsRepository: UserFriendsRepository,
  private val friendRequestRepository: RelationshipRepository = RepositoryProvider.relationshipRepository,
  private val userRecommender: UserRecommender
) : ViewModel() {
  /** Backing property for the friends screen state. */
  private val _uiState = MutableStateFlow(FriendsScreenUIState())

  /** Public immutable state exposed to the UI layer. */
  val uiState: StateFlow<FriendsScreenUIState> = _uiState.asStateFlow()

  private val suggestions = MutableStateFlow(listOf<RecommendationResult>())

  private suspend fun updateUIState(userId: Id) {
    try {
      val isCurrentUser = userId == currentUserId
      val userFriends = userFriendsRepository.getAllFriendsOfUser(userId)
      val currentUserFriends = userFriendsRepository.getAllFriendsOfUser(currentUserId)
      val currentUserSentRequests = friendRequestRepository.getAllPendingRelationshipsBySender(currentUserId)
      val friendsStates = userFriends.map {
        FriendState(
          friend = userRepository.getSimpleUser(it),
          isFriend = currentUserFriends.contains(it),
          isPending = currentUserSentRequests.any { request -> request.receiverId == it }
        )
      }
      suggestions.value = if (isCurrentUser) userRecommender.getRecommendedUsers() else emptyList()
      val receivedRequests = if (isCurrentUser){
        friendRequestRepository.getAllPendingRelationshipsByReceiver(currentUserId)
      } else emptyList()
      val sentRequests = if (isCurrentUser){
        friendRequestRepository.getAllPendingRelationshipsBySender(currentUserId)
      } else emptyList()
      _uiState.value =
        _uiState.value.copy(
          friends = friendsStates,
          suggestions = suggestions.value.take(5),
          receivedRequests = receivedRequests,
          sentRequests = sentRequests,
          isCurrentUser = isCurrentUser,
          isRefreshing = false,
          isLoading = false,
          errorMsg = null,
          isError = false,
        )
    } catch (e: Exception) {
      setErrorMsg(e.localizedMessage ?: "Failed to load friends and requests.")
      _uiState.value = _uiState.value.copy(isRefreshing = false, isLoading = false, isError = true)
    }
  }

  fun loadUIState(userId: Id) {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState(userId) }
  }

  fun refreshUIState(userId: Id) {
    _uiState.value = _uiState.value.copy(isRefreshing = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState(userId) }
  }

  fun sendRequestToUser(userId: Id){
    viewModelScope.launch {
      val state = _uiState.value
      val request = Relationship(
        senderId = currentUserId,
        receiverId = userId,
        status = StatusEnum.PENDING
      )
      val sentRequests = state.sentRequests
      //if the screen shows the current user's friendships state, append the new request to show on the screen,
      // otherwise we don't need to do it since we don't show requests of other users
      val newSentRequests = if (state.isCurrentUser) sentRequests + listOf(request) else sentRequests
      val friends = state.friends

      //if the screen is the current user's, we remove the friend from the friend list, if the request was done there, to be moved to the requests
      // otherwise we show that there is a pending request
      val newFriends = if (state.isCurrentUser){
        friends.filter {
          it.friend.userId != userId
        }
      } else {
        friends.map {
          if (it.friend.userId == userId) {
            it.copy(isPending = true)
          } else it
        }
      }

      //if the screen is the current user's, we remove the suggestion, if the request was done there, to be moved to the requests
      // otherwise we don't need to do it since we don't show suggestions on other users' friend screens.
      suggestions.value = if (state.isCurrentUser){
        suggestions.value.filter {
          it.user.userId != userId
        }
      } else suggestions.value

      _uiState.value = state.copy(
        friends = newFriends,
        sentRequests = newSentRequests,
        suggestions = suggestions.value.take(5)
      )

      try{
        friendRequestRepository.initializeRelationship(currentUserId, userId)
        //to always display some suggestions to the current user, we get new ones when there are not enough left
        if (state.isCurrentUser && suggestions.value.size < 5) {
          suggestions.value = userRecommender.getRecommendedUsers()
        }
      } catch (e: Exception){
        setErrorMsg("Failed to send request to user $userId : ${e.message}")
        _uiState.value = state
        //if the request was done in the suggestions, we need to retrieve the suggestion
        if (state.isCurrentUser) suggestions.value = state.suggestions
      }
    }
  }

  fun unfollowUser(userId: Id){
    viewModelScope.launch {
      val state = _uiState.value

      val friends = state.friends
      val newFriends = friends.map {
          if (it.friend.userId == userId) it.copy(isFriend = false, isPending = false)
          else it
        }

      _uiState.value = state.copy(
        friends = newFriends
      )

      try {
        userFriendsRepository.deleteFriendToUserFriendsOfUser(userId, currentUserId)
        userFriendsRepository.deleteFriendToUserFriendsOfUser(currentUserId, userId)
      } catch (e: Exception) {
        setErrorMsg("Failed to unfollow user $userId : ${e.message}")
        _uiState.value = state
      }
    }
  }

  fun acceptReceivedRequest(userId: Id){
    viewModelScope.launch {
      val state = _uiState.value

      val receivedRequests = state.receivedRequests
      val newReceivedRequests = receivedRequests.filter {
        it.senderId != userId
      }

      val friends = state.friends
      val newFriends = friends + listOf(
        FriendState(
          friend = userRepository.getSimpleUser(userId),
          isFriend = true,
          isPending = false
        )
      )

      _uiState.value = state.copy(
        friends = newFriends,
        receivedRequests = newReceivedRequests
      )

      try {
        friendRequestRepository.acceptRelationship(Relationship(userId, currentUserId))
        userFriendsRepository.addFriendToUserFriendsOfUser(userId, currentUserId)
        userFriendsRepository.addFriendToUserFriendsOfUser(currentUserId, userId)
      } catch (e: Exception){
        setErrorMsg("Failed to accept request from user $userId : ${e.message}")
        _uiState.value = state
      }
    }
  }

  fun declineReceivedRequest(userId: Id){
    viewModelScope.launch {
      val state = _uiState.value

      val receivedRequests = state.receivedRequests
      val newReceivedRequests = receivedRequests.filter {
        it.senderId != userId
      }

      _uiState.value = state.copy(
        receivedRequests = newReceivedRequests
      )

      try {
        friendRequestRepository.deleteRelationship(Relationship(userId, currentUserId))
      } catch (e: Exception){
        setErrorMsg("Failed to decline request from user $userId : ${e.message}")
        _uiState.value = state
      }
    }
  }

  fun cancelSentRequest(userId: Id){
    viewModelScope.launch {
      val state = _uiState.value

      val sentRequests = state.sentRequests
      //if the screen is the current user's, we remove the cancelled sent request
      // otherwise we don't need to do it since we don't show requests on other users' friend screens
      val newSentRequests = if (state.isCurrentUser) {
        sentRequests.filter {
          it.receiverId != userId
        }
      } else sentRequests

      val friends = state.friends
      //if the screen is another user's, we still show the friend in the other user's list but change the status
      // of this friend relative to the current user.
      // Otherwise, we don't need to touch the friends list
      val newFriends = if (!state.isCurrentUser){
        friends.map {
          if (it.friend.userId == userId) it.copy(isFriend = false, isPending = false)
          else it
        }
      } else friends

      _uiState.value = state.copy(
        friends = newFriends,
        sentRequests = newSentRequests
      )
    }
  }


  /** Clears any existing error message from the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets a new error message in the UI state. */
  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }
}