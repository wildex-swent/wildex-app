package com.android.wildex.ui.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import java.util.Locale

object EditProfileScreenTestTags {
  const val GO_BACK = "edit_profile_screen_go_back_button"
  const val INPUT_NAME = "edit_profile_screen_input_name"
  const val INPUT_SURNAME = "edit_profile_screen_input_surname"
  const val INPUT_USERNAME = "edit_profile_screen_input_username"
  const val INPUT_DESCRIPTION = "edit_profile_screen_input_description"
  const val DROPDOWN_COUNTRY = "edit_profile_screen_dropdown_country"
  const val COUNTRY_ELEMENT = "edit_profile_screen_country_element_"
  const val CHANGE_PROFILE_PICTURE = "edit_profile_screen_change_profile_picture_button"
  const val PROFILE_PICTURE_PREVIEW = "edit_profile_screen_profile_picture_preview"
  const val SAVE = "edit_profile_screen_go_save_button"
  const val ERROR_MESSAGE = "edit_profile_screen_error_message"
}

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
  val userLocale = context.resources.configuration.locales[0]
  val countryNames =
      remember(userLocale) {
        Locale.getISOCountries()
            .map { code -> Locale("", code).getDisplayCountry(userLocale) }
            .sorted()
      }
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      editScreenViewModel.clearErrorMsg()
    }
  }

  // Launcher for image picker
  val pickImageLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { editScreenViewModel.setNewProfileImageUri(it) }
      }

  val cs = colorScheme
  Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = { EditProfileTopBar(onGoBack) },
  ) { pd ->
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
              countryNames = countryNames,
              cs = cs,
              pickImageLauncher = pickImageLauncher)
    }
  }
}

@Composable
fun EditView(
    editScreenViewModel: EditProfileViewModel = viewModel(),
    onSave: () -> Unit = {},
    isNewUser: Boolean = false,
    pd: PaddingValues = PaddingValues(0.dp),
    uiState: EditProfileUIState,
    countryNames: List<String> = emptyList(),
    cs: ColorScheme,
    pickImageLauncher: ManagedActivityResultLauncher<String, Uri?>
) {
  // State for dropdown visibility
  var showDropdown by remember { mutableStateOf(false) }
  Column(
      modifier =
          Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pd).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Name Input
        OutlinedTextField(
            value = uiState.name,
            onValueChange = { editScreenViewModel.setName(it) },
            label = { Text("Name") },
            placeholder = { Text("Name") },
            isError = uiState.invalidNameMsg != null,
            supportingText = {
              uiState.invalidNameMsg?.let {
                Text(it, modifier = Modifier.testTag(EditProfileScreenTestTags.ERROR_MESSAGE))
              }
            },
            modifier = Modifier.fillMaxWidth().testTag(EditProfileScreenTestTags.INPUT_NAME))
        // Surname Input
        OutlinedTextField(
            value = uiState.surname,
            onValueChange = { editScreenViewModel.setSurname(it) },
            label = { Text("Surname") },
            placeholder = { Text("Surname") },
            isError = uiState.invalidSurnameMsg != null,
            supportingText = {
              uiState.invalidSurnameMsg?.let {
                Text(it, modifier = Modifier.testTag(EditProfileScreenTestTags.ERROR_MESSAGE))
              }
            },
            modifier = Modifier.fillMaxWidth().testTag(EditProfileScreenTestTags.INPUT_SURNAME))
        // User Input
        OutlinedTextField(
            value = uiState.username,
            onValueChange = { editScreenViewModel.setUsername(it) },
            label = { Text("Username") },
            placeholder = { Text("Username") },
            isError = uiState.invalidUsernameMsg != null,
            supportingText = {
              uiState.invalidUsernameMsg?.let {
                Text(it, modifier = Modifier.testTag(EditProfileScreenTestTags.ERROR_MESSAGE))
              }
            },
            modifier = Modifier.fillMaxWidth().testTag(EditProfileScreenTestTags.INPUT_USERNAME))
        // Description Input
        OutlinedTextField(
            value = uiState.description,
            onValueChange = { editScreenViewModel.setDescription(it) },
            label = { Text("Description") },
            placeholder = { Text("Description") },
            modifier = Modifier.fillMaxWidth().testTag(EditProfileScreenTestTags.INPUT_DESCRIPTION))
        // Country Input with dropdown

        Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
          val icon =
              if (showDropdown) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
          OutlinedTextField(
              value = uiState.country,
              readOnly = true,
              onValueChange = { /* No-operation: handled by dropdown */},
              modifier =
                  Modifier.fillMaxWidth().testTag(EditProfileScreenTestTags.DROPDOWN_COUNTRY),
              label = { Text("Label") },
              trailingIcon = {
                Icon(
                    icon, "contentDescription", Modifier.clickable { showDropdown = !showDropdown })
              })

          // Dropdown to show location suggestions
          DropdownMenu(
              expanded = showDropdown && countryNames.isNotEmpty(),
              onDismissRequest = { showDropdown = false },
              properties = PopupProperties(focusable = false),
              modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                countryNames.forEach { country ->
                  DropdownMenuItem(
                      text = {
                        Text(text = country, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      },
                      onClick = {
                        editScreenViewModel.setCountry(country)
                        showDropdown = false
                      },
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(horizontal = 16.dp, vertical = 8.dp) // padding(8.dp)
                              .testTag(EditProfileScreenTestTags.COUNTRY_ELEMENT))
                  HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                }
              }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
              val previewModel: Any? =
                  uiState.pendingProfileImageUri ?: uiState.url.takeIf { it.isNotBlank() }
              if (previewModel != null) {
                AsyncImage(
                    model = previewModel,
                    contentDescription = "Profile picture",
                    modifier =
                        Modifier.width(72.dp)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .border(1.dp, cs.outline, CircleShape)
                            .testTag(EditProfileScreenTestTags.PROFILE_PICTURE_PREVIEW),
                    contentScale = ContentScale.Crop)
              } else if (uiState.url.isNotBlank()) {
                AsyncImage(
                    model = uiState.url,
                    contentDescription = "Profile picture",
                    modifier =
                        Modifier.width(72.dp)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .border(1.dp, cs.outline, CircleShape)
                            .testTag(EditProfileScreenTestTags.PROFILE_PICTURE_PREVIEW),
                    contentScale = ContentScale.Crop)
              }
              Button(
                  modifier =
                      Modifier.fillMaxWidth()
                          .testTag(EditProfileScreenTestTags.CHANGE_PROFILE_PICTURE),
                  onClick = {
                    // Opens image picker
                    pickImageLauncher.launch("image/*")
                  },
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = cs.secondary,
                          contentColor = cs.onSecondary,
                      )) {
                    Text(text = "Change profile picture")
                  }
            }
        Button(
            onClick = {
              editScreenViewModel.saveProfileChanges()
              if (isNewUser) {
                onSave()
              }
            },
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = cs.secondary,
                    contentColor = cs.onSecondary,
                ),
            modifier =
                Modifier.testTag(EditProfileScreenTestTags.SAVE)
                    .align(Alignment.CenterHorizontally)) {
              Row {
                Icon(
                    imageVector = Icons.Filled.SaveAlt,
                    contentDescription = "Save",
                    tint = cs.onSecondary,
                )
                Text(text = "Save")
              }
            }
      }
}
