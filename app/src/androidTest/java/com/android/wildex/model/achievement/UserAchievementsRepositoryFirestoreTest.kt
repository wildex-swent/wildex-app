package com.android.wildex.model.achievement

import com.android.wildex.utils.FirebaseEmulator
import com.android.wildex.utils.FirestoreTest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class UserAchievementsRepositoryFirestoreTest : FirestoreTest(USER_ACHIEVEMENTS_COLLECTION_PATH) {

  private var repository = UserAchievementsRepositoryFirestore(FirebaseEmulator.firestore)

  @Before
  override fun setUp() {
    super.setUp()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  private suspend fun getUsersCount(): Int = super.getCount()

  @Test
  fun canInitializeUserAchievements() = runTest {
    val userId = "testUserId"
    repository.initializeUserAchievements(userId)
    val count = getUsersCount()
    assertEquals(1, count)
  }

  @Test
  fun canInitializeUserAchievementsWhenUserExists() = runTest {
    val userId = "testUserId"
    repository.initializeUserAchievements(userId)
    repository.initializeUserAchievements(userId)
    val count = getUsersCount()
    assertEquals(1, count)
  }

  @Test
  fun canRetrieveUserAchievements() = runTest {
    val userId = "testUser"
    repository.initializeUserAchievements(userId)

    val listIds = listOf("mockPostId")
    repository.updateUserAchievements(userId, mapOf(InputKey.TEST_IDS to listIds))

    val achievements = repository.getAllAchievementsByUser(userId)
    assertTrue(achievements.isNotEmpty())
    assertEquals(Achievements.ALL.find { it.achievementId == "mockPostId" }, achievements[0])
  }

  @Test
  fun canRetrieveAchievementsCount() = runTest {
    val userId = "testUser"
    repository.initializeUserAchievements(userId)

    val listIds = listOf("mockPostId")
    repository.updateUserAchievements(userId, mapOf(InputKey.TEST_IDS to listIds))

    val count = repository.getAchievementsCountOfUser(userId)
    assertEquals(1, count)
  }

  @Test
  fun updateUserAchievementsWhenNoChangesDoesNotUpdate() = runTest {
    val userId = "testUser"
    repository.initializeUserAchievements(userId)

    val initialAchievements = repository.getAllAchievementsByUser(userId)

    repository.updateUserAchievements(
        userId, mapOf(InputKey.LIKE_IDS to listOf("RandomIdThatDoesNotExist")))

    val updatedAchievements = repository.getAllAchievementsByUser(userId)
    assertEquals(initialAchievements, updatedAchievements)
  }

  @Test
  fun getAllAchievementsByUserWhenUserNotInitializedThrowsException() =
      runTest(timeout = 60.seconds) {
        val userId = "nonExistentUser"
        val exception =
            runCatching { repository.getAllAchievementsByUser(userId) }.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
      }

  @Test
  fun getAllAchievementsByUserWhenEmptyAchievementsReturnsEmptyList() = runTest {
    val userId = "testUser"
    repository.initializeUserAchievements(userId)
    val achievements = repository.getAllAchievementsByUser(userId)
    assertTrue(achievements.isEmpty())
  }

  @Test
  fun updateUserAchievementsWhenUserNotFoundThrowsException() = runTest {
    val userId = "nonExistentUser"
    val listIds = listOf("mockPostId")

    val exception =
        runCatching {
              repository.updateUserAchievements(userId, mapOf(InputKey.POST_IDS to listIds))
            }
            .exceptionOrNull()

    assertTrue(exception is IllegalArgumentException)
  }

  @Test
  fun updateUserAchievementsWhenAchievementsConditionsMetUpdatesAchievements() = runTest {
    val userId = "testUser"
    repository.initializeUserAchievements(userId)

    val listIds = listOf("mockPostId")
    repository.updateUserAchievements(userId, mapOf(InputKey.TEST_IDS to listIds))

    val achievements = repository.getAllAchievementsByUser(userId)
    assertTrue(achievements.isNotEmpty())
    assertEquals(Achievements.ALL.find { it.achievementId == "mockPostId" }, achievements[0])

    val newListIds = listOf("mockPostId", "RandomId")
    repository.updateUserAchievements(userId, mapOf(InputKey.TEST_IDS to newListIds))

    val updatedAchievements = repository.getAllAchievementsByUser(userId)
    assertEquals(1, updatedAchievements.size)
    assertFalse(updatedAchievements.any { it.achievementId == "mockPostId" })
    assertTrue(updatedAchievements.any { it.achievementId == "mockPostId2" })
  }

  @Test
  fun updateUserAchievementsWhenEmptyListPassesWithoutError() = runTest {
    val userId = "testUser"
    repository.initializeUserAchievements(userId)
    repository.updateUserAchievements(userId, mapOf(InputKey.POST_IDS to emptyList()))
    val achievements = repository.getAllAchievementsByUser(userId)
    assertTrue(achievements.isEmpty())
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
    val exception =
        runCatching {
              repository.updateUserAchievements(
                  userId, mapOf(InputKey.POST_IDS to listOf("mockPostId")))
            }
            .exceptionOrNull()
    assertTrue(exception is IllegalArgumentException)
  }
}
