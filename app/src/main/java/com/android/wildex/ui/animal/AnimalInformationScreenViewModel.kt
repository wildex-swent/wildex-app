package com.android.wildex.ui.animal

import androidx.lifecycle.ViewModel
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
import com.android.wildex.ui.home.defaultUser
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    // TODO: Add animal repository once implemented
    private val currentUserId: Id =
        try {
          Firebase.auth.uid
        } catch (_: Exception) {
          defaultUser.userId
        } ?: defaultUser.userId,
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
    // TODO: set up the UI state
  }
}
