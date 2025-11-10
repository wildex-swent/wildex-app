package com.android.wildex.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.google.firebase.Timestamp

const val UI_WAIT_TIMEOUT = 5_000L

/** Base class for all Wildex tests, providing common setup and utility functions. */
abstract class NavigationTest {
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
          commentsCount = 0,
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
          commentsCount = 0,
      )

  open val animal0 =
      Animal(
          animalId = "0",
          pictureURL = "",
          name = "Lion",
          species = "Big Cat",
          description = "The lion is a species in the family Felidae",
      )

  fun ComposeTestRule.checkBottomNavigationIsDisplayed() {
    onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }


  fun ComposeTestRule.checkAuthScreenIsDisplayed() {
      onNodeWithTag(NavigationTestTags.SIGN_IN_SCREEN).assertIsDisplayed()
  }

  fun ComposeTestRule.checkHomeScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.HOME_SCREEN).assertIsDisplayed()
    onNodeWithTag(NavigationTestTags.HOME_TAB).assertIsDisplayed().assertIsSelected()
  }

  fun ComposeTestRule.checkMapScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.MAP_SCREEN).assertIsDisplayed()
    onNodeWithTag(NavigationTestTags.MAP_TAB).assertIsDisplayed().assertIsSelected()
  }

  fun ComposeTestRule.checkCameraScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()
  }

  fun ComposeTestRule.checkCollectionScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.COLLECTION_SCREEN).assertIsDisplayed()
    onNodeWithTag(NavigationTestTags.COLLECTION_TAB).assertIsDisplayed().assertIsSelected()
  }

  fun ComposeTestRule.checkReportScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.REPORT_SCREEN).assertIsDisplayed()
    onNodeWithTag(NavigationTestTags.REPORT_TAB).assertIsDisplayed().assertIsSelected()
  }

  fun ComposeTestRule.checkProfileScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.PROFILE_SCREEN).assertIsDisplayed()
  }

  fun ComposeTestRule.checkSettingsScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.SETTINGS_SCREEN).assertIsDisplayed()
  }

  fun ComposeTestRule.checkEditProfileScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.EDIT_PROFILE_SCREEN).assertIsDisplayed()
  }

  fun ComposeTestRule.checkPostDetailsScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.POST_DETAILS_SCREEN).assertIsDisplayed()
  }

  fun ComposeTestRule.checkAnimalInformationScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.ANIMAL_INFORMATION_SCREEN).assertIsDisplayed()
  }

  fun ComposeTestRule.checkSubmitReportScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.SUBMIT_REPORT_SCREEN).assertIsDisplayed()
  }

  fun ComposeTestRule.checkAchievementScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.ACHIEVEMENTS_SCREEN).assertIsDisplayed()
  }


  fun ComposeTestRule.navigateToHomeScreen() {
    onNodeWithTag(NavigationTestTags.HOME_TAB).assertIsDisplayed().performClick()
  }

  fun ComposeTestRule.navigateToMapScreen() {
    onNodeWithTag(NavigationTestTags.MAP_TAB).assertIsDisplayed().performClick()
  }

  fun ComposeTestRule.navigateToCameraScreen() {
    onNodeWithTag(NavigationTestTags.CAMERA_TAB).assertIsDisplayed().performClick()
  }

  fun ComposeTestRule.navigateToCollectionScreen() {
    onNodeWithTag(NavigationTestTags.COLLECTION_TAB).assertIsDisplayed().performClick()
  }

  fun ComposeTestRule.navigateToReportScreen() {
    onNodeWithTag(NavigationTestTags.REPORT_TAB).assertIsDisplayed().performClick()
  }

  fun ComposeTestRule.navigateToMyProfileScreen() {}

  fun ComposeTestRule.navigateToProfileScreen(userId: String) {}

  fun ComposeTestRule.navigateToPostDetailsScreen(postUid: String) {}

  fun ComposeTestRule.navigateToAnimalDetailsScreen(animalUid: String) {}

  fun ComposeTestRule.navigateToSubmitReportScreen() {}

  fun ComposeTestRule.navigateToReportDetailsScreen(reportUid: String) {}

  fun ComposeTestRule.navigateToAchievementsScreen() {}

  fun ComposeTestRule.navigateToSettingsScreen() {}

  fun ComposeTestRule.navigateToEditProfileScreen() {}
}
