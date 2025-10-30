package com.android.wildex.ui.collection

import androidx.lifecycle.ViewModel
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animaldetector.AnimalRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
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

data class CollectionUIState(
    val user: SimpleUser = defaultUser,
    val isUserOwner: Boolean = false,
    val animals: List<AnimalState> = emptyList(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMsg: String? = null,
    val isRefreshing: Boolean = false
)

data class AnimalState(
    val animalId: Id = "defaultAnimalId",
    val pictureURL: URL = "",
    val name: String = "Default Animal",
    val isUnlocked: Boolean = false
)

class CollectionScreenViewModel(
    private val userAnimalsRepository : UserAnimalsRepository,
    //= RepositoryProvider.userAnimalsRepository,
    private val animalRepository : AnimalRepository = RepositoryProvider.animalRepository,
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val currentUserId: Id? = Firebase.auth.uid
) : ViewModel() {

  /** Backing property for the collection screen state. */
  private val _uiState = MutableStateFlow(CollectionUIState())
  val uiState: StateFlow<CollectionUIState> = _uiState.asStateFlow()
}
