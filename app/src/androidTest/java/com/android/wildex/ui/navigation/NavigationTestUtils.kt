package com.android.wildex.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.achievement.AchievementsScreenTestTags
import com.android.wildex.ui.animal.AnimalInformationScreenTestTags
import com.android.wildex.ui.authentication.SignInScreenTestTags
import com.android.wildex.ui.collection.CollectionScreenTestTags
import com.android.wildex.ui.home.HomeScreenTestTags
import com.android.wildex.ui.post.PostDetailsScreenTestTags
import com.android.wildex.ui.profile.EditProfileScreenTestTags
import com.android.wildex.ui.profile.ProfileScreenTestTags
import com.android.wildex.ui.settings.SettingsScreenTestTags
import com.android.wildex.utils.FirebaseEmulator
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals

private const val DEFAULT_TIMEOUT = 5_000L

/** Base class for all Wildex tests, providing common setup and utility functions. */
abstract class NavigationTestUtils {

  protected lateinit var navController: NavHostController

  init {
    assert(FirebaseEmulator.isRunning) {
      "FirebaseEmulator must be running before using FirestoreTest"
    }
  }

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
    assertEquals(Screen.Auth.route, navController.currentDestination?.route)
  }

  fun ComposeTestRule.checkHomeScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.HOME_SCREEN).assertIsDisplayed()
    onNodeWithTag(NavigationTestTags.HOME_TAB).assertIsDisplayed().assertIsSelected()
    assertEquals(Screen.Home.route, navController.currentDestination?.route)
  }

  fun ComposeTestRule.checkMapScreenIsDisplayed(userId: Id) {
    onNodeWithTag(NavigationTestTags.MAP_SCREEN).assertIsDisplayed()
    onNodeWithTag(NavigationTestTags.MAP_TAB).assertIsDisplayed().assertIsSelected()
    assertEquals(userId, navController.currentBackStackEntry?.arguments?.getString("userUid"))
    assertEquals(Screen.Map.PATH, navController.currentDestination?.route)
  }

  fun ComposeTestRule.checkCameraScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()
    assertEquals(Screen.Camera.route, navController.currentDestination?.route)
  }

  fun ComposeTestRule.checkCollectionScreenIsDisplayed(userId: Id, isCurrentUser: Boolean = true) {
    onNodeWithTag(NavigationTestTags.COLLECTION_SCREEN).assertIsDisplayed()
    if (isCurrentUser)
        onNodeWithTag(NavigationTestTags.COLLECTION_TAB).assertIsDisplayed().assertIsSelected()
    assertEquals(userId, navController.currentBackStackEntry?.arguments?.getString("userUid"))
    assertEquals(Screen.Collection.PATH, navController.currentDestination?.route)
  }

  fun ComposeTestRule.checkReportScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.REPORT_SCREEN).assertIsDisplayed()
    onNodeWithTag(NavigationTestTags.REPORT_TAB).assertIsDisplayed().assertIsSelected()
    assertEquals(Screen.Report.route, navController.currentDestination?.route)
  }

  fun ComposeTestRule.checkProfileScreenIsDisplayed(userId: Id) {
    assertEquals(Screen.Profile.PATH, navController.currentDestination?.route)
    assertEquals(userId, navController.currentBackStackEntry?.arguments?.getString("userUid"))
    onNodeWithTag(NavigationTestTags.PROFILE_SCREEN).assertIsDisplayed()
  }

  fun ComposeTestRule.checkSettingsScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.SETTINGS_SCREEN).assertIsDisplayed()
    assertEquals(Screen.Settings.route, navController.currentDestination?.route)
  }

  fun ComposeTestRule.checkEditProfileScreenIsDisplayed(isNewUser: Boolean) {
    onNodeWithTag(NavigationTestTags.EDIT_PROFILE_SCREEN).assertIsDisplayed()
    assertEquals(Screen.EditProfile.PATH, navController.currentDestination?.route)
    assertEquals(isNewUser, navController.currentBackStackEntry?.arguments?.getBoolean("isNewUser"))
  }

  fun ComposeTestRule.checkPostDetailsScreenIsDisplayed(postUid: Id) {
    assertEquals(Screen.PostDetails.PATH, navController.currentDestination?.route)
    assertEquals(postUid, navController.currentBackStackEntry?.arguments?.getString("postUid"))
    onNodeWithTag(NavigationTestTags.POST_DETAILS_SCREEN).assertIsDisplayed()
  }

  fun ComposeTestRule.checkAnimalInformationScreenIsDisplayed(animalUid: Id) {
    onNodeWithTag(NavigationTestTags.ANIMAL_INFORMATION_SCREEN).assertIsDisplayed()
    assertEquals(Screen.AnimalInformation.PATH, navController.currentDestination?.route)
    assertEquals(animalUid, navController.currentBackStackEntry?.arguments?.getString("animalUid"))
  }

  fun ComposeTestRule.checkAchievementsScreenIsDisplayed(userId: Id) {
    onNodeWithTag(NavigationTestTags.ACHIEVEMENTS_SCREEN).assertIsDisplayed()
    assertEquals(Screen.Achievements.PATH, navController.currentDestination?.route)
    assertEquals(userId, navController.currentBackStackEntry?.arguments?.getString("userUid"))
  }

  fun ComposeTestRule.checkSubmitReportScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.SUBMIT_REPORT_SCREEN).assertIsDisplayed()
    assertEquals(Screen.SubmitReport.route, navController.currentDestination?.route)
  }

  private fun ComposeTestRule.performClickOnTag(
      tag: String,
      useUnmergedTree: Boolean = true,
      timeout: Long = DEFAULT_TIMEOUT,
  ) {
    val node = onNodeWithTag(tag, useUnmergedTree)
    waitUntil(timeout) { node.isDisplayed() }
    node.performClick()
  }

  // Navigation helpers

  fun ComposeTestRule.navigateToHomeScreenFromBottomBar() {
    onNodeWithTag(NavigationTestTags.HOME_TAB).assertIsDisplayed().performClick()
  }

  fun ComposeTestRule.navigateFromAuth() {
    performClickOnTag(SignInScreenTestTags.LOGIN_BUTTON)
  }

  fun ComposeTestRule.navigateToMapScreenFromBottomBar() {
    onNodeWithTag(NavigationTestTags.MAP_TAB).assertIsDisplayed().performClick()
  }

  fun ComposeTestRule.navigateToMapFromProfile() {
    performClickOnTag(ProfileScreenTestTags.MAP_CTA)
  }

  fun ComposeTestRule.navigateBackFromMap() {}

  fun ComposeTestRule.navigateToCameraScreenFromBottomBar() {
    onNodeWithTag(NavigationTestTags.CAMERA_TAB).assertIsDisplayed().performClick()
  }

  fun ComposeTestRule.navigateToCollectionScreenFromBottomBar() {
    onNodeWithTag(NavigationTestTags.COLLECTION_TAB).assertIsDisplayed().performClick()
  }

  fun ComposeTestRule.navigateToCollectionScreenFromProfile() {
    performClickOnTag(ProfileScreenTestTags.COLLECTION)
  }

  fun ComposeTestRule.navigateBackFromCollection() {
    performClickOnTag(CollectionScreenTestTags.GO_BACK_BUTTON)
  }

  fun ComposeTestRule.navigateBackFromProfile() {
    performClickOnTag(ProfileScreenTestTags.GO_BACK)
  }

  fun ComposeTestRule.navigateToReportScreenFromBottomBar() {
    onNodeWithTag(NavigationTestTags.REPORT_TAB).assertIsDisplayed().performClick()
  }

  fun ComposeTestRule.navigateToMyProfileScreenFromHome() {
    performClickOnTag(HomeScreenTestTags.PROFILE_PICTURE)
  }

  fun ComposeTestRule.navigateToMyProfileScreenFromCollection() {
    performClickOnTag(CollectionScreenTestTags.PROFILE_BUTTON)
  }

  fun ComposeTestRule.navigateToPostDetailsScreenFromHome(postUid: String) {
    performClickOnTag(HomeScreenTestTags.imageTag(postUid))
  }

  fun ComposeTestRule.navigateBackFromPostDetails() {
    performClickOnTag(PostDetailsScreenTestTags.BACK_BUTTON)
  }

  fun ComposeTestRule.navigateToProfileFromPostDetails(userId: String) {
    performClickOnTag(PostDetailsScreenTestTags.testTagForProfilePicture(userId, "author"))
  }

  fun ComposeTestRule.navigateToAnimalInformationScreenFromCollection(animalUid: String) {
    performClickOnTag(CollectionScreenTestTags.testTagForAnimal(animalUid, true))
  }

  fun ComposeTestRule.navigateBackFromAnimalInformation() {
    performClickOnTag(AnimalInformationScreenTestTags.BACK_BUTTON)
  }

  fun ComposeTestRule.navigateToAchievementsScreenFromProfile() {
    performClickOnTag(ProfileScreenTestTags.ACHIEVEMENTS_CTA)
  }

  fun ComposeTestRule.navigateBackFromAchievements() {
    performClickOnTag(AchievementsScreenTestTags.BACK_BUTTON)
  }

  fun ComposeTestRule.navigateToSettingsScreenFromProfile() {
    performClickOnTag(ProfileScreenTestTags.SETTINGS)
  }

  fun ComposeTestRule.navigateBackFromSettings() {
    performClickOnTag(SettingsScreenTestTags.GO_BACK_BUTTON)
  }

  fun ComposeTestRule.navigateToSubmitReportScreenFromReport() {

  }

  fun ComposeTestRule.navigateToEditProfileScreenFromSettings() {
    performClickOnTag(SettingsScreenTestTags.EDIT_PROFILE_BUTTON)
  }

  fun ComposeTestRule.navigateBackFromEditProfile() {
    performClickOnTag(EditProfileScreenTestTags.GO_BACK)
  }
}
