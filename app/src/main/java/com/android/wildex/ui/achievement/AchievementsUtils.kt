package com.android.wildex.ui.achievement

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

/**
 * A linear progress indicator with a circular stop indicator.
 *
 * @param color The color of the progress indicator.
 * @param trackColor The color of the progress track.
 * @param progress The progress value between 0 and 1.
 * @param modifier The modifier to apply to the progress indicator.
 */
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
