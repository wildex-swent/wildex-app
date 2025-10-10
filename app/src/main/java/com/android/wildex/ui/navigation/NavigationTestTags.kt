package com.android.wildex.ui.navigation

object NavigationTestTags {
  const val BOTTOM_NAVIGATION_MENU = "BottomNavigationMenu"
  const val GO_BACK_BUTTON = "GoBackButton"
  const val SETTINGS_BUTTON = "SettingsButton"
  const val AUTH_BUTTON = "AuthButton"
  const val EDIT_PROFILE_BUTTON = "EditProfileButton"
  const val SAVE_PROFILE_BUTTON = "SaveProfileButton"
  const val MAKE_POST_BUTTON = "MakePostButton"
  const val SUBMIT_REPORT_BUTTON = "SubmitReportButton"
  const val SAVE_REPORT_BUTTON = "SaveReportButton"
  const val OVERVIEW_TAB = "OverviewTab"
  const val MAP_TAB = "MapTab"
  const val NEW_POST_TAB = "NewPostTab"
  const val COLLECTION_TAB = "CollectionTab"
  const val REPORT_TAB = "ReportTab"

  fun getTabTestTag(tab: Tab): String =
      when (tab) {
        is Tab.Home -> OVERVIEW_TAB
        is Tab.Map -> MAP_TAB
        is Tab.NewPost -> NEW_POST_TAB
        is Tab.Collection -> COLLECTION_TAB
        is Tab.Report -> REPORT_TAB
      }

  fun getPostDetailsButtonTag(postUid: String) = "PostDetailsButton_$postUid"

  fun getProfileButtonTag(userUid: String) = "ProfileButton_$userUid"

  fun getAchievementsButtonTag(userUid: String) = "AchievementsButton_$userUid"

  fun getAnimalDetailsButtonTag(animalUid: String) = "AnimalDetailsButton_$animalUid"

  fun getReportDetailsButtonTag(reportUid: String) = "ReportDetailsButton_$reportUid"
}
