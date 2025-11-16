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
)

/**
 * Represents a summary of a user's achievements.
 *
 * @property userId The unique identifier of the user to whom these achievements belong.
 * @property achievementsId A list of unique identifiers for the achievements the user has earned.
 * @property achievementsCount The total number of achievements the user has earned.
 */
data class UserAchievements(
    val userId: Id = "",
    val achievementsId: List<Id> = emptyList(),
    val achievementsCount: Int = 0,
)

/**
 * Represents a summary of a user's animals.
 *
 * @property userId The unique identifier of the user to whom these animals belong.
 * @property animalsId A list of unique identifiers for the animals associated with the user.
 * @property animalsCount The total number of animals associated with the user.
 */
data class UserAnimals(
    val userId: Id = "",
    val animalsId: List<Id> = emptyList(),
    val animalsCount: Int = 0,
)

/**
 * Represents a summary of a user's friends.
 *
 * @property userId The unique identifier of the user to whom these friends belong.
 * @property friendsId A list of unique identifiers of the friends the user has.
 * @property friendsCount The total number of friends the user has.
 */
data class UserFriends(
    val userId: Id = "",
    val friendsId: List<Id> = emptyList(),
    val friendsCount: Int = 0,
)

/**
 * Represents the settings of a user.
 *
 * @property userId The unique identifier of the user to whom these settings belong.
 * @property enableNotifications A boolean indicating whether notifications are enabled for the
 *   user.
 * @property appearanceMode The appearance mode preference of the user, defined by the
 *   AppearanceMode enum.
 */
data class UserSettings(
    val userId: Id = "",
    val enableNotifications: Boolean = true,
    val appearanceMode: AppearanceMode = AppearanceMode.AUTOMATIC
)

/** Enum class representing the appearance mode preference of a user. */
enum class AppearanceMode {
  AUTOMATIC,
  LIGHT,
  DARK,
}

/** Enum class representing the type of user. */
enum class UserType {
  REGULAR,
  PROFESSIONAL,
}
