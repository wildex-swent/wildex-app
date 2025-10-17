package com.android.wildex.ui.post

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ImageResult
import coil.request.SuccessResult
import com.android.wildex.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class PostDetailsScreenTest {

  @get:Rule val composeRule = createComposeRule()
  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private fun noOpImageLoader(context: Context) =
      ImageLoader.Builder(context)
          .components {
            add(
                object : Interceptor {
                  override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
                    return SuccessResult(
                        drawable = ColorDrawable(Color.GRAY),
                        request = chain.request,
                        dataSource = DataSource.MEMORY)
                  }
                })
          }
          .build()

  private fun setThemedContent(block: @Composable () -> Unit) {
    composeRule.setContent {
      val ctx = LocalContext.current
      CompositionLocalProvider(LocalImageLoader provides noOpImageLoader(ctx)) {
        MaterialTheme { block() }
      }
    }
  }

  private fun samplePost() =
      PostDetailsUIState(
          postId = "post1",
          authorId = "poster1",
          authorUsername = "tiger_lover",
          authorProfilePictureURL =
              "https://vectorportal.com/storage/d5YN3OWWLMAJMqMZZJsITZT6bUniD0mbd2HGVNkB.jpg",
          animalName = "Tiger",
          date = "2025-10-16",
          location = "India",
          description = "Saw this beautiful tiger during my trip!",
          pictureURL = "https://upload.wikimedia.org/wikipedia/commons/5/56/Tiger.50.jpg",
          likedByCurrentUser = false,
          likesCount = 5,
          commentsUI =
              listOf(
                  CommentWithAuthorUI(
                      authorId = "commentAuthor1",
                      authorUserName = "joe34",
                      authorProfilePictureUrl =
                          "https://vectorportal.com/storage/KIygRdXXMVXBs09f42hJ4VWOYVZIX9WdhOJP7Rf4.jpg",
                      text = "Great post!",
                      date = "2025-10-17")),
          currentUserId = "currentUserId-1",
          currentUserProfilePictureURL = "")

  @Test
  fun topBar_backButton_triggersCallback() {
    var backClicked = 0
    setThemedContent { PostDetailsTopBar(onGoBack = { backClicked++ }) }
    composeRule.onNodeWithContentDescription("Back to Homepage").performClick()
    Assert.assertEquals(1, backClicked)
  }

  @Test
  fun postDetailsScreen_displaysData_and_triggersCallbacks() {
    val post = samplePost()

    val stateFlow = MutableStateFlow(post)

    var profileClicked = ""
    var likeAdded = 0
    var likeRemoved = 0
    var commentAdded = ""

    val vm =
        mockk<PostDetailsScreenViewModel>(relaxed = true) {
          every { uiState } returns stateFlow
          coEvery { addLike() } answers { likeAdded++ }
          coEvery { removeLike() } answers { likeRemoved++ }
          coEvery { addComment(any()) } answers
              {
                commentAdded = firstArg()
                stateFlow.value =
                    stateFlow.value.copy(
                        commentsUI =
                            stateFlow.value.commentsUI +
                                CommentWithAuthorUI(
                                    authorId = "currentUserId-1",
                                    authorUserName = "Me",
                                    authorProfilePictureUrl = "",
                                    text = firstArg(),
                                    date = "2025-10-18"))
              }
        }

    setThemedContent {
      PostDetailsScreen(
          postId = post.postId,
          postDetailsScreenViewModel = vm,
          onGoBack = {},
          onProfile = { profileClicked = it })
    }

    composeRule.waitForIdle()
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    // Check description
    composeRule.onNodeWithText(post.description).assertIsDisplayed()

    // Check author info and animalId
    composeRule.onNodeWithText("${post.authorUsername} saw an animal!").assertIsDisplayed()
    composeRule.onNodeWithText(post.date).assertIsDisplayed()
    composeRule.onNodeWithText(post.location).assertIsDisplayed()

    // Like button click
    composeRule.onNode(hasContentDescription("Like status")).performClick()
    Assert.assertEquals(1, likeAdded)
    Assert.assertEquals(0, likeRemoved)

    // Comments displayed
    composeRule.onNodeWithText("Great post!").assertIsDisplayed()

    // Add comment
    /*composeRule
        .onNode(hasText("Add a comment...") and hasSetTextAction())
        .performTextInput("Great post!")*/
    composeRule.onNode(hasContentDescription("Send comment")).performClick()
    // Assert.assertEquals("Great post!", commentAdded)

    // Click posters profile picture
    composeRule.onNodeWithTag(testTagForProfilePicture(post.authorId, "author")).performClick()
    Assert.assertEquals(post.authorId, profileClicked)

    // Click comment authors profile picture
    composeRule
        .onNodeWithTag(testTagForProfilePicture("commentAuthor1", "commenter"))
        .performClick()
    Assert.assertEquals("commentAuthor1", profileClicked)

    // Click current users profile picture in the comment input
    composeRule
        .onNodeWithTag(testTagForProfilePicture("currentUserId-1", "comment_input"))
        .performClick()
    Assert.assertEquals("currentUserId-1", profileClicked)
  }
}
