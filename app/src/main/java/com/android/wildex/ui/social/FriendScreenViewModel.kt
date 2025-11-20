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
import kotlinx.coroutines.launch

data class FriendsScreenUIState(
  val friends: List<FriendState> = emptyList(),
  val suggestions: List<SuggestionState> = emptyList(),
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

data class SuggestionState(
  val suggestedUser: SimpleUser,
  val suggestionReason: String,
  val isPending: Boolean
)

class FriendScreenViewModel(
  private val currentUserId: Id = Firebase.auth.uid ?: "",
  private val userRepository: UserRepository = RepositoryProvider.userRepository,
  private val userFriendsRepository: UserFriendsRepository,
  private val friendRequestRepository: RelationshipRepository = RepositoryProvider.relationshipRepository,
  //private val userRecommender: UserRecommender = UserRecommender(currentUserId)
) : ViewModel() {
  /** Backing property for the friends screen state. */
  private val _uiState = MutableStateFlow(FriendsScreenUIState())

  /** Public immutable state exposed to the UI layer. */
  val uiState: StateFlow<FriendsScreenUIState> = _uiState.asStateFlow()

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
      //val suggestions = if (isCurrentUser) userRecommender.getRecommendedUsers() else emptyList()
      val receivedRequests = if (isCurrentUser){
        friendRequestRepository.getAllPendingRelationshipsByReceiver(currentUserId)
      } else emptyList()
      val sentRequests = if (isCurrentUser){
        friendRequestRepository.getAllPendingRelationshipsBySender(currentUserId)
      } else emptyList()
      _uiState.value =
        _uiState.value.copy(
          friends = friendsStates,
          //suggestions = suggestions,
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
      val newSentRequests = if (state.isCurrentUser) sentRequests + listOf(request) else sentRequests
      val friends = state.friends
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
      val suggestions = state.suggestions
      val newSuggestions = if (state.isCurrentUser){
        suggestions.filter {
          it.suggestedUser.userId != userId
        }
      } else suggestions

      _uiState.value = state.copy(
        friends = newFriends,
        sentRequests = newSentRequests,
        suggestions = newSuggestions
      )

      try{
        friendRequestRepository.initializeRelationship(currentUserId, userId)
      } catch (e: Exception){
        setErrorMsg("Failed to send request to user $userId : ${e.message}")
        _uiState.value = state
      }
    }
  }

  fun unfollowUser(userId: Id){}

  fun acceptReceivedRequest(userId: Id){}

  fun declineReceivedRequest(userId: Id){}

  fun cancelSentRequest(userId: Id){}


  /** Clears any existing error message from the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets a new error message in the UI state. */
  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }
}