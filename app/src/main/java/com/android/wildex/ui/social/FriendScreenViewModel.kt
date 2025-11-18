package com.android.wildex.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.relationship.Relationship
import com.android.wildex.model.relationship.RelationshipRepository
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
  val receivedRequests: List<RequestState> = emptyList(),
  val sentRequests: List<RequestState> = emptyList(),
  val isCurrentUser: Boolean = false,
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

data class RequestState(
  val request: Relationship,
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

  private suspend fun updateUIState() {
    try {
      _uiState.value =
        _uiState.value.copy(
          isLoading = false,
          errorMsg = null,
          isError = false,
        )
    } catch (e: Exception) {
      setErrorMsg(e.localizedMessage ?: "Failed to load posts.")
      _uiState.value = _uiState.value.copy(isLoading = false, isError = true)
    }
  }

  fun loadUIState() {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState() }
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