package com.android.wildex.ui.camera

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animaldetector.AnimalDetectResponse
import com.android.wildex.model.animaldetector.AnimalInfoRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CameraUiState(
    val animalDetectResponse: AnimalDetectResponse? = null,
    val postInConstruction: Post? = null,
    val errorMsg: String? = null,
    val isLoading: Boolean = false,
    val isDetecting: Boolean = false,
)

class CameraScreenViewModel(
    private val postsRepository: PostsRepository = RepositoryProvider.postRepository,
    private val animalRepository: AnimalInfoRepository = RepositoryProvider.animalInfoRepository,
    // private val collectionRepository: CollectionRepository =
    // RepositoryProvider.collectionRepository,
    private val currentUserId: Id? = Firebase.auth.uid,
) : ViewModel() {
  private val _uiState = MutableStateFlow(CameraUiState())
  val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

  fun resetState() {
    /* resets to default state where camera preview is shown */
  }

  fun detectAnimalImage(imageUri: Uri) {
    /* detects animal in the image, and creates postInConstruction */
  }

  fun updateDescription(description: String) {
    /* updates the description of the post in construction, keeps it across config changes */
  }

  fun createPost(location: Location, onPost: () -> Unit) {
    /* calls `registerAnimal` and creates a post with the detected animal and current user,
    with the location taken from the composable, and triggers onPost when it is posted */
  }

  /** Clears any existing error message from the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets a new error message in the UI state. */
  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  private fun registerAnimal() {
    /* registers the animal from the animal response in the repository if not already there */
  }
}
