package com.android.wildex.model.achievement

import com.android.wildex.model.social.Comment
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.Like
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.utils.Location
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateUserAchievementsUseCaseTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var postsRepository: PostsRepository
  private lateinit var likeRepository: LikeRepository
  private lateinit var commentRepository: CommentRepository
  private lateinit var userAchievementsRepository: UserAchievementsRepository
  private lateinit var useCase: UpdateUserAchievementsUseCase

  private val userId = "u1"

  private val p1 =
      Post(
          postId = "p1",
          authorId = userId,
          pictureURL = "url1",
          location = Location(0.0, 0.0),
          description = "d1",
          date = Timestamp.now(),
          animalId = "a1",
          likesCount = 0,
          commentsCount = 0,
      )
  private val p2 =
      Post(
          postId = "p2",
          authorId = userId,
          pictureURL = "url2",
          location = Location(1.0, 1.0),
          description = "d2",
          date = Timestamp.now(),
          animalId = "a2",
          likesCount = 0,
          commentsCount = 0,
      )

  private val like1 = Like(likeId = "l1", postId = "px", userId = userId)
  private val like2 = Like(likeId = "l2", postId = "py", userId = userId)

  private val c1 =
      Comment(
          commentId = "c1", postId = "p1", authorId = userId, text = "t1", date = Timestamp.now())
  private val c2 =
      Comment(
          commentId = "c2", postId = "p2", authorId = userId, text = "t2", date = Timestamp.now())

  @Before
  fun setUp() {
    postsRepository = mockk()
    likeRepository = mockk()
    commentRepository = mockk()
    userAchievementsRepository = mockk(relaxed = true)

    useCase =
        UpdateUserAchievementsUseCase(
            postsRepository = postsRepository,
            likeRepository = likeRepository,
            commentRepository = commentRepository,
            userAchievementsRepository = userAchievementsRepository,
        )
  }

  @Test
  fun invoke_withData_passesCorrectInputsToRepository() {
    mainDispatcherRule.runTest {
      coEvery { userAchievementsRepository.initializeUserAchievements(userId) } returns Unit
      coEvery { postsRepository.getAllPostsByGivenAuthor(userId) } returns listOf(p1, p2)
      coEvery { likeRepository.getAllLikesByUser(userId) } returns listOf(like1, like2)
      coEvery { commentRepository.getCommentByUser(userId) } returns listOf(c1, c2)

      val inputsSlot = slot<Map<InputKey, List<String>>>()
      coEvery {
        userAchievementsRepository.updateUserAchievements(userId, capture(inputsSlot))
      } returns Unit

      useCase.invoke(userId)
      advanceUntilIdle()

      coVerify(exactly = 1) { userAchievementsRepository.initializeUserAchievements(userId) }
      coVerify(exactly = 1) { userAchievementsRepository.updateUserAchievements(userId, any()) }

      val inputs = inputsSlot.captured
      assertEquals(setOf("p1", "p2"), inputs[InputKey.POST_IDS]!!.toSet())
      assertEquals(setOf("px", "py"), inputs[InputKey.LIKE_IDS]!!.toSet())
      assertEquals(setOf("c1", "c2"), inputs[InputKey.COMMENT_IDS]!!.toSet())
    }
  }

  @Test
  fun invoke_whenReposThrow_passesEmptyLists() {
    mainDispatcherRule.runTest {
      coEvery { userAchievementsRepository.initializeUserAchievements(userId) } returns Unit
      coEvery { postsRepository.getAllPostsByGivenAuthor(userId) } throws
          RuntimeException("boom-posts")
      coEvery { likeRepository.getAllLikesByUser(userId) } throws RuntimeException("boom-likes")
      coEvery { commentRepository.getCommentByUser(userId) } throws
          RuntimeException("boom-comments")

      val inputsSlot = slot<Map<InputKey, List<String>>>()
      coEvery {
        userAchievementsRepository.updateUserAchievements(userId, capture(inputsSlot))
      } returns Unit

      useCase.invoke(userId)
      advanceUntilIdle()

      val inputs = inputsSlot.captured
      assertTrue(inputs[InputKey.POST_IDS]!!.isEmpty())
      assertTrue(inputs[InputKey.LIKE_IDS]!!.isEmpty())
      assertTrue(inputs[InputKey.COMMENT_IDS]!!.isEmpty())
    }
  }

  @Test
  fun invoke_whenNoData_passesEmptyLists() {
    mainDispatcherRule.runTest {
      coEvery { userAchievementsRepository.initializeUserAchievements(userId) } returns Unit
      coEvery { postsRepository.getAllPostsByGivenAuthor(userId) } returns emptyList()
      coEvery { likeRepository.getAllLikesByUser(userId) } returns emptyList()
      coEvery { commentRepository.getCommentByUser(userId) } returns emptyList()

      val inputsSlot = slot<Map<InputKey, List<String>>>()
      coEvery {
        userAchievementsRepository.updateUserAchievements(userId, capture(inputsSlot))
      } returns Unit

      useCase.invoke(userId)
      advanceUntilIdle()

      val inputs = inputsSlot.captured
      assertTrue(inputs[InputKey.POST_IDS]!!.isEmpty())
      assertTrue(inputs[InputKey.LIKE_IDS]!!.isEmpty())
      assertTrue(inputs[InputKey.COMMENT_IDS]!!.isEmpty())
    }
  }

  @Test
  fun invoke_callsInitializeBeforeUpdate() {
    mainDispatcherRule.runTest {
      coEvery { userAchievementsRepository.initializeUserAchievements(userId) } returns Unit
      coEvery { postsRepository.getAllPostsByGivenAuthor(userId) } returns emptyList()
      coEvery { likeRepository.getAllLikesByUser(userId) } returns emptyList()
      coEvery { commentRepository.getCommentByUser(userId) } returns emptyList()

      coEvery { userAchievementsRepository.updateUserAchievements(userId, any()) } returns Unit

      useCase.invoke(userId)
      advanceUntilIdle()

      coVerify(ordering = io.mockk.Ordering.SEQUENCE) {
        userAchievementsRepository.initializeUserAchievements(userId)
        userAchievementsRepository.updateUserAchievements(userId, any())
      }
    }
  }
}
