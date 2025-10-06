package com.android.wildex.model.achievement

/**
 * Represents an achievement earned by a user.
 *
 * @property achievementId The unique identifier for the achievement.
 * @property picture The URL/URI of the image associated with the achievement.
 * @property description A description of the achievement.
 * @property name The name of the achievement.
 * @property condition A lambda function that takes a list of posts and returns true if the
 *   achievement conditions are met.
 */
data class Achievement(
    val achievementId: String,
    val picture: String,
    val description: String,
    val name: String,
    val condition: (List<String>) -> Boolean,
)
