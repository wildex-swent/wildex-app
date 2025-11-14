package com.android.wildex.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.LoadingScreenTestTags
import com.android.wildex.ui.home.HomeScreenTestTags
import com.android.wildex.ui.post.PostDetailsScreenTestTags
import com.android.wildex.ui.profile.EditProfileScreenTestTags
import com.android.wildex.ui.profile.ProfileScreenTestTags
import com.android.wildex.utils.FirebaseEmulator
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import org.junit.Test

class End2EndTest1 : NavigationTestUtils() {

  @Test
  fun userFlow() {
    /* User authenticates, creates his profile, checks his profile, goes to post1 and likes it, goes to post2 and comments on it*/
    val user0 =
        User(
            "author0",
            "name0",
            "surname0",
            "username0",
            "regular user 0",
            "",
            UserType.REGULAR,
            Timestamp.now(),
            "country 0",
            0,
        )
    val user1 =
        User(
            "author1",
            "name1",
            "surname1",
            "username1",
            "regular user 1",
            "",
            UserType.REGULAR,
            Timestamp.now(),
            "country 1",
            0,
        )
    val post0 =
        Post(
            postId = "post0",
            authorId = "author0",
            pictureURL = "",
            location = Location(10.0, 10.0, "location0"),
            description = "description0",
            date = Timestamp.now(),
            likesCount = 0,
            commentsCount = 0,
            animalId = "animal0",
        )
    val post1 =
        Post(
            postId = "post1",
            authorId = "author1",
            pictureURL = "",
            location = Location(10.0, 10.0, "location1"),
            description = "description1",
            date = Timestamp.now(),
            likesCount = 0,
            commentsCount = 0,
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
      FirebaseEmulator.auth.signOut()
      initUser(user0)
      initUser(user1)
      RepositoryProvider.animalRepository.addAnimal(animal0)
      RepositoryProvider.animalRepository.addAnimal(animal1)
      RepositoryProvider.postRepository.addPost(post0)
      RepositoryProvider.postRepository.addPost(post1)
    }
    composeRule.waitForIdle()
    composeRule.checkAuthScreenIsDisplayed()
    composeRule.navigateFromAuth()
    composeRule.waitForIdle()
    composeRule.checkEditProfileScreenIsDisplayed(isNewUser = true)
    val nameNode = composeRule.onNodeWithTag(EditProfileScreenTestTags.INPUT_NAME)
    val surnameNode = composeRule.onNodeWithTag(EditProfileScreenTestTags.INPUT_SURNAME)
    val usernameNode = composeRule.onNodeWithTag(EditProfileScreenTestTags.INPUT_USERNAME)
    val saveNode = composeRule.onNodeWithTag(EditProfileScreenTestTags.SAVE)
    composeRule.waitUntil(DEFAULT_TIMEOUT) {
      nameNode.isDisplayed() ||
          surnameNode.isDisplayed() ||
          usernameNode.isDisplayed() ||
          saveNode.isDisplayed()
    }
    nameNode.performScrollTo().performClick().performTextInput("Name1")
    surnameNode.performScrollTo().performClick().performTextInput("Surname1")
    usernameNode.performScrollTo().performClick().performTextInput("Username1")
    saveNode.performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.waitUntilAfterLoadingScreen()
    composeRule.navigateToMyProfileScreenFromHome()
    composeRule.waitUntilAfterLoadingScreen()
    composeRule.waitForIdle()
    composeRule.checkMyProfileScreenIsDisplayed()
    composeRule.navigateBackFromProfile()
    composeRule.waitUntilAfterLoadingScreen()
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.checkFullHomeScreenIsDisplayedFor(listOf(post0.postId, post1.postId))
    composeRule.navigateToPostDetailsScreenFromHome(post0.postId)
    composeRule.waitUntilAfterLoadingScreen()
    composeRule.waitForIdle()
    composeRule.checkFullPostDetailsIsDisplayedFor(post0, animal0)
    composeRule.likePostAndCheckLikes(post0.likesCount)
    composeRule.waitForIdle()
    composeRule.navigateBackFromPostDetails()
    composeRule.waitForIdle()
    composeRule.waitUntilAfterLoadingScreen()
    composeRule.navigateToPostDetailsScreenFromHome(post1.postId)
    composeRule.waitUntilAfterLoadingScreen()
    composeRule.waitForIdle()
    composeRule.checkFullPostDetailsIsDisplayedFor(post1, animal1)
    composeRule.commentPostAndCheckComments(post1.commentsCount)
    composeRule.waitForIdle()
    composeRule.navigateBackFromPostDetails()
  }

  private suspend fun initUser(user: User) {
    RepositoryProvider.userRepository.addUser(user)
    RepositoryProvider.userSettingsRepository.initializeUserSettings(user.userId)
    RepositoryProvider.userAchievementsRepository.initializeUserAchievements(user.userId)
    RepositoryProvider.userAnimalsRepository.initializeUserAnimals(user.userId)
  }

  private fun ComposeTestRule.likePostAndCheckLikes(likesCount: Int) {
    onNodeWithText(likesCount.toString()).performScrollTo().assertIsDisplayed()
    onNode(hasContentDescription("Like status")).performScrollTo().performClick()
    onNodeWithText((likesCount + 1).toString()).performScrollTo().assertIsDisplayed()
  }

  private fun ComposeTestRule.commentPostAndCheckComments(commentsCount: Int) {
    if (commentsCount == 1) {
      onNodeWithText("1 Comment").performScrollTo().assertIsDisplayed()
    } else {
      onNodeWithText("$commentsCount Comments").performScrollTo().assertIsDisplayed()
    }
    onNode(hasText("Add a comment …") and hasSetTextAction()).performTextInput("New comment")
    onNode(hasContentDescription("Send comment")).performClick()
    onNodeWithText("New comment").performScrollTo().assertIsDisplayed()
    if (commentsCount + 1 == 1) {
      onNodeWithText("1 Comment").performScrollTo().assertIsDisplayed()
    } else {
      onNodeWithText("${commentsCount + 1} Comments").performScrollTo().assertIsDisplayed()
    }
    onNode(hasText("Add a comment …") and hasSetTextAction()).performImeAction()
  }

  private fun ComposeTestRule.checkMyProfileScreenIsDisplayed() {
    checkProfileScreenIsDisplayed(FirebaseEmulator.auth.uid!!)

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
    onNodeWithTag(HomeScreenTestTags.NOTIFICATION_BELL).assertIsDisplayed()
    onNodeWithTag(HomeScreenTestTags.POSTS_LIST).assertIsDisplayed()
    onNodeWithTag(HomeScreenTestTags.TITLE).assertIsDisplayed()
    postIds.forEach {
      checkNodeWithTagScrollAndDisplay(HomeScreenTestTags.imageTag(it))
      checkNodeWithTagScrollAndDisplay(HomeScreenTestTags.authorPictureTag(it))
      checkNodeWithTagScrollAndDisplay(HomeScreenTestTags.commentTag(it))
      checkNodeWithTagScrollAndDisplay(HomeScreenTestTags.likeButtonTag(it))
      checkNodeWithTagScrollAndDisplay(HomeScreenTestTags.likeTag(it))
      checkNodeWithTagScrollAndDisplay(HomeScreenTestTags.locationTag(it))
    }
  }

  private fun ComposeTestRule.checkFullPostDetailsIsDisplayedFor(post: Post, animal: Animal) {
    onNodeWithText(post.location!!.name, useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
    onNodeWithText(animal.species, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    onNodeWithText("${post.likesCount}", useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
    onNodeWithText(post.description, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    onNodeWithTag(PostDetailsScreenTestTags.testTagForProfilePicture(post.authorId, "author"), true)
        .performScrollTo()
        .assertIsDisplayed()
  }

  private fun ComposeTestRule.checkNodeWithTagScrollAndDisplay(tag: String) {
    onNodeWithTag(tag, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
  }

  private fun ComposeTestRule.waitUntilAfterLoadingScreen() {
    waitUntil { onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN).isNotDisplayed() }
  }
}
