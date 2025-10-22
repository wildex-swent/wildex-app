package com.android.wildex.ui.post

import com.android.wildex.model.social.Comment
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.Like
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Location
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostDetailsScreenViewModelTest {
  @get:Rule val mainDispatcherRule = MainDispatcherRule()
  private lateinit var postsRepository: PostsRepository
  private lateinit var userRepository: UserRepository
    private lateinit var commentRepository: CommentRepository
  private lateinit var likeRepository: LikeRepository
  private lateinit var viewModel: PostDetailsScreenViewModel

  private val testPost =
      Post(
          postId = "post1",
          authorId = "poster1",
          pictureURL = "https://upload.wikimedia.org/wikipedia/commons/5/56/Tiger.50.jpg",
          location = Location(37.7749, -122.4194, "India"),
          description = "Saw this beautiful tiger during my trip!",
          date = Timestamp.now(),
          animalId = "Tiger",
          likesCount = 0,
          commentsCount = 4,
      )

  private val testPostSimpleAuthor =
      SimpleUser(
          userId = "poster1",
          username = "tiger_lover",
          profilePictureURL =
              "https://vectorportal.com/storage/d5YN3OWWLMAJMqMZZJsITZT6bUniD0mbd2HGVNkB.jpg",
      )

  private val testCommentsAuthor =
      SimpleUser(
          userId = "commentAuthor1",
          username = "joe34",
          profilePictureURL =
              "https://vectorportal.com/storage/KIygRdXXMVXBs09f42hJ4VWOYVZIX9WdhOJP7Rf4.jpg",
      )

  private val testComments =
      listOf(
          Comment(
              commentId = "comment1",
              postId = "post1",
              authorId = "commentAuthor1",
              text = "Great post!",
              date = Timestamp.now(),
          ),
          Comment(
              commentId = "comment2",
              postId = "post1",
              authorId = "commentAuthor1",
              text = "Thanks for sharing!",
              date = Timestamp.now(),
          ),
          Comment(
              commentId = "comment3",
              postId = "post1",
              authorId = "commentAuthor1",
              text = "It's beautiful!",
              date = Timestamp.now(),
          ),
          Comment(
              commentId = "comment4",
              postId = "post1",
              authorId = "commentAuthor1",
              text = "Would love to see it in person.",
              date = Timestamp.now(),
          ),
      )

  @Before
  fun setUp() {
    postsRepository = mockk()
    userRepository = mockk()
    commentRepository = mockk()
    likeRepository = mockk()
    viewModel =
        PostDetailsScreenViewModel(
            postRepository = postsRepository,
            userRepository = userRepository,
            commentRepository = commentRepository,
            likeRepository = likeRepository,
            "currentUserId-1",
        )
    coEvery { postsRepository.getPost("post1") } returns testPost
    coEvery { userRepository.getSimpleUser("poster1") } returns testPostSimpleAuthor
    coEvery { userRepository.getSimpleUser("commentAuthor1") } returns testCommentsAuthor
    coEvery { userRepository.getSimpleUser("currentUserId-1") } returns
        SimpleUser("currentUserId-1", "me", "url")
    coEvery { commentRepository.getAllCommentsByPost("post1") } returns testComments
    coEvery { postsRepository.editPost(any(), any()) } just Runs
    coEvery { likeRepository.getNewLikeId() } returns "like1"
    coEvery { likeRepository.addLike(any()) } just Runs
    coEvery { likeRepository.deleteLike("like1") } just Runs
  }

  @Test
  fun viewModel_initializes_default_UI_state() {
    coEvery { likeRepository.getLikeForPost("post1") } returns null
    val initialState = viewModel.uiState.value
      assertTrue(initialState.postId == "")
      assertEquals(0, initialState.likesCount)
      assertEquals(0, initialState.commentsCount)
  }

  @Test
  fun loadPostDetails_updates_UI_state() =
      mainDispatcherRule.runTest {
          viewModel.loadPostDetails("post1")
          advanceUntilIdle()

          val state = viewModel.uiState.value
          assertEquals("post1", state.postId)
          assertEquals("Saw this beautiful tiger during my trip!", state.description)
          assertEquals("tiger_lover", state.authorUsername)
          assertEquals(4, state.commentsUI.size)
          assertFalse(state.likedByCurrentUser)
      }

  @Test
  fun loadPostDetails_sets_error_on_exception() =
      mainDispatcherRule.runTest {
          coEvery { postsRepository.getPost("post1") } throws Exception("fail")

          viewModel.loadPostDetails("post1")
          advanceUntilIdle()

          val state = viewModel.uiState.value
          assertTrue(state.errorMsg!!.contains("fail"))
      }

  @Test
  fun addLike_calls_repositories_and_updates_state() =
      mainDispatcherRule.runTest {
          viewModel.loadPostDetails("post1")

          viewModel.addLike()
          advanceUntilIdle()

          coVerify { postsRepository.editPost("post1", any()) }
          coVerify { likeRepository.addLike(any()) }
      }

  @Test
  fun removeLike_calls_repositories_and_updates_state() =
      mainDispatcherRule.runTest {
          coEvery { likeRepository.getLikeForPost("post1") } returns
                  Like("like1", "post1", "currentUserId-1")
          coEvery { likeRepository.deleteLike(any()) } just Runs

          viewModel.loadPostDetails("post1")
          advanceUntilIdle()
          viewModel.removeLike()
          advanceUntilIdle()

          coVerify { postsRepository.editPost("post1", any()) }
          coVerify { likeRepository.deleteLike("like1") }
      }

  @Test
  fun addComment_calls_repositories_and_updates_state() =
      mainDispatcherRule.runTest {
          coEvery { commentRepository.getNewCommentId() } returns "commentNew"
          coEvery { commentRepository.addComment(any()) } just Runs

          viewModel.loadPostDetails("post1")

          viewModel.addComment("Nice!")
          advanceUntilIdle()

          coVerify { postsRepository.editPost("post1", any()) }
          coVerify { commentRepository.addComment(any()) }
      }

  @Test
  fun clearErrorMsg_sets_errorMsg_to_null() =
      mainDispatcherRule.runTest {
          viewModel.loadPostDetails("post1")
          viewModel.clearErrorMsg()
          assertNull(viewModel.uiState.value.errorMsg)
      }

  @Test
  fun commentsToCommentsUI_sets_error_on_exception() =
      mainDispatcherRule.runTest {
          coEvery { userRepository.getSimpleUser("commentAuthor1") } throws Exception("fail")

          viewModel.loadPostDetails("post1")
          advanceUntilIdle()

          val state = viewModel.uiState.value
          assertTrue(state.commentsUI.isEmpty())
          assertTrue(state.errorMsg!!.contains("fail"))
      }

  @Test
  fun currentUserProfilePictureURL_sets_error_on_exception() =
      mainDispatcherRule.runTest {
          coEvery { userRepository.getSimpleUser("poster1") } throws Exception("fail")

          viewModel.loadPostDetails("post1")
          advanceUntilIdle()

          val state = viewModel.uiState.value
          assertTrue(state.currentUserProfilePictureURL == "")
          assertTrue(state.errorMsg!!.contains("fail"))
      }

  @Test
  fun likedByCurrentUser_sets_error_on_exception() =
      mainDispatcherRule.runTest {
          coEvery { likeRepository.getLikeForPost("post1") } throws Exception("fail")

          viewModel.loadPostDetails("post1")
          advanceUntilIdle()

          val state = viewModel.uiState.value
          assertFalse(state.likedByCurrentUser)
          assertTrue(state.errorMsg!!.contains("fail"))
      }

  @Test
  fun addLike_sets_error_on_exception_for_like_handling() =
      mainDispatcherRule.runTest {
          viewModel.loadPostDetails("post1")
          advanceUntilIdle()
          coEvery { likeRepository.addLike(any()) } throws Exception("fail")

          viewModel.addLike()
          advanceUntilIdle()

          val state = viewModel.uiState.value
          assertTrue(state.errorMsg!!.contains("fail"))
      }

  @Test
  fun addLike_sets_error_on_exception_for_post_handling() =
      mainDispatcherRule.runTest {
          viewModel.loadPostDetails("post1")
          coEvery { postsRepository.editPost(any(), any()) } throws Exception("fail")

          viewModel.addLike()
          advanceUntilIdle()

          val errorMsg = viewModel.uiState.value.errorMsg
          assertNotNull(errorMsg)
          assertTrue(errorMsg!!.contains("fail"))
      }

  @Test
  fun removeLike_sets_error_on_exception_for_like_handling() =
      mainDispatcherRule.runTest {
          coEvery { likeRepository.deleteLike(any()) } throws Exception("fail")
          coEvery { likeRepository.getLikeForPost(any()) } returns
                  Like("like1", "post1", "currentUserId-1")

          viewModel.loadPostDetails("post1")
          advanceUntilIdle()
          viewModel.removeLike()
          advanceUntilIdle()

          val errorMsg = viewModel.uiState.value.errorMsg
          assertNotNull(errorMsg)
          assertTrue(errorMsg!!.contains("fail"))
      }

  @Test
  fun removeLike_sets_error_on_exception_for_post_handling() =
      mainDispatcherRule.runTest {
          coEvery { likeRepository.deleteLike(any()) } just Runs
          coEvery { likeRepository.getLikeForPost(any()) } returns
                  Like("like1", "post1", "currentUserId-1")
          coEvery { postsRepository.editPost(any(), any()) } throws Exception("fail")
          viewModel.loadPostDetails("post1")
          advanceUntilIdle()

          viewModel.removeLike()
          advanceUntilIdle()

          val state = viewModel.uiState.value
          assertNotNull(state.errorMsg)
          assertTrue(state.errorMsg!!.contains("fail"))
      }

  @Test
  fun addComment_sets_error_on_exception_for_comment_handling() =
      mainDispatcherRule.runTest {
          viewModel.loadPostDetails("post1")
          advanceUntilIdle()
          coEvery { commentRepository.getNewCommentId() } returns "commentNew"
          coEvery { commentRepository.addComment(any()) } throws Exception("fail")

          viewModel.addComment("comment text")
          advanceUntilIdle()

          val state = viewModel.uiState.value
          assertNotNull(state.errorMsg)
          assertTrue(state.errorMsg!!.contains("fail"))
      }

  @Test
  fun addComment_sets_error_on_exception_for_post_handling() =
      mainDispatcherRule.runTest {
          viewModel.loadPostDetails("post1")
          advanceUntilIdle()
          coEvery { commentRepository.getNewCommentId() } returns "commentNew"
          coEvery { commentRepository.addComment(any()) } just Runs
          coEvery { postsRepository.editPost(any(), any()) } throws Exception("fail")

          viewModel.addComment("comment text")
          advanceUntilIdle()

          val state = viewModel.uiState.value
          assertNotNull(state.errorMsg)
          assertTrue(state.errorMsg!!.contains("fail"))
      }
}
