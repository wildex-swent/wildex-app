package com.android.wildex.ui.map

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.wildex.R

/**
 * Composable that displays a horizontal segmented tab switcher for the map screen.
 *
 * @param modifier Modifier to be applied to the tab switcher.
 * @param activeTab The currently active map tab.
 * @param availableTabs List of available map tabs.
 * @param onTabSelected Callback invoked when a tab is selected.
 */
@Composable
fun MapTabSwitcher(
    modifier: Modifier = Modifier,
    activeTab: MapTab,
    availableTabs: List<MapTab>,
    onTabSelected: (MapTab) -> Unit,
    isCurrentUser: Boolean = true,
) {
  val cs = MaterialTheme.colorScheme
  val containerShape = RoundedCornerShape(999.dp)
  val context = LocalContext.current

  Surface(
      modifier =
          modifier
              .padding(horizontal = 16.dp, vertical = 8.dp)
              .testTag(MapContentTestTags.MAIN_TAB_SWITCHER),
      shape = containerShape,
      color = cs.surface,
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      availableTabs.forEach { tab ->
        val selected = tab == activeTab
        MapTabButton(
            tab = tab,
            selected = selected,
            containerShape = containerShape,
            isCurrentUser = isCurrentUser,
            colorScheme = cs,
            context = context,
            onTabSelected = onTabSelected,
        )
      }
    }
  }
}

@Composable
private fun RowScope.MapTabButton(
    tab: MapTab,
    selected: Boolean,
    containerShape: RoundedCornerShape,
    isCurrentUser: Boolean,
    colorScheme: androidx.compose.material3.ColorScheme,
    context: Context,
    onTabSelected: (MapTab) -> Unit,
) {
  val backgroundColor by
      animateColorAsState(
          targetValue = if (selected) colorScheme.primary else colorScheme.surface,
          animationSpec = tween(durationMillis = 180),
          label = "tab-bg-color",
      )

  val contentColor by
      animateColorAsState(
          targetValue = if (selected) colorScheme.onPrimary else colorScheme.onBackground,
          animationSpec = tween(durationMillis = 180),
          label = "tab-content-color",
      )

  val tabModifier =
      if (selected) {
        Modifier.wrapContentWidth()
      } else {
        Modifier.weight(1f)
      }

  Box(
      modifier =
          tabModifier
              .heightIn(min = 40.dp)
              .clip(containerShape)
              .background(backgroundColor)
              .clickable(enabled = !selected) { onTabSelected(tab) }
              .padding(horizontal = 12.dp, vertical = 8.dp)
              .testTag(MapContentTestTags.getPinTag(tab)),
      contentAlignment = Alignment.Center,
  ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
          imageVector = getIconForMapTab(tab),
          contentDescription = tab.name,
          tint = contentColor,
      )
      Text(
          text = tab.toLabel(isCurrentUser, context),
          color = contentColor,
          style = MaterialTheme.typography.labelLarge,
          maxLines = 1,
          overflow = if (selected) TextOverflow.Clip else TextOverflow.Ellipsis,
      )
    }
  }
}

/**
 * Returns the UI icons for a given map tab.
 *
 * @param tab The map tab.
 * @return The UI icon for the specified map tab.
 */
fun getIconForMapTab(tab: MapTab) =
    when (tab) {
      MapTab.Posts -> Icons.Default.Language
      MapTab.MyPosts -> Icons.Default.Person
      MapTab.Reports -> Icons.Default.Flag
    }

/**
 * String labels instead of enum names.
 *
 * @param isCurrentUser does the viewed map belong to the curent user
 * @return name of the tab
 */
private fun MapTab.toLabel(isCurrentUser: Boolean = true, context: Context): String =
    when (this) {
      MapTab.Posts -> context.getString(R.string.map_posts)
      MapTab.MyPosts ->
          if (isCurrentUser) context.getString(R.string.map_my_posts)
          else context.getString(R.string.map_posts)
      MapTab.Reports -> context.getString(R.string.map_reports)
    }
