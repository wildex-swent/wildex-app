package com.android.wildex.model.posts

import android.util.Log
import com.android.wildex.model.social.LikeRepositoryFirestore
import com.android.wildex.utils.FirestoreTest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

const val LIKE_COLLECTION_PATH = "likes"

class LikeRepositoryFirestoreTest : FirestoreTest(LIKE_COLLECTION_PATH) {
  private var repository = LikeRepositoryFirestore(Firebase.firestore)

  @Before
  override fun setUp() {
    super.setUp()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  suspend fun getLikesCount(): Int {
    return super.getCount()
  }

  @Test
  fun canAddLikeToRepository() = runTest {
    repository.addLike(like1)
    val likes = repository.getLikesForPost(like1.postId)
    Log.e("PostsRepositoryFirestoreTest", "Post Size: $likes.size")
    assertEquals(1, likes.size)
    val storedLike = likes.first()
    Log.e("PostsRepositoryFirestoreTest", "Expected: $like1")
    Log.e("PostsRepositoryFirestoreTest", "Actual: $storedLike")
    assertEquals(like1, storedLike)
  }

  @Test
  fun addLikeWithExistingIdThrowsException() = runTest {
    repository.addLike(like1)
    var exceptionThrown = false
    try {
      repository.addLike(like1)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals("A Like with likeId '${like1.likeId}' already exists.", e.message)
    }
    assert(exceptionThrown) { "Expected IllegalArgumentException was not thrown." }
  }

  @Test
  fun canAddMultipleLikesToRepository() = runTest {
    repository.addLike(like1)
    repository.addLike(like3)
    val likes = repository.getLikesForPost(like1.postId)
    assertEquals(2, likes.size)
    val expectedLikes = setOf(like1, like3)
    val storedLikes = likes.toSet()
    assertEquals(expectedLikes, storedLikes)
  }

  @Test
  fun getNewLikeIdReturnsUniqueIDs() = runTest {
    val numberIDs = 100
    val likeIds = (0 until numberIDs).map { repository.getNewLikeId() }.toSet()
    assertEquals(likeIds.size, numberIDs)
  }

  @Test
  fun likeIdIsUniqueInTheCollection() = runTest {
    val likeId = "likeDuplicate"
    val like1Modified = like1.copy(likeId = likeId)
    val like3WithSameId = like3.copy(likeId = likeId)

    repository.addLike(like1Modified)
    val exception = runCatching { repository.addLike(like3WithSameId) }.exceptionOrNull()

    assertTrue(exception is IllegalArgumentException)
    assertEquals("A Like with likeId '${likeId}' already exists.", exception?.message)

    val likes = repository.getLikesForPost(like1.postId)
    assertEquals(1, likes.size)

    val storedLike = likes.first()
    assertEquals(storedLike.likeId, likeId)
  }

  @Test
  fun canDeleteALikeByID() = runTest {
    repository.addLike(like1)
    repository.addLike(like2)
    repository.addLike(like3)

    repository.deleteLike(like2.likeId)
    assertEquals(2, getLikesCount())

    val expectedLikes = setOf(like1, like3)
    val storedLikes = repository.getLikesForPost(like1.postId).toSet()
    assertEquals(expectedLikes, storedLikes)
  }

  @Test
  fun deleteNonExistentLikeThrowsException() = runTest {
    val exception = runCatching { repository.deleteLike("nonExistentId") }.exceptionOrNull()
    assertTrue(exception is IllegalArgumentException)
    assertEquals("Like with given Id not found", exception?.message)
  }

  @Test
  fun testGetAllLikesByCurrentUser() = runTest {
    Firebase.auth.signInAnonymously().await()

    val currentUserId = Firebase.auth.currentUser?.uid ?: throw Exception("No current user found")

    val like1 = like1.copy(userId = currentUserId)
    val like2 = like2.copy(userId = "author2")
    val like3 = like3.copy(userId = currentUserId)

    repository.addLike(like1)
    repository.addLike(like2)
    repository.addLike(like3)

    val likesByCurrUser = repository.getAllLikesByCurrentUser()

    assertEquals(2, likesByCurrUser.size)
    assertTrue(likesByCurrUser.all { it.userId == currentUserId })
  }

  @Test
  fun testGetLikesForPostWhenNoLikeExists() = runTest {
    val likes = repository.getLikesForPost("nonExistentPostId")
    assertTrue(likes.isEmpty())
  }

  @Test
  fun testGetAllLikesByCurrentUserWhenNoLikesExist() = runTest {
    Firebase.auth.signInAnonymously().await()
    val likesByCurrUser = repository.getAllLikesByCurrentUser()
    assertTrue(likesByCurrUser.isEmpty())
  }

  @Test
  fun testGetCurrentUserLikeForPostWhenNoLikeExistsForPost() = runTest {
    Firebase.auth.signInAnonymously().await()
    val like = repository.getLikeForPost("nonExistentPostId")
    assertEquals(null, like)
  }

  @Test
  fun testGetAllLikesByUser() = runTest {
    val userId = "author1"
    val like1ForUser = like1.copy(userId = userId)
    val like2ForUser = like2.copy(userId = userId)
    val like3ForOtherUser = like3.copy(userId = "author2")

    repository.addLike(like1ForUser)
    repository.addLike(like2ForUser)
    repository.addLike(like3ForOtherUser)

    val likesByUser = repository.getAllLikesByUser(userId)

    assertEquals(2, likesByUser.size)
    val expectedLikes = setOf(like1ForUser, like2ForUser)
    val storedLikes = likesByUser.toSet()
    assertEquals(expectedLikes, storedLikes)
  }
}
