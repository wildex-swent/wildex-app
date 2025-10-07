package com.android.wildex.model.relationship

import com.android.wildex.model.utils.Id

/**
 * Represents a relationship between two users.
 *
 * @property senderId The ID of the user who sent the relationship request.
 * @property receiverId The ID of the user who received the relationship request.
 * @property status The status of the relationship, defined by the StatusEnum.
 */
data class Relationship(val senderId: Id, val receiverId: Id, val status: StatusEnum)

/** Enum class representing the status of a relationship. */
enum class StatusEnum {
  PENDING,
  ACCEPTED,
}
