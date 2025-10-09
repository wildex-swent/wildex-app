package com.android.wildex.model.posts

import android.util.Log
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepositoryFirestore
import com.android.wildex.model.utils.Location
import com.android.wildex.utils.FirebaseEmulator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

const val POSTS_COLLECTION_PATH = "posts"

class PostsRepositoryFirestoreTest {
  private var repository = PostsRepositoryFirestore(Firebase.firestore)

  private val post1 =
      Post(
          postId = "post1",
          authorId = "author1",
          pictureURL = "AuthorOne",
          location = Location(0.0, 0.0),
          description = "Description 1",
          date = Timestamp.now(),
          animalId = "animal1",
          likesCount = 10,
          commentsCount = 5,
      )

  private val post2 =
      Post(
          postId = "post2",
          authorId = "author2",
          pictureURL = "AuthorTwo",
          location = Location(0.1, 0.3),
          description = "Description 2",
          date = Timestamp.now(),
          animalId = "animal2",
          likesCount = 10,
          commentsCount = 5,
      )

  private val post3 =
      Post(
          postId = "post3",
          authorId = "author3",
          pictureURL = "AuthorThree",
          location = Location(0.3, 0.3),
          description = "Description 3",
          date = Timestamp.now(),
          animalId = "animal3",
          likesCount = 10,
          commentsCount = 5,
      )

  @Before
  fun setUp() {
    repository = PostsRepositoryFirestore(Firebase.firestore)
    runTest {
      val todosCount = getPostsCount()
      if (todosCount > 0) {
        Log.w(
            "FirebaseEmulatedTest",
            "Warning: Test collection is not empty at the beginning of the test, count: $todosCount",
        )
        clearTestCollection()
      }
    }
    runTest { FirebaseEmulator.auth.signInAnonymously().await() }
  }

  suspend fun getPostsCount(): Int {
    val user = FirebaseEmulator.auth.currentUser ?: return 0
    return FirebaseEmulator.firestore
        .collection(POSTS_COLLECTION_PATH)
        .whereEqualTo("authorId", user.uid)
        .get()
        .await()
        .size()
  }

  private suspend fun clearTestCollection() {
    val user = FirebaseEmulator.auth.currentUser ?: return
    val todos =
        FirebaseEmulator.firestore
            .collection(POSTS_COLLECTION_PATH)
            .whereEqualTo("authorId", user.uid)
            .get()
            .await()

    val batch = FirebaseEmulator.firestore.batch()
    todos.documents.forEach { batch.delete(it.reference) }
    batch.commit().await()

    assert(getPostsCount() == 0) {
      "Test collection is not empty after clearing, count: ${getPostsCount()}"
    }
  }

  @Test
  fun canAddToDosToRepository() = runTest {
    repository.addPost(post1)
    assertEquals(1, getPostsCount())
    val posts = repository.getAllPosts()

    assertEquals(1, posts.size)
    val expectedPost = post1.copy(postId = "None", authorId = "None")
    val storedPost =
        posts.first().copy(postId = expectedPost.postId, authorId = expectedPost.authorId)

    assertEquals(expectedPost, storedPost)
  }

  @After
  fun tearDown() {
    runTest { clearTestCollection() }
    FirebaseEmulator.clearFirestoreEmulator()
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.auth.signOut()
      FirebaseEmulator.clearAuthEmulator()
    }
  }
}
