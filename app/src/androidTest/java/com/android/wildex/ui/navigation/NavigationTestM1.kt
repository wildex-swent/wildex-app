package com.android.wildex.ui.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.android.wildex.WildexApp
import com.android.wildex.ui.theme.WildexTheme
import com.android.wildex.utils.FirebaseEmulator
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NavigationTestM1 {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var navController: TestNavHostController

  @Before
  fun setup() {
    assert(FirebaseEmulator.isRunning) {
      "FirebaseEmulator must be running before using FirestoreTest"
    }
    composeRule.setContent {
      navController =
          TestNavHostController(LocalContext.current).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
          }
      WildexTheme { WildexApp(context = LocalContext.current, navController = navController) }
    }
  }

  @After
  fun teardown() {
    runBlocking {
      FirebaseEmulator.clearAuthEmulator()
      FirebaseEmulator.clearFirestoreEmulator()
    }
  }

  @Test
  fun startsAtHomeScreen_whenAuthenticated() {
    runBlocking { FirebaseEmulator.auth.signInAnonymously().await() }
    composeRule.waitForIdle()

    assertNotNull(FirebaseEmulator.auth.currentUser)
    assertEquals(Screen.Home.route, navController.currentBackStackEntry?.destination?.route)
  }

  @Test
  fun startsAtAuthScreen_whenNotAuthenticated() {
    // Sign out first just in case
    runBlocking { FirebaseEmulator.auth.signOut() }
    composeRule.waitForIdle()

    assertNull(FirebaseEmulator.auth.currentUser)
    assertEquals(Screen.Auth.route, navController.currentBackStackEntry?.destination?.route)
  }

  @Test
  fun navigation_AuthScreen() {
    runBlocking { FirebaseEmulator.auth.signOut() }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsNotDisplayed()
    assertEquals(Screen.Auth.route, navController.currentBackStackEntry?.destination?.route)
  }

  @Test
  fun navigation_HomeScreen() {
    runBlocking { FirebaseEmulator.auth.signInAnonymously().await() }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    composeRule
        .onNodeWithTag(NavigationTestTags.HOME_TAB)
        .assertIsDisplayed()
        .performClick()
        .assertIsSelected()
    composeRule.waitForIdle()

    assertEquals(Screen.Home.route, navController.currentBackStackEntry?.destination?.route)
  }

  @Test
  fun navigation_MapScreen() {
    runBlocking { FirebaseEmulator.auth.signInAnonymously().await() }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    composeRule
        .onNodeWithTag(NavigationTestTags.MAP_TAB)
        .assertIsDisplayed()
        .performClick()
        .assertIsSelected()
    composeRule.waitForIdle()

    assertEquals(Screen.Map.route, navController.currentBackStackEntry?.destination?.route)
  }

  @Test
  fun navigation_CameraScreen() {
    runBlocking { FirebaseEmulator.auth.signInAnonymously().await() }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    composeRule
        .onNodeWithTag(NavigationTestTags.CAMERA_TAB)
        .assertIsDisplayed()
        .performClick()
        .assertIsSelected()
    composeRule.waitForIdle()

    assertEquals(Screen.Camera.route, navController.currentBackStackEntry?.destination?.route)
  }

  @Test
  fun navigation_CollectionScreen() {
    runBlocking { FirebaseEmulator.auth.signInAnonymously().await() }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    composeRule
        .onNodeWithTag(NavigationTestTags.COLLECTION_TAB)
        .assertIsDisplayed()
        .performClick()
        .assertIsSelected()
    composeRule.waitForIdle()

    assertEquals(Screen.Collection.route, navController.currentBackStackEntry?.destination?.route)
  }

  @Test
  fun navigation_ReportScreen() {
    runBlocking { FirebaseEmulator.auth.signInAnonymously().await() }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    composeRule
        .onNodeWithTag(NavigationTestTags.REPORT_TAB)
        .assertIsDisplayed()
        .performClick()
        .assertIsSelected()
    composeRule.waitForIdle()

    assertEquals(Screen.Report.route, navController.currentBackStackEntry?.destination?.route)
  }

  /*@Test
  fun navigation_ProfileScreen() {
    val uid = runBlocking {
      val result = FirebaseEmulator.auth.signInAnonymously().await()
      val user =
          User(
              userId = result.user!!.uid,
              username = "tester",
              name = "John",
              surname = "Nolan",
              bio = "Police officer",
              profilePictureURL =
                  "https://static.wikia.nocookie.net/whumpapedia/images/5/52/John_Nolan.jpg/revision/latest/thumbnail/width/360/height/450?cb=20210309020955",
              userType = UserType.REGULAR,
              creationDate = Timestamp.now(),
              country = "USA",
              friendsCount = 5000,
          )
      RepositoryProvider.userRepository.addUser(user)
      result.user!!.uid
    }
    composeRule.waitForIdle()
    assertEquals(Screen.Home.route, navController.currentBackStackEntry?.destination?.route)
    composeRule
        .onNodeWithTag(HomeScreenTestTags.PROFILE_PICTURE, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()
    composeRule.waitForIdle()
    assertEquals(
        "${Screen.Profile.PATH}/{userUid}",
        navController.currentBackStackEntry?.destination?.route,
    )
    val actualUid = navController.currentBackStackEntry?.arguments?.getString("userUid")
    assertEquals(uid, actualUid)
  }

  @Test
  fun navigation_PostDetails() {
    val postId = "post_for_profile_nav"
    val uid = runBlocking {
      val result = FirebaseEmulator.auth.signInAnonymously().await()
      val user =
          User(
              userId = result.user!!.uid,
              username = "tester",
              name = "John",
              surname = "Nolan",
              bio = "Police officer",
              profilePictureURL =
                  "https://static.wikia.nocookie.net/whumpapedia/images/5/52/John_Nolan.jpg/revision/latest/thumbnail/width/360/height/450?cb=20210309020955",
              userType = UserType.REGULAR,
              creationDate = Timestamp.now(),
              country = "USA",
              friendsCount = 5000,
          )
      RepositoryProvider.userRepository.addUser(user)
      val post =
          Post(
              postId = postId,
              authorId = result.user!!.uid,
              pictureURL = "https://example.com/post.jpg",
              location = Location(37.7749, -122.4194),
              description = "A sample post",
              date = Timestamp.now(),
              animalId = "animal123",
              likesCount = 0,
              commentsCount = 0,
          )
      RepositoryProvider.postRepository.addPost(post)
      result.user!!.uid
    }
    composeRule.waitForIdle()
    assertEquals(Screen.Home.route, navController.currentBackStackEntry?.destination?.route)

    val homeImageTag = HomeScreenTestTags.imageTag(postId)
    composeRule.waitUntil(5_000) {
      try {
        composeRule
            .onAllNodesWithTag(homeImageTag, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      } catch (_: Throwable) {
        false
      }
    }
    composeRule
        .onNodeWithTag(homeImageTag, useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()
    composeRule.waitForIdle()
    assertEquals("post_details/{postUid}", navController.currentBackStackEntry?.destination?.route)
    val actualUid = navController.currentBackStackEntry?.arguments?.getString("postUid")
    assertEquals(postId, actualUid)
  }*/
}
