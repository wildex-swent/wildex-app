package com.android.wildex.ui.navigation

object NavigationTestTags {
    const val BOTTOM_NAVIGATION_MENU = "BottomNavigationMenu"
    const val GO_BACK_BUTTON = "GoBackButton"
    const val LIKE_BUTTON = "LikeButton"
    const val AUTH_BUTTON = "AuthButton"
    const val ANIMAL_DESCRIPTION_TEXT = "AnimalDescriptionText"
    const val UNLOCKED_ANIMAL = "UnlockedAnimal" // To be moved to CollectionTestTags
    const val HOME_TAB = "HomeTab"
    const val MAP_TAB = "MapTab"
    const val NEW_POST_TAB = "NewPostTab"
    const val COLLECTION_TAB = "CollectionTab"
    const val REPORT_TAB = "ReportTab"

    fun getTabTestTag(tab: Tab): String =
        when (tab) {
            is Tab.Home -> HOME_TAB
            is Tab.Map -> MAP_TAB
            is Tab.NewPost -> NEW_POST_TAB
            is Tab.Collection -> COLLECTION_TAB
            is Tab.Report -> REPORT_TAB
        }

    fun getPostDetailsButtonTag(postUid: String) = "PostDetailsButton_$postUid"

    fun getProfileButtonTag(userUid: String) = "ProfileButton_$userUid"
}
