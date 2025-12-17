package com.android.wildex.ui.utils.offline

import androidx.compose.foundation.layout.*
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

object FriendsOfflineScreenTestTags {
  const val SCREEN = "friends_offline_screen"
  const val TITLE = "friends_offline_title"
  const val SUBTITLE = "friends_offline_subtitle"
  const val MESSAGE = "friends_offline_message"
  const val ANIMATION = "friends_offline_animation"
}

/**
 * Offline state shown on the Friends screen.
 *
 * @param innerPadding The padding values to be applied to the screen content.
 */
@Composable
fun FriendsOfflineScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
  val context = LocalContext.current

  val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.pigeons))

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
              .testTag(FriendsOfflineScreenTestTags.SCREEN),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.weight(0.25f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(0.85f)) {
              Text(
                  text = context.getString(R.string.friends_offline_title),
                  style = typography.titleLarge,
                  color = colorScheme.primary,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.testTag(FriendsOfflineScreenTestTags.TITLE),
              )

              Text(
                  text = context.getString(R.string.friends_offline_subtitle),
                  style = typography.bodyLarge,
                  color = colorScheme.primary,
                  textAlign = TextAlign.Center,
                  modifier =
                      Modifier.padding(top = 4.dp).testTag(FriendsOfflineScreenTestTags.SUBTITLE),
              )
            }

        Spacer(modifier = Modifier.height(16.dp))

        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(240.dp).testTag(FriendsOfflineScreenTestTags.ANIMATION),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = context.getString(R.string.friends_offline_message),
            style = typography.bodyMedium,
            color = colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.9f).testTag(FriendsOfflineScreenTestTags.MESSAGE),
        )

        Spacer(modifier = Modifier.weight(0.35f))
      }
}
