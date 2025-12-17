package com.android.wildex.model.user

import android.util.Log
import com.android.wildex.utils.FirebaseEmulator
import com.android.wildex.utils.FirestoreTest
import com.android.wildex.utils.offline.FakeUserCache
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

const val USERS_COLLECTION_PATH = "users"

class UserRepositoryFirestoreTest : FirestoreTest(USERS_COLLECTION_PATH) {
  private val userCache = FakeUserCache()
  private var repository = UserRepositoryFirestore(Firebase.firestore, userCache)

  @Before
  override fun setUp() {
    super.setUp()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  suspend fun getUsersCount(): Int {
    return super.getCount()
  }

  @Test
  fun canAddUserToRepository() = runTest {
    repository.addUser(user1)
    val retrieved = repository.getUser(user1.userId)
    val expected = user1.copy(userId = user1.userId)
    Log.e("UserRepositoryFirestoreTest", "Expected: $expected")
    Log.e("UserRepositoryFirestoreTest", "Actual: $retrieved")
    assertEquals(expected, retrieved)
  }

  @Test
  fun addUserWithExistingIdThrowsException() = runTest {
    repository.addUser(user1)
    var exceptionThrown = false
    try {
      repository.addUser(user1)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals(
          "UserRepositoryFirestore: A User with userId '${user1.userId}' already exists.",
          e.message,
      )
    }
    assert(exceptionThrown) { "Expected IllegalArgumentException was not thrown." }
  }

  @Test
  fun addMultipleUsersAndRetrieveThem() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)
    repository.addUser(user3)

    val retrieved1 = repository.getUser(user1.userId)
    val retrieved2 = repository.getUser(user2.userId)
    val retrieved3 = repository.getUser(user3.userId)

    assertEquals(user1, retrieved1)
    assertEquals(user2, retrieved2)
    assertEquals(user3, retrieved3)
    assertEquals(3, getUsersCount())
  }

  @Test
  fun userIdIsUniqueInTheCollection() = runTest {
    val duplicateId = "duplicateUser"
    val user1 = user1.copy(userId = duplicateId)
    val user2 = user2.copy(userId = duplicateId)

    repository.addUser(user1)
    val exception = runCatching { repository.addUser(user2) }.exceptionOrNull()

    assertTrue(exception is IllegalArgumentException)
    assertEquals(
        "UserRepositoryFirestore: A User with userId '${duplicateId}' already exists.",
        exception?.message,
    )

    assertEquals(1, getUsersCount())
    val stored = repository.getUser(duplicateId)
    assertEquals(stored.userId, duplicateId)
  }

  @Test
  fun canDeleteAUserByID() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)
    repository.addUser(user3)

    repository.deleteUser(user2.userId)
    assertEquals(2, getUsersCount())

    val retrieved1 = repository.getUser(user1.userId)
    val retrieved3 = repository.getUser(user3.userId)
    assertEquals(user1, retrieved1)
    assertEquals(user3, retrieved3)
  }

  @Test
  fun deleteNonExistentUserThrowsException() = runTest {
    val exception = runCatching { repository.deleteUser("nonExistentId") }.exceptionOrNull()
    assertTrue(exception is IllegalArgumentException)
    assertEquals("UserRepositoryFirestore: User nonExistentId not found", exception?.message)
  }

  @Test
  fun getAllUsersReturnsAllAddedUsers() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)
    repository.addUser(user3)

    val allUsers = repository.getAllUsers()
    assertEquals(3, allUsers.size)
    assertTrue(allUsers.containsAll(listOf(user1, user2, user3)))
  }

  @Test
  fun getAllUsersWhenNoUsersReturnsEmptyList() = runTest {
    val allUsers = repository.getAllUsers()
    assertTrue(allUsers.isEmpty())
  }

  @Test
  fun canEditAUserByID() = runTest {
    repository.addUser(user1)
    assertEquals(1, getUsersCount())

    val modified =
        user1.copy(
            username = "modified_username",
            name = "Modified",
            surname = "Name",
            bio = "Modified bio",
            profilePictureURL = "modifiedUrl",
            country = "ModifiedCountry",
        )

    repository.editUser(user1.userId, modified)
    assertEquals(1, getUsersCount())
    val stored = repository.getUser(user1.userId)
    assertEquals(modified, stored)
  }

  @Test
  fun editNonExistentUserThrowsException() = runTest {
    val exception = runCatching { repository.editUser("nonExistentId", user2) }.exceptionOrNull()
    assertTrue(exception is IllegalArgumentException)
    assertEquals("UserRepositoryFirestore: User nonExistentId not found", exception?.message)
  }

  @Test
  fun canRetrieveAUserByID() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)
    repository.addUser(user3)

    val retrieved = repository.getUser(user2.userId)
    assertEquals(user2, retrieved)
  }

  @Test
  fun getUserWithNonExistentIdThrowsException() = runTest {
    repository.addUser(user1)
    repository.addUser(user2)

    var exceptionThrown = false
    try {
      repository.getUser("nonExistentId")
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals("UserRepositoryFirestore: User nonExistentId not found", e.message)
    }
    assert(exceptionThrown) { "Expected IllegalArgumentException was not thrown." }
  }

  @Test
  fun canRetrieveSimpleUserByID() = runTest {
    repository.addUser(user1)
    val simple = repository.getSimpleUser(user1.userId)
    val expectedSimple =
        SimpleUser(
            userId = user1.userId,
            username = user1.username,
            profilePictureURL = user1.profilePictureURL,
            userType = user1.userType,
        )
    assertEquals(expectedSimple, simple)
  }

  @Test
  fun getSimpleUserWithNonExistentIdThrowsException() = runTest {
    repository.addUser(user1)
    var exceptionThrown = false
    try {
      repository.getSimpleUser("nonExistentId")
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals("UserRepositoryFirestore: User nonExistentId not found", e.message)
    }
    assert(exceptionThrown) { "Expected IllegalArgumentException was not thrown." }
  }

  @Test
  fun getUser_whenDocumentMalformed_throwsGenericException() = runTest {
    val badId = "malformedUser"
    FirebaseEmulator.firestore
        .collection(USERS_COLLECTION_PATH)
        .document(badId)
        .set(mapOf("name" to "no-username"))
        .await()

    val exception = runCatching { repository.getUser(badId) }.exceptionOrNull()
    assertTrue(exception is Exception && exception !is IllegalArgumentException)
    assertEquals("UserRepositoryFirestore: User $badId not found", exception?.message)
  }

  @Test
  fun getSimpleUser_whenDocumentMalformed_throwsGenericException() = runTest {
    val badId = "malformedSimpleUser"
    FirebaseEmulator.firestore
        .collection(USERS_COLLECTION_PATH)
        .document(badId)
        .set(mapOf("profilePictureURL" to "no-username"))
        .await()

    val exception = runCatching { repository.getSimpleUser(badId) }.exceptionOrNull()
    assertTrue(exception is Exception && exception !is IllegalArgumentException)
    assertEquals("UserRepositoryFirestore: User $badId not found", exception?.message)
  }

  @Test
  fun getUser_withInvalidUserType_fallsBackToRegular() = runTest {
    val id = "invalidUserType"
    val creationTs = fromDate(2024, Calendar.JANUARY, 1)
    FirebaseEmulator.firestore
        .collection(USERS_COLLECTION_PATH)
        .document(id)
        .set(
            mapOf(
                "username" to "u", "creationDate" to creationTs, "userType" to "NOT_A_VALID_TYPE"))
        .await()

    val u = repository.getUser(id)
    assertEquals(UserType.REGULAR, u.userType)
  }

  @Test
  fun getUserUsesCacheAfterFirstFetch() {
    runTest {
      repository.addUser(user1)
      assertEquals(user1, repository.getUser(user1.userId))
      assertEquals(user1, userCache.getUser(user1.userId))

      FirebaseEmulator.firestore
          .collection(USERS_COLLECTION_PATH)
          .document(user1.userId)
          .update("username", "tampered")
          .await()

      val retrieved = repository.getUser(user1.userId)
      assertEquals(user1.username, retrieved.username)
    }
  }

  @Test
  fun getAllUsersUsesCacheAfterFetch() {
    runTest {
      repository.addUser(user1)
      repository.addUser(user2)
      repository.addUser(user3)
      val users = listOf(user1, user2, user3)
      val retrieved = repository.getAllUsers()
      assertEquals(users, retrieved)
      assertEquals(users, userCache.getAllUsers())

      retrieved.forEach { user ->
        FirebaseEmulator.firestore
            .collection(USERS_COLLECTION_PATH)
            .document(user.userId)
            .update("username", "tampered")
            .await()
      }

      assertEquals(users, repository.getAllUsers())
    }
  }

  @Test
  fun getAllUsersSavesAllToCache() {
    runTest {
      repository.addUser(user1)
      repository.addUser(user2)
      repository.addUser(user3)

      FirebaseEmulator.firestore
          .collection(USERS_COLLECTION_PATH)
          .document(user2.userId)
          .update("username", "tampered")
          .await()

      val cached = userCache.getUser(user2.userId)
      assertEquals(user2.username, cached!!.username)
    }
  }

  @Test
  fun getSimpleUserUsesCachedFullUser() {
    runTest {
      repository.addUser(user1)
      assertEquals(user1, userCache.getUser(user1.userId))

      FirebaseEmulator.firestore
          .collection(USERS_COLLECTION_PATH)
          .document(user1.userId)
          .update("username", "tampered")
          .await()

      val simpleUser =
          SimpleUser(
              userId = user1.userId,
              username = user1.username,
              profilePictureURL = user1.profilePictureURL,
              userType = user1.userType)
      assertEquals(simpleUser, repository.getSimpleUser(user1.userId))
    }
  }

  @Test
  fun editUserUpdatesCache() {
    runTest {
      repository.addUser(user1)

      val newUser = user1.copy(username = "newUsername", bio = "new bio")

      repository.editUser(user1.userId, newUser)
      val cached = userCache.getUser(user1.userId)
      assertEquals(newUser, cached)
    }
  }

  @Test
  fun deleteUserClearsCacheEntry() {
    runTest {
      repository.addUser(user1)
      assertEquals(user1, userCache.getUser(user1.userId))

      repository.deleteUser(user1.userId)
      assertNull(userCache.getUser(user1.userId))
      val exception = runCatching { repository.getUser(user1.userId) }.exceptionOrNull()
      assertTrue(exception is IllegalArgumentException)
      assertEquals("UserRepositoryFirestore: User ${user1.userId} not found", exception?.message)
    }
  }

  @Test
  fun refreshCacheClearsAllCacheEntries() {
    runTest {
      repository.addUser(user1)
      repository.addUser(user2)
      assertEquals(user1, userCache.getUser(user1.userId))
      assertEquals(user2, userCache.getUser(user2.userId))

      repository.refreshCache()
      assertNull(userCache.getUser(user1.userId))
      assertNull(userCache.getUser(user2.userId))
    }
  }

  @Test
  fun documentToUserMissingUsernameIsSkippedInGetAllUsers() {
    runTest {
      FirebaseEmulator.firestore
          .collection(USERS_COLLECTION_PATH)
          .document("badUser1")
          .set(mapOf("creationDate" to fromDate(2024, Calendar.JANUARY, 1)))
          .await()

      val users = repository.getAllUsers()
      assertTrue(users.isEmpty())
    }
  }

  @Test
  fun documentToUserMissingCreationDateIsSkippedInGetAllUsers() {
    runTest {
      FirebaseEmulator.firestore
          .collection(USERS_COLLECTION_PATH)
          .document("badUser2")
          .set(mapOf("username" to "userWithoutDate"))
          .await()

      val users = repository.getAllUsers()
      assertTrue(users.isEmpty())
    }
  }

  @Test
  fun documentToUserExceptionInsideParsingReturnsNull() {
    runTest {
      FirebaseEmulator.firestore
          .collection(USERS_COLLECTION_PATH)
          .document("crashyUser")
          .set(mapOf("username" to "boom", "creationDate" to "this-is-not-a-timestamp"))
          .await()

      val users = repository.getAllUsers()
      assertTrue(users.isEmpty())
    }
  }

  @Test
  fun documentToSimpleUSerMissingUsernameReturnsNull() {
    runTest {
      FirebaseEmulator.firestore
          .collection(USERS_COLLECTION_PATH)
          .document("badSimple1")
          .set(mapOf("profilePictureURL" to "url"))
          .await()

      val exception = runCatching { repository.getSimpleUser("badSimple1") }.exceptionOrNull()

      assertTrue(exception is Exception)
    }
  }

  @Test
  fun getSimpleUserWithInvalidUserTypeDefaultsToRegular() {
    runTest {
      repository.addUser(user1)
      val id = user1.userId
      FirebaseEmulator.firestore
          .collection(USERS_COLLECTION_PATH)
          .document(id)
          .set(mapOf("username" to "simple", "userType" to "not-a-valid-user-type"))
          .await()

      val simple = repository.getSimpleUser(id)
      assertEquals(UserType.REGULAR, simple.userType)
    }
  }

  @Test
  fun getAllUsersReturnsCachedUsersWithoutFirestoreAccess() {
    runTest {
      userCache.saveUsers(listOf(user1, user2))
      val users = repository.getAllUsers()
      assertEquals(listOf(user1, user2), users)
    }
  }
}
