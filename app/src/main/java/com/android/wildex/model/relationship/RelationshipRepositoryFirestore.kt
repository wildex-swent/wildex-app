package com.android.wildex.model.relationship

import com.android.wildex.model.utils.Id
import com.google.firebase.firestore.DocumentReference
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
    collection.document().set(Relationship(senderId = senderId, receiverId = receiverId)).await()
  }

  /**
   * Retrieves all Pending Relationships associated with a specific user as a sender.
   *
   * @param senderId the Id of the user who is a sender
   * @return The list of all Relationships with a specified sender
   */
  override suspend fun getAllPendingRelationshipsBySender(senderId: Id): List<Relationship> {
    return getPendingOrAcceptedRelationshipsByReceiverOrSender(
        userId = senderId, isReceiver = false, relationshipStatus = StatusEnum.PENDING)
  }

  /**
   * Retrieves all Pending Relationships associated with a specific user as a receiver.
   *
   * @param receiverId the Id of the user who is a receiver
   * @return The list of all Relationships with a specified receiver
   */
  override suspend fun getAllPendingRelationshipsByReceiver(receiverId: Id): List<Relationship> {
    return getPendingOrAcceptedRelationshipsByReceiverOrSender(
        userId = receiverId, isReceiver = true, relationshipStatus = StatusEnum.PENDING)
  }

  /**
   * Retrieves all Accepted Relationships associated with a specific user, no matter its role.
   *
   * @param userId the Id of the user who is either a sender or a receiver
   * @return The list of all Relationships with a specified user
   */
  override suspend fun getAllAcceptedRelationshipsByUser(userId: Id): List<Relationship> {
    val relationshipAsSender =
        getPendingOrAcceptedRelationshipsByReceiverOrSender(
            userId = userId, isReceiver = false, relationshipStatus = StatusEnum.ACCEPTED)
    val relationshipAsReceiver =
        getPendingOrAcceptedRelationshipsByReceiverOrSender(
            userId = userId, isReceiver = true, relationshipStatus = StatusEnum.ACCEPTED)

    return relationshipAsSender + relationshipAsReceiver
  }

  /**
   * Accepts a new Relationship to the repository.
   *
   * @param relationship The relationship whose status is to be set to ACCEPTED
   */
  override suspend fun acceptRelationship(relationship: Relationship) {
    getRelationshipDocRef(relationship.senderId, relationship.receiverId)
        .update(RelationshipsFields.STATUS, StatusEnum.ACCEPTED)
        .await()
  }

  /**
   * Deletes a Relationship from the repository.
   *
   * @param relationship The relationship who will be deleted
   */
  override suspend fun deleteRelationship(relationship: Relationship) {
    getRelationshipDocRef(relationship.senderId, relationship.receiverId).delete().await()
  }

  /**
   * Retrieves the pending or accepted relationships of the given user, initiated or received by him
   *
   * @param userId the user whose relationships we want to fetch
   * @param isReceiver false if we want the relationships initiated by the given user, true
   *   otherwise
   * @param relationshipStatus defines which relationships we fetch, either pending relationships or
   *   accepted ones
   */
  private suspend fun getPendingOrAcceptedRelationshipsByReceiverOrSender(
      userId: Id,
      isReceiver: Boolean,
      relationshipStatus: StatusEnum
  ): List<Relationship> {
    return collection
        .whereEqualTo(
            if (isReceiver) RelationshipsFields.RECEIVER_ID else RelationshipsFields.SENDER_ID,
            userId)
        .whereEqualTo(RelationshipsFields.STATUS, relationshipStatus)
        .get()
        .await()
        .documents
        .mapNotNull { documentToRelationship(it) }
  }

  /**
   * Retrieves the document reference of the relationship with the given sender and receiver
   *
   * @param senderId the sender of the relationship whose document reference is to be retrieved
   * @param receiverId the receiver of the relationship whose document reference is to be retrieved
   */
  private suspend fun getRelationshipDocRef(senderId: Id, receiverId: Id): DocumentReference {
    return collection
        .whereEqualTo(RelationshipsFields.SENDER_ID, senderId)
        .whereEqualTo(RelationshipsFields.RECEIVER_ID, receiverId)
        .get()
        .await()
        .documents
        .first() // assuming only one relationship between two users should exist
        .reference
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
