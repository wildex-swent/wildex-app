package com.android.wildex.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animaldetector.AnimalDetectResponse
import com.android.wildex.model.animaldetector.AnimalInfoRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class CameraUiState(
    val animalDetectResponse: AnimalDetectResponse? = null,
    val currentImageUri: Uri? = null,
    val description: String = "",
    val addLocation: Boolean = false,
    val errorMsg: String? = null,
    val isLoading: Boolean = false,
    val isDetecting: Boolean = false,
)

class CameraScreenViewModel(
    private val postsRepository: PostsRepository = RepositoryProvider.postRepository,
    private val animalRepository: AnimalInfoRepository = RepositoryProvider.animalInfoRepository,
    private val storageRepository: StorageRepository = RepositoryProvider.storageRepository,
    // private val collectionRepository: CollectionRepository =
    // RepositoryProvider.collectionRepository,
    private val currentUserId: Id? = Firebase.auth.uid,
) : ViewModel() {
  private val _uiState = MutableStateFlow(CameraUiState())
  val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

  /* resets to default state where camera preview is shown */
  fun resetState() {
    _uiState.value = CameraUiState()
  }

  /* detects animal in the image */
  fun detectAnimalImage(imageUri: Uri, context: Context) {
    _uiState.value = _uiState.value.copy(isDetecting = true)
    viewModelScope.launch {
      try {
        val animalDetectResponse = animalRepository.detectAnimal(context, imageUri).first()
        _uiState.value =
            _uiState.value.copy(
                animalDetectResponse = animalDetectResponse,
                currentImageUri = imageUri,
                isDetecting = false,
            )
        Log.d("CameraScreenViewModel", "Animal detected: ${animalDetectResponse.animalType}")
      } catch (e: Exception) {
        setErrorMsg("Failed to detect animal and start post creation : ${e.message}")
        resetState()
      }
    }
  }

  /* updates the image uri of the post in construction, keeps it across config changes */
  fun updateImageUri(imageUri: Uri) {
    _uiState.value = _uiState.value.copy(currentImageUri = imageUri)
  }

  /* updates the description of the post in construction, keeps it across config changes */
  fun updateDescription(description: String) {
    _uiState.value = _uiState.value.copy(description = description)
  }

  /* toggles whether to add location to the post in construction */
  fun toggleAddLocation() {
    _uiState.value = _uiState.value.copy(addLocation = !_uiState.value.addLocation)
  }

  /* calls `registerAnimal` and creates a post with the detected animal and current user,
  with the location taken from the composable, and triggers onPost when it is posted */
  @SuppressLint("MissingPermission")
  fun createPost(context: Context, onPost: () -> Unit) {
    val uri = uiState.value.currentImageUri ?: return
    val response = uiState.value.animalDetectResponse ?: return
    _uiState.value = _uiState.value.copy(isLoading = true)
    viewModelScope.launch {
      try {
        val postId = postsRepository.getNewPostId()
        val imageUrl = storageRepository.uploadPostImage(postId, uri)!!
        val animalId = response.taxonomy.id
        val location =
            LocationServices.getFusedLocationProviderClient(context).lastLocation.await().let {
              Location(it.latitude, it.longitude)
            }
        val finalPost =
            Post(
                postId = postId,
                authorId = currentUserId!!,
                description = uiState.value.description,
                location = location,
                date = Timestamp.now(),
                pictureURL = imageUrl,
                animalId = animalId,
                likesCount = 0,
                commentsCount = 0,
            )
        postsRepository.addPost(finalPost)
        registerAnimal(animalId)
        resetState()
        onPost()
      } catch (e: Exception) {
        setErrorMsg("Failed to create post : ${e.message}")
        _uiState.value = _uiState.value.copy(isLoading = false)
      }
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

  /* registers the animal from the animal response in the repository if not already there */
  private suspend fun registerAnimal(animalId: Id) {
    val detection = uiState.value.animalDetectResponse ?: return
    val animalDescription = animalRepository.getAnimalDescription(detection.animalType)
    /*
    val animalPicture = animalRepository.getAnimalPicture(detection.animalType)
    val animalPictureURL = storageRepository.uploadAnimalPicture(animalId,
    animalPic)
    val animal = Animal(
        animalId,
        animalPictureId,
        detection.animalType,
        detection.taxonomy.species,
        animalDescription
    )
    collectionRepository.addAnimalIfNotExists(animal)
    */
  }
}
