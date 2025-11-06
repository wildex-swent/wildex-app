package com.android.wildex.utils

import android.util.Log
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.report.Report
import com.android.wildex.model.report.ReportStatus
import com.android.wildex.model.social.Comment
import com.android.wildex.model.social.CommentTag
import com.android.wildex.model.social.Like
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
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

  init {
    assert(FirebaseEmulator.isRunning) {
      "FirebaseEmulator must be running before using FirestoreTest"
    }
  }

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
          pictureURL =
              "https://t4.ftcdn.net/jpg/04/15/79/09/360_F_415790935_7va5lMHOmyhvAcdskXbSx7lDJUp0cfja.jpg",
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
          pictureURL =
              "https://t4.ftcdn.net/jpg/04/15/79/09/360_F_415790935_7va5lMHOmyhvAcdskXbSx7lDJUp0cfja.jpg",
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
          pictureURL =
              "https://t4.ftcdn.net/jpg/04/15/79/09/360_F_415790935_7va5lMHOmyhvAcdskXbSx7lDJUp0cfja.jpg",
          location = Location(0.3, 0.3),
          description = "Description 3",
          date = Timestamp.Companion.fromDate(2024, Calendar.SEPTEMBER, 8),
          animalId = "animal3",
          likesCount = 10,
          commentsCount = 5,
      )

  open val user1 =
      User(
          userId = "user1",
          username = "user_one",
          name = "First",
          surname = "User",
          bio = "Bio 1",
          profilePictureURL = "url1",
          userType = UserType.REGULAR,
          creationDate = Timestamp.Companion.fromDate(2024, Calendar.JANUARY, 1),
          country = "Country1",
          friendsCount = 2,
      )

  open val user2 =
      User(
          userId = "user2",
          username = "user_two",
          name = "Second",
          surname = "User",
          bio = "Bio 2",
          profilePictureURL = "url2",
          userType = UserType.REGULAR,
          creationDate = Timestamp.Companion.fromDate(2025, Calendar.FEBRUARY, 2),
          country = "Country2",
          friendsCount = 3,
      )

  open val user3 =
      User(
          userId = "user3",
          username = "user_three",
          name = "Third",
          surname = "User",
          bio = "Bio 3",
          profilePictureURL = "url3",
          userType = UserType.REGULAR,
          creationDate = Timestamp.Companion.fromDate(2023, Calendar.MARCH, 3),
          country = "Country3",
          friendsCount = 0,
      )

  open val like1 = Like("like1", postId = post1.postId, userId = user1.userId)

  open val like2 = Like("like2", postId = post2.postId, userId = user2.userId)

  open val like3 = Like("like3", postId = post1.postId, userId = user3.userId)

  open val comment1 =
      Comment(
          commentId = "comment1",
          postId = "post1",
          authorId = "author1",
          text = "text1",
          date = Timestamp.fromDate(2003, 11, 21),
          tag = CommentTag.POST_COMMENT)

  open val comment2 =
      Comment(
          commentId = "comment2",
          postId = "post2",
          authorId = "author2",
          text = "text2",
          date = Timestamp.fromDate(2012, 12, 12),
          tag = CommentTag.REPORT_COMMENT)

  open val animal1 =
      Animal(
          animalId = "animalId1",
          pictureURL = "pictureURL1",
          name = "animalName1",
          species = "animalType1",
          description = "animalDescription1",
      )

  open val animal2 =
      Animal(
          animalId = "animalId2",
          pictureURL = "pictureURL2",
          name = "animalName2",
          species = "animalType2",
          description = "animalDescription2",
      )

  open val report1 =
      Report(
          reportId = "reportId1",
          imageURL = "imageURL1",
          location = Location(0.3, 0.3),
          date = Timestamp.fromDate(2017, 4, 29),
          description = "description1",
          authorId = "authorId1",
          assigneeId = "assigneeId1",
          status = ReportStatus.PENDING)

  open val report2 =
      Report(
          reportId = "reportId2",
          imageURL = "imageURL2",
          location = Location(0.8, 0.8),
          date = Timestamp.now(),
          description = "description2",
          authorId = "authorId2",
          assigneeId = "assigneeId2",
          status = ReportStatus.RESOLVED)
}
