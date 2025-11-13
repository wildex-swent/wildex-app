package com.android.wildex.ui.navigation

import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.utils.FirebaseEmulator
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NavigationTest : NavigationTestUtils() {

  @Test
  fun startsAtHomeScreen_whenAuthenticated_oldUser() {
    composeRule.waitForIdle()
    assertNotNull(FirebaseEmulator.auth.currentUser)
    composeRule.checkHomeScreenIsDisplayed()
  }

  @Test
  fun startsAtAuthScreen_whenNotAuthenticated() {
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
  fun navigation_AuthScreen_ToEditProfile_ToHome() {
    runBlocking { FirebaseEmulator.auth.signOut() }
    composeRule.waitForIdle()
    composeRule.checkAuthScreenIsDisplayed()
    composeRule.navigateFromAuth()
    composeRule.waitForIdle()
    composeRule.checkEditProfileScreenIsDisplayed(true)
    composeRule.navigateFromEditProfile()
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
  }

  @Test
  fun navigation_HomeScreen_FromAuth() {
    runBlocking { FirebaseEmulator.auth.signOut() }
    composeRule.waitForIdle()
    composeRule.checkAuthScreenIsDisplayed()
    composeRule.navigateFromAuth()
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
  }

  @Test
  fun navigation_HomeScreen_FromBottomBar() {
    composeRule.waitForIdle()
    composeRule.navigateToHomeScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.checkBottomNavigationIsDisplayed()
  }

  @Test
  fun navigation_CameraScreen_FromBottomBar() {
    composeRule.waitForIdle()
    composeRule.navigateToCameraScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkCameraScreenIsDisplayed()
    composeRule.checkBottomNavigationIsDisplayed()
  }

  @Test
  fun navigation_ReportScreen_FromBottomBar() {
    composeRule.waitForIdle()
    composeRule.navigateToReportScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkReportScreenIsDisplayed()
    composeRule.checkBottomNavigationIsDisplayed()
  }

  @Test
  fun navigation_ProfileScreen_FromHome_CurrentUser_AndGoBack() {
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToMyProfileScreenFromHome()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
    composeRule.navigateBackFromProfile()
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
  }

  @Test
  fun navigation_PostDetails_AndGoBack() {
    val postId = "post_for_profile_nav"
    runBlocking {
      val post = post0.copy(authorId = userId, postId = postId)
      RepositoryProvider.postRepository.addPost(post)
      val animal = animal0.copy(animalId = post.animalId)
      RepositoryProvider.animalRepository.addAnimal(animal)
    }
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToPostDetailsScreenFromHome(postId)
    composeRule.waitForIdle()
    composeRule.checkPostDetailsScreenIsDisplayed(postId)
    composeRule.navigateToProfileFromPostDetails(userId)
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
    composeRule.navigateBackFromProfile()
    composeRule.waitForIdle()
    composeRule.checkPostDetailsScreenIsDisplayed(postId)
    composeRule.navigateBackFromPostDetails()
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
  }

  @Test
  fun navigation_AchievementsScreenFromProfile() {
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToMyProfileScreenFromHome()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
    composeRule.navigateToAchievementsScreenFromProfile()
    composeRule.waitForIdle()
    composeRule.checkAchievementsScreenIsDisplayed(userId)
    composeRule.navigateBackFromAchievements()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
  }

  @Test
  fun navigation_CollectionScreenFromMyProfile_AndGoBack() {
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToMyProfileScreenFromHome()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
    composeRule.navigateToCollectionScreenFromProfile()
    composeRule.waitForIdle()
    composeRule.checkCollectionScreenIsDisplayed(userId)
  }

  @Test
  fun navigationCollectionScreenFromOtherProfile_AndGoBack() {
    val userId2 = "userId"
    val postId2 = "postId2"
    val animal2 = "animal2"
    runBlocking {
      val user =
          User(
              userId = userId2,
              name = "name2",
              username = "username2",
              surname = "surname2",
              bio = "bio2",
              profilePictureURL = "",
              userType = UserType.REGULAR,
              creationDate = Timestamp.now(),
              country = "country2",
              friendsCount = 2,
          )
      RepositoryProvider.userRepository.addUser(user)
      RepositoryProvider.userAnimalsRepository.initializeUserAnimals(userId2)
      RepositoryProvider.userAchievementsRepository.initializeUserAchievements(userId2)
      val post = post0.copy(authorId = userId2, postId = postId2, animalId = animal2)
      RepositoryProvider.postRepository.addPost(post)
      val animal = animal0.copy(animalId = animal2)
      RepositoryProvider.animalRepository.addAnimal(animal)
    }
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToPostDetailsScreenFromHome(postId2)
    composeRule.waitForIdle()
    composeRule.checkPostDetailsScreenIsDisplayed(postId2)
    composeRule.navigateToProfileFromPostDetails(userId2)
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId2)
    composeRule.navigateToCollectionScreenFromProfile()
    composeRule.waitForIdle()
    composeRule.checkCollectionScreenIsDisplayed(userId2, false)
    composeRule.navigateBackFromCollection()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId2)
  }

  @Test
  fun navigation_MapScreen_CurrentUser() {
    composeRule.waitForIdle()
    composeRule.navigateToMapScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkMapScreenIsDisplayed(userId)
    composeRule.checkBottomNavigationIsDisplayed()
  }

  @Test
  fun navigation_CollectionScreen_CurrentUser() {
    composeRule.waitForIdle()
    assertEquals(userId, FirebaseEmulator.auth.uid)
    composeRule.navigateToCollectionScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkCollectionScreenIsDisplayed(userId)
    composeRule.checkBottomNavigationIsDisplayed()
  }

  @Test
  fun navigation_AnimalDetailScreen_AndGoBack() {
    val animalId = "animal_id"
    runBlocking {
      val animal = Animal(animalId, "", "animal", "animal", "")
      RepositoryProvider.animalRepository.addAnimal(animal)
      RepositoryProvider.userAnimalsRepository.addAnimalToUserAnimals(userId, animalId)
    }
    assertEquals(FirebaseEmulator.auth.currentUser!!.uid, userId)
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToCollectionScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkCollectionScreenIsDisplayed(userId)
    composeRule.navigateToAnimalInformationScreenFromCollection(animalId)
    composeRule.waitForIdle()
    composeRule.checkAnimalInformationScreenIsDisplayed(animalId)
    composeRule.navigateBackFromAnimalInformation()
    composeRule.waitForIdle()
    composeRule.checkCollectionScreenIsDisplayed(userId)
  }

  @Test
  fun navigation_ProfileScreen_FromCollection_CurrentUser_AndGoBack() {
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToCollectionScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkCollectionScreenIsDisplayed(userId)
    composeRule.navigateToMyProfileScreenFromCollection()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
    composeRule.navigateBackFromProfile()
    composeRule.waitForIdle()
    composeRule.checkCollectionScreenIsDisplayed(userId)
  }

  @Test
  fun navigation_SettingsScreen_FromMyProfile_AndGoBack() {
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToMyProfileScreenFromHome()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
    composeRule.navigateToSettingsScreenFromProfile()
    composeRule.waitForIdle()
    composeRule.checkSettingsScreenIsDisplayed()
    composeRule.navigateBackFromSettings()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
  }

  @Test
  fun navigation_EditProfile_FromSettings_AndGoBack() {
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToMyProfileScreenFromHome()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
    composeRule.navigateToSettingsScreenFromProfile()
    composeRule.waitForIdle()
    composeRule.checkSettingsScreenIsDisplayed()
    composeRule.navigateToEditProfileScreenFromSettings()
    composeRule.waitForIdle()
    composeRule.checkEditProfileScreenIsDisplayed(false)
    composeRule.navigateBackFromEditProfile()
    composeRule.waitForIdle()
    composeRule.checkSettingsScreenIsDisplayed()
  }

  @Test
  fun navigation_MapScreen_FromOtherProfile_AndGoBack() {
    val userId2 = "userId"
    val postId2 = "postId2"
    val animal2 = "animal2"
    runBlocking {
      val user =
          User(
              userId = userId2,
              name = "name2",
              username = "username2",
              surname = "surname2",
              bio = "bio2",
              profilePictureURL = "",
              userType = UserType.REGULAR,
              creationDate = Timestamp.now(),
              country = "country2",
              friendsCount = 2,
          )
      RepositoryProvider.userRepository.addUser(user)
      RepositoryProvider.userAnimalsRepository.initializeUserAnimals(userId2)
      RepositoryProvider.userAchievementsRepository.initializeUserAchievements(userId2)
      val post = post0.copy(authorId = userId2, postId = postId2, animalId = animal2)
      RepositoryProvider.postRepository.addPost(post)
      val animal = animal0.copy(animalId = animal2)
      RepositoryProvider.animalRepository.addAnimal(animal)
    }
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToPostDetailsScreenFromHome(postId2)
    composeRule.waitForIdle()
    composeRule.checkPostDetailsScreenIsDisplayed(postId2)
    composeRule.navigateToProfileFromPostDetails(userId2)
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId2)
    composeRule.navigateToMapFromProfile()
    composeRule.waitForIdle()
    composeRule.checkMapScreenIsDisplayed(userId2)
    composeRule.navigateBackFromMap()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId2)
  }
}
