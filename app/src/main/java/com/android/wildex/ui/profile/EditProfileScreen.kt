package com.android.wildex.ui.profile

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.utils.CountryDropdown
import com.android.wildex.ui.utils.offline.OfflineScreen

object EditProfileScreenTestTags {
  const val GO_BACK = "edit_profile_screen_go_back_button"
  const val INPUT_NAME = "edit_profile_screen_input_name"
  const val INPUT_SURNAME = "edit_profile_screen_input_surname"
  const val INPUT_USERNAME = "edit_profile_screen_input_username"
  const val INPUT_DESCRIPTION = "edit_profile_screen_input_description"
  const val PROFILE_PICTURE_PREVIEW = "edit_profile_screen_profile_picture_preview"
  const val SAVE = "edit_profile_screen_go_save_button"
  const val ERROR_MESSAGE = "edit_profile_screen_error_message"
}

/**
 * Screen composable for creating or editing a user profile.
 *
 * @param editScreenViewModel ViewModel that provides UI state and actions.
 * @param onGoBack Callback invoked to navigate back.
 * @param onSave Callback invoked after successful save (used for new user flow).
 * @param isNewUser True when creating a new profile.
 */
@SuppressLint("LocalContextConfigurationRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    editScreenViewModel: EditProfileViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onSave: () -> Unit = {},
    isNewUser: Boolean = false,
) {
  LaunchedEffect(Unit) { editScreenViewModel.loadUIState() }
  val uiState by editScreenViewModel.uiState.collectAsState()
  val context = LocalContext.current
  val connectivityObserver = LocalConnectivityObserver.current
  val isOnline by connectivityObserver.isOnline.collectAsState()
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      editScreenViewModel.clearErrorMsg()
    }
  }
  val pickImageLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { editScreenViewModel.setNewProfileImageUri(it) }
      }
  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(NavigationTestTags.EDIT_PROFILE_SCREEN),
      topBar = { EditProfileTopBar(isNewUser, onGoBack) },
  ) { pd ->
    if (isOnline) {
      when {
        uiState.isError -> LoadingFail()
        uiState.isLoading -> LoadingScreen()
        else ->
            EditView(
                editScreenViewModel = editScreenViewModel,
                onSave = onSave,
                isNewUser = isNewUser,
                pd = pd,
                uiState = uiState,
                pickImageLauncher = pickImageLauncher,
            )
      }
    } else {
      OfflineScreen(innerPadding = pd)
    }
  }
}

/**
 * Main editable view with profile picture, inputs and save button.
 *
 * @param editScreenViewModel ViewModel that backs the view.
 * @param onSave Callback invoked when save finishes.
 * @param isNewUser True when creating a new profile.
 * @param pd Padding values from parent.
 * @param uiState Current UI state.
 * @param pickImageLauncher Activity launcher used to pick an image.
 */
@Composable
fun EditView(
    editScreenViewModel: EditProfileViewModel = viewModel(),
    onSave: () -> Unit = {},
    isNewUser: Boolean = false,
    pd: PaddingValues = PaddingValues(0.dp),
    uiState: EditProfileUIState,
    pickImageLauncher: ManagedActivityResultLauncher<String, Uri?>,
) {
  // State for dropdown visibility
  val defaultUri: Uri =
      LocalContext.current.getString(R.string.default_profile_picture_link).toUri()
  Column(
      modifier =
          Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pd).padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
      AsyncImage(
          model = uiState.pendingProfileImageUri ?: defaultUri,
          contentDescription = "Profile picture",
          modifier =
              Modifier.width(96.dp)
                  .aspectRatio(1f)
                  .clip(CircleShape)
                  .border(1.dp, colorScheme.outline, CircleShape)
                  .clickable {
                    pickImageLauncher.launch("image/*")
                    editScreenViewModel.clearProfileSaved()
                  }
                  .testTag(EditProfileScreenTestTags.PROFILE_PICTURE_PREVIEW),
          contentScale = ContentScale.Crop,
      )
      Icon(
          imageVector = Icons.Filled.Create,
          contentDescription = "Change profile picture",
          tint = colorScheme.onPrimary,
          modifier =
              Modifier.align(Alignment.TopEnd)
                  .size(20.dp)
                  .clip(CircleShape)
                  .background(colorScheme.secondary)
                  .padding(4.dp),
      )
    }
    Spacer(modifier = Modifier.height(30.dp))
    // Name Input
    OutlinedTextField(
        value = uiState.name,
        onValueChange = {
          editScreenViewModel.setName(it)
          editScreenViewModel.clearProfileSaved()
        },
        label = { Text("Name") },
        placeholder = { Text("Name") },
        isError = uiState.invalidNameMsg != null,
        supportingText = {
          uiState.invalidNameMsg?.let {
            Text(it, modifier = Modifier.testTag(EditProfileScreenTestTags.ERROR_MESSAGE))
          }
        },
        modifier = Modifier.fillMaxWidth().testTag(EditProfileScreenTestTags.INPUT_NAME),
    )
    // Surname Input
    OutlinedTextField(
        value = uiState.surname,
        onValueChange = {
          editScreenViewModel.setSurname(it)
          editScreenViewModel.clearProfileSaved()
        },
        label = { Text("Surname") },
        placeholder = { Text("Surname") },
        isError = uiState.invalidSurnameMsg != null,
        supportingText = {
          uiState.invalidSurnameMsg?.let {
            Text(it, modifier = Modifier.testTag(EditProfileScreenTestTags.ERROR_MESSAGE))
          }
        },
        modifier = Modifier.fillMaxWidth().testTag(EditProfileScreenTestTags.INPUT_SURNAME),
    )
    // User Input
    OutlinedTextField(
        value = uiState.username,
        onValueChange = {
          editScreenViewModel.setUsername(it)
          editScreenViewModel.clearProfileSaved()
        },
        label = { Text("Username") },
        placeholder = { Text("Username") },
        isError = uiState.invalidUsernameMsg != null,
        supportingText = {
          uiState.invalidUsernameMsg?.let {
            Text(it, modifier = Modifier.testTag(EditProfileScreenTestTags.ERROR_MESSAGE))
          }
        },
        modifier = Modifier.fillMaxWidth().testTag(EditProfileScreenTestTags.INPUT_USERNAME),
    )
    // Description Input
    OutlinedTextField(
        value = uiState.description,
        onValueChange = {
          editScreenViewModel.setDescription(it)
          editScreenViewModel.clearProfileSaved()
        },
        label = { Text("Description") },
        placeholder = { Text("Description") },
        modifier = Modifier.fillMaxWidth().testTag(EditProfileScreenTestTags.INPUT_DESCRIPTION),
    )
    Spacer(modifier = Modifier.height(4.dp))
    // Country Input with dropdown
    CountryDropdown(
        selectedCountry = uiState.country,
        onCountrySelected = {
          editScreenViewModel.setCountry(it)
          editScreenViewModel.clearProfileSaved()
        },
    )
    if (uiState.profileSaved) {
      Row(
          modifier = Modifier.align(Alignment.CenterHorizontally),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Saved successfully",
            tint = colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = LocalContext.current.getString(R.string.edit_profile_save_successfully),
            color = colorScheme.primary,
        )
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = {
          editScreenViewModel.saveProfileChanges {
            if (isNewUser) {
              onSave()
            }
          }
        },
        enabled = uiState.isValid,
        shape = RoundedCornerShape(8.dp),
        modifier =
            Modifier.testTag(EditProfileScreenTestTags.SAVE)
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(),
    ) {
      Text(text = "Save")
    }
  }
}
