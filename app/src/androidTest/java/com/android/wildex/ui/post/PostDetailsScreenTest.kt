package com.android.wildex.ui.post

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.social.Comment
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.CommentTag
import com.android.wildex.model.social.Like
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.LoadingScreenTestTags
import com.android.wildex.utils.LocalRepositories
import com.google.firebase.Timestamp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostDetailsScreenTest {

  private val postRepository: PostsRepository = LocalRepositories.postsRepository
  private val userRepository: UserRepository = LocalRepositories.userRepository
  private val commentRepository: CommentRepository = LocalRepositories.commentRepository
  private val likeRepository: LikeRepository = LocalRepositories.likeRepository
  private val animalRepository = LocalRepositories.animalRepository
  private lateinit var postDetailsViewModel: PostDetailsScreenViewModel

  @get:Rule val composeRule = createComposeRule()

  @Before
  fun setup() = runBlocking {
    // Create users
    val currentUser =
        User(
            userId = "currentUserId-1",
            username = "current_user",
            name = "Current",
            surname = "User",
            bio = "I love wildlife.",
            profilePictureURL = "https://randomuser.me/api/portraits/men/32.jpg",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "USA",
        )
    val author =
        User(
            userId = "poster1",
            username = "tiger_lover",
            name = "Tiger",
            surname = "Lover",
            bio = "Tiger enthusiast.",
            profilePictureURL =
                "https://vectorportal.com/storage/d5YN3OWWLMAJMqMZZJsITZT6bUniD0mbd2HGVNkB.jpg",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "India",
        )
    val commenter1 =
        User(
            userId = "commenter1",
            username = "joe34",
            name = "Joe",
            surname = "Smith",
            bio = "Nature lover.",
            profilePictureURL =
                "https://vectorportal.com/storage/KIygRdXXMVXBs09f42hJ4VWOYVZIX9WdhOJP7Rf4.jpg",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "USA",
        )
    val commenter2 =
        User(
            userId = "commenter2",
            username = "sara88",
            name = "Sara",
            surname = "Lee",
            bio = "Wildlife photographer.",
            profilePictureURL = "https://randomuser.me/api/portraits/women/88.jpg",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "UK",
        )
    val liker1 =
        User(
            userId = "liker1",
            username = "alex99",
            name = "Alex",
            surname = "Brown",
            bio = "Explorer.",
            profilePictureURL = "https://randomuser.me/api/portraits/men/99.jpg",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "Canada",
        )
    val liker2 =
        User(
            userId = "liker2",
            username = "emma77",
            name = "Emma",
            surname = "White",
            bio = "Animal lover.",
            profilePictureURL = "https://randomuser.me/api/portraits/women/77.jpg",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "Australia",
        )
    userRepository.addUser(currentUser)
    userRepository.addUser(author)
    userRepository.addUser(commenter1)
    userRepository.addUser(commenter2)
    userRepository.addUser(liker1)
    userRepository.addUser(liker2)

    // Create post
    val post =
        Post(
            postId = "post1",
            authorId = author.userId,
            pictureURL = "https://upload.wikimedia.org/wikipedia/commons/5/56/Tiger.50.jpg",
            location = Location(0.0, 0.0, "India"),
            description = "Saw this beautiful tiger during my trip!",
            date = Timestamp.now(),
            animalId = "tiger",
        )
    postRepository.addPost(post)

    // Add likes
    likeRepository.addLike(Like(postId = post.postId, userId = liker1.userId, likeId = "like1"))
    likeRepository.addLike(Like(postId = post.postId, userId = liker2.userId, likeId = "like2"))

    // Add comments
    commentRepository.addComment(
        Comment(
            commentId = "comment1",
            parentId = post.postId,
            authorId = commenter1.userId,
            text = "Amazing shot!",
            date = Timestamp.now(),
            tag = CommentTag.POST_COMMENT))
    commentRepository.addComment(
        Comment(
            commentId = "comment2",
            parentId = post.postId,
            authorId = commenter2.userId,
            text = "Love this!",
            date = Timestamp.now(),
            tag = CommentTag.POST_COMMENT))

    val animal =
        Animal(
            animalId = "tiger",
            name = "Tiger",
            description = "",
            pictureURL = "",
            species = "Bengal Tiger",
        )
    animalRepository.addAnimal(animal)

    postDetailsViewModel =
        PostDetailsScreenViewModel(
            postRepository,
            userRepository,
            commentRepository,
            animalRepository,
            likeRepository,
            "currentUserId-1",
        )
  }

  @After
  fun teardown() {
    LocalRepositories.clearAll()
  }

  @Test
  fun topBar_backButton_triggersCallback() {
    var backClicked = 0
    composeRule.setContent { PostDetailsTopBar(onGoBack = { backClicked++ }) }
    composeRule.onNodeWithContentDescription("Back to Homepage").performClick()
    Assert.assertEquals(1, backClicked)
  }

  @Test
  fun postDetailsScreen_displaysDescriptionAndMeta() {
    // Use real ViewModel and repositories
    runBlocking { postDetailsViewModel.loadPostDetails("post1") }
    composeRule.setContent {
      PostDetailsScreen(
          postId = "post1",
          postDetailsScreenViewModel = postDetailsViewModel,
          onGoBack = {},
          onProfile = {},
      )
    }
    composeRule.waitForIdle()
    // Check description
    composeRule.onNodeWithText("Saw this beautiful tiger during my trip!").assertIsDisplayed()
    // Check author info and animalId
    composeRule.onNodeWithText("tiger_lover", substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("saw a", substring = true).assertIsDisplayed()
    // Date is dynamic, so just check location
    composeRule.onNodeWithText("India").assertIsDisplayed()
  }

  @Test
  fun postDetailsScreen_likeButton_addsLike_removesLike() {
    runBlocking { postDetailsViewModel.loadPostDetails("post1") }
    composeRule.setContent {
      PostDetailsScreen(
          postId = "post1",
          postDetailsScreenViewModel = postDetailsViewModel,
          onGoBack = {},
          onProfile = {},
      )
    }
    composeRule.waitForIdle()
    // Like count before
    composeRule.onNodeWithText("2").assertIsDisplayed()

    // Perform like
    composeRule.onNode(hasContentDescription("Like status")).performClick()
    // Like count after (should increment)
    composeRule.onNodeWithText("3").assertIsDisplayed()

    // Perform unlike
    composeRule.onNode(hasContentDescription("Like status")).performClick()
    // Like count after (should decrement)
    composeRule.onNodeWithText("2").assertIsDisplayed()
  }

  @Test
  fun postDetailsScreen_commentIsDisplayedAndCanBeAdded() {
    runBlocking { postDetailsViewModel.loadPostDetails("post1") }
    composeRule.setContent {
      PostDetailsScreen(
          postId = "post1",
          postDetailsScreenViewModel = postDetailsViewModel,
          onGoBack = {},
          onProfile = {},
      )
    }
    composeRule.waitForIdle()
    // Comments displayed
    composeRule.onNodeWithText("Amazing shot!").assertIsDisplayed()
    composeRule.onNodeWithText("Love this!").assertIsDisplayed()
    // Add comment
    val commentText = "Great post!"
    composeRule
        .onNode(hasText("Add a comment …") and hasSetTextAction())
        .performTextInput(commentText)
    composeRule.onNode(hasContentDescription("Send comment")).performClick()
    // Optionally, check if the new comment appears (if your ViewModel updates state synchronously)
    composeRule.onNodeWithText(commentText).assertIsDisplayed()
  }

  @Test
  fun postDetailsScreen_profilePictureClicks_triggerCallback() {
    runBlocking { postDetailsViewModel.loadPostDetails("post1") }
    var profileClicked = ""
    composeRule.setContent {
      PostDetailsScreen(
          postId = "post1",
          postDetailsScreenViewModel = postDetailsViewModel,
          onGoBack = {},
          onProfile = { profileClicked = it },
      )
    }
    composeRule.waitForIdle()
    // Click posters profile picture
    composeRule
        .onNodeWithTag(PostDetailsScreenTestTags.testTagForProfilePicture("poster1", "author"))
        .performClick()
    Assert.assertEquals("poster1", profileClicked)
    // Click comment authors profile picture
    composeRule
        .onNodeWithTag(
            PostDetailsScreenTestTags.testTagForProfilePicture("commenter1", "commenter"))
        .performClick()
    Assert.assertEquals("commenter1", profileClicked)
    // Click current users profile picture in the comment input
    composeRule
        .onNodeWithTag(
            PostDetailsScreenTestTags.testTagForProfilePicture("currentUserId-1", "comment_input"))
        .performClick()
    Assert.assertEquals("currentUserId-1", profileClicked)
  }

  @Test
  fun loadingScreen_showsWhileFetchingPosts() {
    val fetchSignal = CompletableDeferred<Unit>()
    val delayedPostsRepo =
        object : LocalRepositories.PostsRepositoryImpl() {
          override suspend fun getPost(postId: Id): Post {
            fetchSignal.await()
            return super.getPost(postId)
          }
        }
    runBlocking {
      delayedPostsRepo.addPost(
          Post(
              postId = "post1",
              authorId = "poster1",
              pictureURL = "",
              location = Location(0.0, 0.0, ""),
              description = "",
              date = Timestamp.now(),
              animalId = "",
          ))
      val vm =
          PostDetailsScreenViewModel(
              delayedPostsRepo,
              LocalRepositories.userRepository,
              LocalRepositories.commentRepository,
              LocalRepositories.animalRepository,
              LocalRepositories.likeRepository,
              "currentUserId-1",
          )
      composeRule.setContent { PostDetailsScreen("post1", vm) }
      composeRule
          .onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN, useUnmergedTree = true)
          .assertIsDisplayed()
      fetchSignal.complete(Unit)
      composeRule.waitForIdle()
      composeRule
          .onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN, useUnmergedTree = true)
          .assertIsNotDisplayed()
    }
  }

  @Test
  fun failScreenShown_whenPostLookupFails() {
    composeRule.setContent { PostDetailsScreen("bad", postDetailsViewModel) }
    composeRule.waitForIdle()

    composeRule
        .onNodeWithTag(LoadingScreenTestTags.LOADING_FAIL, useUnmergedTree = true)
        .assertIsDisplayed()
  }
}
