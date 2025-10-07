package com.android.wildex.model.user

import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
import com.google.firebase.Timestamp

/**
 * Represents a user in the system.
 *
 * @property userId Unique identifier for the user.
 * @property username The username chosen by the user.
 * @property name The first name of the user.
 * @property surname The last name of the user.
 * @property bio A short biography or description of the user.
 * @property profilePictureURL The URL of the user's profile picture.
 * @property userType The type of user, defined by the UserType enum.
 * @property creationDate The date the user's account was created.
 * @property country The country the user is from.
 * @property friendsCount The number of friends the user has.
 * @property animalsId List of animal IDs the user has interacted with.
 * @property animalsCount The number of animals the user has interacted with.
 * @property achievementsId List of achievement IDs the user has earned.
 * @property achievementsCount The number of achievements the user has earned.
 */
data class User(
    val userId: Id,
    val username: String,
    val name: String,
    val surname: String,
    val bio: String,
    val profilePictureURL: URL,
    val userType: UserType,
    val creationDate: Timestamp,
    val country: String,
    val friendsCount: Int,
    val animalsId: List<Id>,
    val animalsCount: Int,
    val achievementsId: List<Id>,
    val achievementsCount: Int,
)

/** Enum class representing the type of user. */
enum class UserType {
  REGULAR,
  PROFESSIONAL,
}
