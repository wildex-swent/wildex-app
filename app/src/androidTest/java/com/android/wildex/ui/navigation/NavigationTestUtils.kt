package com.android.wildex.ui.navigation

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.rule.GrantPermissionRule
import com.android.wildex.BuildConfig
import com.android.wildex.WildexApp
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.OnBoardingStage
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingScreenTestTags
import com.android.wildex.ui.achievement.AchievementsScreenTestTags
import com.android.wildex.ui.animal.AnimalInformationScreenTestTags
import com.android.wildex.ui.authentication.NamingScreenTestTags
import com.android.wildex.ui.authentication.OptionalInfoScreenTestTags
import com.android.wildex.ui.authentication.SignInScreenTestTags
import com.android.wildex.ui.authentication.UserTypeScreenTestTags
import com.android.wildex.ui.collection.CollectionScreenTestTags
import com.android.wildex.ui.home.HomeScreenTestTags
import com.android.wildex.ui.map.MapContentTestTags
import com.android.wildex.ui.post.PostDetailsScreenTestTags
import com.android.wildex.ui.profile.EditProfileScreenTestTags
import com.android.wildex.ui.profile.ProfileScreenTestTags
import com.android.wildex.ui.report.ReportScreenTestTags
import com.android.wildex.ui.report.SubmitReportFormScreenTestTags
import com.android.wildex.ui.settings.SettingsScreenTestTags
import com.android.wildex.ui.social.FriendScreenTestTags
import com.android.wildex.ui.theme.WildexTheme
import com.android.wildex.utils.FakeCredentialManager
import com.android.wildex.utils.FakeJwtGenerator
import com.android.wildex.utils.FirebaseEmulator
import com.android.wildex.utils.offline.FakeConnectivityObserver
import com.google.firebase.Timestamp
import com.mapbox.common.MapboxOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule

const val DEFAULT_TIMEOUT = 10_000L

/** Base class for all Wildex tests, providing common setup and utility functions. */
abstract class NavigationTestUtils {
  protected lateinit var navController: NavHostController

  protected lateinit var userId: Id
  protected val fakeObserver = FakeConnectivityObserver(initial = true)

  init {
    assert(FirebaseEmulator.isRunning) {
      "FirebaseEmulator must be running before using FirestoreTest"
    }
  }

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    RepositoryProvider.init(composeRule.activity)
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken("12345", email = "test@example.com")
    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)

    composeRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        navController = rememberNavController()
        WildexTheme {
          WildexApp(
              context = LocalContext.current,
              navController = navController,
              credentialManager = fakeCredentialManager,
          )
        }
      }
    }
    userId = runBlocking {
      val result = FirebaseEmulator.auth.signInAnonymously().await()
      val user = user0.copy(userId = result.user!!.uid)
      RepositoryProvider.userRepository.addUser(user)
      RepositoryProvider.userAnimalsRepository.initializeUserAnimals(user.userId)
      RepositoryProvider.userAchievementsRepository.initializeUserAchievements(user.userId)
      RepositoryProvider.userSettingsRepository.initializeUserSettings(user.userId)
      RepositoryProvider.userFriendsRepository.initializeUserFriends(user.userId)
      RepositoryProvider.userTokensRepository.initializeUserTokens(user.userId)
      result.user!!.uid
    }
  }

  @After
  fun teardown() {
    FirebaseEmulator.auth.signOut()
    FirebaseEmulator.clearAuthEmulator()
    FirebaseEmulator.clearFirestoreEmulator()
  }

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
      )

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
          onBoardingStage = OnBoardingStage.COMPLETE,
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
          onBoardingStage = OnBoardingStage.COMPLETE,
      )

  open val post0 =
      Post(
          postId = "0",
          authorId = user0.userId,
          pictureURL = "https://cdn-icons-png.flaticon.com/512/4823/4823463.png",
          location = null,
          description = "This is my first post",
          date = Timestamp.now(),
          animalId = "0",
      )

  open val post1 =
      Post(
          postId = "1",
          authorId = user1.userId,
          pictureURL = "https://cdn-icons-png.flaticon.com/512/4823/4823463.png",
          location = null,
          description = "This my post",
          date = Timestamp.now(),
          animalId = "0",
      )

  open val animal0 =
      Animal(
          animalId = "0",
          pictureURL = "",
          name = "Lion",
          species = "Big Cat",
          description = "The lion is a species in the family Felidae",
      )

  // BOTTOM NAVIGATION BAR
  fun ComposeTestRule.checkBottomNavigationIsDisplayed() {
    checkNodeWithTagGetsDisplayed(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
  }

  // AUTH SCREEN
  fun ComposeTestRule.checkAuthScreenIsDisplayed() {
    checkNodeWithTagGetsDisplayed(NavigationTestTags.SIGN_IN_SCREEN)
    assertEquals(Screen.Auth.route, navController.currentDestination?.route)
  }

  // HOME SCREEN
  fun ComposeTestRule.checkHomeScreenIsDisplayed() {
    checkNodeWithTagGetsDisplayed(NavigationTestTags.HOME_SCREEN)
    onNodeWithTag(NavigationTestTags.HOME_TAB).assertIsDisplayed().assertIsSelected()
    assertEquals(Screen.Home.route, navController.currentDestination?.route)
  }

  // MAP SCREEN
  fun ComposeTestRule.checkMapScreenIsDisplayed(userId: Id, isCurrentUser: Boolean = true) {
    checkNodeWithTagGetsDisplayed(NavigationTestTags.MAP_SCREEN)
    assertEquals(Screen.Map.PATH, navController.currentDestination?.route)
    if (isCurrentUser)
        onNodeWithTag(NavigationTestTags.MAP_TAB).assertIsDisplayed().assertIsSelected()
    assertEquals(userId, navController.currentBackStackEntry?.arguments?.getString("userUid"))
  }

  // CAMERA SCREEN
  fun ComposeTestRule.checkCameraScreenIsDisplayed() {
    checkNodeWithTagGetsDisplayed(NavigationTestTags.CAMERA_SCREEN)
    assertEquals(Screen.Camera.route, navController.currentDestination?.route)
  }

  // COLLECTION SCREEN
  fun ComposeTestRule.checkCollectionScreenIsDisplayed(userId: Id, isCurrentUser: Boolean = true) {
    checkNodeWithTagGetsDisplayed(NavigationTestTags.COLLECTION_SCREEN)
    assertEquals(Screen.Collection.PATH, navController.currentDestination?.route)
    if (isCurrentUser)
        onNodeWithTag(NavigationTestTags.COLLECTION_TAB).assertIsDisplayed().assertIsSelected()
    assertEquals(userId, navController.currentBackStackEntry?.arguments?.getString("userUid"))
  }

  // REPORT SCREEN
  fun ComposeTestRule.checkReportScreenIsDisplayed() {
    checkNodeWithTagGetsDisplayed(NavigationTestTags.REPORT_SCREEN)
    onNodeWithTag(NavigationTestTags.REPORT_TAB).assertIsDisplayed().assertIsSelected()
    assertEquals(Screen.Report.route, navController.currentDestination?.route)
  }

  // PROFILE SCREEN
  fun ComposeTestRule.checkProfileScreenIsDisplayed(userId: Id) {
    checkNodeWithTagGetsDisplayed(NavigationTestTags.PROFILE_SCREEN)
    assertEquals(Screen.Profile.PATH, navController.currentDestination?.route)
    assertEquals(userId, navController.currentBackStackEntry?.arguments?.getString("userUid"))
  }

  // SETTINGS SCREEN
  fun ComposeTestRule.checkSettingsScreenIsDisplayed() {
    checkNodeWithTagGetsDisplayed(NavigationTestTags.SETTINGS_SCREEN)
    assertEquals(Screen.Settings.route, navController.currentDestination?.route)
  }

  // EDIT PROFILE SCREEN
  fun ComposeTestRule.checkEditProfileScreenIsDisplayed() {
    checkNodeWithTagGetsDisplayed(NavigationTestTags.EDIT_PROFILE_SCREEN)
    assertEquals(Screen.EditProfile.route, navController.currentDestination?.route)
  }

  // POST DETAILS SCREEN
  fun ComposeTestRule.checkPostDetailsScreenIsDisplayed(postUid: Id) {
    checkNodeWithTagGetsDisplayed(NavigationTestTags.POST_DETAILS_SCREEN)
    assertEquals(Screen.PostDetails.PATH, navController.currentDestination?.route)
    assertEquals(postUid, navController.currentBackStackEntry?.arguments?.getString("postUid"))
  }

  // ANIMAL INFORMATION SCREEN
  fun ComposeTestRule.checkAnimalInformationScreenIsDisplayed(animalUid: Id) {
    checkNodeWithTagGetsDisplayed(NavigationTestTags.ANIMAL_INFORMATION_SCREEN)
    assertEquals(Screen.AnimalInformation.PATH, navController.currentDestination?.route)
    assertEquals(animalUid, navController.currentBackStackEntry?.arguments?.getString("animalUid"))
  }

  // ACHIEVEMENTS SCREEN
  fun ComposeTestRule.checkAchievementsScreenIsDisplayed(userId: Id) {
    checkNodeWithTagGetsDisplayed(NavigationTestTags.ACHIEVEMENTS_SCREEN)
    assertEquals(Screen.Achievements.PATH, navController.currentDestination?.route)
    assertEquals(userId, navController.currentBackStackEntry?.arguments?.getString("userUid"))
  }

  // SUBMIT REPORT SCREEN
  fun ComposeTestRule.checkSubmitReportScreenIsDisplayed() {
    checkNodeWithTagGetsDisplayed(NavigationTestTags.SUBMIT_REPORT_SCREEN)
    assertEquals(Screen.SubmitReport.route, navController.currentDestination?.route)
  }

  // FRIEND SCREEN
  fun ComposeTestRule.checkFriendScreenIsDisplayed() {
    checkNodeWithTagGetsDisplayed(NavigationTestTags.FRIEND_SCREEN)
    assertEquals(Screen.Social.PATH, navController.currentDestination?.route)
    assertEquals(userId, navController.currentBackStackEntry?.arguments?.getString("userUid"))
  }

  // BOTTOM BAR -> HOME SCREEN
  fun ComposeTestRule.navigateToHomeScreenFromBottomBar() {
    onNodeWithTag(NavigationTestTags.HOME_TAB).assertIsDisplayed().performClick()
  }

  // AUTH SCREEN -> HOME SCREEN
  fun ComposeTestRule.navigateToHomeScreenFromAuth() {
    performClickOnTag(SignInScreenTestTags.LOGIN_BUTTON)
    checkNodeWithTagGetsDisplayed(NamingScreenTestTags.NAMING_SCREEN)
    onNodeWithTag(NamingScreenTestTags.NAME_FIELD).performTextInput("TestName")
    onNodeWithTag(NamingScreenTestTags.SURNAME_FIELD).performTextInput("TestSurname")
    onNodeWithTag(NamingScreenTestTags.USERNAME_FIELD).performTextInput("TestUsername")
    performClickOnTag(NamingScreenTestTags.NEXT_BUTTON)
    performClickOnTag(OptionalInfoScreenTestTags.NEXT_BUTTON)
    performClickOnTag(UserTypeScreenTestTags.COMPLETE_BUTTON)
  }

  // BOTTOM BAR -> MAP SCREEN
  fun ComposeTestRule.navigateToMapScreenFromBottomBar() {
    onNodeWithTag(NavigationTestTags.MAP_TAB).assertIsDisplayed().performClick()
  }

  // PROFILE SCREEN -> MAP SCREEN
  fun ComposeTestRule.navigateToMapScreenFromProfile() {
    performClickOnTag(ProfileScreenTestTags.MAP_CTA)
  }

  // MAP SCREEN (BACK)
  fun ComposeTestRule.navigateBackFromMap() {
    performClickOnTag(MapContentTestTags.BACK_BUTTON)
  }

  // BOTTOM BAR -> CAMERA SCREEN
  fun ComposeTestRule.navigateToCameraScreenFromBottomBar() {
    onNodeWithTag(NavigationTestTags.CAMERA_TAB).assertIsDisplayed().performClick()
  }

  // BOTTOM BAR -> COLLECTION SCREEN
  fun ComposeTestRule.navigateToCollectionScreenFromBottomBar() {
    onNodeWithTag(NavigationTestTags.COLLECTION_TAB).assertIsDisplayed().performClick()
  }

  // PROFILE SCREEN -> COLLECTION SCREEN
  fun ComposeTestRule.navigateToCollectionScreenFromProfile() {
    performClickOnTag(ProfileScreenTestTags.ANIMAL_COUNT)
  }

  // COLLECTION SCREEN (BACK)
  fun ComposeTestRule.navigateBackFromCollection() {
    performClickOnTag(CollectionScreenTestTags.GO_BACK_BUTTON)
  }

  // PROFILE SCREEN (BACK)
  fun ComposeTestRule.navigateBackFromProfile() {
    performClickOnTag(ProfileScreenTestTags.GO_BACK)
  }

  // BOTTOM BAR -> REPORT SCREEN
  fun ComposeTestRule.navigateToReportScreenFromBottomBar() {
    onNodeWithTag(NavigationTestTags.REPORT_TAB).assertIsDisplayed().performClick()
  }

  // HOME SCREEN -> MY PROFILE SCREEN
  fun ComposeTestRule.navigateToMyProfileScreenFromHome() {
    performClickOnTag(NavigationTestTags.TOP_BAR_PROFILE_PICTURE)
  }

  // HOME SCREEN -> PROFILE SCREEN

  fun ComposeTestRule.navigateToProfileScreenFromHome(postId: Id) {
    performClickOnTag(HomeScreenTestTags.authorPictureTag(postId))
  }

  // COLLECTION SCREEN -> PROFILE SCREEN
  fun ComposeTestRule.navigateToMyProfileScreenFromCollection() {
    performClickOnTag(NavigationTestTags.TOP_BAR_PROFILE_PICTURE)
  }

  // HOME SCREEN -> POST DETAILS SCREEN
  fun ComposeTestRule.navigateToPostDetailsScreenFromHome(postUid: String) {
    performClickOnTag(HomeScreenTestTags.imageTag(postUid))
  }

  // POST DETAILS SCREEN (BACK)
  fun ComposeTestRule.navigateBackFromPostDetails() {
    performClickOnTag(PostDetailsScreenTestTags.BACK_BUTTON)
  }

  // POST DETAILS SCREEN -> PROFILE SCREEN
  fun ComposeTestRule.navigateToProfileFromPostDetails(userId: String) {
    performClickOnTag(PostDetailsScreenTestTags.testTagForProfilePicture(userId, "author"))
  }

  // COLLECTION SCREEN -> ANIMAL INFORMATION SCREEN
  fun ComposeTestRule.navigateToAnimalInformationScreenFromCollection(animalUid: String) {
    performClickOnTag(CollectionScreenTestTags.testTagForAnimal(animalUid, true))
  }

  // ANIMAL INFORMATION SCREEN (BACK)
  fun ComposeTestRule.navigateBackFromAnimalInformation() {
    performClickOnTag(AnimalInformationScreenTestTags.BACK_BUTTON)
  }

  // PROFILE SCREEN -> ACHIEVEMENTS SCREEN
  fun ComposeTestRule.navigateToAchievementsScreenFromProfile() {
    performClickOnTag(ProfileScreenTestTags.ACHIEVEMENTS_CTA)
  }

  // PROFILE SCREEN -> FRIEND SCREEN
  fun ComposeTestRule.navigateToFriendScreenFromProfile() {
    performClickOnTag(ProfileScreenTestTags.FRIENDS_COUNT)
  }

  fun ComposeTestRule.navigateBackFromFriend() {
    performClickOnTag(FriendScreenTestTags.GO_BACK_BUTTON)
  }

  // ACHIEVEMENTS SCREEN (BACK)
  fun ComposeTestRule.navigateBackFromAchievements() {
    performClickOnTag(AchievementsScreenTestTags.BACK_BUTTON)
  }

  // PROFILE SCREEN -> SETTINGS SCREEN
  fun ComposeTestRule.navigateToSettingsScreenFromProfile() {
    performClickOnTag(ProfileScreenTestTags.SETTINGS)
  }

  // SETTINGS SCREEN (BACK)
  fun ComposeTestRule.navigateBackFromSettings() {
    performClickOnTag(SettingsScreenTestTags.GO_BACK_BUTTON)
  }

  // REPORT SCREEN -> SUBMIT REPORT SCREEN
  fun ComposeTestRule.navigateToSubmitReportScreenFromReport() {
    performClickOnTag(ReportScreenTestTags.MORE_ACTIONS_BUTTON)
    performClickOnTag(ReportScreenTestTags.SUBMIT_REPORT_BUTTON)
  }

  // SUBMIT REPORT SCREEN (BACK)
  fun ComposeTestRule.navigateBackFromSubmitReport() {
    performClickOnTag(SubmitReportFormScreenTestTags.BACK_BUTTON)
  }

  // SETTINGS SCREEN -> EDIT PROFILE SCREEN
  fun ComposeTestRule.navigateToEditProfileScreenFromSettings() {
    performClickOnTag(SettingsScreenTestTags.EDIT_PROFILE_BUTTON)
  }

  // EDIT PROFILE SCREEN (BACK)
  fun ComposeTestRule.navigateBackFromEditProfile() {
    performClickOnTag(EditProfileScreenTestTags.GO_BACK)
  }

  // SETTINGS SCREEN -> AUTH SCREEN
  fun ComposeTestRule.navigateFromSettingsScreenToAuthScreen_LogOut() {
    performClickOnTag(SettingsScreenTestTags.SIGN_OUT_BUTTON)
  }

  // SETTINGS SCREEN -> AUTH SCREEN
  fun ComposeTestRule.navigateFromSettingsScreenToAuthScreen_DeleteAccount() {
    performClickOnTag(SettingsScreenTestTags.DELETE_ACCOUNT_BUTTON)
    performClickOnTag(SettingsScreenTestTags.DELETE_ACCOUNT_CONFIRM_BUTTON)
  }

  // HOME SCREEN -> PROFILE SCREEN
  fun ComposeTestRule.navigateToMyProfileScreenFromReport() {
    performClickOnTag(NavigationTestTags.TOP_BAR_PROFILE_PICTURE)
  }

  private fun ComposeTestRule.checkNodeWithTagGetsDisplayed(tag: String) {
    waitUntil(DEFAULT_TIMEOUT) { onNodeWithTag(tag, useUnmergedTree = true).isDisplayed() }
  }

  private fun ComposeTestRule.performClickOnTag(
      tag: String,
      useUnmergedTree: Boolean = true,
      timeout: Long = DEFAULT_TIMEOUT,
  ) {
    waitUntil(DEFAULT_TIMEOUT) {
      onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN).isNotDisplayed()
    }
    val node = onNodeWithTag(tag, useUnmergedTree)
    try {
      node.performScrollTo()
    } catch (_: AssertionError) {}
    waitUntil(timeout) { node.isDisplayed() }
    node.performClick()
  }
}
