package com.android.wildex.ui.home

import com.android.wildex.model.social.Comment
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.Like
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.post.PostDetailsScreenViewModel
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
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

  @After fun tearDown() {}

  @Test
  fun viewModel_initializes_default_UI_state() {
    coEvery { likeRepository.getLikeForPost("post1") } returns null
    val initialState = viewModel.uiState.value
    Assert.assertTrue(initialState.postId == "")
    Assert.assertEquals(0, initialState.likesCount)
    Assert.assertEquals(0, initialState.commentsCount)
  }

  @Test
  fun loadPostDetails_updates_UI_state() = runBlocking {
    viewModel.loadPostDetails("post1")
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertEquals("post1", state.postId)
    Assert.assertEquals("Saw this beautiful tiger during my trip!", state.description)
    Assert.assertEquals("tiger_lover", state.authorUsername)
    Assert.assertEquals(4, state.commentsUI.size)
    Assert.assertFalse(state.likedByCurrentUser)
  }

  @Test
  fun loadPostDetails_sets_error_on_exception() = runBlocking {
    coEvery { postsRepository.getPost("post1") } throws Exception("fail")

    viewModel.loadPostDetails("post1")
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertTrue(state.errorMsg!!.contains("fail"))
  }

  @Test
  fun addLike_calls_repositories_and_updates_state() = runBlocking {
    viewModel.loadPostDetails("post1")

    viewModel.addLike()
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    coVerify { postsRepository.editPost("post1", any()) }
    coVerify { likeRepository.addLike(any()) }
  }

  @Test
  fun removeLike_calls_repositories_and_updates_state() = runBlocking {
    coEvery { likeRepository.getLikeForPost("post1") } returns
        Like("like1", "post1", "currentUserId-1")
    viewModel.loadPostDetails("post1")

    viewModel.removeLike()
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    coVerify { postsRepository.editPost("post1", any()) }
    coVerify { likeRepository.deleteLike("like1") }
  }

  @Test
  fun addComment_calls_repositories_and_updates_state() = runBlocking {
    coEvery { commentRepository.getNewCommentId() } returns "commentNew"
    coEvery { commentRepository.addComment(any()) } just Runs

    viewModel.loadPostDetails("post1")

    viewModel.addComment("Nice!")
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    coVerify { postsRepository.editPost("post1", any()) }
    coVerify { commentRepository.addComment(any()) }
  }

  @Test
  fun clearErrorMsg_sets_errorMsg_to_null() = runBlocking {
    viewModel.loadPostDetails("post1")
    viewModel.clearErrorMsg()
    Assert.assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun commentsToCommentsUI_sets_error_on_exception() = runBlocking {
    coEvery { userRepository.getSimpleUser("commentAuthor1") } throws Exception("fail")

    viewModel.loadPostDetails("post1")
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertTrue(state.commentsUI.isEmpty())
    Assert.assertTrue(state.errorMsg!!.contains("fail"))
  }

  @Test
  fun currentUserProfilePictureURL_sets_error_on_exception() = runBlocking {
    coEvery { userRepository.getSimpleUser("currentUserId-1") } throws Exception("fail")

    viewModel.loadPostDetails("post1")
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertTrue(state.currentUserProfilePictureURL == "")
    Assert.assertTrue(state.errorMsg!!.contains("fail"))
  }

  @Test
  fun likedByCurrentUser_sets_error_on_exception() = runBlocking {
    coEvery { likeRepository.getLikeForPost("post1") } throws Exception("fail")

    viewModel.loadPostDetails("post1")
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertFalse(state.likedByCurrentUser)
    Assert.assertTrue(state.errorMsg!!.contains("fail"))
  }

  @Test
  fun addLike_sets_error_on_exception_for_like_handling() = runBlocking {
    viewModel.loadPostDetails("post1")
    coEvery { likeRepository.addLike(any()) } throws Exception("fail")

    viewModel.addLike()
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertTrue(state.errorMsg!!.contains("fail"))
  }

  @Test
  fun addLike_sets_error_on_exception_for_post_handling() = runBlocking {
    viewModel.loadPostDetails("post1")
    coEvery { postsRepository.editPost(any(), any()) } throws Exception("fail")

    viewModel.addLike()
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertTrue(state.errorMsg!!.contains("fail"))
  }

  @Test
  fun removeLike_sets_error_on_exception_for_like_handling() = runBlocking {
    viewModel.loadPostDetails("post1")
    coEvery { likeRepository.deleteLike(any()) } throws Exception("fail")
    coEvery { likeRepository.getLikeForPost(any()) } returns
        Like("like1", "post1", "currentUserId-1")

    viewModel.removeLike()
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertTrue(state.errorMsg!!.contains("fail"))
  }

  @Test
  fun removeLike_sets_error_on_exception_for_post_handling() = runBlocking {
    viewModel.loadPostDetails("post1")
    coEvery { likeRepository.deleteLike(any()) } just Runs
    coEvery { likeRepository.getLikeForPost(any()) } returns
        Like("like1", "post1", "currentUserId-1")
    coEvery { postsRepository.editPost(any(), any()) } throws Exception("fail")

    viewModel.removeLike()
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertTrue(state.errorMsg!!.contains("fail"))
  }

  @Test
  fun addComment_sets_error_on_exception_for_comment_handling() = runBlocking {
    viewModel.loadPostDetails("post1")
    coEvery { commentRepository.getNewCommentId() } returns "commentNew"
    coEvery { commentRepository.addComment(any()) } throws Exception("fail")

    viewModel.addComment()
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertTrue(state.errorMsg!!.contains("fail"))
  }

  @Test
  fun addComment_sets_error_on_exception_for_post_handling() = runBlocking {
    viewModel.loadPostDetails("post1")
    coEvery { commentRepository.getNewCommentId() } returns "commentNew"
    coEvery { commentRepository.addComment(any()) } just Runs
    coEvery { postsRepository.editPost(any(), any()) } throws Exception("fail")

    viewModel.addComment()
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertTrue(state.errorMsg!!.contains("fail"))
  }
}
