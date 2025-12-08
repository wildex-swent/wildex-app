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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.android.wildex.R

sealed class Tab(val name: String, val selectedIcon: Int, val unselectedIcon: Int) {
  object Home : Tab("Home", R.drawable.home_filled, R.drawable.home_outlined)

  object Map : Tab("Map", R.drawable.map_filled, R.drawable.map_outlined)

  object Camera : Tab("Camera", R.drawable.camera_filled, R.drawable.camera_outlined)

  object Collection :
      Tab("Collection", R.drawable.collection_filled, R.drawable.collection_outlined)

  object Report : Tab("Report", R.drawable.report_filled, R.drawable.report_outlined)
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
      val isSelected = tab == selectedTab
      NavigationBarItem(
          icon = {
            Icon(
                painter = painterResource(if (isSelected) tab.selectedIcon else tab.unselectedIcon),
                contentDescription = tab.name,
                modifier = Modifier.size(32.dp))
          },
          selected = isSelected,
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
