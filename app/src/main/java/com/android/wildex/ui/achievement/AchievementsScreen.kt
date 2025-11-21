package com.android.wildex.ui.achievement

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.navigation.NavigationTestTags

object AchievementsScreenTestTags {
  const val ACHIEVEMENT_ITEM = "achievementItem"
  const val UNLOCKED_SECTION = "unlockedSection"
  const val LOCKED_SECTION = "lockedSection"
  const val LOADING = "achievements_loading"
  const val TOP_APP_BAR = "achievements_top_app_bar"
  const val ACHIEVEMENT_IMAGE = "achievement_image"
  const val ACHIEVEMENT_NAME = "achievement_name"
  const val BACK_BUTTON = "achievements_back_button"
  const val NOTIFICATION_BUTTON = "achievements_notification_button"
  const val PROFILE_BUTTON = "achievements_profile_button"
  const val DETAILS_DIALOG = "achievement_details_dialog"
  const val DETAILS_CLOSE_BUTTON = "achievement_details_close_button"
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
 * @param onProfileClick Callback invoked when the profile picture is clicked.
 * @param onNotificationsClick Callback invoked when the notifications button is pressed.
 * @param onGoBack Callback invoked when the back button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: AchievementsScreenViewModel = viewModel(),
    userId: String = "",
    onProfileClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onGoBack: () -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  var selectedAchievement by remember { mutableStateOf<Achievement?>(null) }

  LaunchedEffect(Unit) { viewModel.loadUIState(userId) }
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      viewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.testTag(NavigationTestTags.ACHIEVEMENTS_SCREEN),
      topBar = {
        if (!uiState.isLoading) {
          AchievementsTopBar(
              isUserOwner = uiState.isUserOwner,
              userProfilePictureURL = uiState.user.profilePictureURL,
              onGoBack = onGoBack,
              onProfileClick = onProfileClick,
              onNotificationsClick = onNotificationsClick,
              context = context,
          )
        }
      },
      content = { paddingValues ->
        when {
          uiState.isLoading -> LoadingScreen()
          uiState.isError -> LoadingFail()
          else -> {
            Column(
                modifier =
                    Modifier.padding(paddingValues).padding(horizontal = 16.dp).fillMaxSize()) {
                  Spacer(modifier = Modifier.height(8.dp))
                  LabeledDivider(
                      text = context.getString(R.string.unlocked_achievements),
                      color = colorScheme.primary,
                  )
                  LazyVerticalGrid(
                      columns = GridCells.Fixed(3),
                      verticalArrangement = Arrangement.spacedBy(16.dp),
                      horizontalArrangement = Arrangement.spacedBy(16.dp),
                      modifier =
                          Modifier.heightIn(max = 200.dp)
                              .testTag(AchievementsScreenTestTags.UNLOCKED_SECTION),
                  ) {
                    items(uiState.unlocked) { achievement ->
                      AchievementItem(
                          achievement = achievement,
                          isUnlocked = true,
                          onAchievementClick = { selectedAchievement = achievement },
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(24.dp))
                  LabeledDivider(
                      text = context.getString(R.string.to_discover),
                      color = colorScheme.primary,
                  )
                  LazyVerticalGrid(
                      columns = GridCells.Fixed(3),
                      verticalArrangement = Arrangement.spacedBy(16.dp),
                      horizontalArrangement = Arrangement.spacedBy(16.dp),
                      modifier = Modifier.testTag(AchievementsScreenTestTags.LOCKED_SECTION),
                  ) {
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
        }
        selectedAchievement?.let { achievement ->
          AchievementDetailsDialog(
              achievement = achievement,
              onClose = { selectedAchievement = null },
          )
        }
      },
  )
}

/**
 * Top app bar for the Achievements screen.
 *
 * Displays the title, a back button or notifications button depending on whether the user is
 * viewing their own profile, and the user's profile picture if they are the owner.
 *
 * @param isUserOwner Whether the current user is viewing their own achievements.
 * @param userProfilePictureURL URL of the user's profile picture.
 * @param onGoBack Callback invoked when the back button is pressed.
 * @param onProfileClick Callback invoked when the profile picture is clicked.
 * @param onNotificationsClick Callback invoked when the notifications button is pressed.
 * @param context The [Context] used to access string resources.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsTopBar(
    isUserOwner: Boolean,
    userProfilePictureURL: String,
    onGoBack: () -> Unit,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    context: Context,
) {
  TopAppBar(
      title = {
        Text(
            modifier = Modifier.fillMaxWidth().testTag(AchievementsScreenTestTags.TOP_APP_BAR),
            text = context.getString(R.string.trophies),
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
      },
      navigationIcon = {
        IconButton(
            modifier =
                Modifier.testTag(
                    if (isUserOwner) AchievementsScreenTestTags.NOTIFICATION_BUTTON
                    else AchievementsScreenTestTags.BACK_BUTTON),
            onClick = { if (isUserOwner) onNotificationsClick() else onGoBack() },
        ) {
          Icon(
              imageVector =
                  if (isUserOwner) Icons.Default.Notifications
                  else Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription =
                  if (isUserOwner) context.getString(R.string.notifications)
                  else context.getString(R.string.back),
              tint = colorScheme.onBackground,
          )
        }
      },
      actions = {
        if (isUserOwner) {
          IconButton(
              onClick = { onProfileClick() },
              modifier = Modifier.testTag(AchievementsScreenTestTags.PROFILE_BUTTON),
          ) {
            AsyncImage(
                model = userProfilePictureURL,
                contentDescription = context.getString(R.string.profile_picture),
                modifier =
                    Modifier.size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, colorScheme.primary, CircleShape),
                contentScale = ContentScale.Crop,
            )
          }
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
fun AchievementItem(achievement: Achievement, isUnlocked: Boolean, onAchievementClick: () -> Unit) {
  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.width(90.dp).testTag(AchievementsScreenTestTags.ACHIEVEMENT_ITEM),
  ) {
    AsyncImage(
        model = achievement.pictureURL,
        contentDescription = achievement.name,
        modifier =
            Modifier.size(72.dp)
                .clip(CircleShape)
                .alpha(if (isUnlocked) 1f else 0.3f)
                .semantics {
                  achievementAlpha = if (isUnlocked) 1f else 0.3f
                  achievementId = achievement.achievementId
                }
                .clickable { onAchievementClick() }
                .testTag(AchievementsScreenTestTags.ACHIEVEMENT_IMAGE),
        contentScale = ContentScale.Crop,
    )

    Text(
        text = achievement.name,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        modifier =
            Modifier.padding(top = 4.dp).testTag(AchievementsScreenTestTags.ACHIEVEMENT_NAME),
        maxLines = 2,
    )
  }
}

@Composable
fun AchievementDetailsDialog(achievement: Achievement, onClose: () -> Unit) {
  Dialog(onDismissRequest = onClose) {
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = Modifier.fillMaxWidth().testTag(AchievementsScreenTestTags.DETAILS_DIALOG),
    ) {
      Surface(
          shape = MaterialTheme.shapes.medium,
          tonalElevation = 8.dp,
          modifier = Modifier.padding(horizontal = 32.dp, vertical = 48.dp).fillMaxWidth(),
      ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          AsyncImage(
              model = achievement.pictureURL,
              contentDescription = achievement.name,
              modifier = Modifier.size(80.dp),
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
              text = achievement.name,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
              text = achievement.description,
              style = MaterialTheme.typography.bodyMedium,
              textAlign = TextAlign.Center,
          )
        }
      }
      IconButton(
          onClick = onClose,
          modifier =
              Modifier.padding(top = 32.dp, end = 32.dp)
                  .size(30.dp)
                  .clip(CircleShape)
                  .testTag(AchievementsScreenTestTags.DETAILS_CLOSE_BUTTON),
      ) {
        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
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
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(horizontal = padding),
    )
    HorizontalDivider(modifier = Modifier.weight(4f).height(thickness), color = color)
  }
  Spacer(modifier = Modifier.height(8.dp))
}
