package com.android.wildex.model.relationship

import com.android.wildex.model.utils.Id
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val RELATIONSHIPS_COLLECTION_PATH = "relationships"

private object RelationshipsFields {
  const val SENDER_ID = "senderId"
  const val RECEIVER_ID = "receiverId"
  const val STATUS = "status"
}

/** Represents a repository that manages Relationship. */
class RelationshipRepositoryFirestore(private val db: FirebaseFirestore) : RelationshipRepository {

  private val collection = db.collection(RELATIONSHIPS_COLLECTION_PATH)

  /**
   * Initialize a Relationship between two users: one sender and one receiver.
   *
   * @param senderId the Id of the user who is the sender
   * @param receiverId the Id of the user who is the receiver
   */
  override suspend fun initializeRelationship(senderId: Id, receiverId: Id) {
    collection
        .document()
        .set(
            Relationship(senderId = senderId, receiverId = receiverId, status = StatusEnum.PENDING))
        .await()
  }

  /**
   * Retrieves all Pending Relationships associated with a specific user as a sender.
   *
   * @param senderId the Id of the user who is a sender
   * @return The list of all Relationships with a specified sender
   */
  override suspend fun getAllPendingRelationshipsBySender(senderId: Id): List<Relationship> {
    return collection
        .whereEqualTo(RelationshipsFields.STATUS, StatusEnum.PENDING)
        .whereEqualTo(RelationshipsFields.SENDER_ID, senderId)
        .get()
        .await()
        .documents
        .mapNotNull { documentToRelationship(it) }
  }

  /**
   * Retrieves all Pending Relationships associated with a specific user as a receiver.
   *
   * @param receiverId the Id of the user who is a receiver
   * @return The list of all Relationships with a specified receiver
   */
  override suspend fun getAllPendingRelationshipsByReceiver(receiverId: Id): List<Relationship> {
    return collection
        .whereEqualTo(RelationshipsFields.STATUS, StatusEnum.PENDING)
        .whereEqualTo(RelationshipsFields.RECEIVER_ID, receiverId)
        .get()
        .await()
        .documents
        .mapNotNull { documentToRelationship(it) }
  }

  /**
   * Retrieves all Accepted Relationships associated with a specific user, no matter its role.
   *
   * @param userId the Id of the user who is either a sender or a receiver
   * @return The list of all Relationships with a specified user
   */
  override suspend fun getAllAcceptedRelationshipsByUser(userId: Id): List<Relationship> {
    val relationshipAsSender =
        collection
            .whereEqualTo(RelationshipsFields.STATUS, StatusEnum.ACCEPTED)
            .whereEqualTo(RelationshipsFields.SENDER_ID, userId)
            .get()
            .await()
            .documents
            .mapNotNull { documentToRelationship(it) }
    val relationshipAsReceiver =
        collection
            .whereEqualTo(RelationshipsFields.STATUS, StatusEnum.ACCEPTED)
            .whereEqualTo(RelationshipsFields.RECEIVER_ID, userId)
            .get()
            .await()
            .documents
            .mapNotNull { documentToRelationship(it) }

    return relationshipAsSender + relationshipAsReceiver
  }

  /**
   * Accepts a new Relationship to the repository.
   *
   * @param relationship The relationship whose status is to be set to ACCEPTED
   */
  override suspend fun acceptRelationship(relationship: Relationship) {
    collection
        .whereEqualTo(RelationshipsFields.SENDER_ID, relationship.senderId)
        .whereEqualTo(RelationshipsFields.RECEIVER_ID, relationship.receiverId)
        .get()
        .await()
        .documents
        .first() // assuming only one relationship between two users should exist
        .reference
        .update(RelationshipsFields.STATUS, StatusEnum.ACCEPTED)
        .await()
  }

  /**
   * Deletes a Relationship from the repository.
   *
   * @param relationship The relationship who will be deleted
   */
  override suspend fun deleteRelationship(relationship: Relationship) {
    collection
        .whereEqualTo(RelationshipsFields.SENDER_ID, relationship.senderId)
        .whereEqualTo(RelationshipsFields.RECEIVER_ID, relationship.receiverId)
        .get()
        .await()
        .documents
        .first() // assuming only one relationship between two users should exist
        .reference
        .delete()
        .await()
  }

  /**
   * Converts a document reference to a relationship.
   *
   * @param document The document to be converted into a relationship.
   * @return The transformed [Relationship] object.
   */
  private fun documentToRelationship(document: DocumentSnapshot): Relationship? {
    return try {
      val senderId =
          document.getString(RelationshipsFields.SENDER_ID)
              ?: throwMissingFieldException(RelationshipsFields.SENDER_ID)
      val receiverId =
          document.getString(RelationshipsFields.RECEIVER_ID)
              ?: throwMissingFieldException(RelationshipsFields.RECEIVER_ID)
      val statusData =
          document.getString(RelationshipsFields.STATUS)
              ?: throwMissingFieldException(RelationshipsFields.STATUS)
      val status = StatusEnum.valueOf(statusData.uppercase())

      Relationship(senderId = senderId, receiverId = receiverId, status = status)
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Throws an error for the missing comment field.
   *
   * @param field The name of the missing field.
   * @throws IllegalArgumentException if the field is missing.
   */
  private fun throwMissingFieldException(field: String): Nothing {
    throw IllegalArgumentException("Missing required field in Relationship: $field")
  }
}
