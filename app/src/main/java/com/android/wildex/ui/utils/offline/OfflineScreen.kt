package com.android.wildex.ui.utils.offline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.android.wildex.R

object OfflineScreenTestTags {
  const val ANIMATION = "offline_screen_animation"
  const val OFFLINE_SCREEN = "offline_screen"
  const val OFFLINE_TITLE = "offline_screen_title"
  const val OFFLINE_SUBTITLE = "offline_screen_subtitle"
  const val OFFLINE_MESSAGE = "offline_screen_message"
}

@Composable
fun OfflineScreen() {
  val context = LocalContext.current
  val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.kitty_cat_error))
  val progress by
      animateLottieCompositionAsState(
          composition = composition,
          iterations = LottieConstants.IterateForever,
      )
  Box(
      modifier = Modifier.testTag(OfflineScreenTestTags.OFFLINE_SCREEN),
      contentAlignment = Alignment.Center,
  ) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp)) {
          Text(
              text = context.getString(R.string.offline_title),
              style = typography.headlineMedium,
              color = colorScheme.primary,
              modifier = Modifier.testTag(OfflineScreenTestTags.OFFLINE_TITLE),
              textAlign = TextAlign.Center)

          Text(
              text = context.getString(R.string.offline_subtitle),
              style = typography.headlineSmall,
              color = colorScheme.primary,
              modifier =
                  Modifier.padding(top = 8.dp).testTag(OfflineScreenTestTags.OFFLINE_SUBTITLE),
              textAlign = TextAlign.Center)

          LottieAnimation(
              composition = composition,
              progress = { progress },
              modifier = Modifier.size(404.dp).testTag(OfflineScreenTestTags.ANIMATION),
          )

          Text(
              text = context.getString(R.string.offline_message),
              style = typography.bodyLarge,
              color = colorScheme.onBackground,
              modifier = Modifier.testTag(OfflineScreenTestTags.OFFLINE_MESSAGE),
              textAlign = TextAlign.Center)
        }
  }
}
