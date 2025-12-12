package com.android.wildex.ui.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.BuildConfig
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.social.Comment
import com.android.wildex.model.social.CommentTag
import com.android.wildex.model.social.Like
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.LoadingScreenTestTags
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.utils.LocalRepositories
import com.android.wildex.utils.offline.FakeConnectivityObserver
import com.google.firebase.Timestamp
import com.mapbox.common.MapboxOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
  private val commentRepository = LocalRepositories.commentRepository
  private val animalRepository = LocalRepositories.animalRepository
  private val userSettingsRepository = LocalRepositories.userSettingsRepository
  private val fakeObserver = FakeConnectivityObserver(initial = true)
  private val userFriendsRepository = LocalRepositories.userFriendsRepository

  private val fullPost =
      Post(
          postId = "uid",
          authorId = "poster0",
          pictureURL =
              "https://img.freepik.com/premium-photo/fun-unique-cartoon-profile-picture-that-represents-your-style-personality_1283595-14213.jpg",
          location = Location(0.0, 0.0, "Casablanca", "Casablanca", "Casablanca"),
          description = "Description 1",
          date = Timestamp.now(),
          animalId = "a1",
      )

  private lateinit var homeScreenVM: HomeScreenViewModel

  @Before
  fun setup() = runBlocking {
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    homeScreenVM =
        HomeScreenViewModel(
            postRepository,
            userRepository,
            likeRepository,
            commentRepository,
            animalRepository,
            userSettingsRepository,
            userFriendsRepository,
            "currentUserId-1",
        )
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
        ))
    userSettingsRepository.initializeUserSettings("currentUserId-1")
    userSettingsRepository.initializeUserSettings("poster0")
    animalRepository.addAnimal(
        Animal(
            animalId = "a1",
            name = "ant",
            description = "animal1",
            pictureURL = "",
            species = "species1",
        ),
    )

    animalRepository.addAnimal(
        Animal(
            animalId = "a2",
            name = "eagle",
            description = "animal2",
            pictureURL = "",
            species = "species2",
        ))
    animalRepository.addAnimal(
        Animal(
            animalId = "a3",
            name = "iguana",
            description = "animal3",
            pictureURL = "",
            species = "species3",
        ))
    animalRepository.addAnimal(
        Animal(
            animalId = "a4",
            name = "orca",
            description = "animal4",
            pictureURL = "",
            species = "species4",
        ))
    animalRepository.addAnimal(
        Animal(
            animalId = "a5",
            name = "unicorn",
            description = "animal5",
            pictureURL = "",
            species = "species5",
        ))
    animalRepository.addAnimal(
        Animal(
            animalId = "a6",
            name = "dolphin",
            description = "animal6",
            pictureURL = "",
            species = "species6",
        ))
  }

  @After
  fun tearDown() {
    LocalRepositories.clearAll()
  }

  @Test
  fun testTagsAreCorrectlySetWhenNoPost() {
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        HomeScreen(homeScreenVM)
      }
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.NOTIFICATION_BELL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_PROFILE_PICTURE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertIsDisplayed()
        .assertTextEquals("Wildex")
    composeTestRule.onNodeWithTag(HomeScreenTestTags.NO_POST_ICON).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.authorPictureTag("currentUserId-1"))
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.likeButtonTag("currentUserId-1"))
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.commentTag("currentUserId-1"))
        .assertIsNotDisplayed()
  }

  @Test
  fun testTagsAreCorrectlySetWhenPosts() {
    fakeObserver.setOnline(true)
    val fullPost2 = fullPost.copy(postId = "post2", animalId = "a2")
    val fullPost3 = fullPost.copy(postId = "post3", animalId = "a3")
    val fullPost4 = fullPost.copy(postId = "post4", animalId = "a4")
    val fullPost5 = fullPost.copy(postId = "post5", animalId = "a5")
    val fullPost6 = fullPost.copy(postId = "post6", animalId = "a6")

    runBlocking {
      postRepository.addPost(fullPost)
      postRepository.addPost(fullPost2)
      postRepository.addPost(fullPost3)
      postRepository.addPost(fullPost4)
      postRepository.addPost(fullPost5)
      postRepository.addPost(fullPost6)
      homeScreenVM.refreshUIState()
    }

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        HomeScreen(homeScreenVM)
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(NavigationTestTags.NOTIFICATION_BELL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_PROFILE_PICTURE).assertIsDisplayed()

    // Ensure each post is brought into view before asserting
    scrollToPost(fullPost.postId)
    assertFullPostIsDisplayed(fullPost.postId)
    composeTestRule
        .onNodeWithText("Ant", useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()

    scrollToPost(fullPost2.postId)
    assertFullPostIsDisplayed(fullPost2.postId)
    composeTestRule
        .onNodeWithText("Eagle", useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()

    scrollToPost(fullPost3.postId)
    assertFullPostIsDisplayed(fullPost3.postId)
    composeTestRule
        .onNodeWithText("Iguana", useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()

    scrollToPost(fullPost4.postId)
    assertFullPostIsDisplayed(fullPost4.postId)
    composeTestRule
        .onNodeWithText("Orca", useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()

    scrollToPost(fullPost5.postId)
    assertFullPostIsDisplayed(fullPost5.postId)
    composeTestRule
        .onNodeWithText("Unicorn", useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()

    scrollToPost(fullPost6.postId)
    assertFullPostIsDisplayed(fullPost6.postId)
    composeTestRule
        .onNodeWithText("Dolphin", useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.NO_POST_ICON, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun postWithoutLocation_doesNotShowLocation() {
    fakeObserver.setOnline(true)
    val noLocationPost = fullPost.copy(postId = "noLoc", location = null)
    val blankLocationPost = fullPost.copy(postId = "blankLoc", location = Location(0.0, 0.0, "   "))
    runBlocking {
      postRepository.addPost(noLocationPost)
      postRepository.addPost(blankLocationPost)
    }

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        HomeScreen(homeScreenVM)
      }
    }

    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.locationTag(noLocationPost.postId),
            useUnmergedTree = true,
        )
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.mapLocationTag(noLocationPost.postId),
            useUnmergedTree = true,
        )
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.sliderStateTag(noLocationPost.postId), useUnmergedTree = true)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.locationTag(blankLocationPost.postId),
            useUnmergedTree = true,
        )
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.sliderStateTag(blankLocationPost.postId), useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun currentProfilePictureClick_invokesCallback() {
    fakeObserver.setOnline(true)
    var profileClicked = false

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        HomeScreen(homeScreenVM, onCurrentProfilePictureClick = { profileClicked = true })
      }
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_PROFILE_PICTURE).performClick()
    assert(profileClicked)
  }

  @Test
  fun profilePictureClick_invokesCallback() {
    fakeObserver.setOnline(true)
    val fullPost2 = fullPost.copy(postId = "post2", authorId = "lakaka")
    runBlocking {
      postRepository.addPost(fullPost)
      userRepository.addUser(
          User(
              userId = "lakaka",
              username = "testuser",
              name = "Test",
              surname = "User",
              bio = "This is a test user.",
              profilePictureURL =
                  "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
              userType = UserType.REGULAR,
              creationDate = Timestamp.now(),
              country = "Testland",
          ))
      postRepository.addPost(fullPost2)
      homeScreenVM.refreshUIState()
    }
    var profileClicked = ""

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        HomeScreen(homeScreenVM, onProfilePictureClick = { profileClicked = it })
      }
    }

    composeTestRule.onNodeWithTag(HomeScreenTestTags.authorPictureTag("uid")).performClick()
    assert(profileClicked == "poster0")

    composeTestRule.onNodeWithTag(HomeScreenTestTags.authorPictureTag("post2")).performClick()
    assert(profileClicked == "lakaka")
  }

  @Test
  fun mapPreviewClick_invokesCallback() {
    fakeObserver.setOnline(true)
    var postClicked = false
    runBlocking {
      postRepository.addPost(fullPost)
      homeScreenVM.refreshUIState()
    }
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        HomeScreen(
            homeScreenVM,
            onPostClick = { postId ->
              if (postId == fullPost.postId) {
                postClicked = true
              }
            },
        )
      }
    }
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.sliderTag(fullPost.postId), useUnmergedTree = true)
        .performScrollToIndex(1)
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.mapPreviewTag(fullPost.postId), useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.mapPreviewButtonTag(fullPost.postId), useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()
    assert(postClicked)
  }

  @Test
  fun notificationBellClick_invokesCallback() {
    fakeObserver.setOnline(true)
    var notificationClicked = false
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        HomeScreen(homeScreenVM, onNotificationClick = { notificationClicked = true })
      }
    }
    composeTestRule.onNodeWithTag(NavigationTestTags.NOTIFICATION_BELL).performClick()
    assert(notificationClicked)
  }

  @Test
  fun mapPreviewWorksAsIntended() {
    fakeObserver.setOnline(true)
    val noLocationPost = fullPost.copy(postId = "noLocation", location = null, animalId = "a2")
    runBlocking {
      postRepository.addPost(fullPost)
      postRepository.addPost(noLocationPost)
      homeScreenVM.refreshUIState()
    }
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        HomeScreen(homeScreenVM)
      }
    }
    scrollToPost(fullPost.postId)
    assertFullPostIsDisplayed(fullPost.postId)
    composeTestRule
        .onNodeWithText("Ant", useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.sliderStateTag(fullPost.postId), useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.mapPreviewTag(fullPost.postId), useUnmergedTree = true)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.mapPreviewButtonTag(fullPost.postId), useUnmergedTree = true)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.mapLocationTag(fullPost.postId), useUnmergedTree = true)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.sliderTag(fullPost.postId), useUnmergedTree = true)
        .performScrollToIndex(1)
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.mapPreviewTag(fullPost.postId), useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.mapPreviewButtonTag(fullPost.postId), useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.mapLocationTag(fullPost.postId), useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals("Casablanca")
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.sliderTag(fullPost.postId), useUnmergedTree = true)
        .performScrollToIndex(0)
    assertFullPostIsDisplayed(fullPost.postId)
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.mapPreviewTag(fullPost.postId), useUnmergedTree = true)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.mapPreviewButtonTag(fullPost.postId), useUnmergedTree = true)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.mapLocationTag(fullPost.postId), useUnmergedTree = true)
        .assertIsNotDisplayed()

    scrollToPost(noLocationPost.postId)
    composeTestRule
        .onNodeWithText("Eagle", useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.imageTag(noLocationPost.postId), useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.sliderStateTag(noLocationPost.postId), useUnmergedTree = true)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.mapPreviewTag(noLocationPost.postId), useUnmergedTree = true)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.mapPreviewButtonTag(noLocationPost.postId), useUnmergedTree = true)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.mapLocationTag(noLocationPost.postId), useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun likeButtonAndLikeCountClick_togglesLikeToRepository() {
    fakeObserver.setOnline(true)
    runBlocking {
      postRepository.addPost(fullPost)
      homeScreenVM.refreshUIState()
    }
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        HomeScreen(homeScreenVM)
      }
    }
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
  }

  @Test
  fun postImageAndCommentCountClick_invokesCallback() {
    fakeObserver.setOnline(true)
    var postClicked = false
    runBlocking {
      postRepository.addPost(fullPost)
      homeScreenVM.refreshUIState()
    }
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        HomeScreen(
            homeScreenVM,
            onPostClick = { postId ->
              if (postId == fullPost.postId) {
                postClicked = true
              }
            },
        )
      }
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
  fun postGetsSkipped_whenAuthorLookupFails() {
    fakeObserver.setOnline(true)
    val badPost = fullPost.copy(postId = "bad", authorId = "unknown-author")
    runBlocking { postRepository.addPost(badPost) }

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        HomeScreen(homeScreenVM)
      }
    }
    composeTestRule.waitForIdle()

    assertFullPostIsNotDisplayed("bad")
  }

  @Test
  fun likeAndCommentCountsAreDisplayedCorrectly() {
    fakeObserver.setOnline(true)
    val postWithCounts = fullPost.copy(postId = "counts")
    val postWithCount = fullPost.copy(postId = "count")
    runBlocking {
      (1..42).forEach { likeRepository.addLike(Like("like${it}", "counts", "currentUserId-1")) }
      likeRepository.addLike(Like("like1", "count", "currentUserId-1"))
      (1..7).forEach {
        commentRepository.addComment(
            Comment(
                "comment$it",
                "counts",
                "currentUserId-1",
                "",
                Timestamp.now(),
                CommentTag.POST_COMMENT,
            ))
      }
      commentRepository.addComment(
          Comment(
              "comment1",
              "count",
              "currentUserId-1",
              "",
              Timestamp.now(),
              CommentTag.POST_COMMENT,
          ))
      postRepository.addPost(postWithCounts)
      postRepository.addPost(postWithCount)
      homeScreenVM.refreshUIState()
    }
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        HomeScreen(homeScreenVM)
      }
    }

    // Scroll to first post and assert
    scrollToPost(postWithCounts.postId)
    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.likeButtonTag(postWithCounts.postId),
            useUnmergedTree = true,
        )
        .assertIsDisplayed()
        .onChildren()
        .assertAny(hasText("42"))
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.commentTag(postWithCounts.postId), useUnmergedTree = true)
        .assertIsDisplayed()
        .onChildren()
        .assertAny(hasText("7"))

    // Scroll to second post and assert
    scrollToPost(postWithCount.postId)
    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.likeButtonTag(postWithCount.postId),
            useUnmergedTree = true,
        )
        .assertIsDisplayed()
        .onChildren()
        .assertAny(hasText("1"))
    composeTestRule
        .onNodeWithTag(
            HomeScreenTestTags.commentTag(postWithCount.postId),
            useUnmergedTree = true,
        )
        .assertIsDisplayed()
        .onChildren()
        .assertAny(hasText("1"))
  }

  @Test
  fun loadingScreen_showsWhileFetchingPosts() {
    fakeObserver.setOnline(true)
    val fetchSignal = CompletableDeferred<Unit>()
    val delayedPostsRepo =
        object : LocalRepositories.PostsRepositoryImpl() {
          override suspend fun getAllPosts(): List<Post> {
            fetchSignal.await()
            return super.getAllPosts()
          }
        }
    runBlocking {
      val vm =
          HomeScreenViewModel(
              delayedPostsRepo,
              LocalRepositories.userRepository,
              LocalRepositories.likeRepository,
              LocalRepositories.commentRepository,
              LocalRepositories.animalRepository,
              LocalRepositories.userSettingsRepository,
              LocalRepositories.userFriendsRepository,
              "currentUserId-1",
          )
      vm.loadUIState()
      composeTestRule.setContent {
        CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) { HomeScreen(vm) }
      }
      composeTestRule
          .onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN, useUnmergedTree = true)
          .assertIsDisplayed()
      fetchSignal.complete(Unit)
      composeTestRule.waitForIdle()
      composeTestRule
          .onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN, useUnmergedTree = true)
          .assertIsNotDisplayed()
    }
  }

  @Test
  fun refreshDisabledWhenOfflineWithPostsHomeScreen() {
    fakeObserver.setOnline(false)
    val fullPost2 = fullPost.copy(postId = "post2", animalId = "a2")
    val fullPost3 = fullPost.copy(postId = "post3", animalId = "a3")

    runBlocking {
      postRepository.addPost(fullPost)
      postRepository.addPost(fullPost2)
      postRepository.addPost(fullPost3)
      homeScreenVM.refreshUIState()
    }

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        HomeScreen(homeScreenVM)
      }
    }
    composeTestRule.onNodeWithTag(HomeScreenTestTags.PULL_TO_REFRESH).performTouchInput {
      swipeDown()
    }
    assertFalse(homeScreenVM.uiState.value.isRefreshing)
  }

  @Test
  fun refreshDisabledWhenOfflineWithoutPostsHomeScreen() {
    fakeObserver.setOnline(false)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        HomeScreen(homeScreenVM)
      }
    }
    composeTestRule.onNodeWithTag(HomeScreenTestTags.PULL_TO_REFRESH).performTouchInput {
      swipeDown()
    }
    assertFalse(homeScreenVM.uiState.value.isRefreshing)
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
        .onNodeWithTag(HomeScreenTestTags.commentTag(postId), useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.imageTag(postId), useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.sliderTag(postId), useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.likeButtonTag(postId), useUnmergedTree = true)
        .assertIsDisplayed()
  }

  private fun assertFullPostIsNotDisplayed(postId: String) {
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.locationTag(postId), useUnmergedTree = true)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.authorPictureTag(postId), useUnmergedTree = true)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.commentTag(postId), useUnmergedTree = true)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.imageTag(postId), useUnmergedTree = true)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.likeButtonTag(postId), useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun openFiltersButtonOpensFilterManager() {
    composeTestRule.setContent { OpenFiltersButton(homeScreenVM) }

    composeTestRule.onNodeWithTag(HomeScreenTestTags.OPEN_FILTERS_MANAGER).performClick()

    composeTestRule.onNodeWithTag(HomeScreenTestTags.FILTERS_MANAGER).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.FILTERS_MANAGER_FROM_AUTHOR)
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(HomeScreenTestTags.FILTERS_MANAGER_FROM_PLACE).assertIsDisplayed()

    composeTestRule.onNodeWithTag(HomeScreenTestTags.FILTERS_MANAGER_OF_ANIMAL).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.FILTERS_MANAGER_ONLY_FRIENDS_POSTS)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.FILTERS_MANAGER_ONLY_MY_POSTS)
        .assertIsDisplayed()
  }

  @Test
  fun applyButtonChangesFilters() {
    val fromAuthor: String? by mutableStateOf("John")
    val fromPlace: String? by mutableStateOf("EPFL")
    val ofAnimal: String? by mutableStateOf("Cat")
    val onlyFriendsPosts by mutableStateOf(true)
    val onlyMyPosts by mutableStateOf(true)

    var showFilters by mutableStateOf(true)

    composeTestRule.setContent {
      FiltersManager(
          postsFilters = homeScreenVM.uiState.value.postsFilters,
          onFilterChange =
              OnFilterChange(
                  onFromAuthorChange = {},
                  onFromPlaceChange = {},
                  onOfAnimalChange = {},
                  onOnlyFriendsPostsChange = {},
                  onOnlyMyPostsChange = {}),
          onDismissRequest = {},
          onApply = {
            homeScreenVM.setPostsFilter(
                fromPlace = fromPlace,
                fromAuthor = fromAuthor,
                ofAnimal = ofAnimal,
                onlyFriendsPosts = onlyFriendsPosts,
                onlyMyPosts = onlyMyPosts,
            )

            showFilters = false
          },
          onReset = {})
    }

    composeTestRule.onNodeWithTag(HomeScreenTestTags.FILTERS_MANAGER_APPLY).performClick()

    assertEquals("John", homeScreenVM.uiState.value.postsFilters.fromAuthor)
    assertEquals("EPFL", homeScreenVM.uiState.value.postsFilters.fromPlace)
    assertEquals("Cat", homeScreenVM.uiState.value.postsFilters.ofAnimal)

    assertTrue(homeScreenVM.uiState.value.postsFilters.onlyFriendsPosts)
    assertTrue(homeScreenVM.uiState.value.postsFilters.onlyMyPosts)

    assertFalse(showFilters)
  }

  @Test
  fun resetButtonResetsFilters() {
    var fromAuthor: String? by mutableStateOf("John")
    var fromPlace: String? by mutableStateOf("EPFL")
    var ofAnimal: String? by mutableStateOf("Cat")
    var onlyFriendsPosts by mutableStateOf(true)
    var onlyMyPosts by mutableStateOf(true)

    var showFilters by mutableStateOf(true)

    homeScreenVM.setPostsFilter(
        fromAuthor = fromAuthor,
        fromPlace = fromPlace,
        ofAnimal = ofAnimal,
        onlyFriendsPosts = onlyFriendsPosts,
        onlyMyPosts = onlyMyPosts)

    composeTestRule.setContent {
      FiltersManager(
          postsFilters = homeScreenVM.uiState.value.postsFilters,
          onFilterChange =
              OnFilterChange(
                  onFromAuthorChange = {},
                  onFromPlaceChange = {},
                  onOfAnimalChange = {},
                  onOnlyFriendsPostsChange = {},
                  onOnlyMyPostsChange = {}),
          onDismissRequest = {},
          onApply = {},
          onReset = {
            fromAuthor = null
            fromPlace = null
            ofAnimal = null
            onlyFriendsPosts = false
            onlyMyPosts = false

            homeScreenVM.setPostsFilter(
                fromAuthor = null,
                fromPlace = null,
                ofAnimal = null,
                onlyFriendsPosts = false,
                onlyMyPosts = false,
            )

            showFilters = false
          })
    }

    composeTestRule.onNodeWithTag(HomeScreenTestTags.FILTERS_MANAGER_RESET).performClick()

    assertEquals(null, homeScreenVM.uiState.value.postsFilters.fromAuthor)
    assertEquals(null, homeScreenVM.uiState.value.postsFilters.fromPlace)
    assertEquals(null, homeScreenVM.uiState.value.postsFilters.ofAnimal)

    assertFalse(homeScreenVM.uiState.value.postsFilters.onlyFriendsPosts)
    assertFalse(homeScreenVM.uiState.value.postsFilters.onlyMyPosts)

    assertFalse(showFilters)
  }

  @Test
  fun closeFiltersDoesNotChangeFilters() {
    val fromAuthor: String? by mutableStateOf("John")
    val fromPlace: String? by mutableStateOf(null)
    val ofAnimal: String? by mutableStateOf("Cat")
    val onlyFriendsPosts by mutableStateOf(true)
    val onlyMyPosts by mutableStateOf(false)

    homeScreenVM.setPostsFilter(
        fromAuthor = fromAuthor,
        fromPlace = fromPlace,
        ofAnimal = ofAnimal,
        onlyFriendsPosts = onlyFriendsPosts,
        onlyMyPosts = onlyMyPosts)

    composeTestRule.setContent { OpenFiltersButton(homeScreenVM) }

    composeTestRule.onNodeWithTag(HomeScreenTestTags.OPEN_FILTERS_MANAGER).performClick()

    composeTestRule.onNodeWithTag(HomeScreenTestTags.OPEN_FILTERS_MANAGER).performClick()

    assertEquals("John", homeScreenVM.uiState.value.postsFilters.fromAuthor)
    assertEquals(null, homeScreenVM.uiState.value.postsFilters.fromPlace)
    assertEquals("Cat", homeScreenVM.uiState.value.postsFilters.ofAnimal)

    assertTrue(homeScreenVM.uiState.value.postsFilters.onlyFriendsPosts)
    assertFalse(homeScreenVM.uiState.value.postsFilters.onlyMyPosts)
  }
}
