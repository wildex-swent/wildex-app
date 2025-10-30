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

class AnimalInformationScreenViewModel(
    private val animalRepository: AnimalRepository = RepositoryProvider.animalRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AnimalInformationUIState())
  val uiState: StateFlow<AnimalInformationUIState> = _uiState.asStateFlow()

  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  fun loadAnimalInformation(animalId: Id) {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null, isError = false)
    viewModelScope.launch { updateAnimalInformation(animalId) }
  }

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
