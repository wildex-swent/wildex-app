package com.android.wildex.utils

import android.util.Log
import com.android.wildex.model.social.Post
import com.android.wildex.model.utils.Location
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.Calendar
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

open class FirestoreTest(val collectionPath: String) {

  @Before
  open fun setUp() {
    runTest {
      val count = getCount()
      if (count > 0) {
        Log.w(
            "FirebaseEmulatedTest",
            "Warning: Test collection is not empty at the beginning of the test, count: $count",
        )
        clearTestCollection()
      }
    }
  }

  open suspend fun getCount(): Int {
    return FirebaseEmulator.firestore.collection(collectionPath).get().await().size()
  }

  private suspend fun clearTestCollection() {
    val posts = FirebaseEmulator.firestore.collection(collectionPath).get().await()

    val batch = FirebaseEmulator.firestore.batch()
    posts.documents.forEach { batch.delete(it.reference) }
    batch.commit().await()

    assert(getCount() == 0) { "Test collection is not empty after clearing, count: ${getCount()}" }
  }

  fun Timestamp.Companion.fromDate(year: Int, month: Int, day: Int): Timestamp {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, day, 0, 0, 0)
    return Timestamp(calendar.time)
  }

  @After
  open fun tearDown() {
    runTest { clearTestCollection() }
    FirebaseEmulator.clearFirestoreEmulator()
    if (Firebase.auth.currentUser != null) {
      Firebase.auth.signOut()
    }
  }

  // --------------------Sample Data--------------------

  open val post1 =
      Post(
          postId = "0",
          authorId = "author1",
          pictureURL = "AuthorOne",
          location = Location(0.0, 0.0),
          description = "Description 1",
          date = Timestamp.Companion.fromDate(2025, Calendar.SEPTEMBER, 1),
          animalId = "animal1",
          likesCount = 10,
          commentsCount = 5,
      )

  open val post2 =
      Post(
          postId = "1",
          authorId = "author2",
          pictureURL = "AuthorTwo",
          location = Location(0.1, 0.3),
          description = "Description 2",
          date = Timestamp.Companion.fromDate(2035, Calendar.SEPTEMBER, 4),
          animalId = "animal2",
          likesCount = 10,
          commentsCount = 5,
      )

  open val post3 =
      Post(
          postId = "2",
          authorId = "author3",
          pictureURL = "AuthorThree",
          location = Location(0.3, 0.3),
          description = "Description 3",
          date = Timestamp.Companion.fromDate(2024, Calendar.SEPTEMBER, 8),
          animalId = "animal3",
          likesCount = 10,
          commentsCount = 5,
      )
}
