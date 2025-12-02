package com.android.wildex.ui.achievement

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.navigation.NavigationTestTags

object AchievementsScreenTestTags {
  const val ACHIEVEMENT_GRID = "achievementGrid"

  // Top Bar
  const val TOP_APP_BAR = "achievements_top_app_bar"
  const val TITLE = "achievements_title"
  const val BACK_BUTTON = "achievements_back_button"

  // Details Dialog
  const val DETAILS_DIALOG = "achievement_details_dialog"
  const val DETAILS_IMAGE = "achievement_details_image"
  const val DETAILS_PROGRESS = "achievement_details_progress"
  const val DETAILS_NAME = "achievement_details_name"
  const val DETAILS_DESCRIPTION = "achievement_details_description"
  const val DETAILS_STATUS = "achievement_details_status"
  const val DETAILS_CLOSE_BUTTON = "achievement_details_close_button"
  const val ACHIEVEMENTS_PROGRESS_CARD = "achievements_progress_card"

  // Achievement item
  fun getTagForAchievement(achievementId: Id, isUnlocked: Boolean) =
      "achievement_${achievementId}_${if (isUnlocked) "unlocked" else "locked"}"

  fun getNameTagForAchievement(achievementId: Id, isUnlocked: Boolean, name: String) =
      "${getTagForAchievement(achievementId, isUnlocked)}_${name}"

  fun getImageTagForAchievement(achievementId: Id, isUnlocked: Boolean) =
      "${getTagForAchievement(achievementId, isUnlocked)}_icon"
}

val AchievementAlphaKey = SemanticsPropertyKey<Float>("AchievementAlpha")
var SemanticsPropertyReceiver.achievementAlpha by AchievementAlphaKey

/**
 * Screen displaying the user's achievements.
 *
 * Shows unlocked and locked achievements in separate sections. Allows viewing achievement details
 * in a dialog.
 *
 * @param viewModel The ViewModel managing the screen's state.
 * @param userId The ID of the user whose achievements are being displayed.
 * @param onGoBack Callback invoked when the back button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: AchievementsScreenViewModel = viewModel(),
    userId: String = "",
    onGoBack: () -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  var selectedAchievement by remember { mutableStateOf<AchievementUIState?>(null) }

  LaunchedEffect(Unit) { viewModel.loadUIState(userId) }
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      viewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.testTag(NavigationTestTags.ACHIEVEMENTS_SCREEN),
      topBar = { if (!uiState.isLoading) AchievementsTopBar(onGoBack = onGoBack) },
  ) { paddingValues ->
    when {
      uiState.isLoading -> LoadingScreen()
      uiState.isError -> LoadingFail()
      else -> {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier.padding(paddingValues)
                    .padding(20.dp)
                    .testTag(AchievementsScreenTestTags.ACHIEVEMENT_GRID),
        ) {
          item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.fillMaxWidth()) {
              AchievementsProgressCard(uiState.overallProgress)
              Spacer(modifier = Modifier.height(16.dp))
              LabeledDivider(
                  text = context.getString(R.string.unlocked_achievements),
                  color = colorScheme.onBackground,
              )
              Spacer(modifier = Modifier.height(8.dp))
            }
          }
          if (uiState.unlocked.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
              Column(
                  modifier = Modifier.fillMaxWidth(.3f).padding(vertical = 32.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(24.dp),
              ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.binoculars_icon),
                    contentDescription = "Binoculars",
                    tint = colorScheme.primary.copy(alpha = .8f),
                )

                Text(stringResource(R.string.no_discoveries), color = colorScheme.primary)
              }
            }
          }
          items(uiState.unlocked) { achievement ->
            AchievementItem(
                achievement = achievement,
                isUnlocked = true,
                onAchievementClick = { selectedAchievement = achievement },
            )
          }
          item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(8.dp))
            LabeledDivider(
                text = context.getString(R.string.to_discover),
                color = colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
          }
          items(uiState.locked) { achievement ->
            AchievementItem(
                achievement = achievement,
                isUnlocked = false,
                onAchievementClick = { selectedAchievement = achievement },
            )
          }
        }
      }
    }
    selectedAchievement?.let { achievement ->
      AchievementDetailsDialog(
          achievement = achievement,
          onClose = { selectedAchievement = null },
      )
    }
  }
}

/**
 * Composable representing a single achievement item.
 *
 * Displays the achievement's image and name. The image is shown with reduced opacity if the
 * achievement is locked.
 *
 * @param achievement The [Achievement] to display.
 * @param isUnlocked Whether the achievement is unlocked.
 * @param onAchievementClick Callback invoked when the achievement item is clicked.
 */
@Composable
fun AchievementItem(
    achievement: AchievementUIState,
    isUnlocked: Boolean,
    onAchievementClick: () -> Unit,
) {
  val alpha = if (isUnlocked) 1f else 0.5f
  val modifier =
      Modifier.testTag(AchievementsScreenTestTags.getTagForAchievement(achievement.id, isUnlocked))
          .graphicsLayer { this.alpha = alpha }
          .semantics { achievementAlpha = alpha }
  Surface(
      tonalElevation = 8.dp,
      shape = RoundedCornerShape(10.dp),
      modifier =
          if (!isUnlocked) modifier
          else modifier.border(2.dp, colorScheme.primary, RoundedCornerShape(10.dp)),
      onClick = onAchievementClick,
  ) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(12.dp),
    ) {
      AsyncImage(
          model = achievement.pictureURL,
          contentDescription = achievement.name,
          modifier =
              Modifier.fillMaxSize(.7f)
                  .clip(CircleShape)
                  .testTag(
                      AchievementsScreenTestTags.getImageTagForAchievement(
                          achievement.id,
                          isUnlocked,
                      )),
          contentScale = ContentScale.Fit,
      )
      Text(
          text = achievement.name,
          style = typography.bodySmall,
          textAlign = TextAlign.Center,
          fontWeight = FontWeight.SemiBold,
          modifier =
              Modifier.padding(top = 4.dp)
                  .testTag(
                      AchievementsScreenTestTags.getNameTagForAchievement(
                          achievement.id,
                          isUnlocked,
                          achievement.name,
                      )),
          maxLines = 2,
          minLines = 2,
          overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
