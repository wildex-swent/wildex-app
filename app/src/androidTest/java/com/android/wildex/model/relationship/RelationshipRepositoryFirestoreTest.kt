package com.android.wildex.model.relationship

import com.android.wildex.utils.FirebaseEmulator
import com.android.wildex.utils.FirestoreTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val RELATIONSHIPS_COLLECTION_PATH = "relationships"

class RelationshipRepositoryFirestoreTest : FirestoreTest(RELATIONSHIPS_COLLECTION_PATH) {

  private var repository = RelationshipRepositoryFirestore(FirebaseEmulator.firestore)

  @Test
  fun initializeRelationshipWorks() = runTest {
    repository.initializeRelationship(user1.userId, user2.userId)

    var relationships = repository.getAllPendingRelationshipsBySender(user1.userId)

    assertEquals(1, relationships.size)
    assertTrue(relationships.contains(relationship1pending))

    relationships = repository.getAllPendingRelationshipsByReceiver(user2.userId)

    assertEquals(1, relationships.size)
    assertTrue(relationships.contains(relationship1pending))
  }

  @Test
  fun getAllPendingRelationshipsBySenderWhenPending() = runTest {
    repository.initializeRelationship(user1.userId, user2.userId)
    repository.initializeRelationship(user3.userId, user1.userId)

    val relationships = repository.getAllPendingRelationshipsBySender(user1.userId)

    assertEquals(1, relationships.size)
    assertTrue(relationships.contains(relationship1pending))
  }

  @Test
  fun getAllPendingRelationshipsBySenderWhenAccepted() = runTest {
    repository.initializeRelationship(user1.userId, user2.userId)
    repository.initializeRelationship(user3.userId, user1.userId)

    repository.acceptRelationship(relationship1pending)

    val relationships = repository.getAllPendingRelationshipsBySender(user1.userId)

    assertTrue(relationships.isEmpty())
  }

  @Test
  fun getAllPendingRelationshipsByReceiverWhenPending() = runTest {
    repository.initializeRelationship(user1.userId, user2.userId)
    repository.initializeRelationship(user3.userId, user1.userId)

    val relationships = repository.getAllPendingRelationshipsByReceiver(user2.userId)

    assertEquals(1, relationships.size)
    assertEquals(relationship1pending, relationships.first())
  }

  @Test
  fun getAllPendingRelationshipsByReceiverWhenAccepted() = runTest {
    repository.initializeRelationship(user1.userId, user2.userId)
    repository.initializeRelationship(user3.userId, user1.userId)
    repository.acceptRelationship(relationship1pending)

    val relationships = repository.getAllPendingRelationshipsByReceiver(user2.userId)

    assertTrue(relationships.isEmpty())
  }

  @Test
  fun getAllRelationshipsByUserWorks() = runTest {
    repository.initializeRelationship(user1.userId, user2.userId)
    repository.initializeRelationship(user3.userId, user1.userId)

    var relationships = repository.getAllAcceptedRelationshipsByUser(user1.userId)

    assertTrue(relationships.isEmpty())

    repository.acceptRelationship(relationship1pending)
    repository.acceptRelationship(relationship2pending)

    relationships = repository.getAllAcceptedRelationshipsByUser(user1.userId)

    assertEquals(2, relationships.size)
    assertTrue(relationships.contains(relationship1accepted))
    assertTrue(relationships.contains(relationship2accepted))
  }

  @Test
  fun acceptRelationshipWorks() = runTest {
    repository.initializeRelationship(user1.userId, user2.userId)

    var relationships = repository.getAllAcceptedRelationshipsByUser(user1.userId)

    assertTrue(relationships.isEmpty())

    repository.acceptRelationship(relationship1pending)

    relationships = repository.getAllAcceptedRelationshipsByUser(user1.userId)

    assertEquals(1, relationships.size)
    assertTrue(relationships.contains(relationship1accepted))
  }

  @Test
  fun deleteRelationshipWorks() = runTest {
    repository.initializeRelationship(user1.userId, user2.userId)
    repository.acceptRelationship(relationship1pending)

    var relationships = repository.getAllAcceptedRelationshipsByUser(user1.userId)

    assertEquals(1, relationships.size)
    assertTrue(relationships.contains(relationship1accepted))

    repository.deleteRelationship(relationship1accepted)

    relationships = repository.getAllAcceptedRelationshipsByUser(user1.userId)

    assertTrue(relationships.isEmpty())
  }
}
