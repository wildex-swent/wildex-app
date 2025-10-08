package com.android.wildex.model.user

import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL

/**
 * Represents a simplified user with essential information.
 *
 * @property userId The unique identifier for the user.
 * @property username The username of the user.
 * @property profilePictureURL The URL of the user's profile picture.
 */
data class SimpleUser(val userId: Id, val username: String, val profilePictureURL: URL)
