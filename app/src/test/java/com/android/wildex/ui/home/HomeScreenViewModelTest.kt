// kotlin
package com.android.wildex.ui.home

import com.android.wildex.model.animal.Animal
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.social.Comment
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.CommentTag
import com.android.wildex.model.social.Like
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserSettingsRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Location
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.Calendar
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenViewModelTest {
  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var postsRepository: PostsRepository
  private lateinit var userRepository: UserRepository
  private lateinit var likeRepository: LikeRepository
  private lateinit var commentRepository: CommentRepository
  private lateinit var animalRepository: AnimalRepository
  private lateinit var userSettingsRepository: UserSettingsRepository
  private lateinit var viewModel: HomeScreenViewModel

  private val defaultUser: SimpleUser =
      SimpleUser(
          userId = "defaultUserId",
          username = "defaultUsername",
          profilePictureURL = "",
          userType = UserType.REGULAR,
      )

  private val p1 =
      Post(
          postId = "p1",
          authorId = "author1",
          pictureURL = "url1",
          location = Location(0.0, 0.0),
          description = "d1",
          date = Timestamp(Calendar.getInstance().time),
          animalId = "a1",
      )

  private val p2 =
      Post(
          postId = "p2",
          authorId = "author2",
          pictureURL = "url2",
          location = Location(1.0, 1.0),
          description = "d2",
          date = Timestamp(Calendar.getInstance().time),
          animalId = "a2",
      )

  private val u1 =
      SimpleUser(
          userId = "uid-1",
          username = "user_one",
          profilePictureURL = "u",
          userType = UserType.REGULAR,
      )

  private val author1 = SimpleUser("author1", "author_one", "url1", userType = UserType.REGULAR)
  private val author2 = SimpleUser("author2", "author_two", "url2", userType = UserType.REGULAR)

  private val like1 = Like(likeId = "like1", postId = "p1", userId = "author-2")

  private val like2 = Like(likeId = "like2", postId = "p2", userId = "author-1")

  private val comment1 =
      Comment(
          commentId = "comment1",
          parentId = "p1",
          authorId = "author2",
          text = "text1",
          date = Timestamp(Calendar.getInstance().time),
          tag = CommentTag.POST_COMMENT,
      )
  private val comment2 =
      Comment(
          commentId = "comment2",
          parentId = "p2",
          authorId = "author1",
          text = "text2",
          date = Timestamp(Calendar.getInstance().time),
          tag = CommentTag.POST_COMMENT,
      )

  private val animal1 =
      Animal(
          animalId = "a1",
          name = "animal_one",
          species = "species_one",
          description = "description_one",
          pictureURL = "url_one",
      )

  private val animal2 =
      Animal(
          animalId = "a2",
          name = "animal_two",
          species = "species_two",
          description = "description_two",
          pictureURL = "url_two",
      )

  @Before
  fun setUp() {
    postsRepository = mockk()
    userRepository = mockk()
    likeRepository = mockk()
    commentRepository = mockk()
    animalRepository = mockk()
    userSettingsRepository = mockk()
    viewModel =
        HomeScreenViewModel(
            postsRepository,
            userRepository,
            likeRepository,
            commentRepository,
            animalRepository,
            userSettingsRepository,
            "uid-1",
        )
    coEvery { userSettingsRepository.getAppearanceMode("uid-1") } returns AppearanceMode.AUTOMATIC
    coEvery { userRepository.getSimpleUser("author1") } returns author1
    coEvery { userRepository.getSimpleUser("author2") } returns author2
    coEvery { likeRepository.getLikeForPost("p1") } returns null
    coEvery { likeRepository.getLikeForPost("p2") } returns null
    coEvery { likeRepository.getLikesForPost("p1") } returns listOf(like1)
    coEvery { likeRepository.getLikesForPost("p2") } returns listOf(like2)
    coEvery { commentRepository.getAllCommentsByPost("p1") } returns listOf(comment1)
    coEvery { commentRepository.getAllCommentsByPost("p2") } returns listOf(comment2)
    coEvery { animalRepository.getAnimal("a1") } returns animal1
    coEvery { animalRepository.getAnimal("a2") } returns animal2
  }

  @Test
  fun viewModel_initializes_default_UI_state() {
    val initialState = viewModel.uiState.value
    Assert.assertTrue(initialState.postStates.isEmpty())
    Assert.assertEquals(initialState.currentUser, defaultUser)
    Assert.assertFalse(initialState.isRefreshing)
    Assert.assertFalse(initialState.isLoading)
    Assert.assertNull(initialState.errorMsg)
    Assert.assertFalse(initialState.isError)
  }

  @Test
  fun refreshUIState_updates_UI_state_success() {
    mainDispatcherRule.runTest {
      val deferred = CompletableDeferred<List<Post>>()
      coEvery { postsRepository.getAllPosts() } coAnswers { deferred.await() }
      coEvery { likeRepository.getLikeForPost("p1") } returns like1
      coEvery { userRepository.getSimpleUser("uid-1") } returns u1
      viewModel.refreshUIState()

      Assert.assertTrue(viewModel.uiState.value.isRefreshing)
      Assert.assertFalse(viewModel.uiState.value.isLoading)
      deferred.complete(listOf(p1, p2))
      advanceUntilIdle()
      val expectedStates =
          listOf(
              PostState(
                  p1,
                  isLiked = true,
                  author = author1,
                  animalName = animal1.name,
                  likeCount = 1,
                  commentsCount = 1),
              PostState(
                  p2,
                  isLiked = false,
                  author = author2,
                  animalName = animal2.name,
                  likeCount = 1,
                  commentsCount = 1),
          )
      val updatedState = viewModel.uiState.value
      Assert.assertEquals(expectedStates, updatedState.postStates)
      Assert.assertEquals(u1, updatedState.currentUser)
      Assert.assertFalse(updatedState.isLoading)
      Assert.assertNull(updatedState.errorMsg)
      Assert.assertFalse(updatedState.isRefreshing)
    }
  }

  @Test
  fun loadUIState_updates_UI_state_success() {
    mainDispatcherRule.runTest {
      val deferred = CompletableDeferred<List<Post>>()
      coEvery { postsRepository.getAllPosts() } coAnswers { deferred.await() }
      coEvery { likeRepository.getLikeForPost("p1") } returns like1
      coEvery { userRepository.getSimpleUser("uid-1") } returns u1
      viewModel.loadUIState()

      Assert.assertTrue(viewModel.uiState.value.isLoading)
      Assert.assertFalse(viewModel.uiState.value.isRefreshing)
      deferred.complete(listOf(p1, p2))
      advanceUntilIdle()
      val expectedStates =
          listOf(
              PostState(
                  p1,
                  isLiked = true,
                  author = author1,
                  animalName = animal1.name,
                  likeCount = 1,
                  commentsCount = 1),
              PostState(
                  p2,
                  isLiked = false,
                  author = author2,
                  animalName = animal2.name,
                  likeCount = 1,
                  commentsCount = 1),
          )
      val updatedState = viewModel.uiState.value
      Assert.assertEquals(expectedStates, updatedState.postStates)
      Assert.assertEquals(u1, updatedState.currentUser)
      Assert.assertFalse(updatedState.isLoading)
      Assert.assertNull(updatedState.errorMsg)
      Assert.assertFalse(updatedState.isRefreshing)
    }
  }

  @Test
  fun refreshUIState_whenCurrentUserRepoThrows_setsErrorAndKeepsEmptyPosts() {
    mainDispatcherRule.runTest {
      coEvery { postsRepository.getAllPosts() } returns listOf(p1, p2)
      coEvery { userRepository.getSimpleUser("uid-1") } throws RuntimeException("boom")

      viewModel.refreshUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertTrue(s.postStates.isEmpty())
      Assert.assertEquals(s.currentUser, defaultUser)
      Assert.assertFalse(s.isLoading)
      Assert.assertFalse(s.isRefreshing)
      Assert.assertNotNull(s.errorMsg)
    }
  }

  @Test
  fun refreshUIState_whenCurrentUserFetchFails_keepsEmptyUserAndSetsError() {
    mainDispatcherRule.runTest {
      coEvery { postsRepository.getAllPosts() } returns listOf(p1)

      viewModel =
          HomeScreenViewModel(
              postsRepository,
              userRepository,
              likeRepository,
              animalRepository,
              userSettingsRepository,
              "")
      viewModel.refreshUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertTrue(s.postStates.isEmpty())
      Assert.assertEquals(s.currentUser.userId, defaultUser.userId)
      Assert.assertEquals(s.currentUser.username, defaultUser.username)
      Assert.assertFalse(s.isLoading)
      Assert.assertFalse(s.isRefreshing)
      Assert.assertNotNull(s.errorMsg)
    }
  }

  @Test
  fun refreshUIState_whenPostsRepoThrows_keepsPreviousState() {
    mainDispatcherRule.runTest {
      coEvery { postsRepository.getAllPosts() } returns listOf(p1)
      coEvery { userRepository.getSimpleUser("uid-1") } returns u1

      viewModel.refreshUIState()
      advanceUntilIdle()
      val s1 = viewModel.uiState.value

      coEvery { postsRepository.getAllPosts() } throws RuntimeException("fail")
      viewModel.refreshUIState()
      advanceUntilIdle()
      val s2 = viewModel.uiState.value

      Assert.assertEquals(s1.postStates, s2.postStates)
      Assert.assertEquals(s1.currentUser, s2.currentUser)
      Assert.assertNotNull(s2.errorMsg)
      Assert.assertFalse(s2.isLoading)
      Assert.assertFalse(s2.isRefreshing)
    }
  }

  @Test
  fun refreshUIState_multipleCalls_updatesWithLatestData() {
    mainDispatcherRule.runTest {
      coEvery { postsRepository.getAllPosts() } returns listOf(p1)
      coEvery { userRepository.getSimpleUser("uid-1") } returns u1
      coEvery { likeRepository.getLikeForPost("p1") } returns like1
      viewModel.refreshUIState()
      advanceUntilIdle()

      val u2 = u1.copy(username = "user_one_2")
      coEvery { postsRepository.getAllPosts() } returns listOf(p2)
      coEvery { userRepository.getSimpleUser("uid-1") } returns u2
      coEvery { likeRepository.getLikeForPost("p2") } returns like2
      viewModel.refreshUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      val expectedStates =
          listOf(
              PostState(
                  p2,
                  isLiked = true,
                  author = author2,
                  animalName = animal2.name,
                  likeCount = 1,
                  commentsCount = 1),
          )
      Assert.assertEquals(expectedStates, s.postStates)
      Assert.assertEquals(u2, s.currentUser)
    }
  }

  @Test
  fun homeUIState_defaultValues_areCorrect() {
    val s = HomeUIState()
    Assert.assertTrue(s.postStates.isEmpty())
    Assert.assertEquals(s.currentUser, defaultUser)
  }

  @Test
  fun toggleLike_addsLike_whenNotAlreadyLiked() {
    mainDispatcherRule.runTest {
      val newLike = Like(likeId = "like42", postId = "p1", userId = "uid-1")
      var liked = false
      coEvery { likeRepository.getLikeForPost("p1") } returns null
      coEvery { likeRepository.getNewLikeId() } returns "like42"

      coEvery { likeRepository.addLike(newLike) } coAnswers { liked = true }

      viewModel.toggleLike("p1")
      advanceUntilIdle()

      assert(liked)
      coVerify(exactly = 0) { likeRepository.deleteLike(any()) }
    }
  }

  @Test
  fun toggleLike_removesLike_whenAlreadyLiked() {
    mainDispatcherRule.runTest {
      var isDeleted = false
      coEvery { likeRepository.getLikeForPost("p1") } returns like1
      coEvery { likeRepository.deleteLike("like1") } coAnswers { isDeleted = true }

      viewModel.toggleLike("p1")
      advanceUntilIdle()
      assert(isDeleted)
      coVerify(exactly = 0) { likeRepository.addLike(any()) }
    }
  }

  @Test
  fun refreshUIState_setsAndClears_isLoading_and_clearsErrorOnSuccess() {
    mainDispatcherRule.runTest {
      coEvery { postsRepository.getAllPosts() } returns listOf(p1)
      coEvery { userRepository.getSimpleUser("uid-1") } returns u1

      viewModel.refreshUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertFalse(s.isLoading)
      Assert.assertNull(s.errorMsg)
      Assert.assertEquals(
          listOf(
              PostState(
                  p1,
                  isLiked = false,
                  author = author1,
                  animalName = animal1.name,
                  likeCount = 1,
                  commentsCount = 1)),
          s.postStates,
      )
    }
  }

  @Test
  fun clearErrorMsg_resets_errorMsg_to_null() {
    mainDispatcherRule.runTest {
      coEvery { postsRepository.getAllPosts() } throws RuntimeException("boom")
      viewModel.refreshUIState()
      advanceUntilIdle()
      Assert.assertNotNull(viewModel.uiState.value.errorMsg)

      viewModel.clearErrorMsg()
      Assert.assertNull(viewModel.uiState.value.errorMsg)
    }
  }
}
