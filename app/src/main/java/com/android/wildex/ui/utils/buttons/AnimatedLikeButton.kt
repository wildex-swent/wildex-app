package com.android.wildex.ui.utils.buttons

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A composable that displays a like button with a spring animation and a like count. The icon is a
 * filled heart when liked, and an outlined heart when not.
 *
 * @param likedByCurrentUser A boolean indicating if the current user has liked the item.
 * @param onToggleLike A callback that is invoked when the user clicks the icon.
 * @param iconSize The size of the heart icon.
 */
@Composable
fun AnimatedLikeButton(
    likedByCurrentUser: Boolean,
    onToggleLike: () -> Unit,
    iconSize: Dp = 30.dp,
) {

  val scale = remember { Animatable(1f) }

  LaunchedEffect(likedByCurrentUser) {
    if (likedByCurrentUser) {
      scale.animateTo(1.4f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
      scale.animateTo(1.0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
    } else {
      scale.snapTo(1.0f)
    }
  }
  Icon(
      imageVector =
          if (likedByCurrentUser) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
      contentDescription = "Like status",
      tint = if (likedByCurrentUser) colorScheme.primary else colorScheme.onBackground,
      modifier =
          Modifier.size(iconSize)
              .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
              }
              .clickable { onToggleLike() })
}
