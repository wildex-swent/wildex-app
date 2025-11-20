package com.android.wildex.model.relationship

import com.android.wildex.model.utils.Id

/**
 * Represents a friend request between two users.
 *
 * @property senderId The ID of the user who sent the friend request.
 * @property receiverId The ID of the user who received the friend request.
 */
data class Relationship(
    val senderId: Id,
    val receiverId: Id,
)
