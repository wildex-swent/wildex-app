package com.android.wildex.model.social

/**
 * Represents a like on a post.
 *
 * @property likeId The unique identifier for the like.
 * @property postId The ID of the post that was liked.
 * @property userId The ID of the user who liked the post.
 */
data class Like(val likeId: String, val postId: String, val userId: String)
