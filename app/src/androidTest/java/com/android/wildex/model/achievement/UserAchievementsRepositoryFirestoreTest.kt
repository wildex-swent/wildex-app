package com.android.wildex.model.achievement

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.utils.FirebaseEmulator
import com.android.wildex.utils.FirestoreTest
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class UserAchievementsRepositoryFirestoreTest : FirestoreTest(USER_ACHIEVEMENTS_COLLECTION_PATH) {

  private var repository = UserAchievementsRepositoryFirestore(FirebaseEmulator.firestore)
  private val testUserId = "testUserId"

  private suspend fun getUsersCount(): Int = super.getCount()

  @Before
  override fun setUp() {
    super.setUp()
    runBlocking {
      RepositoryProvider.userRepository.addUser(
          User(
              userId = testUserId,
              username = "",
              name = "",
              surname = "",
              bio = "",
              profilePictureURL = "",
              userType = UserType.REGULAR,
              creationDate = Timestamp.now(),
              country = "",
          ))
      RepositoryProvider.userAnimalsRepository.initializeUserAnimals(testUserId)
      RepositoryProvider.userSettingsRepository.initializeUserSettings(testUserId)
    }
  }

  @Test
  fun canInitializeUserAchievements() = runTest {
    repository.initializeUserAchievements(testUserId)
    val count = getUsersCount()
    assertEquals(1, count)
  }

  @Test
  fun canInitializeUserAchievementsWhenUserExists() = runTest {
    repository.initializeUserAchievements(testUserId)
    repository.initializeUserAchievements(testUserId)
    val count = getUsersCount()
    assertEquals(1, count)
  }

  @Test
  fun canRetrieveUserAchievements() = runTest {
    repository.initializeUserAchievements(testUserId)
    RepositoryProvider.postRepository.addPost(
        Post(
            postId = "post1",
            authorId = testUserId,
            pictureURL = "",
            location = null,
            description = "",
            date = Timestamp.now(),
            animalId = "",
            likesCount = 0,
            commentsCount = 0,
        )) // For the firstPost achievement
    repository.updateUserAchievements(testUserId)

    val achievements = repository.getAllAchievementsByUser(testUserId)
    assertTrue(achievements.isNotEmpty())
    assertEquals(Achievements.ALL.find { it.achievementId == "achievement_5" }, achievements[0])
  }

  @Test
  fun canRetrieveAchievementsCount() = runTest {
    repository.initializeUserAchievements(testUserId)

    RepositoryProvider.postRepository.addPost(
        Post(
            postId = "post1",
            authorId = testUserId,
            pictureURL = "",
            location = null,
            description = "",
            date = Timestamp.now(),
            animalId = "",
            likesCount = 0,
            commentsCount = 0,
        )) // For the firstPost achievement
    repository.updateUserAchievements(testUserId)

    val count = repository.getAchievementsCountOfUser(testUserId)
    assertEquals(1, count)
  }

  @Test
  fun updateUserAchievementsWhenNoChangesDoesNotUpdate() = runTest {

    // Setup
    repository.initializeUserAchievements(testUserId)
    RepositoryProvider.postRepository.addPost(
        Post(
            postId = "post1",
            authorId = testUserId,
            pictureURL = "",
            location = null,
            description = "",
            date = Timestamp.now(),
            animalId = "",
            likesCount = 0,
            commentsCount = 0,
        )) // For the firstPost achievement
    repository.updateUserAchievements(testUserId)

    val initialAchievements = repository.getAllAchievementsByUser(testUserId)
    repository.updateUserAchievements(testUserId)

    val updatedAchievements = repository.getAllAchievementsByUser(testUserId)
    assertEquals(initialAchievements, updatedAchievements)
  }

  @Test
  fun getAllAchievementsByUserWhenEmptyAchievementsReturnsEmptyList() = runTest {
    repository.initializeUserAchievements(testUserId)
    val achievements = repository.getAllAchievementsByUser(testUserId)
    assertTrue(achievements.isEmpty())
  }

  @Test
  fun updateUserAchievementsWhenUserNotFoundThrowsException() = runTest {
    val userId = "nonExistentUser"

    val exception = runCatching { repository.updateUserAchievements(userId) }.exceptionOrNull()

    assertTrue(exception is IllegalArgumentException)
  }

  @Test
  fun initializeUserAchievementsWithInvalidUserIdThrowsException() = runTest {
    val userId = ""
    val exception = runCatching { repository.initializeUserAchievements(userId) }.exceptionOrNull()
    assertTrue(exception is IllegalArgumentException)
  }

  @Test
  fun getAchievementsCountWhenUserNotInitializedThrowsException() = runTest {
    val userId = "nonExistentUser"
    val exception = runCatching { repository.getAchievementsCountOfUser(userId) }.exceptionOrNull()
    assertTrue(exception is IllegalArgumentException)
  }

  @Test
  fun getAllAchievementsByCurrentUserWhenNotLoggedInThrowsException() = runTest {
    val exception = runCatching { repository.getAllAchievementsByCurrentUser() }.exceptionOrNull()
    assertTrue(exception is Exception)
    assertEquals("UserAchievementsRepositoryFirestore: User not logged in.", exception?.message)
  }

  @Test
  fun getAllAchievementsByCurrentUserWhenLoggedIn() = runTest {
    Firebase.auth.signInAnonymously().await()
    repository.initializeUserAchievements(Firebase.auth.currentUser!!.uid)
    val achievements = repository.getAllAchievementsByCurrentUser()
    assertTrue(achievements.isEmpty())
  }

  @Test
  fun updateUserAchievementsWhenUserNotFoundThrowsIllegalArgumentException() = runTest {
    val userId = "nonExistentUser"
    val exception = runCatching { repository.updateUserAchievements(userId) }.exceptionOrNull()
    assertTrue(exception is IllegalArgumentException)
  }

  @Test
  fun testMalformedUserAchievementDocument() = runTest {
    val userId = "testEmptyListUser"

    // create an empty invalid document (so .exists() is true but no valid fields)
    FirebaseEmulator.firestore
        .collection(USER_ACHIEVEMENTS_COLLECTION_PATH)
        .document(userId)
        .set(mapOf<Int, Any>())
        .await()

    // call the method to this will trigger the `?: emptyList()` branch
    val achievements = repository.getAllAchievementsByUser(userId)

    assertTrue(achievements.isEmpty())
    assertEquals(0, repository.getAchievementsCountOfUser(userId))
  }

  @Test
  fun deleteUserAchievementsWhenNoUserExists() = runTest {
    var exceptionThrown = false

    try {
      repository.deleteUserAchievements(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userAchievements with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun deleteAccountWhenUserExists() = runTest {
    var exceptionThrown = false
    repository.initializeUserAchievements(user1.userId)

    repository.deleteUserAchievements(user1.userId)
    try {
      repository.deleteUserAchievements(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals("A userAchievements with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }
}
