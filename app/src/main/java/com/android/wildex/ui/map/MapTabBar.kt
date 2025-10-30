package com.android.wildex.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MapTabBar(
    modifier: Modifier = Modifier,
    tabs: List<MapTab>,
    active: MapTab,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
  if (tabs.isEmpty()) return

  val ty = MaterialTheme.typography
  val density = LocalDensity.current
  val measurer = rememberTextMeasurer()

  val ui = colorsForMapTab(active, MaterialTheme.colorScheme)

  val arrowColors =
      IconButtonDefaults.filledTonalIconButtonColors(containerColor = ui.bg, contentColor = ui.fg)

  // Compute adaptive pill size from the longest visible label
  val labels =
      remember(tabs) {
        tabs.map {
          when (it) {
            MapTab.Posts -> "Posts"
            MapTab.MyPosts -> "My Posts"
            MapTab.Reports -> "Reports"
          }
        }
      }
  val maxTextWidthDp =
      with(density) {
        labels.maxOf { measurer.measure(it, style = ty.titleMedium).size.width }.toDp()
      }
  val textHeightDp =
      with(density) { measurer.measure("Ag", style = ty.titleMedium).size.height.toDp() }

  // Comfortable padding that scales well; no fixed overall size
  val horizontalPadding = 20.dp
  val verticalPadding = 10.dp

  // Use REQUIRED size to avoid the child expanding to infinite constraints
  val pillWidth = maxTextWidthDp + horizontalPadding * 2
  val pillHeight = textHeightDp + verticalPadding * 2

  Row(
      modifier = modifier.semantics { contentDescription = MapScreenTestTags.TAB_BAR },
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    FilledTonalIconButton(
        modifier = Modifier.semantics { contentDescription = MapScreenTestTags.TAB_PREV },
        onClick = onPrev,
        colors = arrowColors,
    ) {
      Icon(Icons.Default.ArrowBackIosNew, contentDescription = null)
    }

    Surface(
        modifier =
            Modifier.requiredWidth(pillWidth) // fixed from measured width
                .requiredHeight(pillHeight) // fixed from measured height
                .semantics { contentDescription = MapScreenTestTags.TAB_TITLE },
        shape = RoundedCornerShape(24.dp),
        color = ui.bg,
        contentColor = ui.fg,
        tonalElevation = 2.dp,
    ) {
      // Center label without forcing parent to grow
      Box(
          modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
          contentAlignment = Alignment.Center,
      ) {
        Text(
            text =
                when (active) {
                  MapTab.Posts -> "Posts"
                  MapTab.MyPosts -> "My Posts"
                  MapTab.Reports -> "Reports"
                },
            style = ty.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      }
    }

    FilledTonalIconButton(
        modifier = Modifier.semantics { contentDescription = MapScreenTestTags.TAB_NEXT },
        onClick = onNext,
        colors = arrowColors,
    ) {
      Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null)
    }
  }
}
