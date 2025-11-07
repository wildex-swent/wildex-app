package com.android.wildex.ui.map

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun MapTabSwitcher(
    modifier: Modifier = Modifier,
    activeTab: MapTab,
    availableTabs: List<MapTab>,
    onTabSelected: (MapTab) -> Unit,
) {
  val cs = MaterialTheme.colorScheme
  val uiActive = colorsForMapTab(activeTab, cs)
  var expanded by remember { mutableStateOf(false) }
  val expansion by
      animateFloatAsState(
          targetValue = if (expanded) 1f else 0f,
          animationSpec = tween(220),
          label = "tab-expansion")
  val otherTabs = availableTabs.filter { it != activeTab }

  Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
    if (expanded) {
      Box(Modifier.matchParentSize().background(Color.Transparent).clickable { expanded = false })
    }

    Box(
        modifier = Modifier.graphicsLayer { alpha = expansion },
        contentAlignment = Alignment.TopCenter) {
          otherTabs.forEachIndexed { index, tab ->
            val tabUi = colorsForMapTab(tab, cs)
            val tabIcon = getIconForMapTab(tab)
            val baseOffset = 60.dp // distance to first circle
            val step = 54.dp // distance between circles
            val offset = (baseOffset + step * index) * expansion

            IconButton(
                onClick = {
                  onTabSelected(tab)
                  expanded = false
                },
                modifier =
                    Modifier.offset(y = offset).size(32.dp).clip(CircleShape).background(tabUi.bg),
            ) {
              Icon(tabIcon, contentDescription = tab.name, tint = Color.White)
            }
          }
        }

    val mainIcon = getIconForMapTab(activeTab)
    IconButton(
        onClick = { expanded = !expanded },
        modifier =
            Modifier.clip(CircleShape)
                .background(uiActive.bg)
                .testTag(MapContentTestTags.TAB_SWITCHER),
    ) {
      Icon(mainIcon, contentDescription = activeTab.name, tint = Color.White)
    }
  }
}

fun getIconForMapTab(tab: MapTab) =
    when (tab) {
      MapTab.Posts -> Icons.Default.Language
      MapTab.MyPosts -> Icons.Default.Person
      MapTab.Reports -> Icons.Default.Flag
    }
