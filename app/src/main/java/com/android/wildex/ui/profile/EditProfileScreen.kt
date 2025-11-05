package com.android.wildex.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.maps.extension.style.expressions.dsl.generated.color

object EditProfileScreenTestTags {
  const val GO_BACK = "edit_profile_screen_go_back_button"
  const val INPUT_NAME = "edit_profile_screen_input_name"
  const val INPUT_SURNAME = "edit_profile_screen_input_surname"
  const val INPUT_USERNAME = "edit_profile_screen_input_username"
  const val INPUT_DESCRIPTION = "edit_profile_screen_input_description"
  const val INPUT_COUNTRY = "edit_profile_screen_input_country"
  const val DROPDOWN_COUNTRY = "edit_profile_screen_dropdown_country"
  const val COUNTRY_ELEMENT = "edit_profile_screen_country_element_"
  const val CHANGE_PROFILE_PICTURE = "edit_profile_screen_change_profile_picture_button"
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
  val errorMsg = uiState.errorMsg
  // State for dropdown visibility
  var showDropdown by remember { mutableStateOf(false) }

  val countryList = uiState.countryList

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
    Column(
        modifier = Modifier.fillMaxSize().padding(pd).padding(16.dp),
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
              modifier =
                  Modifier.fillMaxWidth().testTag(EditProfileScreenTestTags.INPUT_DESCRIPTION))
          // Country Input with dropdown

          Box(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = {
                  // editScreenViewModel.setCountry("")
                  showDropdown = true
                },
                modifier =
                    Modifier.align(Alignment.Center)
                        .testTag(EditProfileScreenTestTags.DROPDOWN_COUNTRY)) {
                  Text(text = uiState.country)
                }

            // Dropdown to show location suggestions
            DropdownMenu(
                expanded = showDropdown && countryList.isNotEmpty(),
                onDismissRequest = { showDropdown = false },
                properties = PopupProperties(focusable = false),
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(
                            max = 200.dp) // Set max height to make it scrollable if more than 3
                // items
                ) {
                  countryList.forEach { country ->
                    DropdownMenuItem(
                        text = {
                          Text(
                              text =
                                  country.take(30) +
                                      if (country.length > 30) "..."
                                      else "", // Limit name length and add ellipsis
                              maxLines = 1, // Ensure name doesn't overflow
                          )
                        },
                        onClick = {
                          // Update country
                          editScreenViewModel.setCountry(country)
                          showDropdown = false // Close dropdown on selection
                        },
                        modifier =
                            Modifier.padding(8.dp)
                                .testTag(
                                    EditProfileScreenTestTags
                                        .COUNTRY_ELEMENT) // Add padding for better
                        // separation
                        )
                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        DividerDefaults.color) // Separate items with a divider
                  }

                  if (countryList.size > 3) {
                    DropdownMenuItem(
                        text = { Text("More...") },
                        onClick = { /* Optionally show more results */},
                        modifier = Modifier.padding(8.dp))
                  }
                }
          }
          Button(
              modifier =
                  Modifier.fillMaxWidth().testTag(EditProfileScreenTestTags.CHANGE_PROFILE_PICTURE),
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
          Button(
              onClick = {
                if (isNewUser) {
                  onSave()
                }
                editScreenViewModel.saveProfileChanges()
              },
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = cs.secondary,
                      contentColor = cs.onSecondary,
                  )) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileTopBar(onGoBack: () -> Unit) {
  val cs = colorScheme
  TopAppBar(
      title = {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Text(
              text = "Edit Profile",
              fontWeight = FontWeight.SemiBold,
              color = cs.onBackground,
          )
        }
      },
      navigationIcon = {
        IconButton(
            modifier = Modifier.testTag(EditProfileScreenTestTags.GO_BACK),
            onClick = { onGoBack() },
        ) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = cs.onBackground,
          )
        }
      })
}
