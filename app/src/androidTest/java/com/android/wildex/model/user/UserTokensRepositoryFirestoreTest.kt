package com.android.wildex.model.user

import com.android.wildex.utils.FirebaseEmulator
import com.android.wildex.utils.FirestoreTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val USER_TOKENS_COLLECTION_PATH = "userTokens"

class UserTokensRepositoryFirestoreTest : FirestoreTest(USER_TOKENS_COLLECTION_PATH) {
  private val repository = UserTokensRepositoryFirestore(FirebaseEmulator.firestore)

  val userTokens1 = UserTokens(userId = "user1", tokens = listOf("token1", "token2"))

  val userTokens2 = UserTokens(userId = "user2", tokens = listOf("token3", "token4", "token5"))

  val userTokens3 = UserTokens(userId = "user3", tokens = listOf("token6"))

  @Before
  fun setup() {
    super.setUp()
    runBlocking {
      FirebaseEmulator.firestore
          .collection(USER_TOKENS_COLLECTION_PATH)
          .document(userTokens1.userId)
          .set(userTokens1)
          .await()
      FirebaseEmulator.firestore
          .collection(USER_TOKENS_COLLECTION_PATH)
          .document(userTokens2.userId)
          .set(userTokens2)
          .await()
      FirebaseEmulator.firestore
          .collection(USER_TOKENS_COLLECTION_PATH)
          .document(userTokens3.userId)
          .set(userTokens3)
          .await()
    }
  }

  @Test
  fun testInitializeUserTokens() = runTest {
    val newUserId = "newUserId"
    assertFalse(
        FirebaseEmulator.firestore
            .collection(USER_TOKENS_COLLECTION_PATH)
            .document(newUserId)
            .get()
            .await()
            .exists())
    repository.initializeUserTokens(newUserId)
    val userTokensSnapshot =
        FirebaseEmulator.firestore
            .collection(USER_TOKENS_COLLECTION_PATH)
            .document(newUserId)
            .get()
            .await()
    assertTrue(userTokensSnapshot.exists())
    assertEquals(
        userTokensSnapshot.toObject(UserTokens::class.java),
        UserTokens(userId = newUserId),
    )
  }

  @Test(expected = IllegalArgumentException::class)
  fun testInitializeUserTokens_ExistingUserId() = runTest {
    repository.initializeUserTokens("user1")
  }

  @Test
  fun testGetAllTokensOfUser() = runTest {
    val tokens = repository.getAllTokensOfUser("user1")
    assertEquals(listOf("token1", "token2"), tokens)
  }

  @Test(expected = IllegalArgumentException::class)
  fun testGetAllTokensOfUser_MissingUserId() = runTest {
    repository.getAllTokensOfUser("nonExistentUserId")
  }

  @Test
  fun testAddTokenToUser() = runTest {
    val newToken = "newToken"
    repository.addTokenToUser("user1", newToken)
    assertEquals(
        UserTokens("user1", listOf("token1", "token2", newToken)),
        FirebaseEmulator.firestore
            .collection(USER_TOKENS_COLLECTION_PATH)
            .document("user1")
            .get()
            .await()
            .toObject(UserTokens::class.java),
    )
  }

  @Test(expected = IllegalArgumentException::class)
  fun testAddTokenToUser_MissingUserId() = runTest {
    repository.addTokenToUser("nonExistentUserId", "newToken")
  }

  @Test
  fun testDeleteTokenOfUser() = runTest {
    val tokenToDelete = "token2"
    repository.deleteTokenOfUser("user1", tokenToDelete)
    assertEquals(
        UserTokens("user1", listOf("token1")),
        FirebaseEmulator.firestore
            .collection(USER_TOKENS_COLLECTION_PATH)
            .document("user1")
            .get()
            .await()
            .toObject(UserTokens::class.java),
    )
  }

  @Test(expected = IllegalArgumentException::class)
  fun testDeleteTokenOfUser_MissingUserId() = runTest {
    repository.deleteTokenOfUser("nonExistentUserId", "tokenToDelete")
  }

  @Test
  fun testDeleteUserTokens() = runTest {
    repository.deleteUserTokens("user1")
    assertFalse(
        FirebaseEmulator.firestore
            .collection(USER_TOKENS_COLLECTION_PATH)
            .document("user1")
            .get()
            .await()
            .exists())
  }

  @Test(expected = IllegalArgumentException::class)
  fun testDeleteUserTokens_MissingUserId() = runTest {
    repository.deleteUserTokens("nonExistentUserId")
  }
}
