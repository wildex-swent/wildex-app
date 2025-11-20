package com.android.wildex.model.friendRequest

import com.android.wildex.utils.FirebaseEmulator
import com.android.wildex.utils.FirestoreTest
import junit.framework.TestCase
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val FRIEND_REQUESTS_COLLECTION_PATH = "friendRequests"

class FriendRequestRepositoryFirestoreTest : FirestoreTest(FRIEND_REQUESTS_COLLECTION_PATH) {

  private var repository = FriendRequestRepositoryFirestore(FirebaseEmulator.firestore)

  @Test
  fun initializeFriendRequestWorks() = runTest {
    repository.initializeFriendRequest(user1.userId, user2.userId)

    var friendRequests = repository.getAllFriendRequestsBySender(user1.userId)

    TestCase.assertEquals(1, friendRequests.size)
    TestCase.assertTrue(friendRequests.contains(friendRequest1))

    friendRequests = repository.getAllFriendRequestsByReceiver(user2.userId)

    TestCase.assertEquals(1, friendRequests.size)
    TestCase.assertTrue(friendRequests.contains(friendRequest1))
  }

  @Test
  fun getAllFriendRequestsBySenderWorks() = runTest {
    repository.initializeFriendRequest(user1.userId, user2.userId)
    repository.initializeFriendRequest(user3.userId, user1.userId)

    val friendRequests = repository.getAllFriendRequestsBySender(user1.userId)

    TestCase.assertEquals(1, friendRequests.size)
    TestCase.assertTrue(friendRequests.contains(friendRequest1))
  }

  @Test
  fun getAllFriendRequestsByReceiverWorks() = runTest {
    repository.initializeFriendRequest(user1.userId, user2.userId)
    repository.initializeFriendRequest(user3.userId, user1.userId)

    val friendRequests = repository.getAllFriendRequestsByReceiver(user2.userId)

    TestCase.assertEquals(1, friendRequests.size)
    TestCase.assertTrue(friendRequests.contains(friendRequest1))
  }

  /*@Test
  fun acceptFriendRequestWorks() = runTest {
    repository.initializeFriendRequest(user1.userId, user2.userId)
    repository.initializeFriendRequest(user3.userId, user1.userId)

    repository.acceptFriendRequest(friendRequest1)

    var friendRequests = repository.getAllFriendRequestsBySender(user1.userId)

    TestCase.assertTrue(friendRequests.isEmpty())

    friendRequests = repository.getAllFriendRequestsByReceiver(user1.userId)

    TestCase.assertEquals(1, friendRequests.size)
    TestCase.assertTrue(friendRequests.contains(friendRequest2))
  }*/

  @Test
  fun refuseFriendRequestWorks() = runTest {
    repository.initializeFriendRequest(user1.userId, user2.userId)
    repository.initializeFriendRequest(user3.userId, user1.userId)

    repository.refuseFriendRequest(friendRequest1)

    var friendRequests = repository.getAllFriendRequestsBySender(user1.userId)

    TestCase.assertTrue(friendRequests.isEmpty())

    friendRequests = repository.getAllFriendRequestsByReceiver(user1.userId)

    TestCase.assertEquals(1, friendRequests.size)
    TestCase.assertTrue(friendRequests.contains(friendRequest2))
  }

  @Test
  fun deleteAllFriendRequestsOfUserWorks() = runTest {
    repository.initializeFriendRequest(user1.userId, user2.userId)
    repository.initializeFriendRequest(user3.userId, user1.userId)

    repository.deleteAllFriendRequestsOfUser(user1.userId)

    var friendRequests = repository.getAllFriendRequestsBySender(user1.userId)

    TestCase.assertTrue(friendRequests.isEmpty())

    friendRequests = repository.getAllFriendRequestsByReceiver(user1.userId)

    TestCase.assertTrue(friendRequests.isEmpty())
  }
}
