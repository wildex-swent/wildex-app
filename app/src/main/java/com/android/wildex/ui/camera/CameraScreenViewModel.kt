package com.android.wildex.ui.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.animaldetector.AnimalDetectResponse
import com.android.wildex.model.animaldetector.AnimalInfoRepository
import com.android.wildex.model.location.GeocodingRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.utils.Id
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
    val isSavingOffline: Boolean = false,
)

class CameraScreenViewModel(
    private val postsRepository: PostsRepository = RepositoryProvider.postRepository,
    private val storageRepository: StorageRepository = RepositoryProvider.storageRepository,
    private val userAnimalsRepository: UserAnimalsRepository =
        RepositoryProvider.userAnimalsRepository,
    private val animalRepository: AnimalRepository = RepositoryProvider.animalRepository,
    private val animalInfoRepository: AnimalInfoRepository =
        RepositoryProvider.animalInfoRepository,
    private val geocodingRepository: GeocodingRepository = RepositoryProvider.geocodingRepository,
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
    _uiState.value = _uiState.value.copy(isDetecting = true, currentImageUri = imageUri)
    viewModelScope.launch {
      try {
        val animalDetectResponse = animalInfoRepository.detectAnimal(context, imageUri).first()
        _uiState.value =
            _uiState.value.copy(animalDetectResponse = animalDetectResponse, isDetecting = false)
        Log.d("CameraScreenViewModel", "Animal detected: ${animalDetectResponse.animalType}")
      } catch (e: Exception) {
        setErrorMsg("Failed to detect animal and start post creation : ${e.message}")
        _uiState.value = _uiState.value.copy(isDetecting = false, currentImageUri = null)
      }
    }
  }

  /**
   * Enters offline preview mode with the given image URI.
   *
   * @param imageUri The URI of the image to preview offline.
   */
  fun enterOfflinePreview(imageUri: Uri) {
    _uiState.value = _uiState.value.copy(isSavingOffline = true, currentImageUri = imageUri)
  }

  /**
   * Saves the current image to the device gallery.
   *
   * @param context The context used to access the device's media storage.
   */
  fun saveImageToGallery(context: Context) {
    val uri = _uiState.value.currentImageUri ?: return
    viewModelScope.launch {
      try {
        val resolver = context.contentResolver
        resolver.openInputStream(uri).use { inputStream ->
          if (inputStream == null) {
            setErrorMsg("Failed to open source image.")
            return@launch
          }

          val contentValues =
              ContentValues().apply {
                put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    "wildex_${System.currentTimeMillis()}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Wildex")
              }

          val galleryUri =
              resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                  ?: throw Exception("Gallery entry could not be created")

          resolver.openOutputStream(galleryUri).use { outputStream ->
            if (outputStream == null) {
              throw Exception("Could not open gallery output stream")
            }
            inputStream.copyTo(outputStream)
          }
        }

        setErrorMsg("Image saved to gallery")
        _uiState.value = _uiState.value.copy(currentImageUri = null, isSavingOffline = false)
      } catch (e: Exception) {
        setErrorMsg("Failed to save image: ${e.message}")
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
        val imageUrl = storageRepository.uploadPostImage(postId, uri)
        val animalId = response.taxonomy.id
        val location =
            if (_uiState.value.addLocation) {
              LocationServices.getFusedLocationProviderClient(context).lastLocation.await().let {
                geocodingRepository.reverseGeocode(it.latitude, it.longitude)
              }
            } else null
        val finalPost =
            Post(
                postId = postId,
                authorId = currentUserId!!,
                description = uiState.value.description,
                location = location,
                date = Timestamp.now(),
                pictureURL = imageUrl,
                animalId = animalId,
            )
        postsRepository.addPost(finalPost)
        registerAnimal(animalId, context)
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
  private suspend fun registerAnimal(animalId: Id, context: Context) {
    val detection = uiState.value.animalDetectResponse ?: return

    val animalDescription = animalInfoRepository.getAnimalDescription(detection.animalType)
    val animalPicture = animalInfoRepository.getAnimalPicture(context, detection.animalType)
    val animalPictureURL = storageRepository.uploadAnimalPicture(animalId, animalPicture)
    val animal =
        Animal(
            animalId,
            animalPictureURL,
            detection.animalType,
            detection.taxonomy.species,
            animalDescription,
        )
    animalRepository.addAnimal(animal)
    userAnimalsRepository.addAnimalToUserAnimals(currentUserId!!, animalId)
  }
}
