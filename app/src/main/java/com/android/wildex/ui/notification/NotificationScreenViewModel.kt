package com.android.wildex.ui.notification

import androidx.lifecycle.ViewModel
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

val defaultUser = User(
    "",
    "defaultUsername",
    "defaultName",
    "defaultSurname",
    "This is a default bio.",
    "",
    UserType.REGULAR,
    Timestamp.now(),
    "DefaultCountry",
    0,
)

data class NotificationUIState(
    val profilePictureUrl: URL = defaultUser.profilePictureURL,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMsg: String? = null,
    val isError: Boolean = false,
)

class NotificationScreenViewModel (
    private val currentUserId: Id = Firebase.auth.uid ?: "",
) : ViewModel() {
    /** Backing property for the home screen state. */
    private val _uiState = MutableStateFlow(NotificationUIState())

    /** Public immutable state exposed to the UI layer. */
    val uiState: StateFlow<NotificationUIState> = _uiState.asStateFlow()

}