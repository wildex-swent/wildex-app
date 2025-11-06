package com.android.wildex.ui.achievement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.ui.LoadingScreen

object AchievementsScreenTestTags {
  const val ACHIEVEMENT_ITEM = "achievementItem"
  const val UNLOCKED_SECTION = "unlockedSection"
  const val LOCKED_SECTION = "lockedSection"

  // Added test tags
  const val LOADING = "achievements_loading"
  const val ERROR = "achievements_error"
  const val TOP_APP_BAR = "achievements_top_app_bar"
  const val ACHIEVEMENT_IMAGE = "achievement_image"
  const val ACHIEVEMENT_NAME = "achievement_name"
  const val BACK_BUTTON = "achievements_back_button"
}

val AchievementAlphaKey = SemanticsPropertyKey<Float>("AchievementAlpha")
var SemanticsPropertyReceiver.achievementAlpha by AchievementAlphaKey

val AchievementIdKey = SemanticsPropertyKey<String>("AchievementId")
var SemanticsPropertyReceiver.achievementId by AchievementIdKey

/**
 * Screen that displays the user's achievements in two sections: unlocked and locked.
 *
 * @param viewModel The [AchievementsScreenViewModel] that provides the UI state and handles data
 *   loading.
 * @param onGoBack Callback invoked when the user presses the back button in the top app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(viewModel: AchievementsScreenViewModel, onGoBack: () -> Unit) {
  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(Unit) { viewModel.loadAchievements() }

  Scaffold(
      topBar = {
        TopAppBar(
            modifier = Modifier.testTag(AchievementsScreenTestTags.TOP_APP_BAR),
            title = { LocalContext.current.getString(R.string.trophies) },
            navigationIcon = {
              IconButton(
                  onClick = onGoBack,
                  modifier = Modifier.testTag(AchievementsScreenTestTags.BACK_BUTTON)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                  }
            })
      },
      content = { paddingValues ->
        when {
          uiState.isLoading -> {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(paddingValues)
                        .testTag(AchievementsScreenTestTags.LOADING),
                contentAlignment = Alignment.Center) {
                  LoadingScreen()
                }
          }
          uiState.isError -> {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(paddingValues)
                        .testTag(AchievementsScreenTestTags.ERROR),
                contentAlignment = Alignment.Center) {
                  Text(
                      text =
                          uiState.errorMsg ?: LocalContext.current.getString(R.string.prev_arrow),
                      color = MaterialTheme.colorScheme.error)
                }
          }
          else -> {
            Column(
                modifier =
                    Modifier.padding(paddingValues).padding(horizontal = 16.dp).fillMaxSize()) {
                  Spacer(modifier = Modifier.height(8.dp))

                  LabeledDivider(
                      text = LocalContext.current.getString(R.string.unlocked_achievements),
                      color = MaterialTheme.colorScheme.primary)

                  LazyVerticalGrid(
                      columns = GridCells.Fixed(3),
                      verticalArrangement = Arrangement.spacedBy(16.dp),
                      horizontalArrangement = Arrangement.spacedBy(16.dp),
                      modifier =
                          Modifier.heightIn(max = 200.dp)
                              .testTag(AchievementsScreenTestTags.UNLOCKED_SECTION)) {
                        items(uiState.unlocked) { achievement ->
                          AchievementItem(achievement = achievement, unlocked = true)
                        }
                      }

                  Spacer(modifier = Modifier.height(24.dp))

                  LabeledDivider(
                      text = LocalContext.current.getString(R.string.to_discover),
                      color = MaterialTheme.colorScheme.primary)

                  LazyVerticalGrid(
                      columns = GridCells.Fixed(3),
                      verticalArrangement = Arrangement.spacedBy(16.dp),
                      horizontalArrangement = Arrangement.spacedBy(16.dp),
                      modifier = Modifier.testTag(AchievementsScreenTestTags.LOCKED_SECTION)) {
                        items(uiState.locked) { achievement ->
                          AchievementItem(achievement = achievement, unlocked = false)
                        }
                      }
                }
          }
        }
      })
}

/**
 * Represents a single achievement item displayed in the achievements grid.
 *
 * The visual appearance depends on whether the achievement has been unlocked:
 * - Unlocked achievements are shown in full opacity.
 * - Locked achievements appear faded to indicate inaccessibility.
 *
 * @param achievement The [Achievement] to display, including its name and image URL.
 * @param unlocked Whether the achievement has been unlocked by the user.
 */
@Composable
fun AchievementItem(achievement: Achievement, unlocked: Boolean) {
  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.width(90.dp).testTag(AchievementsScreenTestTags.ACHIEVEMENT_ITEM)) {
        AsyncImage(
            model = achievement.pictureURL,
            contentDescription = achievement.name,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier.size(72.dp)
                    .clip(CircleShape)
                    .alpha(if (unlocked) 1f else 0.3f)
                    .semantics {
                      achievementAlpha = if (unlocked) 1f else 0.3f
                      achievementId = achievement.achievementId
                    }
                    .testTag(AchievementsScreenTestTags.ACHIEVEMENT_IMAGE))

        Text(
            text = achievement.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier =
                Modifier.padding(top = 4.dp).testTag(AchievementsScreenTestTags.ACHIEVEMENT_NAME),
            maxLines = 2,
            color =
                if (unlocked) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
      }
}

/**
 * A horizontal divider with a centered label.
 *
 * @param text The label text to display in the center of the divider.
 * @param color The color of the divider and text.
 * @param thickness The thickness of the divider lines.
 * @param padding The horizontal padding around the label text.
 */
@Composable
fun LabeledDivider(text: String, color: Color, thickness: Dp = 2.dp, padding: Dp = 8.dp) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    HorizontalDivider(modifier = Modifier.weight(1f).height(thickness), color = color)
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(horizontal = padding))
    HorizontalDivider(modifier = Modifier.weight(4f).height(thickness), color = color)
  }
  Spacer(modifier = Modifier.height(8.dp))
}
