package com.android.wildex.ui.navigation

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.utils.FirebaseEmulator
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NavigationTest : NavigationTestUtils() {

  @Test
  fun startsAtHomeScreen_whenAuthenticated() {
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
  }

  /*@Test
  fun navigation_HomeScreen_FromAuth_OldUser() {
    runBlocking {
      FirebaseEmulator.auth.signOut()
      delay(1000)
    }
    composeRule.waitForIdle()
    composeRule.checkAuthScreenIsDisplayed()
    composeRule.navigateToHomeScreenFromAuth()
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
  }*/

  @Test
  fun navigation_HomeScreen_FromAuth_NewUser() {
    runBlocking {
      FirebaseEmulator.auth.signOut()
      RepositoryProvider.userRepository.deleteUser(userId)
      RepositoryProvider.userAnimalsRepository.deleteUserAnimals(userId)
      RepositoryProvider.userAchievementsRepository.deleteUserAchievements(userId)
      RepositoryProvider.userSettingsRepository.deleteUserSettings(userId)
      RepositoryProvider.userFriendsRepository.deleteUserFriendsOfUser(userId)
      RepositoryProvider.userTokensRepository.deleteUserTokens(userId)
      delay(1000)
    }
    composeRule.waitForIdle()
    composeRule.checkAuthScreenIsDisplayed()
    composeRule.navigateToHomeScreenFromAuth()
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
  fun navigation_MapScreen_FromBottomBar() {
    composeRule.waitForIdle()
    composeRule.navigateToMapScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkMapScreenIsDisplayed(userId)
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
  fun navigation_CollectionScreen_FromBottomBar() {
    composeRule.waitForIdle()
    composeRule.navigateToCollectionScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkCollectionScreenIsDisplayed(userId, true)
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
  fun navigation_ProfileScreen_FromCollection_CurrentUser_AndGoBack() {
    composeRule.waitForIdle()
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
  fun navigation_Profile_FromReport_CurrentUser_AndGoBack() {
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToReportScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkReportScreenIsDisplayed()
    composeRule.navigateToMyProfileScreenFromReport()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
    composeRule.navigateBackFromProfile()
    composeRule.waitForIdle()
    composeRule.checkReportScreenIsDisplayed()
  }

  @Test
  fun navigation_CollectionScreenFromMyProfile() {
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
  fun navigation_AuthScreen_FromSettings_LogOut() {
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToMyProfileScreenFromHome()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
    composeRule.navigateToSettingsScreenFromProfile()
    composeRule.waitForIdle()
    composeRule.checkSettingsScreenIsDisplayed()
    composeRule.navigateFromSettingsScreenToAuthScreen_LogOut()
    composeRule.waitForIdle()
    composeRule.checkAuthScreenIsDisplayed()
  }

  @Test
  fun navigation_AuthScreen_FromSettings_DeleteUser() {
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToMyProfileScreenFromHome()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
    composeRule.navigateToSettingsScreenFromProfile()
    composeRule.waitForIdle()
    composeRule.checkSettingsScreenIsDisplayed()
    composeRule.navigateFromSettingsScreenToAuthScreen_DeleteAccount()
    composeRule.waitForIdle()
    composeRule.checkAuthScreenIsDisplayed()
  }

  @Test
  fun navigation_AchievementsScreen_FromMyProfile_AndGoBack() {
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
  fun navigation_FriendScreen_FromMyProfile_AndGoBack() {
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToMyProfileScreenFromHome()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
    composeRule.navigateToFriendScreenFromProfile()
    composeRule.waitForIdle()
    composeRule.checkFriendScreenIsDisplayed()
    composeRule.navigateBackFromFriend()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
  }

  @Test
  fun navigation_MapScreen_FromMyProfile() {
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToMyProfileScreenFromHome()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
    composeRule.navigateToMapScreenFromProfile()
    composeRule.waitForIdle()
    composeRule.checkMapScreenIsDisplayed(userId, true)
  }

  @Test
  fun navigate_EditProfileScreen_FromSettings_AndGoBack() {
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
    composeRule.checkEditProfileScreenIsDisplayed()
    composeRule.navigateBackFromEditProfile()
    composeRule.waitForIdle()
    composeRule.checkSettingsScreenIsDisplayed()
  }

  /*@Test
  fun navigation_PostDetails_FromHome_AndGoBack() {
    val postId = "post_for_profile_nav"
    runBlocking {
      val post = post0.copy(authorId = userId, postId = postId)
      RepositoryProvider.postRepository.addPost(post)
      val animal = animal0.copy(animalId = post.animalId)
      RepositoryProvider.animalRepository.addAnimal(animal)
      delay(1000)
    }
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToPostDetailsScreenFromHome(postId)
    composeRule.waitForIdle()
    composeRule.checkPostDetailsScreenIsDisplayed(postId)
    composeRule.navigateToProfileScreenFromPostDetails(userId)
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
  fun navigation_CollectionScreen_FromOtherProfile_AndGoBack() {
    val userId2 = "userId"
    val postId2 = "postId2"
    val animal2 = "animal2"
    runBlocking {
      val user = user0.copy(userId = userId2, username = "username2")
      RepositoryProvider.userRepository.addUser(user)
      RepositoryProvider.userAnimalsRepository.initializeUserAnimals(userId2)
      RepositoryProvider.userAchievementsRepository.initializeUserAchievements(userId2)
      RepositoryProvider.userFriendsRepository.initializeUserFriends(userId2)
      RepositoryProvider.userTokensRepository.initializeUserTokens(userId2)
      RepositoryProvider.userSettingsRepository.initializeUserSettings(userId2)
      val post = post0.copy(authorId = userId2, postId = postId2, animalId = animal2)
      RepositoryProvider.postRepository.addPost(post)
      val animal = animal0.copy(animalId = animal2)
      RepositoryProvider.animalRepository.addAnimal(animal)
      delay(1000)
    }
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToProfileScreenFromHome(postId2)
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
  fun navigation_MapScreen_FromOtherProfile_AndGoBack() {
    val userId2 = "userId"
    val postId2 = "postId2"
    val animal2 = "animal2"
    runBlocking {
      val user = user0.copy(userId = userId2, username = "username2")
      RepositoryProvider.userRepository.addUser(user)
      RepositoryProvider.userAnimalsRepository.initializeUserAnimals(userId2)
      RepositoryProvider.userAchievementsRepository.initializeUserAchievements(userId2)
      RepositoryProvider.userFriendsRepository.initializeUserFriends(userId2)
      RepositoryProvider.userTokensRepository.initializeUserTokens(userId2)
      RepositoryProvider.userSettingsRepository.initializeUserSettings(userId2)
      val post = post0.copy(authorId = userId2, postId = postId2, animalId = animal2)
      RepositoryProvider.postRepository.addPost(post)
      val animal = animal0.copy(animalId = animal2)
      RepositoryProvider.animalRepository.addAnimal(animal)
      delay(1000)
    }
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToProfileScreenFromHome(postId2)
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId2)
    composeRule.navigateToMapScreenFromProfile()
    composeRule.waitForIdle()
    composeRule.checkMapScreenIsDisplayed(userId2, false)
    composeRule.navigateBackFromMap()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId2)
  }*/

  @Test
  fun navigation_AnimalDetailScreen_AndGoBack() {
    val animalId = "animal_id"
    runBlocking {
      val animal = animal0.copy(animalId = animalId)
      RepositoryProvider.animalRepository.addAnimal(animal)
      RepositoryProvider.userAnimalsRepository.addAnimalToUserAnimals(userId, animalId)
      delay(1000)
    }
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
  fun navigation_SubmitReport_AndGoBack() {
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToReportScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkReportScreenIsDisplayed()
    composeRule.navigateToSubmitReportScreenFromReport()
    composeRule.waitForIdle()
    composeRule.checkSubmitReportScreenIsDisplayed()
    composeRule.navigateBackFromSubmitReport()
    composeRule.waitForIdle()
    composeRule.checkReportScreenIsDisplayed()
  }

  @Test
  fun navigation_ReportDetails_FromReport_AndGoBack() {
    val reportId = "report_id_for_nav"
    runBlocking {
      val report = report0.copy(reportId = reportId, authorId = userId)
      RepositoryProvider.reportRepository.addReport(report)
      delay(1000)
    }
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToReportScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkReportScreenIsDisplayed()
    composeRule.navigateToReportDetailsScreenFromReport(reportId)
    composeRule.waitForIdle()
    composeRule.checkReportDetailScreenIsDisplayed(reportId)
    composeRule.navigateToProfileScreenFromReportDetails()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
    composeRule.navigateBackFromProfile()
    composeRule.waitForIdle()
    composeRule.checkReportDetailScreenIsDisplayed(reportId)
    composeRule.navigateBackFromReportDetails()
    composeRule.waitForIdle()
    composeRule.checkReportScreenIsDisplayed()
  }

  @Test
  fun navigation_Profile_FromReport_AndGoBack() {
    val userId2 = "userId2"
    val reportId2 = "reportId"
    runBlocking {
      val user = user0.copy(userId = userId2, username = "username2")
      RepositoryProvider.userRepository.addUser(user)
      RepositoryProvider.userAnimalsRepository.initializeUserAnimals(userId2)
      RepositoryProvider.userAchievementsRepository.initializeUserAchievements(userId2)
      RepositoryProvider.userFriendsRepository.initializeUserFriends(userId2)
      RepositoryProvider.userTokensRepository.initializeUserTokens(userId2)
      RepositoryProvider.userSettingsRepository.initializeUserSettings(userId2)
      val report = report0.copy(authorId = userId2, reportId = reportId2)
      RepositoryProvider.reportRepository.addReport(report)
      delay(1000)
    }
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToReportScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkReportScreenIsDisplayed()
    composeRule.navigateToProfileScreenFromReport(userId2)
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId2)
    composeRule.navigateBackFromProfile()
    composeRule.waitForIdle()
    composeRule.checkReportScreenIsDisplayed()
  }

  @Test
  fun navigation_NotificationScreen_FromHome_AndGoBack() {
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToNotificationScreenFromHome()
    composeRule.waitForIdle()
    composeRule.checkNotificationScreenIsDisplayed()
    composeRule.navigateBackFromNotification()
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
  }

  @Test
  fun navigation_NotificationScreen_FromCollection_AndGoBack() {
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToCollectionScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkCollectionScreenIsDisplayed(userId)
    composeRule.navigateToNotificationScreenFromCollection()
    composeRule.waitForIdle()
    composeRule.checkNotificationScreenIsDisplayed()
    composeRule.navigateBackFromNotification()
    composeRule.waitForIdle()
    composeRule.checkCollectionScreenIsDisplayed(userId)
  }

  @Test
  fun navigation_NotificationScreen_FromReport_AndGoBack() {
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToReportScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkReportScreenIsDisplayed()
    composeRule.navigateToNotificationScreenFromReport()
    composeRule.waitForIdle()
    composeRule.checkNotificationScreenIsDisplayed()
    composeRule.navigateBackFromNotification()
    composeRule.waitForIdle()
    composeRule.checkReportScreenIsDisplayed()
  }

  @Test
  fun navigation_ProfileScreen_FromFriend_AndGoBack() {
    val userId2 = "userId2"
    runBlocking {
      val user = user0.copy(userId = userId2, username = "username2")
      RepositoryProvider.userRepository.addUser(user)
      RepositoryProvider.userAnimalsRepository.initializeUserAnimals(userId2)
      RepositoryProvider.userAchievementsRepository.initializeUserAchievements(userId2)
      RepositoryProvider.userFriendsRepository.initializeUserFriends(userId2)
      RepositoryProvider.userTokensRepository.initializeUserTokens(userId2)
      RepositoryProvider.userSettingsRepository.initializeUserSettings(userId2)
      RepositoryProvider.userFriendsRepository.addFriendToUserFriendsOfUser(userId, userId2)
      RepositoryProvider.userFriendsRepository.addFriendToUserFriendsOfUser(userId2, userId)
      delay(1000)
    }
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToMyProfileScreenFromHome()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
    composeRule.navigateToFriendScreenFromProfile()
    composeRule.waitForIdle()
    composeRule.checkFriendScreenIsDisplayed()
    composeRule.navigateToProfileScreenFromFriend(userId2)
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId2)
  }
}
