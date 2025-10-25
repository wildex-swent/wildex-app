package com.android.wildex.model.achievement

import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL

/**
 * Represents an achievement earned by a user.
 *
 * @property achievementId The unique identifier for the achievement.
 * @property pictureURL The URL of the image associated with the achievement.
 * @property description A description of the achievement.
 * @property name The name of the achievement.
 * @property condition A lambda function that takes a list of posts and returns true if the
 *   achievement conditions are met.
 */
data class Achievement(
    val achievementId: Id,
    val pictureURL: URL,
    val description: String,
    val name: String,
    val expects: Set<InputKey>,
    val condition: (suspend (List<Id>) -> Boolean)? = null,
    val multiCondition: (suspend (Map<InputKey, List<Id>>) -> Boolean)? = null,
)

/** Indicates the type of input key used to evaluate achievement conditions. */
enum class InputKey {
  TEST_IDS,
  POST_IDS,
  LIKE_IDS,
  COMMENT_IDS,
}
