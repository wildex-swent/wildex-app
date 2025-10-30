package com.android.wildex.ui.map

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun MapRefreshButton(
    modifier: Modifier = Modifier,
    isRefreshing: Boolean,
    currentTab: MapTab,
    onRefresh: () -> Unit,
) {
  val cs = MaterialTheme.colorScheme
  val mapUi = colorsForMapTab(currentTab, cs)

  // Infinite rotation animation
  val infiniteTransition = rememberInfiniteTransition(label = "refreshRotation")
  val rotation by
      infiniteTransition.animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(durationMillis = 800, easing = LinearEasing),
                  repeatMode = RepeatMode.Restart),
          label = "rotationAnim")

  // Rotate only while refreshing
  val currentRotation = if (isRefreshing) rotation else 0f

  Box(
      modifier =
          modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(mapUi.bg)
              .semantics { contentDescription = "refresh_button" }
              .clickable(enabled = !isRefreshing) { onRefresh() },
      contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Refresh",
            tint = cs.background,
            modifier = Modifier.size(24.dp).graphicsLayer(rotationZ = currentRotation))
      }
}
