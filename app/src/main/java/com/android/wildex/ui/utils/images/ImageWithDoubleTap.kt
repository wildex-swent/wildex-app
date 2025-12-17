package com.android.wildex.ui.utils.images

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.wildex.model.utils.URL

/**
 * A composable that displays an image with a heart overlay when double-tapped.
 *
 * @param pictureURL The URL of the image to display.
 * @param likedByCurrentUser A boolean indicating if the current user has liked the image.
 * @param onDoubleTap A callback that is invoked when the user double-taps the image.
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
fun ImageWithDoubleTapLike(
    pictureURL: URL,
    likedByCurrentUser: Boolean,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    onDoubleTap: () -> Unit,
) {
  var isHeartOverlayVisible by remember { mutableStateOf(false) }

  Box(
      modifier =
          modifier.fillMaxWidth().pointerInput(Unit) {
            detectTapGestures(
                onTap = { onTap?.invoke() },
                onDoubleTap = {
                  if (!likedByCurrentUser) onDoubleTap()
                  isHeartOverlayVisible = true
                })
          }) {
        AsyncImage(
            model = pictureURL,
            contentDescription = "Post picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize())

        DoubleTapHeartOverlay(
            isVisible = isHeartOverlayVisible,
            onAnimationEnd = { isHeartOverlayVisible = false },
            modifier = Modifier.align(Alignment.Center))
      }
}

/**
 * A composable that displays a heart overlay when double-tapped.
 *
 * @param isVisible A boolean indicating if the heart overlay should be visible.
 * @param onAnimationEnd A callback that is invoked when the heart overlay animation ends.
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
fun DoubleTapHeartOverlay(
    isVisible: Boolean,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
  if (!isVisible) return

  val scale = remember { Animatable(0f) }
  val alpha = remember { Animatable(1f) }

  LaunchedEffect(Unit) {
    scale.animateTo(1.5f, animationSpec = tween(durationMillis = 300))

    alpha.animateTo(0f, animationSpec = tween(durationMillis = 400, delayMillis = 300))

    scale.snapTo(0f)
    onAnimationEnd()
  }
  Icon(
      imageVector = Icons.Filled.Favorite,
      contentDescription = "Heart overlay",
      tint = colorScheme.primary,
      modifier =
          modifier.size(100.dp).graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            this.alpha = alpha.value
          })
}
