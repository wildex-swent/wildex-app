package com.android.wildex.model.relationship

/**
 * Represents a relationship between two users.
 *
 * @property senderId The ID of the user who sent the relationship request.
 * @property receiverId The ID of the user who received the relationship request.
 * @property status The status of the relationship, defined by the StatusEnum.
 */
data class Relationship(val senderId: String, val receiverId: String, val status: StatusEnum)

/** Enum class representing the status of a relationship. */
enum class StatusEnum {
  PENDING,
  ACCEPTED,
}
