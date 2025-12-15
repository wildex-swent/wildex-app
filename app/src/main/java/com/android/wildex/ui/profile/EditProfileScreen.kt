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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.utils.offline.OfflineScreen
import java.util.Locale

object EditProfileScreenTestTags {
  const val GO_BACK = "edit_profile_screen_go_back_button"
  const val INPUT_NAME = "edit_profile_screen_input_name"
  const val INPUT_SURNAME = "edit_profile_screen_input_surname"
  const val INPUT_USERNAME = "edit_profile_screen_input_username"
  const val INPUT_DESCRIPTION = "edit_profile_screen_input_description"
  const val DROPDOWN_COUNTRY = "edit_profile_screen_dropdown_country"
  const val COUNTRY_ELEMENT = "edit_profile_screen_country_element_"
  const val PROFILE_PICTURE_PREVIEW = "edit_profile_screen_profile_picture_preview"
  const val SAVE = "edit_profile_screen_go_save_button"
  const val ERROR_MESSAGE = "edit_profile_screen_error_message"
}

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
  val userLocale = context.resources.configuration.locales[0]
  val countryNames =
      remember(userLocale) {
        Locale.getISOCountries()
            .map { code ->
              val locale = Locale("", code)
              val name = locale.getDisplayCountry(userLocale)
              val flag = code.toFlagEmoji()
              "$flag  $name"
            }
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
                countryNames = countryNames,
                cs = cs,
                pickImageLauncher = pickImageLauncher,
            )
      }
    } else {
      OfflineScreen(innerPadding = pd)
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
                  .border(1.dp, cs.outline, CircleShape)
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
          tint = cs.onPrimary,
          modifier =
              Modifier.align(Alignment.TopEnd)
                  .size(20.dp)
                  .clip(CircleShape)
                  .background(cs.secondary)
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
        countries = countryNames,
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
            tint = cs.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = LocalContext.current.getString(R.string.edit_profile_save_successfully),
            color = cs.primary,
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
        colors =
            ButtonDefaults.buttonColors(
                containerColor = cs.secondary,
                contentColor = cs.onSecondary,
            ),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryDropdown(
    modifier: Modifier = Modifier,
    label: String = "Country",
    selectedCountry: String,
    countries: List<String>,
    onCountrySelected: (String) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { expanded = !expanded },
      modifier = modifier.testTag(EditProfileScreenTestTags.DROPDOWN_COUNTRY),
  ) {
    OutlinedTextField(
        value = selectedCountry,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        modifier =
            Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
    )

    ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.background(colorScheme.surface),
    ) {
      countries.forEach { countryName ->
        DropdownMenuItem(
            modifier = Modifier.testTag(EditProfileScreenTestTags.COUNTRY_ELEMENT + countryName),
            text = { Text(countryName) },
            onClick = {
              val startIndex = countryName.indexOfFirst { it.isLetter() }
              val cleaned =
                  if (startIndex >= 0) countryName.substring(startIndex).trim()
                  else countryName.trim()
              onCountrySelected(cleaned)
              expanded = false
            },
        )
      }
    }
  }
}

fun String.toFlagEmoji(): String {
  val first = Character.codePointAt(this, 0) - 0x41 + 0x1F1E6
  val second = Character.codePointAt(this, 1) - 0x41 + 0x1F1E6
  return String(Character.toChars(first)) + String(Character.toChars(second))
}
