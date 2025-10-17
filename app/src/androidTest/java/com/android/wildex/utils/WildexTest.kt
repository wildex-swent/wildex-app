package com.android.wildex.utils

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.ui.navigation.NavigationTestTags
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth

const val UI_WAIT_TIMEOUT = 5_000L

/** Base class for all Wildex tests, providing common setup and utility functions. */
abstract class WildexTest {
    open val user0 =
        User(
            userId = "0",
            username = "user0",
            name = "Hello",
            surname = "World",
            bio = "This is my bio",
            profilePictureURL = "",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "Italy",
            friendsCount = 0,
            animalsId = emptyList(),
            animalsCount = 0,
            achievementsId = emptyList(),
            achievementsCount = 0
        )

    open val user1 =
        User(
            userId = "1",
            username = "user1",
            name = "James",
            surname = "Bond",
            bio = "Let the sky fall",
            profilePictureURL = "",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "England",
            friendsCount = 0,
            animalsId = emptyList(),
            animalsCount = 0,
            achievementsId = emptyList(),
            achievementsCount = 0
        )

    open val post0 =
        Post(
            postId = "0",
            authorId = user0.userId,
            pictureURL = "",
            location = null,
            description = "This is my first post",
            date = Timestamp.now(),
            animalId = "0",
            likesCount = 0,
            commentsCount = 0
        )

    open val post1 =
        Post(
            postId = "1",
            authorId = user1.userId,
            pictureURL = "",
            location = null,
            description = "This my post",
            date = Timestamp.now(),
            animalId = "0",
            likesCount = 0,
            commentsCount = 0
        )

    open val animal0 =
        Animal(
            animalId = "0",
            pictureURL = "",
            name = "Lion",
            species = "Big Cat",
            description = "The lion is a species in the family Felidae"
        )

    fun ComposeTestRule.checkAuthScreenIsDisplayed() {
        onNodeWithTag(NavigationTestTags.AUTH_BUTTON).assertIsDisplayed()
    }

    fun ComposeTestRule.checkHomeScreenIsDisplayed() {
        onNodeWithTag(NavigationTestTags.HOME_TAB).assertIsDisplayed().assertIsSelected()
    }

    fun ComposeTestRule.checkMapScreenIsDisplayed() {
        onNodeWithTag(NavigationTestTags.MAP_TAB).assertIsDisplayed().assertIsSelected()
    }

    fun ComposeTestRule.checkNewPostScreenIsDisplayed() {
        onNodeWithTag(NavigationTestTags.NEW_POST_TAB).assertIsDisplayed().assertIsSelected()
    }

    fun ComposeTestRule.checkCollectionScreenIsDisplayed() {
        onNodeWithTag(NavigationTestTags.COLLECTION_TAB).assertIsDisplayed().assertIsSelected()
    }

    fun ComposeTestRule.checkReportScreenIsDisplayed() {
        onNodeWithTag(NavigationTestTags.REPORT_TAB).assertIsDisplayed().assertIsSelected()
    }

    fun ComposeTestRule.checkProfileScreenIsDisplayed() {
        onNodeWithTag(NavigationTestTags.ACHIEVEMENTS_BUTTON).assertIsDisplayed()
    }

    fun ComposeTestRule.checkSettingsScreenIsDisplayed() {
        onNodeWithTag(NavigationTestTags.EDIT_PROFILE_BUTTON).assertIsDisplayed()
    }

    fun ComposeTestRule.checkEditProfileScreenIsDisplayed() {
        onNodeWithTag(NavigationTestTags.SAVE_PROFILE_BUTTON).assertIsDisplayed()
    }

    fun ComposeTestRule.checkPostDetailsScreenIsDisplayed() {
        onNodeWithTag(NavigationTestTags.LIKE_BUTTON).assertIsDisplayed()
    }

    fun ComposeTestRule.checkAnimalDetailsScreenIsDisplayed() {
        onNodeWithTag(NavigationTestTags.ANIMAL_DESCRIPTION_TEXT).assertIsDisplayed()
    }

    fun ComposeTestRule.checkSubmitReportScreenIsDisplayed() {
        onNodeWithTag(NavigationTestTags.SAVE_REPORT_BUTTON).assertIsDisplayed()
    }

    fun ComposeTestRule.checkAchievementScreenIsDisplayed() {
        onNodeWithTag(NavigationTestTags.UNLOCKED_ANIMAL).assertIsDisplayed()
    }

    fun ComposeTestRule.navigateAuthToHomeScreen() {
        onNodeWithTag(NavigationTestTags.AUTH_BUTTON).assertIsDisplayed().performClick()
    }

    fun ComposeTestRule.navigateToHomeScreen() {
        onNodeWithTag(NavigationTestTags.HOME_TAB).assertIsDisplayed().performClick()
    }

    fun ComposeTestRule.navigateToMapScreen() {
        onNodeWithTag(NavigationTestTags.MAP_TAB).assertIsDisplayed().performClick()
    }

    fun ComposeTestRule.navigateToNewPostScreen() {
        onNodeWithTag(NavigationTestTags.NEW_POST_TAB).assertIsDisplayed().performClick()
    }

    fun ComposeTestRule.navigateToCollectionScreen() {
        onNodeWithTag(NavigationTestTags.COLLECTION_TAB).assertIsDisplayed().performClick()
    }

    fun ComposeTestRule.navigateToReportScreen() {
        onNodeWithTag(NavigationTestTags.REPORT_TAB).assertIsDisplayed().performClick()
    }

    fun ComposeTestRule.navigateToMyProfileScreen() {
        onNodeWithTag(
            NavigationTestTags.getProfileButtonTag(
                Firebase.auth.currentUser?.uid ?: throw Exception()
            )
        )
            .assertIsDisplayed()
            .performClick()
    }

    fun ComposeTestRule.navigateToProfileScreen(userId: String) {
        onNodeWithTag(NavigationTestTags.getProfileButtonTag(userId)).assertIsDisplayed()
            .performClick()
    }

    fun ComposeTestRule.navigateToPostDetailsScreen(postUid: String) {
        onNodeWithTag(NavigationTestTags.getPostDetailsButtonTag(postUid))
            .assertIsDisplayed()
            .performClick()
    }

    fun ComposeTestRule.navigateToAnimalDetailsScreen(animalUid: String) {
        onNodeWithTag(NavigationTestTags.getAnimalDetailsButtonTag(animalUid))
            .assertIsDisplayed()
            .performClick()
    }

    fun ComposeTestRule.navigateToSubmitReportScreen() {
        onNodeWithTag(NavigationTestTags.SUBMIT_REPORT_BUTTON).assertIsDisplayed().performClick()
    }

    fun ComposeTestRule.navigateToReportDetailsScreen(reportUid: String) {
        onNodeWithTag(NavigationTestTags.getReportDetailsButtonTag(reportUid))
            .assertIsDisplayed()
            .performClick()
    }

    fun ComposeTestRule.navigateToAchievementsScreen() {
        onNodeWithTag(NavigationTestTags.ACHIEVEMENTS_BUTTON).assertIsDisplayed().performClick()
    }

    fun ComposeTestRule.navigateToSettingsScreen() {
        onNodeWithTag(NavigationTestTags.SETTINGS_BUTTON).assertIsDisplayed().performClick()
    }

    fun ComposeTestRule.navigateToEditProfileScreen() {
        onNodeWithTag(NavigationTestTags.EDIT_PROFILE_BUTTON).assertIsDisplayed().performClick()
    }
}
