package com.android.wildex.ui.profile

import androidx.lifecycle.ViewModel
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EditProfileUIState(
    val name: String = "",
    val surname: String = "",
    val username: String = "",
    val description: String = "",
    val country: String = "",
    val profileImageUrl: URL = "",
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val isError: Boolean = false,
)

class EditProfileViewModel(
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val storageRepository: StorageRepository = RepositoryProvider.storageRepository,
    private val currentUserId: Id? = Firebase.auth.uid
) : ViewModel() {
  private val _uiState = MutableStateFlow(EditProfileUIState())
  val uiState: StateFlow<EditProfileUIState> = _uiState.asStateFlow()
}
