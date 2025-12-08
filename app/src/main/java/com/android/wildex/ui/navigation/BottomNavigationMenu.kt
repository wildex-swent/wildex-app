package com.android.wildex.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

sealed class Tab(val name: String) {
  object Home : Tab("Home")

  object Map : Tab("Map")

  object Camera : Tab("Camera")

  object Collection : Tab("Collection")

  object Report : Tab("Report")
}

private val tabs = listOf(Tab.Home, Tab.Map, Tab.Camera, Tab.Collection, Tab.Report)

@Composable
fun BottomNavigationMenu(selectedTab: Tab, onTabSelected: (Tab) -> Unit = {}) {
  val cs = MaterialTheme.colorScheme

  NavigationBar(
      modifier =
          Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
              .fillMaxWidth()
              .height(50.dp)
              .background(cs.background)
              .testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU),
      containerColor = cs.background,
      tonalElevation = 0.dp,
  ) {
    tabs.forEach { tab ->
      NavigationBarItem(
          icon = {
            Icon(
                painter =
                    painterResource(
                        LocalContext.current.resources.getIdentifier(
                            getResourcePath(tab, tab == selectedTab),
                            "drawable",
                            LocalContext.current.packageName)),
                contentDescription = tab.name,
                modifier = Modifier.size(32.dp))
          },
          selected = tab == selectedTab,
          onClick = { onTabSelected(tab) },
          modifier =
              Modifier.clip(RoundedCornerShape(50.dp))
                  .testTag(NavigationTestTags.getTabTestTag(tab)),
          colors =
              NavigationBarItemDefaults.colors(
                  selectedIconColor = cs.primary,
                  unselectedIconColor = cs.onBackground,
                  indicatorColor = cs.background,
              ),
      )
    }
  }
}

private fun getResourcePath(tab: Tab, isSelected: Boolean): String {
  val icon = tab.name.lowercase()
  val iconFill = if (isSelected) "filled" else "outlined"
  return "${icon}_${iconFill}"
}
