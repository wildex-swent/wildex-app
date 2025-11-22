package com.android.wildex.model.user

import com.android.wildex.utils.FirebaseEmulator
import com.android.wildex.utils.FirestoreTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UserFriendsRepositoryFirestoreTest : FirestoreTest(USER_ANIMALS_COLLECTION_PATH) {

  private var repository = UserFriendsRepositoryFirestore(FirebaseEmulator.firestore)
  private var userRepository = UserRepositoryFirestore(FirebaseEmulator.firestore)

  @Test
  fun initializeUserFriendsWhenNoUserExists() = runTest {
    var exceptionThrown = false

    try {
      repository.initializeUserFriends(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assertFalse(exceptionThrown)

    val friendsId = repository.getAllFriendsOfUser(user1.userId)
    val friendsCount = repository.getFriendsCountOfUser(user1.userId)

    assertTrue(friendsId.isEmpty())
    assertEquals(0, friendsCount)
  }

  @Test
  fun initializeUserAnimalsWhenUserAlreadyExists() = runTest {
    repository.initializeUserFriends(user1.userId)

    var exceptionThrown = false

    try {
      repository.initializeUserFriends(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userFriends with userId '${user1.userId}' already exists.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun getAllFriendsOfUserWhenNoUserExists() = runTest {
    var exceptionThrown = false

    try {
      repository.getAllFriendsOfUser(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userFriends with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun getFriendsCountOfUserWhenNoUserExists() = runTest {
    var exceptionThrown = false

    try {
      repository.getFriendsCountOfUser(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userFriends with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun addFriendToUserFriendsOfUserWhenNoUserExists() = runTest {
    var exceptionThrown = false

    try {
      repository.addFriendToUserFriendsOfUser(user2.userId, user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userFriends with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun addFriendToUserFriendsOfUserWhenUserAlreadyExists() = runTest {
    repository.initializeUserFriends(user1.userId)
    userRepository.addUser(user2)

    var exceptionThrown = false

    try {
      repository.addFriendToUserFriendsOfUser(user2.userId, user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assertFalse(exceptionThrown)

    val friendsId = repository.getAllFriendsOfUser(user1.userId)
    val friendsCount = repository.getFriendsCountOfUser(user1.userId)

    assertEquals(1, friendsCount)
    assertEquals(1, friendsId.size)
    assertTrue(friendsId.contains(user2))
  }

  @Test
  fun addFriendToUserFriendsOfUserTwiceSameFriend() = runTest {
    repository.initializeUserFriends(user1.userId)
    userRepository.addUser(user2)

    var exceptionThrown = false

    try {
      repository.addFriendToUserFriendsOfUser(user2.userId, user1.userId)
      repository.addFriendToUserFriendsOfUser(user2.userId, user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assertFalse(exceptionThrown)

    val friendsId = repository.getAllFriendsOfUser(user1.userId)
    val friendsCount = repository.getFriendsCountOfUser(user1.userId)

    assertEquals(1, friendsCount)
    assertEquals(1, friendsId.size)
    assertTrue(friendsId.contains(user2))
  }

  @Test
  fun deleteFriendToUserFriendsOfUserWhenNoUserExists() = runTest {
    var exceptionThrown = false

    try {
      repository.deleteFriendToUserFriendsOfUser(user2.userId, user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userFriends with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun deleteFriendToUserFriendsOfUserWhenUserAlreadyExists() = runTest {
    repository.initializeUserFriends(user1.userId)
    repository.addFriendToUserFriendsOfUser(user2.userId, user1.userId)

    var exceptionThrown = false

    try {
      repository.deleteFriendToUserFriendsOfUser(user2.userId, user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assertFalse(exceptionThrown)

    val friendsId = repository.getAllFriendsOfUser(user1.userId)
    val friendsCount = repository.getFriendsCountOfUser(user1.userId)

    assertTrue(friendsId.isEmpty())
    assertEquals(0, friendsCount)
  }

  @Test
  fun deleteFriendToUserFriendsOfUserTwiceSameAnimal() = runTest {
    repository.initializeUserFriends(user1.userId)
    repository.addFriendToUserFriendsOfUser(user2.userId, user1.userId)

    var exceptionThrown = false

    try {
      repository.deleteFriendToUserFriendsOfUser(user2.userId, user1.userId)
      repository.deleteFriendToUserFriendsOfUser(user2.userId, user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assertFalse(exceptionThrown)

    val friendsId = repository.getAllFriendsOfUser(user1.userId)
    val friendsCount = repository.getFriendsCountOfUser(user1.userId)

    assertTrue(friendsId.isEmpty())
    assertEquals(0, friendsCount)
  }

  @Test
  fun deleteUserFriendsOfUserWhenNoUserExists() = runTest {
    var exceptionThrown = false

    try {
      repository.deleteUserFriendsOfUser(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userFriends with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun deleteUserFriendsOfUserWhenUserExists() = runTest {
    var exceptionThrown = false
    repository.initializeUserFriends(user1.userId)
    repository.deleteUserFriendsOfUser(user1.userId)

    try {
      repository.getAllFriendsOfUser(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals("A userFriends with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }
}
