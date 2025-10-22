package com.android.wildex.ui.home

import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Location
import com.android.wildex.utils.LocalRepositories
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val postRepository = LocalRepositories.postsRepository
  private val userRepository = LocalRepositories.userRepository
  private val likeRepository = LocalRepositories.likeRepository
  private val fullPost =
      Post(
          postId = "uid",
          authorId = "poster0",
          pictureURL =
              "https://img.freepik.com/premium-photo/fun-unique-cartoon-profile-picture-that-represents-your-style-personality_1283595-14213.jpg",
          location = Location(0.0, 0.0, "Casablanca"),
          description = "Description 1",
          date = Timestamp.now(),
          animalId = "animal1",
          likesCount = 10,
          commentsCount = 5,
      )

  private lateinit var homeScreenVM: HomeScreenViewModel

  @Before
  fun setup() = runBlocking {
    homeScreenVM =
        HomeScreenViewModel(postRepository, userRepository, likeRepository, "currentUserId-1")
    userRepository.addUser(
        User(
            userId = "currentUserId-1",
            username = "testuser",
            name = "Test",
            surname = "User",
            bio = "This is a test user.",
            profilePictureURL =
                "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "Testland",
            friendsCount = 0,
        ))
    userRepository.addUser(
        User(
            userId = "poster0",
            username = "testuser",
            name = "Test",
            surname = "User",
            bio = "This is a test user.",
            profilePictureURL =
                "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "Testland",
            friendsCount = 0,
        ))
  }

  @After
  fun tearDown() {
    LocalRepositories.clearAll()
  }

  @Test
  fun testTagsAreCorrectlySetWhenNoPost() {
    composeTestRule.setContent { HomeScreen(homeScreenVM) }

    composeTestRule.onNodeWithTag(HomeScreenTestTags.NOTIFICATION_BELL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.PROFILE_PICTURE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.TITLE)
        .assertIsDisplayed()
        .onChildren()
        .assertAny(hasText("Wildex"))
    composeTestRule.onNodeWithTag(HomeScreenTestTags.NO_POST_ICON).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.authorPictureTag("currentUserId-1"))
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.likeTag("currentUserId-1"))
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.commentTag("currentUserId-1"))
        .assertIsNotDisplayed()
  }

  @Test
  fun testTagsAreCorrectlySetWhenPosts() {
    val fullPost2 = fullPost.copy(postId = "post2")
    runBlocking {
      postRepository.addPost(fullPost)
      postRepository.addPost(fullPost2)
      homeScreenVM.refreshUIState()
    }

    composeTestRule.setContent { HomeScreen(homeScreenVM) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HomeScreenTestTags.NOTIFICATION_BELL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.PROFILE_PICTURE).assertIsDisplayed()

    // Ensure each post is brought into view before asserting
    scrollToPost(fullPost.postId)
    assertFullPostIsDisplayed(fullPost.postId)

    scrollToPost(fullPost2.postId)
    assertFullPostIsDisplayed(fullPost2.postId)
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.NO_POST_ICON, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun postWithoutLocation_doesNotShowLocation() {
    val noLocationPost = fullPost.copy(location = null)
    val blankLocationPost = fullPost.copy(location = Location(0.0, 0.0, "   "))
    runBlocking {
      postRepository.addPost(noLocationPost)
      postRepository.addPost(blankLocationPost)
    }

    composeTestRule.setContent { HomeScreen(homeScreenVM) }

    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.locationTag(noLocationPost.postId),
            useUnmergedTree = true,
        )
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.locationTag(blankLocationPost.postId),
            useUnmergedTree = true,
        )
        .assertIsNotDisplayed()
  }

  @Test
  fun profilePictureClick_invokesCallback() {
    var profileClicked = false

    composeTestRule.setContent {
      HomeScreen(homeScreenVM, onProfilePictureClick = { profileClicked = true })
    }

    composeTestRule.onNodeWithTag(HomeScreenTestTags.PROFILE_PICTURE).performClick()
    assert(profileClicked)
  }

  @Test
  fun notificationBellClick_invokesCallback() {
    var notificationClicked = false
    composeTestRule.setContent {
      HomeScreen(homeScreenVM, onNotificationClick = { notificationClicked = true })
    }
    composeTestRule.onNodeWithTag(HomeScreenTestTags.NOTIFICATION_BELL).performClick()
    assert(notificationClicked)
  }

  @Test
  fun likeButtonAndLikeCountClick_togglesLikeToRepository() {
    runBlocking {
      postRepository.addPost(fullPost)
      homeScreenVM.refreshUIState()
    }
    composeTestRule.setContent { HomeScreen(homeScreenVM) }
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.likeButtonTag(fullPost.postId), useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()
    runBlocking {
      val like = likeRepository.getLikeForPost(fullPost.postId)
      assert(like != null)
    }
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.likeButtonTag(fullPost.postId), useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()
    runBlocking {
      val like = likeRepository.getLikeForPost(fullPost.postId)
      assert(like == null)
    }

    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.likeTag(fullPost.postId), useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()
    runBlocking {
      val like = likeRepository.getLikeForPost(fullPost.postId)
      assert(like != null)
    }
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.likeTag(fullPost.postId), useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()
    runBlocking {
      val like = likeRepository.getLikeForPost(fullPost.postId)
      assert(like == null)
    }
  }

  @Test
  fun postImageAndCommentCountClick_invokesCallback() {
    var postClicked = false
    runBlocking {
      postRepository.addPost(fullPost)
      homeScreenVM.refreshUIState()
    }
    composeTestRule.setContent {
      HomeScreen(
          homeScreenVM,
          onPostClick = { postId ->
            if (postId == fullPost.postId) {
              postClicked = true
            }
          },
      )
    }

    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.imageTag(fullPost.postId), useUnmergedTree = true)
        .performClick()

    assert(postClicked)

    postClicked = false
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.commentTag(fullPost.postId), useUnmergedTree = true)
        .performClick()

    assert(postClicked)
  }

  @Test
  fun likeAndCommentCountsAreDisplayedCorrectly() {
    val postWithCounts = fullPost.copy(postId = "counts", likesCount = 42, commentsCount = 7)
    val postWithCount = fullPost.copy(postId = "count", likesCount = 1, commentsCount = 1)
    runBlocking {
      postRepository.addPost(postWithCounts)
      postRepository.addPost(postWithCount)
      homeScreenVM.refreshUIState()
    }
    composeTestRule.setContent { HomeScreen(homeScreenVM) }

    // Scroll to first post and assert
    scrollToPost(postWithCounts.postId)
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.likeTag(postWithCounts.postId), useUnmergedTree = true)
        .assertIsDisplayed()
        .onChildren()
        .assertAny(hasText("42 likes"))
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.commentTag(postWithCounts.postId), useUnmergedTree = true)
        .assertIsDisplayed()
        .onChildren()
        .assertAny(hasText("7 comments"))

    // Scroll to second post and assert
    scrollToPost(postWithCount.postId)
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.likeTag(postWithCount.postId), useUnmergedTree = true)
        .assertIsDisplayed()
        .onChildren()
        .assertAny(hasText("1 like"))
    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.commentTag(postWithCount.postId),
            useUnmergedTree = true,
        )
        .assertIsDisplayed()
        .onChildren()
        .assertAny(hasText("1 comment"))
  }

  private fun scrollToPost(postId: String) {
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.POSTS_LIST, useUnmergedTree = true)
        .performScrollToNode(hasTestTag(HomeScreenTestTags.imageTag(postId)))
    composeTestRule.waitForIdle()
  }

  private fun assertFullPostIsDisplayed(postId: String) {
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.locationTag(postId), useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.authorPictureTag(postId), useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.likeTag(postId), useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.commentTag(postId), useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.imageTag(postId), useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.likeButtonTag(postId), useUnmergedTree = true)
        .assertIsDisplayed()
  }
}
