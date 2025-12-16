package com.android.wildex.ui.animal

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Animal Information screen.
 *
 * Holds the currently loaded animal fields and loading/error flags.
 */
data class AnimalInformationUIState(
    val animalId: Id = "",
    val pictureURL: URL = "",
    val name: String = "",
    val species: String = "",
    val description: String = "",
    val errorMsg: String? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
)

/**
 * ViewModel that loads and exposes animal information for the details screen.
 *
 * @param animalRepository Repository used to fetch animal data.
 */
class AnimalInformationScreenViewModel(
    private val animalRepository: AnimalRepository = RepositoryProvider.animalRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AnimalInformationUIState())
  val uiState: StateFlow<AnimalInformationUIState> = _uiState.asStateFlow()

  /** Clears any currently stored error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message into the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /**
   * Initiates loading of animal information for the given id.
   *
   * This sets the loading flag and launches a coroutine to fetch data.
   *
   * @param animalId Identifier of the animal to load.
   */
  fun loadAnimalInformation(animalId: Id) {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateAnimalInformation(animalId) }
  }

  /**
   * Suspends to fetch the animal information and updates the UI state.
   *
   * Handles exceptions by setting appropriate error messages and flags.
   *
   * @param animalId Identifier of the animal to fetch.
   */
  private suspend fun updateAnimalInformation(animalId: Id) {
    try {
      val animal = animalRepository.getAnimal(animalId)

      _uiState.value =
          AnimalInformationUIState(
              animalId = animalId,
              pictureURL = animal.pictureURL,
              name = animal.name,
              species = animal.species,
              description = animal.description,
              errorMsg = null,
              isLoading = false,
              isError = false)
    } catch (e: Exception) {
      Log.e("AnimalInformationViewModel", "Error loading animal information for $animalId", e)
      setErrorMsg("Failed to load animal information: ${e.message}")
      _uiState.value = _uiState.value.copy(isLoading = false, isError = true)
    }
  }
}
