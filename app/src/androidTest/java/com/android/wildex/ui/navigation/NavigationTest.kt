package com.android.wildex.ui.navigation

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import androidx.test.rule.GrantPermissionRule
import com.android.wildex.BuildConfig
import com.android.wildex.WildexApp
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animal.Animal
import com.android.wildex.ui.theme.WildexTheme
import com.android.wildex.utils.FirebaseEmulator
import com.mapbox.common.MapboxOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NavigationTest : NavigationTestUtils() {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    composeRule.setContent {
      navController = rememberNavController()
      WildexTheme { WildexApp(context = LocalContext.current, navController = navController) }
    }
  }

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.CAMERA,
      )

  @Test
  fun startsAtHomeScreen_whenAuthenticated() {
    runBlocking { FirebaseEmulator.auth.signInAnonymously().await() }
    composeRule.waitForIdle()
    assertNotNull(FirebaseEmulator.auth.currentUser)
    composeRule.checkHomeScreenIsDisplayed()
  }

  @Test
  fun startsAtAuthScreen_whenNotAuthenticated() {
    // Sign out first just in case
    runBlocking { FirebaseEmulator.auth.signOut() }
    composeRule.waitForIdle()
    composeRule.checkAuthScreenIsDisplayed()
    assertNull(FirebaseEmulator.auth.currentUser)
  }

  @Test
  fun navigation_AuthScreen() {
    runBlocking { FirebaseEmulator.auth.signOut() }
    composeRule.waitForIdle()
    composeRule.checkAuthScreenIsDisplayed()
    composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsNotDisplayed()
  }

  @Test
  fun navigation_HomeScreen() {
    runBlocking {
      val result = FirebaseEmulator.auth.signInAnonymously().await()
      val user = user0.copy(userId = result.user!!.uid)
      RepositoryProvider.userRepository.addUser(user)
    }
    composeRule.waitForIdle()
    composeRule.navigateToHomeScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  @Test
  fun navigation_MapScreen() {
    val uid = runBlocking { FirebaseEmulator.auth.signInAnonymously().await().user!!.uid }
    composeRule.waitForIdle()
    composeRule.navigateToMapScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkMapScreenIsDisplayed(uid)
    composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  @Test
  fun navigation_CameraScreen() {
    runBlocking { FirebaseEmulator.auth.signInAnonymously().await() }
    composeRule.waitForIdle()
    composeRule.navigateToCameraScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkCameraScreenIsDisplayed()
    composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  @Test
  fun navigation_CollectionScreen() {
    val uid = runBlocking {
      val result = FirebaseEmulator.auth.signInAnonymously().await()
      val user = user0.copy(userId = result.user!!.uid)
      RepositoryProvider.userRepository.addUser(user)
      result.user!!.uid
    }
    composeRule.waitForIdle()
    composeRule.navigateToCollectionScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkCollectionScreenIsDisplayed(uid)
    composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  @Test
  fun navigation_ReportScreen() {
    runBlocking {
      val result = FirebaseEmulator.auth.signInAnonymously().await()
      val user = user0.copy(userId = result.user!!.uid)
      RepositoryProvider.userRepository.addUser(user)
    }
    composeRule.waitForIdle()
    composeRule.navigateToReportScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkReportScreenIsDisplayed()
    composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  @Test
  fun navigation_ProfileScreen_FromHome() {
    val uid = runBlocking {
      val result = FirebaseEmulator.auth.signInAnonymously().await()
      val user = user0.copy(userId = result.user!!.uid)
      RepositoryProvider.userRepository.addUser(user)
      result.user!!.uid
    }
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToMyProfileScreenFromHome()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(uid)
  }

  @Test
  fun navigation_PostDetails() {
    val postId = "post_for_profile_nav"
    runBlocking {
      val result = FirebaseEmulator.auth.signInAnonymously().await()
      val user = user0.copy(userId = result.user!!.uid)
      RepositoryProvider.userRepository.addUser(user)
      val post = post0.copy(authorId = result.user!!.uid, postId = postId)
      RepositoryProvider.postRepository.addPost(post)
    }
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToPostDetailsScreenFromHome(postId)
    composeRule.waitForIdle()
    composeRule.checkPostDetailsScreenIsDisplayed(postId)
  }

  @Test
  fun navigation_ProfileScreen_FromCollection() {
    val uid = runBlocking {
      val result = FirebaseEmulator.auth.signInAnonymously().await()
      val user = user0.copy(userId = result.user!!.uid)
      RepositoryProvider.userRepository.addUser(user)
      result.user!!.uid
    }
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToCollectionScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkCollectionScreenIsDisplayed(uid)
    composeRule.navigateToMyProfileScreenFromCollection()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(uid)
  }

  @Test
  fun navigation_AnimalDetailScreen() {
    val animalId = "animal_id"
    runBlocking {
      val result = FirebaseEmulator.auth.signInAnonymously().await()
      val user = user0.copy(userId = result.user!!.uid)
      RepositoryProvider.userRepository.addUser(user)
      val animal = Animal(animalId, "", "animal", "animal", "")
      RepositoryProvider.animalRepository.addAnimal(animal)
      RepositoryProvider.userAnimalsRepository.initializeUserAnimals(user.userId)
      RepositoryProvider.userAnimalsRepository.addAnimalToUserAnimals(user.userId, animalId)
    }
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToCollectionScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.navigateToAnimalInformationScreenFromCollection(animalId)
    composeRule.waitForIdle()
    composeRule.checkAnimalInformationScreenIsDisplayed(animalId)
  }

  @Test
  fun navigation_AchievementsScreen() {
    val uid = runBlocking {
      val result = FirebaseEmulator.auth.signInAnonymously().await()
      val user = user0.copy(userId = result.user!!.uid)
      RepositoryProvider.userRepository.addUser(user)
      RepositoryProvider.userAchievementsRepository.initializeUserAchievements(user.userId)
      result.user!!.uid
    }
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToMyProfileScreenFromHome()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(uid)
    composeRule.navigateToAchievementsScreenFromProfile()
    composeRule.waitForIdle()
    composeRule.checkAchievementsScreenIsDisplayed(uid)
  }
}
