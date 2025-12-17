package com.android.wildex.ui.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.android.wildex.R

/**
 * Top bar for the Edit Profile screen.
 *
 * Shows an appropriate title depending on whether the profile is new and a back button.
 *
 * @param isNewUser True if creating a new profile, false when editing existing.
 * @param onGoBack Called when the back button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileTopBar(onGoBack: () -> Unit) {
  CenterAlignedTopAppBar(
      title = {
        Text(
            text = stringResource(R.string.edit_profile_title),
            style = typography.titleLarge,
            color = colorScheme.onBackground,
        )
      },
      navigationIcon = {
        IconButton(
            modifier = Modifier.testTag(EditProfileScreenTestTags.GO_BACK),
            onClick = onGoBack,
        ) {
          Icon(
              imageVector = Icons.Default.ChevronLeft,
              contentDescription = stringResource(R.string.back),
              tint = colorScheme.onBackground,
          )
        }
      },
  )
}
