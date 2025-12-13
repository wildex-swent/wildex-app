package com.android.wildex.ui.achievement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.android.wildex.R

/**
 * Composable representing a dialog displaying details about an achievement.
 *
 * @param achievement The [AchievementUIState] to display.
 * @param onClose Callback invoked when the close button is clicked.
 */
@Composable
fun AchievementDetailsDialog(achievement: AchievementUIState, onClose: () -> Unit) {
  Dialog(onDismissRequest = onClose) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = colorScheme.onSurface)
                .clip(RoundedCornerShape(16.dp))
                .background(colorScheme.surface)
                .testTag(AchievementsScreenTestTags.DETAILS_DIALOG)) {
          Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            AsyncImage(
                model = achievement.pictureURL,
                contentDescription = achievement.name,
                modifier =
                    Modifier.align(Alignment.Center)
                        .fillMaxSize(.6f)
                        .testTag(AchievementsScreenTestTags.DETAILS_IMAGE),
            )
            IconButton(
                onClick = onClose,
                modifier =
                    Modifier.align(Alignment.TopEnd)
                        .testTag(AchievementsScreenTestTags.DETAILS_CLOSE_BUTTON),
            ) {
              Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Close",
                  tint = colorScheme.surface,
              )
            }
          }
          Column(
              modifier = Modifier.fillMaxWidth().padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            val isUnlocked = achievement.progress.all { it.second >= it.third }
            if (isUnlocked) {
              AchievementStatus(
                  text = stringResource(R.string.completed),
                  icon = Icons.Default.Check,
                  contentColor = colorScheme.onPrimary,
                  background = colorScheme.primary,
              )
            } else {
              AchievementStatus(
                  text = stringResource(R.string.in_progress),
                  icon = Icons.Default.AccessTime,
                  contentColor = colorScheme.onSecondary,
                  background = colorScheme.secondary,
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = achievement.name,
                style = typography.titleLarge,
                modifier = Modifier.testTag(AchievementsScreenTestTags.DETAILS_NAME),
            )
            Text(
                text = achievement.description,
                style = typography.bodyLarge,
                modifier = Modifier.testTag(AchievementsScreenTestTags.DETAILS_DESCRIPTION),
            )
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
                thickness = .5.dp,
                color = colorScheme.onSurface,
            )
            Text(text = stringResource(R.string.progress), style = typography.titleMedium)
            LazyColumn(
                modifier =
                    Modifier.fillMaxWidth().testTag(AchievementsScreenTestTags.DETAILS_PROGRESS)) {
                  items(achievement.progress) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Text(it.first, style = typography.bodyMedium)
                      Text(
                          stringResource(R.string.achieved_expected, it.second, it.third),
                          style = typography.bodyMedium,
                      )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ProgressBar(
                        color = colorScheme.primary,
                        trackColor = colorScheme.onSurface,
                        progress = it.second.toFloat() / it.third,
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                  }
                }
          }
        }
  }
}

@Composable
private fun ColumnScope.AchievementStatus(
    text: String,
    icon: ImageVector,
    background: Color,
    contentColor: Color,
) {
  Row(
      modifier =
          Modifier.clip(RoundedCornerShape(8.dp))
              .background(background)
              .padding(4.dp)
              .align(Alignment.CenterHorizontally)
              .testTag(AchievementsScreenTestTags.DETAILS_STATUS),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
        imageVector = icon,
        contentDescription = "Status",
        tint = contentColor,
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
        text = text,
        color = contentColor,
        style = typography.bodyMedium,
    )
  }
}
