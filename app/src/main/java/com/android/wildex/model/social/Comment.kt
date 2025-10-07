package com.android.wildex.model.social

import com.android.wildex.model.utils.Id
import com.google.firebase.Timestamp

/**
 * Represents a comment on a post.
 *
 * @property commentId The unique identifier for the comment.
 * @property postId The ID of the post that this comment belongs to.
 * @property authorId The ID of the user who made the comment.
 * @property text The content of the comment.
 * @property date The date and time when the comment was made.
 */
data class Comment(
    val commentId: Id,
    val postId: Id,
    val authorId: Id,
    val text: String,
    val date: Timestamp,
)
