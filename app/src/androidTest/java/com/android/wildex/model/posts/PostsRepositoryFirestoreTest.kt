package com.android.wildex.model.posts

import android.util.Log
import com.android.wildex.model.social.PostsRepositoryFirestore
import com.android.wildex.model.utils.Location
import com.android.wildex.utils.FirestoreTest
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

const val POSTS_COLLECTION_PATH = "posts"

class PostsRepositoryFirestoreTest : FirestoreTest(POSTS_COLLECTION_PATH) {
  private var repository = PostsRepositoryFirestore(Firebase.firestore)

  @Before
  override fun setUp() {
    super.setUp()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  suspend fun getPostsCount(): Int {
    return super.getCount()
  }

  @Test
  fun canAddPostsToRepository() =
      runTest(timeout = 60.seconds) {
        repository.addPost(post3)
        val posts = repository.getAllPosts()
        Log.e("PostsRepositoryFirestoreTest", "Post Size: $posts.size")
        assertEquals(1, posts.size)
        val expectedPost = post3.copy(postId = "2", authorId = "author3")
        val storedPost =
            posts.first().copy(postId = expectedPost.postId, authorId = expectedPost.authorId)
        Log.e("PostsRepositoryFirestoreTest", "Expected: $expectedPost")
        Log.e("PostsRepositoryFirestoreTest", "Actual: $storedPost")
        assertEquals(expectedPost, storedPost)
      }

  @Test
  fun addPostWithExistingIdThrowsException() =
      runTest(timeout = 60.seconds) {
        repository.addPost(post1)
        var exceptionThrown = false
        try {
          repository.addPost(post1)
        } catch (e: IllegalArgumentException) {
          exceptionThrown = true
          assertEquals("A Post with postId '${post1.postId}' already exists.", e.message)
        }
        assert(exceptionThrown) { "Expected IllegalArgumentException was not thrown." }
      }

  @Test
  fun addPostWithTheCorrectId() =
      runTest(timeout = 60.seconds) {
        repository.addPost(post1)
        repository.addPost(post2)
        repository.addPost(post3)
        val posts = repository.getAllPosts()
        assertEquals(3, posts.size)
        val expectedPosts = setOf(post1, post2, post3)
        val storedPosts = posts.map { it.copy(postId = it.postId, authorId = it.authorId) }.toSet()
        assertEquals(expectedPosts, storedPosts)
      }

  @Test
  fun canAddMultiplePostsToRepository() =
      runTest(timeout = 60.seconds) {
        repository.addPost(post1)
        repository.addPost(post2)
        repository.addPost(post3)
        val posts = repository.getAllPosts()
        assertEquals(3, posts.size)
        val expectedPosts = setOf(post1, post2, post3)
        val storedPosts = posts.map { it.copy(postId = it.postId, authorId = it.authorId) }.toSet()
        assertEquals(expectedPosts, storedPosts)
      }

  @Test
  fun postIdIsUniqueInTheCollection() =
      runTest(timeout = 60.seconds) {
        val postId = "duplicateId"
        val post1Modified = post1.copy(postId = postId)
        val post2WithSameId = post2.copy(postId = postId)

        repository.addPost(post1Modified)
        val exception = runCatching { repository.addPost(post2WithSameId) }.exceptionOrNull()

        assertTrue(exception is IllegalArgumentException)
        assertEquals("A Post with postId '${postId}' already exists.", exception?.message)

        val posts = repository.getAllPosts()
        assertEquals(1, posts.size)

        val storedPost = posts.first()
        assertEquals(storedPost.postId, postId)
      }

  @Test
  fun getNewPostIdReturnsUniqueIDs() =
      runTest(timeout = 60.seconds) {
        val numberIDs = 100
        val postIds = (0 until numberIDs).toSet<Int>().map { repository.getNewPostId() }.toSet()
        assertEquals(postIds.size, numberIDs)
      }

  @Test
  fun canDeleteAPostByID() =
      runTest(timeout = 60.seconds) {
        repository.addPost(post1)
        repository.addPost(post2)
        repository.addPost(post3)

        repository.deletePost(post2.postId)
        assertEquals(2, getPostsCount())
        val posts = repository.getAllPosts()
        assertEquals(posts.size, 2)

        val expectedPosts = setOf(post1, post3)
        val storedPosts = posts.map { it.copy(postId = it.postId, authorId = it.authorId) }.toSet()
        assertEquals(expectedPosts, storedPosts)
      }

  @Test
  fun deleteNonExistentPostThrowsException() =
      runTest(timeout = 60.seconds) {
        val exception = runCatching { repository.deletePost("nonExistentId") }.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertEquals("Post with given Id not found", exception?.message)
      }

  @Test
  fun canEditAPostByID() =
      runTest(timeout = 60.seconds) {
        repository.addPost(post1)
        assertEquals(1, getPostsCount())
        val posts = repository.getAllPosts()
        assertEquals(1, posts.size)

        val modifiedPost =
            post1.copy(
                pictureURL = "Modified PictureURL",
                location = Location(1.0, 1.0),
                description = "Modified Description",
                date = Timestamp.Companion.fromDate(2026, Calendar.JANUARY, 1),
                animalId = "modifiedAnimalId",
                likesCount = 20,
                commentsCount = 10,
            )
        repository.editPost(post1.postId, modifiedPost)
        assertEquals(1, getPostsCount())
        val postsAfterEdit = repository.getAllPosts()
        assertEquals(postsAfterEdit.size, 1)
        val storedPost = postsAfterEdit.first()
        assertEquals(storedPost, modifiedPost)
      }

  @Test
  fun canEditTheCorrectPostByID() =
      runTest(timeout = 60.seconds) {
        repository.addPost(post1)
        repository.addPost(post2)
        repository.addPost(post3)

        assertEquals(3, getPostsCount())

        val posts = repository.getAllPosts()
        assertEquals(posts.size, 3)

        val modifiedPost =
            post1.copy(
                pictureURL = "Modified PictureURL",
                location = Location(1.0, 1.0),
                description = "Modified Description",
                date = Timestamp.Companion.fromDate(2026, Calendar.JANUARY, 1),
                animalId = "modifiedAnimalId",
                likesCount = 20,
                commentsCount = 10,
            )
        repository.editPost(post1.postId, modifiedPost)
        val postsAfterEdit = repository.getAllPosts()
        assertEquals(postsAfterEdit.size, 3)
        assertEquals(postsAfterEdit.find { it.postId == post1.postId }, modifiedPost)
        assertEquals(postsAfterEdit.find { it.postId == post2.postId }, post2)
        assertEquals(postsAfterEdit.find { it.postId == post3.postId }, post3)
      }

  @Test
  fun editNonExistentPostThrowsException() =
      runTest(timeout = 60.seconds) {
        val exception =
            runCatching { repository.editPost("nonExistentId", post2) }.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertEquals("Post with given Id not found", exception?.message)
      }

  @Test
  fun canRetrieveAPostByID() =
      runTest(timeout = 60.seconds) {
        repository.addPost(post1)
        repository.addPost(post2)
        repository.addPost(post3)

        val retrievedPost = repository.getPost(post2.postId)
        assertEquals(post2, retrievedPost)
      }

  @Test
  fun getPostWithNonExistentIdThrowsException() =
      runTest(timeout = 60.seconds) {
        repository.addPost(post1)
        repository.addPost(post2)
        repository.addPost(post3)

        val nonExistentId = "nonExistentId"
        var exceptionThrown = false
        try {
          repository.getPost(nonExistentId)
        } catch (e: IllegalArgumentException) {
          exceptionThrown = true
          assertEquals("Post with given Id not found", e.message)
        }
        assert(exceptionThrown) { "Expected IllegalArgumentException was not thrown." }
      }

  @Test
  fun testGetAllPostsByAuthor() =
      runTest(timeout = 60.seconds) {
        Firebase.auth.signInAnonymously().await()

        val currentUserId =
            Firebase.auth.currentUser?.uid ?: throw Exception("No current user found")

        val post1 = post1.copy(authorId = currentUserId)
        val post2 = post2.copy(authorId = "author2")
        val post3 = post3.copy(authorId = currentUserId)

        repository.addPost(post1)
        repository.addPost(post2)
        repository.addPost(post3)

        val postsByAuthor = repository.getAllPostsByAuthor()

        assertEquals(2, postsByAuthor.size)
        assertTrue(postsByAuthor.all { it.authorId == currentUserId })
      }

  @Test
  fun testGetAllPostsWhenNoPostsExist() =
      runTest(timeout = 60.seconds) {
        val posts = repository.getAllPosts()
        assertTrue(posts.isEmpty())
      }

  @Test
  fun testGetAllPostsByAuthorWhenNoPostsExist() =
      runTest(timeout = 60.seconds) {
        Firebase.auth.signInAnonymously().await()
        val postsByAuthor = repository.getAllPostsByAuthor()
        assertTrue(postsByAuthor.isEmpty())
      }

  @Test
  fun testGetAllPostsByAuthorWhenNoPostsByAuthorExist() =
      runTest(timeout = 60.seconds) {
        Firebase.auth.signInAnonymously().await()
        val currentUserId =
            Firebase.auth.currentUser?.uid ?: throw Exception("No current user found")
        val post1 = post1.copy(authorId = "author1")
        val post2 = post2.copy(authorId = "author2")
        repository.addPost(post1)
        repository.addPost(post2)
        val postsByAuthor = repository.getAllPostsByAuthor()
        assertTrue(postsByAuthor.isEmpty())
      }

  @Test
  fun canCreatePostWithEmptyDescription() =
      runTest(timeout = 60.seconds) {
        val postWithEmptyDescription = post1.copy(description = "")
        repository.addPost(postWithEmptyDescription)
        val posts = repository.getAllPosts()
        assertEquals(1, posts.size)
        val storedPost = posts.first()
        assertEquals(postWithEmptyDescription.copy(postId = storedPost.postId), storedPost)
      }

  @Test
  fun getAllPostsByGivenAuthorWhenNoPostsExist() =
      runTest(timeout = 60.seconds) {
        val authorId = "nonExistentAuthor"
        val posts = repository.getAllPostsByGivenAuthor(authorId)
        assertTrue(posts.isEmpty())
      }

  @Test
  fun getAllPostsByGivenAuthorWhenPostsExist() =
      runTest(timeout = 60.seconds) {
        val authorId = "author1"
        val post1 = post1.copy(authorId = authorId)
        val post2 = post2.copy(authorId = authorId)

        repository.addPost(post1)
        repository.addPost(post2)

        val posts = repository.getAllPostsByGivenAuthor(authorId)
        assertEquals(2, posts.size)
        assertTrue(posts.all { it.authorId == authorId })
      }

  @Test
  fun getPostWithNonExistentGivenIdThrowsException() =
      runTest(timeout = 60.seconds) {
        val exception = runCatching { repository.getPost("nonExistentId") }.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertEquals("Post with given Id not found", exception?.message)
      }
}
