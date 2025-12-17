package com.android.wildex.model.social

import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.android.wildex.model.utils.URL
import com.google.firebase.Timestamp

/**
 * Represents a post made by a user.
 *
 * @property postId The unique identifier for the post.
 * @property authorId The ID of the user who created the post.
 * @property pictureURL The URL of the image in the post.
 * @property location The location where the post was made.
 * @property description The description of the post.
 * @property date The date and time when the post was created.
 * @property animalId The ID of the animal related to the post.
 */
data class Post(
    val postId: Id,
    val authorId: Id,
    val pictureURL: URL,
    val location: Location?,
    val description: String = "",
    val date: Timestamp,
    val animalId: Id
)
