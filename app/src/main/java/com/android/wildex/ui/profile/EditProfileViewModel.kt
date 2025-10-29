package com.android.wildex.ui.profile

import androidx.lifecycle.ViewModel
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.google.firebase.Timestamp

private val emptyUser = User("", "", "", "", "", "", UserType.REGULAR, Timestamp(0, 0), "", 0)

data class EditProfileUIState(
    val user: User = emptyUser,
    val isNewUser: Boolean = false,
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val isError: Boolean = false,
)

class EditProfileViewModel() : ViewModel() {}
