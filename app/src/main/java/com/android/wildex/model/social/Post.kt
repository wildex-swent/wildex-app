package com.android.wildex.model.social

import com.android.wildex.model.utils.Location
import com.google.firebase.Timestamp

/**
 * Represents a post made by a user.
 *
 * @property postId The unique identifier for the post.
 * @property authorId The ID of the user who created the post.
 * @property picture The URL/URI of the image in the post.
 * @property location The location where the post was made.
 * @property date The date and time when the post was created.
 * @property animalId The ID of the animal related to the post.
 * @property likesCount The number of likes the post has received.
 * @property commentsCount The number of comments the post has received.
 */
data class Post(
    val postId: String,
    val authorId: String,
    val picture: String,
    val location: Location,
    val date: Timestamp,
    val animalId: String,
    val likesCount: Int,
    val commentsCount: Int,
)
