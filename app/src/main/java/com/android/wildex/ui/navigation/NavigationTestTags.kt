package com.android.wildex.ui.navigation

object NavigationTestTags {
  const val BOTTOM_NAVIGATION_MENU = "BottomNavigationMenu"
  const val TOP_BAR_TITLE = "top_bar_title"
  const val NOTIFICATION_BELL = "notification_bell"
  const val TOP_BAR_PROFILE_PICTURE = "top_bar_profile_picture"
  const val HOME_TAB = "HomeTab"
  const val MAP_TAB = "MapTab"
  const val CAMERA_TAB = "CameraTab"
  const val COLLECTION_TAB = "CollectionTab"
  const val REPORT_TAB = "ReportTab"
  const val SIGN_IN_SCREEN = "AuthScreen"
  const val HOME_SCREEN = "HomeScreen"
  const val MAP_SCREEN = "MapScreen"
  const val CAMERA_SCREEN = "CameraScreen"
  const val COLLECTION_SCREEN = "CollectionScreen"
  const val REPORT_SCREEN = "ReportScreen"
  const val REPORT_DETAILS_SCREEN = "ReportDetailScreen"
  const val ACHIEVEMENTS_SCREEN = "AchievementScreen"
  const val ANIMAL_INFORMATION_SCREEN = "AnimalInformationScreen"
  const val POST_DETAILS_SCREEN = "PostDetailScreen"
  const val SETTINGS_SCREEN = "SettingsScreen"
  const val PROFILE_SCREEN = "ProfileScreen"
  const val EDIT_PROFILE_SCREEN = "EditProfileScreen"
  const val SUBMIT_REPORT_SCREEN = "SubmitReportScreen"
  const val FRIEND_SCREEN = "FriendScreen"
  const val LOCATION_PICKER_SCREEN = "LocationPicker"

  const val NOTIFICATION_SCREEN = "NotificationScreen"

  fun getTabTestTag(tab: Tab): String =
      when (tab) {
        is Tab.Home -> HOME_TAB
        is Tab.Map -> MAP_TAB
        is Tab.Camera -> CAMERA_TAB
        is Tab.Collection -> COLLECTION_TAB
        is Tab.Report -> REPORT_TAB
      }
}
