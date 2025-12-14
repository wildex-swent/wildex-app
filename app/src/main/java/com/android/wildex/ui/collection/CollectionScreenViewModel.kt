package com.android.wildex.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Default placeholder user used when no valid user is loaded. */
private val defaultUser: SimpleUser =
    SimpleUser(
        userId = "defaultUserId",
        username = "defaultUsername",
        profilePictureURL = "",
        userType = UserType.REGULAR,
    )

data class CollectionUIState(
    val user: SimpleUser = defaultUser,
    val isUserOwner: Boolean = true,
    val animals: List<AnimalState> = emptyList(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMsg: String? = null,
    val isRefreshing: Boolean = false,
)

data class AnimalState(
    val animalId: Id,
    val pictureURL: URL,
    val name: String,
    val isUnlocked: Boolean,
)

class CollectionScreenViewModel(
    private val animalRepository: AnimalRepository = RepositoryProvider.animalRepository,
    private val userAnimalsRepository: UserAnimalsRepository =
        RepositoryProvider.userAnimalsRepository,
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val currentUserId: Id = Firebase.auth.uid ?: "",
) : ViewModel() {

  /** Backing property for the collection screen state. */
  private val _uiState = MutableStateFlow(CollectionUIState())

  /** Public immutable state exposed to the UI layer. */
  val uiState: StateFlow<CollectionUIState> = _uiState.asStateFlow()

  private suspend fun updateUIState(userUid: String, calledFromRefresh: Boolean = false) {
    try {
      if (calledFromRefresh) {
        animalRepository.refreshCache()
        userAnimalsRepository.refreshCache()
        userRepository.refreshCache()
      }
      val user = userRepository.getSimpleUser(userUid)
      _uiState.value = _uiState.value.copy(user = user, isUserOwner = userUid == currentUserId)
      val userAnimals = userAnimalsRepository.getAllAnimalsByUser(userUid).map { it.animalId }
      val animals = animalRepository.getAllAnimals()
      val animalStates =
          animals
              .filter { userUid == currentUserId || userAnimals.contains(it.animalId) }
              .map { animal ->
                AnimalState(
                    animalId = animal.animalId,
                    pictureURL = animal.pictureURL,
                    name = animal.name,
                    isUnlocked = userAnimals.contains(animal.animalId),
                )
              }
              .sortedBy { !it.isUnlocked }
      _uiState.value =
          _uiState.value.copy(
              animals = animalStates,
              isLoading = false,
              errorMsg = null,
              isError = false,
              isRefreshing = false)
    } catch (e: Exception) {
      setErrorMsg(e.localizedMessage ?: "Failed to load collection.")
      _uiState.value = _uiState.value.copy(isLoading = false, isError = true, isRefreshing = false)
    }
  }

  fun loadUIState(userUid: Id) {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState(userUid) }
  }

  fun refreshUIState(userId: Id) {
    _uiState.value = _uiState.value.copy(isRefreshing = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState(userId, calledFromRefresh = true) }
  }

  fun refreshOffline() {
    setErrorMsg("You are currently offline\nYou can not refresh for now :/")
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
