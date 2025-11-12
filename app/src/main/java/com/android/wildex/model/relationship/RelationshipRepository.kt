package com.android.wildex.model.relationship

import com.android.wildex.model.utils.Id

/** Represents a repository that manages Relationship. */
interface RelationshipRepository {

  /** Initialize a Relationship between two users: one sender and one receiver. */
  suspend fun initializeRelationship(senderId: Id, receiverId: Id)

  /** Retrieves all Pending Relationships associated with a specific user as a sender. */
  suspend fun getAllPendingRelationshipsBySender(senderId: Id): List<Relationship>

  /** Retrieves all Pending Relationships associated with a specific user as a receiver. */
  suspend fun getAllPendingRelationshipsByReceiver(receiverId: Id): List<Relationship>

  /** Retrieves all Accepted Relationships associated with a specific user, no matter its role. */
  suspend fun getAllAcceptedRelationshipsByUser(userId: Id): List<Relationship>

  /** Accepts a new Relationship to the repository. */
  suspend fun acceptRelationship(relationship: Relationship)

  /** Deletes a Relationship from the repository. */
  suspend fun deleteRelationship(relationship: Relationship)
}
