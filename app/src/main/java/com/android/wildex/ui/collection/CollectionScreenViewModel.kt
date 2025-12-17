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

/**
 * UI state for the Collection screen.
 *
 * Contains the displayed user, whether the viewed collection belongs to the current user, the list
 * of animal states and loading / error flags.
 */
data class CollectionUIState(
    val user: SimpleUser = defaultUser,
    val isUserOwner: Boolean = true,
    val animals: List<AnimalState> = emptyList(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMsg: String? = null,
    val isRefreshing: Boolean = false,
)

/**
 * Represents a single animal presentation state in the collection list.
 *
 * @property animalId The unique id of the animal.
 * @property pictureURL URL to the animal picture.
 * @property name The display name of the animal.
 * @property isUnlocked True if the user has unlocked this animal.
 */
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

  /**
   * Internal suspend function that loads and updates the UI state.
   *
   * This function fetches user info, all animals, and the animals of the given user. It will
   * optionally refresh underlying repositories when called from a refresh request.
   *
   * Note: this method performs multiple repository calls and mapping operations (can fail), and
   * thus contains error handling to update the UI state accordingly.
   *
   * @param userUid The user id whose collection should be displayed.
   * @param calledFromRefresh If true, force a cache refresh on involved repositories first.
   */
  private suspend fun updateUIState(userUid: String, calledFromRefresh: Boolean = false) {
    try {
      if (calledFromRefresh) {
        animalRepository.refreshCache()
        userAnimalsRepository.refreshCache()
        userRepository.refreshCache()
      }
      val user = userRepository.getSimpleUser(userUid)
      _uiState.value = _uiState.value.copy(user = user, isUserOwner = userUid == currentUserId)
      val animals = animalRepository.getAllAnimals()
      val userAnimals = userAnimalsRepository.getAllAnimalsByUser(userUid).map { it.animalId }
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

  /**
   * Begins loading the UI state for the collection of [userUid].
   *
   * This updates loading flags and launches a coroutine to perform the actual loading.
   *
   * @param userUid The user id to load the collection for.
   */
  fun loadUIState(userUid: Id) {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState(userUid) }
  }

  /**
   * Refreshes the collection UI state and forces repository caches to update.
   *
   * @param userId The user id to refresh the collection for.
   */
  fun refreshUIState(userId: Id) {
    _uiState.value = _uiState.value.copy(isRefreshing = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateUIState(userId, calledFromRefresh = true) }
  }

  /**
   * Called when the user triggers a refresh while offline.
   *
   * Updates the UI with an appropriate offline error message.
   */
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
