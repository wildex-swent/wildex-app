package com.android.wildex.ui.achievement

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.wildex.AppTheme
import com.android.wildex.R
import com.android.wildex.model.user.AppearanceMode

/**
 * Composable representing a progress card for achievements.
 *
 * @param progression The pair of achieved and total achievements.
 */
@Composable
fun AchievementsProgressCard(progression: Pair<Int, Int>) {
  val progress =
      if (progression.second <= 0) 1f else (progression.first.toFloat() / progression.second)

  val lightColor =
      when (AppTheme.appearanceMode) {
        AppearanceMode.AUTOMATIC ->
            if (isSystemInDarkTheme()) colorScheme.onSurface else colorScheme.surface
        AppearanceMode.LIGHT -> colorScheme.surface
        AppearanceMode.DARK -> colorScheme.onSurface
      }

  val trackColor =
      when (AppTheme.appearanceMode) {
        AppearanceMode.AUTOMATIC ->
            if (isSystemInDarkTheme()) colorScheme.surface else colorScheme.primary.copy(0.5f)
        AppearanceMode.LIGHT -> colorScheme.primary.copy(0.2f)
        AppearanceMode.DARK -> colorScheme.surface
      }

  Column(
      modifier =
          Modifier.fillMaxWidth()
              .aspectRatio(2f)
              .clip(RoundedCornerShape(10.dp))
              .background(
                  brush =
                      Brush.linearGradient(
                          colors =
                              listOf(
                                  colorScheme.primary,
                                  colorScheme.primary.copy(alpha = 0.9f),
                                  colorScheme.primary.copy(alpha = 0.65f),
                                  colorScheme.primary.copy(alpha = 0.45f),
                                  colorScheme.tertiary,
                              ),
                          start = Offset.Zero,
                          end = Offset.Infinite,
                      ))
              .padding(horizontal = 20.dp, vertical = 24.dp)
              .testTag(AchievementsScreenTestTags.ACHIEVEMENTS_PROGRESS_CARD),
      verticalArrangement = Arrangement.SpaceEvenly,
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(modifier = Modifier.fillMaxWidth()) {
      Icon(
          imageVector = Icons.Default.EmojiEvents,
          contentDescription = "Achievements",
          tint = lightColor,
          modifier = Modifier.align(Alignment.CenterVertically).size(40.dp),
      )
      Spacer(modifier = Modifier.width(16.dp))
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.achievement_progress),
            style = typography.titleLarge,
            color = lightColor,
        )
        Text(
            text =
                if (progress >= 1) stringResource(R.string.wildex_certified)
                else stringResource(R.string.keep_exploring),
            style = typography.bodyLarge,
            color = lightColor,
        )
      }
    }
    Spacer(modifier = Modifier.height(12.dp))
    ProgressBar(
        color = lightColor,
        trackColor = trackColor,
        progress = progress,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(.18f),
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        color = lightColor,
        text =
            StringBuilder(stringResource(R.string.percentage_completed, (progress * 100).toInt()))
                .append(" (")
                .append(
                    stringResource(
                        R.string.achieved_expected,
                        progression.first,
                        progression.second,
                    ))
                .append(")")
                .toString(),
        style = typography.bodyLarge,
    )
  }
}
