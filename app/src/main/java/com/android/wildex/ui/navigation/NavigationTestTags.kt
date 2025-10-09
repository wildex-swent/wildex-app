package com.android.wildex.ui.navigation

object NavigationTestTags {
    const val BOTTOM_NAVIGATION_MENU = "BottomNavigationMenu"
    const val GO_BACK_BUTTON = "GoBackButton"
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
}