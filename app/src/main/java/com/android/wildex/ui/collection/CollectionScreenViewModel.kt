package com.android.wildex.ui.collection

import androidx.lifecycle.ViewModel
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.utils.Id
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Default placeholder user used when no valid user is loaded. */
val defaultUser: SimpleUser =
    SimpleUser(
        userId = "defaultUserId",
        username = "defaultUsername",
        profilePictureURL = "",
    )

/** Default placeholder animal used when no valid animal is associated with a post. */
val defaultAnimal: Animal =
    Animal(
        animalId = "defaultAnimalId",
        name = "Default Animal",
        species = "Unknown",
        description = "This is a default animal.",
        pictureURL =
            "https://www.publicdomainpictures.net/pictures/320000/velka/background-image.png",
    )

data class CollectionUIState(
    val userUid: String = "",
    val isUserOwner: Boolean = false,
    val animals: List<AnimalState> = emptyList(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMsg: String? = null,
    val isRefreshing: Boolean = false
)

data class AnimalState(val animal: Animal = defaultAnimal, val isUnlocked: Boolean = false)

class CollectionScreenViewModel(
    // private val userAnimalsRepository : UserAnimalsRepository =
    // RepositoryProvider.userAnimalsRepository,
    // private val animalRepository : AnimalRepository = RepositoryProvider.animalRepository,
    private val currentUserId: Id? = Firebase.auth.uid
) : ViewModel() {

  /** Backing property for the collection screen state. */
  private val _uiState = MutableStateFlow(CollectionUIState())
  val uiState: StateFlow<CollectionUIState> = _uiState.asStateFlow()
}
