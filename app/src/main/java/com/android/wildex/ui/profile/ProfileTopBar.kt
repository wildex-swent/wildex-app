package com.android.wildex.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.android.wildex.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopBar(ownerProfile: Boolean = true, onGoBack: () -> Unit, onSettings: () -> Unit) {
  val cs = colorScheme
  TopAppBar(
      title = {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Text(
              text = if (ownerProfile) LocalContext.current.getString(R.string.profile) else "",
              fontWeight = FontWeight.SemiBold,
              color = cs.onBackground,
          )
        }
      },
      navigationIcon = {
        IconButton(
            modifier = Modifier.testTag(ProfileScreenTestTags.GO_BACK),
            onClick = { onGoBack() },
        ) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = cs.onBackground,
          )
        }
      },
      actions = {
        if (ownerProfile) {
          IconButton(
              modifier = Modifier.testTag(ProfileScreenTestTags.SETTINGS),
              onClick = { onSettings() },
          ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = cs.onBackground,
            )
          }
        }
      },
  )
}
