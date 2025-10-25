package com.android.wildex.ui.navigation

object NavigationTestTags {
  const val BOTTOM_NAVIGATION_MENU = "BottomNavigationMenu"
  const val HOME_TAB = "HomeTab"
  const val MAP_TAB = "MapTab"
  const val CAMERA_TAB = "CameraTab"
  const val COLLECTION_TAB = "CollectionTab"
  const val REPORT_TAB = "ReportTab"

  fun getTabTestTag(tab: Tab): String =
      when (tab) {
        is Tab.Home -> HOME_TAB
        is Tab.Map -> MAP_TAB
        is Tab.Camera -> CAMERA_TAB
        is Tab.Collection -> COLLECTION_TAB
        is Tab.Report -> REPORT_TAB
      }
}
