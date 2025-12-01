package com.android.wildex.ui.achievement

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.AppTheme
import com.android.wildex.R
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.navigation.NavigationTestTags

object AchievementsScreenTestTags {
  const val ACHIEVEMENT_GRID = "achievementGrid"
  const val TOP_APP_BAR = "achievements_top_app_bar"
  const val ACHIEVEMENT_IMAGE = "achievement_image"
  const val ACHIEVEMENT_NAME = "achievement_name"
  const val BACK_BUTTON = "achievements_back_button"
  const val DETAILS_DIALOG = "achievement_details_dialog"
  const val DETAILS_CLOSE_BUTTON = "achievement_details_close_button"

  const val OVERALL_PROGRESS_BAR = "overall_progress_bar"

  fun getTagForAchievement(achievementId: Id, isUnlocked: Boolean) =
      "achievement_${achievementId}_${if (isUnlocked) "unlocked" else "locked"}"

  fun getNameTagForAchievement(achievementId: Id, isUnlocked: Boolean, name: String) =
      "${getTagForAchievement(achievementId, isUnlocked)}_${name}"

  fun getImageIconTagForAchievement(achievementId: Id, isUnlocked: Boolean) =
      "${getTagForAchievement(achievementId, isUnlocked)}_icon"
}

val AchievementAlphaKey = SemanticsPropertyKey<Float>("AchievementAlpha")
var SemanticsPropertyReceiver.achievementAlpha by AchievementAlphaKey

val AchievementIdKey = SemanticsPropertyKey<String>("AchievementId")
var SemanticsPropertyReceiver.achievementId by AchievementIdKey

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
              AchievementsProgress(uiState.overallProgress)
              Spacer(modifier = Modifier.height(16.dp))
              LabeledDivider(
                  text = context.getString(R.string.unlocked_achievements),
                  color = colorScheme.onBackground,
              )
              Spacer(modifier = Modifier.height(8.dp))
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

@Composable
private fun AchievementsProgress(progression: Pair<Int, Int>) {
  val progress = (progression.first.toFloat() / progression.second)

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
                                  Color.Transparent,
                              ),
                          start = Offset.Zero,
                          end = Offset.Infinite,
                      ))
              .padding(horizontal = 20.dp, vertical = 24.dp)
              .testTag(AchievementsScreenTestTags.OVERALL_PROGRESS_BAR),
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
        modifier = Modifier.fillMaxWidth().height(12.dp),
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        color = lightColor,
        text =
            "${stringResource(R.string.percentage_completed, (progress * 100).toInt()) } (${stringResource(R.string.achieved_expected, progression.first, progression.second)})",
        style = typography.bodyLarge,
    )
  }
}

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
            modifier = Modifier.testTag(AchievementsScreenTestTags.TOP_APP_BAR),
            text = context.getString(R.string.trophies),
            style = typography.titleLarge,
            color = colorScheme.primary,
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
              tint = colorScheme.primary,
          )
        }
      },
  )
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
  val modifier =
      Modifier.testTag(AchievementsScreenTestTags.getTagForAchievement(achievement.id, isUnlocked))
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
                  .semantics {
                    achievementAlpha = if (isUnlocked) 1f else 0.3f
                    achievementId = achievement.id
                  }
                  .testTag(AchievementsScreenTestTags.ACHIEVEMENT_IMAGE),
          contentScale = ContentScale.Fit,
          alpha = if (isUnlocked) 1f else 0.3f,
      )
      Text(
          text = achievement.name,
          style = typography.bodySmall,
          textAlign = TextAlign.Center,
          fontWeight = FontWeight.SemiBold,
          modifier =
              Modifier.padding(top = 4.dp).testTag(AchievementsScreenTestTags.ACHIEVEMENT_NAME),
          maxLines = 2,
          minLines = 2,
          overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

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
                modifier = Modifier.align(Alignment.Center),
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
            val text =
                if (isUnlocked) stringResource(R.string.completed)
                else stringResource(R.string.in_progress)
            Row(
                modifier =
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (isUnlocked) colorScheme.primary else colorScheme.secondary)
                        .padding(4.dp)
                        .align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                  imageVector = if (isUnlocked) Icons.Default.Check else Icons.Default.AccessTime,
                  contentDescription = text,
                  tint = if (isUnlocked) colorScheme.onPrimary else colorScheme.onSecondary,
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                  text = text,
                  color = if (isUnlocked) colorScheme.onPrimary else colorScheme.onSecondary,
                  style = typography.bodyMedium,
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = achievement.name, style = typography.titleLarge)
            Text(text = achievement.description, style = typography.bodyLarge)
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
                thickness = .5.dp,
                color = colorScheme.onSurface,
            )
            Text(text = stringResource(R.string.progress), style = typography.titleMedium)
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
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
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
              }
            }
          }
        }
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
        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(horizontal = padding),
    )
    HorizontalDivider(modifier = Modifier.weight(4f).height(thickness), color = color)
  }
  Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun ProgressBar(color: Color, trackColor: Color, progress: Float, modifier: Modifier = Modifier) {
  LinearProgressIndicator(
      progress = { progress },
      modifier = modifier.clip(RoundedCornerShape(6.dp)),
      color = color,
      trackColor = trackColor,
      strokeCap = StrokeCap.Butt,
      drawStopIndicator = {
        drawCircle(
            color = color,
            radius = size.height / 2,
            center = Offset(size.width * progress, size.height / 2),
        )
      },
  )
}
