package com.android.wildex.ui.utils.offline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.android.wildex.AppTheme
import com.android.wildex.R
import com.android.wildex.model.user.AppearanceMode

object OfflineScreenTestTags {
  const val ANIMATION = "offline_screen_animation"
  const val OFFLINE_SCREEN = "offline_screen"
  const val OFFLINE_TITLE = "offline_screen_title"
  const val OFFLINE_SUBTITLE = "offline_screen_subtitle"
  const val OFFLINE_MESSAGE = "offline_screen_message"
}

/**
 * Defines the screen to display in offline mode.
 *
 * @param innerPadding The padding values to be applied to the screen content.
 */
@Composable
fun OfflineScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
  val context = LocalContext.current
  val composition by
      rememberLottieComposition(
          if (AppTheme.appearanceMode == AppearanceMode.LIGHT)
              LottieCompositionSpec.RawRes(R.raw.kitty_cat_error_dark)
          else LottieCompositionSpec.RawRes(R.raw.kitty_cat_error_light))
  val progress by
      animateLottieCompositionAsState(
          composition = composition,
          iterations = LottieConstants.IterateForever,
      )
  Column(
      modifier =
          Modifier.fillMaxSize()
              .padding(innerPadding)
              .padding(horizontal = 24.dp)
              .testTag(OfflineScreenTestTags.OFFLINE_SCREEN),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(modifier = Modifier.weight(0.25f))

    // Text block (grouped)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(0.85f),
    ) {
      Text(
          text = context.getString(R.string.offline_title),
          style = typography.titleLarge,
          color = colorScheme.primary,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag(OfflineScreenTestTags.OFFLINE_TITLE),
      )

      Text(
          text = context.getString(R.string.offline_subtitle),
          style = typography.bodyLarge,
          color = colorScheme.primary,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 4.dp).testTag(OfflineScreenTestTags.OFFLINE_SUBTITLE),
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.size(260.dp).testTag(OfflineScreenTestTags.ANIMATION),
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = context.getString(R.string.offline_message),
        style = typography.bodyMedium,
        color = colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(0.9f).testTag(OfflineScreenTestTags.OFFLINE_MESSAGE),
    )

    Spacer(modifier = Modifier.weight(0.35f))
  }
}
