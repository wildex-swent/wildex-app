package com.android.wildex.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.android.wildex.R

@Composable
fun OfflineMapPlaceholder(modifier: Modifier = Modifier, skipLottie: Boolean = false) {
  val cs = MaterialTheme.colorScheme
  val context = LocalContext.current

  val title = context.getString(R.string.map_offline_placeholder_title)
  val subtitle = context.getString(R.string.map_offline_placeholder_subtitle)
  val hint = context.getString(R.string.map_offline_placeholder_hint)

  val composition = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.location_pin))
  val progress =
      animateLottieCompositionAsState(
          composition = composition.value,
          iterations = LottieConstants.IterateForever,
          speed = 0.8f,
      )

  Box(
      modifier = modifier.background(cs.surface),
  ) {
    // 1) map grid background so it's not a dead blank page
    Canvas(modifier = Modifier.fillMaxSize()) {
      val step = 56f
      val alpha = 0.06f
      var x = 0f
      while (x <= size.width) {
        drawLine(
            color = cs.onSurface.copy(alpha = alpha),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f,
        )
        x += step
      }
      var y = 0f
      while (y <= size.height) {
        drawLine(
            color = cs.onSurface.copy(alpha = alpha),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f,
        )
        y += step
      }
    }

    // 2) compact floating card
    Surface(
        modifier =
            Modifier.align(Alignment.Center).padding(horizontal = 24.dp).widthIn(max = 380.dp),
        shape = RoundedCornerShape(24.dp),
        color = cs.surface.copy(alpha = 0.92f),
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
    ) {
      Column(
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (!skipLottie) {
          LottieAnimation(
              composition = composition.value,
              progress = { progress.value },
              modifier = Modifier.size(72.dp),
          )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = cs.onSurface,
            textAlign = TextAlign.Center,
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
      }
    }
  }
}
