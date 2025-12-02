package com.android.wildex.ui.achievement

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.android.wildex.R

/**
 * Top app bar for the Achievements screen. Displays the title and a back button.
 *
 * @param onGoBack Callback invoked when the back button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsTopBar(onGoBack: () -> Unit) {
  val context = LocalContext.current
  CenterAlignedTopAppBar(
      title = {
        Text(
            modifier = Modifier.testTag(AchievementsScreenTestTags.TITLE),
            text = context.getString(R.string.achievements),
            style = typography.titleLarge,
            textAlign = TextAlign.Center,
        )
      },
      navigationIcon = {
        IconButton(
            modifier = Modifier.testTag(AchievementsScreenTestTags.BACK_BUTTON),
            onClick = { onGoBack() },
        ) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = context.getString(R.string.back),
          )
        }
      },
      modifier = Modifier.testTag(AchievementsScreenTestTags.TOP_APP_BAR),
  )
}
