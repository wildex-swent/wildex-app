package com.android.wildex.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

sealed class Tab(val name: String, val icon: ImageVector, val destination: Screen) {
  object Home : Tab("Home", Icons.Filled.Home, Screen.Home)

  object Map : Tab("Map", Icons.Filled.LocationOn, Screen.Map)

  object NewPost : Tab("New Post", Icons.Filled.AddCircle, Screen.NewPost)

  object Collection : Tab("Collection", Icons.Filled.Search, Screen.Collection)

  object Report : Tab("Report", Icons.Filled.Warning, Screen.Report)
}

private val tabs = listOf(Tab.Home, Tab.Map, Tab.NewPost, Tab.Collection, Tab.Report)

@Composable
fun BottomNavigationMenu(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
  NavigationBar(
      modifier =
          modifier
              .fillMaxWidth()
              .height(60.dp)
              .testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
              .background(MaterialTheme.colorScheme.surface),
      containerColor = MaterialTheme.colorScheme.surface,
  ) {
    tabs.forEach { tab ->
      NavigationBarItem(
          icon = { Icon(tab.icon, contentDescription = tab.name, modifier.size(37.dp)) },
          selected = tab == selectedTab,
          onClick = { onTabSelected(tab) },
          modifier =
              Modifier.clip(RoundedCornerShape(50.dp))
                  .testTag(NavigationTestTags.getTabTestTag(tab)),
          colors =
              NavigationBarItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.secondary,
                  unselectedIconColor = MaterialTheme.colorScheme.primary,
                  indicatorColor = MaterialTheme.colorScheme.surface,
              ),
      )
    }
  }
}
