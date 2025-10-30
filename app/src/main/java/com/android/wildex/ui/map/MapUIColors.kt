package com.android.wildex.ui.map

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

data class MapUiColors(
    val bg: Color, // container color
    val fg: Color // content color
)

/** One source of truth for Map tab/card colors. */
fun colorsForMapTab(tab: MapTab, cs: ColorScheme): MapUiColors =
    when (tab) {
      MapTab.Posts -> MapUiColors(cs.secondary, cs.onSecondary)
      MapTab.MyPosts -> MapUiColors(cs.primary, cs.onPrimary)
      MapTab.Reports -> MapUiColors(cs.tertiary, cs.onTertiary)
    }
