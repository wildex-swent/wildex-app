package com.android.wildex.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel

object EditProfileScreenTestTags {
  const val GO_BACK = "edit_profile_screen_go_back_button"
  const val SAVE = "edit_profile_screen_go_save_button"
}

@Composable
fun EditProfileScreen(
    editScreenViewModel: EditProfileViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onSave: () -> Unit = {},
    isNewUser: Boolean = false,
) {
  val cs = colorScheme
  Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = { EditProfileTopBar(onGoBack) },
  ) { pd ->
    Box(modifier = Modifier.fillMaxSize().padding(pd)) {
      Button(
          onClick = {
            if (isNewUser) {
              onSave()
            }
          },
      ) {
        Row {
          Icon(
              imageVector = Icons.Filled.SaveAlt,
              contentDescription = "Save",
              tint = cs.tertiary,
              modifier = Modifier.fillMaxSize(),
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
