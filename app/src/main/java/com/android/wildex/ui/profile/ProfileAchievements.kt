package com.android.wildex.ui.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.achievement.AchievementsScreenTestTags

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ProfileAchievements(
    id: Id = "",
    onAchievements: (Id) -> Unit = {},
    listAchievement: List<Achievement> = emptyList(),
    isOnline: Boolean = true,
) {
  val cs = colorScheme

  if (listAchievement.isEmpty()) {
    ElevatedCard(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(
                    1.dp,
                    cs.onBackground.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp),
                )
                .testTag(ProfileScreenTestTags.ACHIEVEMENTS),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cs.background),
    ) {
      Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.Start) {
        Text(
            text =
                if (isOnline) LocalContext.current.getString(R.string.no_achievements)
                else LocalContext.current.getString(R.string.no_achievements_offline),
            color = cs.onBackground,
            style = typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onAchievements(id) },
            enabled = isOnline,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = cs.onBackground,
                    contentColor = cs.background,
                ),
            modifier = Modifier.testTag(ProfileScreenTestTags.ACHIEVEMENTS_CTA),
        ) {
          Text(
              text = LocalContext.current.getString(R.string.view_achievements),
              style = typography.titleSmall,
          )
        }
      }
    }
    return
  }

  var startIndex by rememberSaveable { mutableIntStateOf(0) }
  var navDirection by rememberSaveable { mutableIntStateOf(0) } // -1 left, +1 right
  fun wrap(i: Int) = (i % listAchievement.size + listAchievement.size) % listAchievement.size
  val windowSize = minOf(3, listAchievement.size)
  val visible =
      remember(startIndex, listAchievement) {
        List(windowSize) { k -> listAchievement[wrap(startIndex + k)] }
      }

  ElevatedCard(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp)
              .border(
                  1.dp,
                  cs.onBackground.copy(alpha = 0.08f),
                  shape = RoundedCornerShape(14.dp),
              )
              .testTag(ProfileScreenTestTags.ACHIEVEMENTS),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = cs.background),
  ) {
    Column(
        modifier =
            Modifier.border(1.dp, cs.background, shape = RoundedCornerShape(14.dp))
                .padding(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            ArrowButton(
                isLeft = true,
                tint = cs.onBackground,
                onClick = {
                  navDirection = -1
                  startIndex = wrap(startIndex - 1)
                },
                modifier = Modifier.testTag(ProfileScreenTestTags.ACHIEVEMENTS_PREV),
            )

            Spacer(Modifier.width(6.dp))

            AnimatedContent(
                targetState = visible,
                transitionSpec = {
                  val duration = 220
                  if (navDirection >= 0) {
                    (slideInHorizontally(
                        animationSpec = tween(duration, easing = FastOutSlowInEasing),
                        initialOffsetX = { it / 2 },
                    ) + fadeIn(tween(duration))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(duration, easing = FastOutSlowInEasing),
                            targetOffsetX = { -it / 2 },
                        ) + fadeOut(tween(duration)))
                  } else {
                    (slideInHorizontally(
                        animationSpec = tween(duration, easing = FastOutSlowInEasing),
                        initialOffsetX = { -it / 2 },
                    ) + fadeIn(tween(duration))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(duration, easing = FastOutSlowInEasing),
                            targetOffsetX = { it / 2 },
                        ) + fadeOut(tween(duration)))
                  }
                },
                modifier = Modifier.weight(1f).height(124.dp),
            ) { trio ->
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
              ) {
                trio.forEach { a ->
                  Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AchievementChip(a)
                  }
                }
              }
            }

            Spacer(Modifier.width(6.dp))

            ArrowButton(
                isLeft = false,
                tint = cs.onBackground,
                onClick = {
                  navDirection = +1
                  startIndex = wrap(startIndex + 1)
                },
                modifier = Modifier.testTag(ProfileScreenTestTags.ACHIEVEMENTS_NEXT),
            )
          }

          Spacer(Modifier.height(8.dp))

          Button(
              onClick = { onAchievements(id) },
              modifier =
                  Modifier.align(Alignment.Start).testTag(ProfileScreenTestTags.ACHIEVEMENTS_CTA),
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = cs.onBackground,
                      contentColor = cs.background,
                  ),
              enabled = isOnline,
          ) {
            Text(
                text = LocalContext.current.getString(R.string.view_achievements),
                style = typography.titleSmall,
            )
          }
        }
  }
}

@Composable
private fun AchievementChip(a: Achievement) {
  val cs = colorScheme
  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
          Modifier.width(92.dp)
              .testTag(
                  AchievementsScreenTestTags.getTagForAchievement(
                      achievementId = a.achievementId,
                      true,
                  )),
  ) {
    ElevatedCard(shape = RoundedCornerShape(12.dp)) {
      Box(
          modifier = Modifier.size(72.dp).background(cs.background),
          contentAlignment = Alignment.Center,
      ) {
        AsyncImage(
            model = a.pictureURL,
            contentDescription = a.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
        )
      }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = a.name,
        color = cs.onBackground,
        style = typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ArrowButton(
    isLeft: Boolean,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val scale by
      animateFloatAsState(
          targetValue = if (pressed) 0.94f else 1f,
          animationSpec = tween(100, easing = FastOutSlowInEasing),
          label = "arrowScale",
      )
  IconButton(
      onClick = onClick,
      interactionSource = interaction,
      modifier =
          modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
          },
  ) {
    Icon(
        imageVector = if (isLeft) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
        contentDescription = if (isLeft) "Previous" else "Next",
        tint = tint,
        modifier = Modifier.size(68.dp),
    )
  }
}
