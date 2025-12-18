package com.android.wildex.ui.end2end

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.achievement.Achievements
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.OnBoardingStage
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.LoadingScreenTestTags
import com.android.wildex.ui.achievement.AchievementsScreenTestTags
import com.android.wildex.ui.home.HomeScreenTestTags
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.navigation.NavigationTestUtils
import com.android.wildex.ui.profile.ProfileScreenTestTags
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

class End2EndTest3 : NavigationTestUtils() {
  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun userFlow3() = runTest {
    val user0 =
        User(
            userId = "author0",
            username = "name0",
            name = "surname0",
            surname = "username0",
            bio = "regular user 0",
            profilePictureURL = "",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "country 0",
            onBoardingStage = OnBoardingStage.COMPLETE,
        )
    val user1 =
        User(
            userId = "author1",
            username = "name1",
            name = "surname1",
            surname = "username1",
            bio = "regular user 1",
            profilePictureURL = "",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "country 1",
            onBoardingStage = OnBoardingStage.COMPLETE,
        )
    val post0 =
        Post(
            postId = "post0",
            authorId = "author0",
            pictureURL = "",
            location = Location(10.0, 10.0, "location0", "address0", "country0"),
            description = "description0",
            date = Timestamp.now(),
            animalId = "animal0",
        )
    val post1 =
        Post(
            postId = "post1",
            authorId = "author1",
            pictureURL = "",
            location = Location(10.0, 10.0, "location1", "address1", "country1"),
            description = "description1",
            date = Timestamp.now(),
            animalId = "animal1",
        )
    val animal0 =
        Animal(
            animalId = "animal0",
            pictureURL = "",
            name = "Animal0",
            description = "Description0",
            species = "species0",
        )
    val animal1 =
        Animal(
            animalId = "animal1",
            pictureURL = "",
            name = "Animal1",
            description = "Description1",
            species = "species1",
        )
    runBlocking {
      initUser(user0)
      initUser(user1)
      RepositoryProvider.animalRepository.addAnimal(animal0)
      RepositoryProvider.animalRepository.addAnimal(animal1)
      RepositoryProvider.postRepository.addPost(post0)
      RepositoryProvider.postRepository.addPost(post1)
    }
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToMyProfileScreenFromHome()
    composeRule.waitForIdle()
    composeRule.checkMyProfileScreenIsDisplayed()
    composeRule.navigateToAchievementsScreenFromProfile()
    composeRule.waitForIdle()
    composeRule.checkAchievementsScreenIsDisplayed(userId)
    composeRule.checkFullAchievementScreenIsDisplayed(true)
    composeRule.navigateBackFromAchievements()
    composeRule.waitForIdle()
    composeRule.checkMyProfileScreenIsDisplayed()
    composeRule.navigateBackFromProfile()
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.checkFullHomeScreenIsDisplayedFor(listOf(post0.postId, post1.postId))
    composeRule.performClickOnTag(HomeScreenTestTags.likeButtonTag(post0.postId))
    composeRule.performClickOnTag(HomeScreenTestTags.likeButtonTag(post1.postId))
    composeRule.navigateToMyProfileScreenFromHome()
    composeRule.waitForIdle()
    composeRule.checkMyProfileScreenIsDisplayed()
    advanceUntilIdle()
    composeRule.navigateToAchievementsScreenFromProfile()
    composeRule.waitForIdle()
    composeRule.checkAchievementsScreenIsDisplayed(userId)
    composeRule.checkFullAchievementScreenIsDisplayed(false)
  }

  private suspend fun initUser(user: User) {
    RepositoryProvider.userRepository.addUser(user)
    RepositoryProvider.userSettingsRepository.initializeUserSettings(user.userId)
    RepositoryProvider.userAchievementsRepository.initializeUserAchievements(user.userId)
    RepositoryProvider.userAnimalsRepository.initializeUserAnimals(user.userId)
  }

  private fun ComposeTestRule.checkMyProfileScreenIsDisplayed() {
    checkProfileScreenIsDisplayed(userId)

    waitUntil { onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN).isNotDisplayed() }
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.PROFILE_NAME)
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.PROFILE_USERNAME)
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.PROFILE_DESCRIPTION)
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.MAP)
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.MAP_CTA)
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.COLLECTION)
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.FRIENDS)
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.ACHIEVEMENTS)
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.ACHIEVEMENTS_CTA)
    onNodeWithTag(ProfileScreenTestTags.SETTINGS).assertIsDisplayed()
  }

  private fun ComposeTestRule.checkFullHomeScreenIsDisplayedFor(postIds: List<Id>) {
    checkHomeScreenIsDisplayed()
    onNodeWithTag(NavigationTestTags.NOTIFICATION_BELL).assertIsDisplayed()
    onNodeWithTag(HomeScreenTestTags.POSTS_LIST).assertIsDisplayed()
    onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    onNodeWithTag(NavigationTestTags.TOP_BAR_PROFILE_PICTURE).assertIsDisplayed()
    postIds.forEach {
      checkNodeWithTagScrollAndDisplay(HomeScreenTestTags.imageTag(it))
      checkNodeWithTagScrollAndDisplay(HomeScreenTestTags.authorPictureTag(it))
      checkNodeWithTagScrollAndDisplay(HomeScreenTestTags.commentTag(it))
      checkNodeWithTagScrollAndDisplay(HomeScreenTestTags.likeButtonTag(it))
      checkNodeWithTagScrollAndDisplay(HomeScreenTestTags.sliderTag(it))
      checkNodeWithTagScrollAndDisplay(HomeScreenTestTags.locationTag(it))
      checkNodeWithTagScrollAndDisplay(HomeScreenTestTags.sliderStateTag(it))
      onNodeWithTag(HomeScreenTestTags.sliderTag(it)).performScrollToIndex(1)
      checkNodeWithTagScrollAndDisplay(HomeScreenTestTags.mapPreviewTag(it))
      checkNodeWithTagScrollAndDisplay(HomeScreenTestTags.mapPreviewButtonTag(it))
      checkNodeWithTagScrollAndDisplay(HomeScreenTestTags.mapLocationTag(it))
      onNodeWithTag(HomeScreenTestTags.sliderTag(it)).performScrollToIndex(0)
    }
  }

  private fun ComposeTestRule.checkNodeWithTagScrollAndDisplay(tag: String) {
    onNodeWithTag(tag, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
  }

  private fun ComposeTestRule.checkFullAchievementScreenIsDisplayed(initial: Boolean) {
    checkNodeWithTagGetsDisplayed(AchievementsScreenTestTags.TITLE)
    checkNodeWithTagGetsDisplayed(AchievementsScreenTestTags.BACK_BUTTON)
    checkNodeWithTagGetsDisplayed(AchievementsScreenTestTags.ACHIEVEMENT_GRID)
    checkNodeWithTagGetsDisplayed(AchievementsScreenTestTags.ACHIEVEMENTS_PROGRESS_CARD)
    if (initial) checkNodeWithTagGetsDisplayed(AchievementsScreenTestTags.ACHIEVEMENTS_PLACEHOLDER)
    checkNodeWithTagGetsDisplayed(AchievementsScreenTestTags.TOP_APP_BAR)
    checkNodeWithTagGetsDisplayed(
        AchievementsScreenTestTags.getTagForAchievement(
            Achievements.firstLike.achievementId,
            !initial,
        ))
    performClickOnTag(
        AchievementsScreenTestTags.getTagForAchievement(
            Achievements.firstLike.achievementId,
            !initial,
        ))
    checkNodeWithTagGetsDisplayed(AchievementsScreenTestTags.DETAILS_DIALOG)
    checkNodeWithTagGetsDisplayed(AchievementsScreenTestTags.DETAILS_NAME)
    checkNodeWithTagGetsDisplayed(AchievementsScreenTestTags.DETAILS_PROGRESS)
    checkNodeWithTagGetsDisplayed(AchievementsScreenTestTags.DETAILS_DESCRIPTION)
    checkNodeWithTagGetsDisplayed(AchievementsScreenTestTags.DETAILS_IMAGE)
    checkNodeWithTagGetsDisplayed(AchievementsScreenTestTags.DETAILS_STATUS)
    performClickOnTag(AchievementsScreenTestTags.DETAILS_CLOSE_BUTTON)
  }
}
