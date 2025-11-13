package com.android.wildex.model.map

import com.android.wildex.model.report.Report
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.android.wildex.model.utils.URL

/**
 * Represents the kind of pin on the map. POST: A pin representing a user post. REPORT: A pin
 * representing a user report.
 */
enum class PinKind {
  POST,
  REPORT,
}

/**
 * Sealed class representing a pin on the map.
 *
 * @property id The unique identifier for the pin (post/report id).
 * @property kind The kind of pin (POST or REPORT).
 * @property location The geographical location of the pin.
 * @property imageURL The URL of the image associated with the pin.
 * @property authorId The ID of the user who created the post/report.
 */
sealed class MapPin {
  abstract val id: Id // post/report id
  abstract val kind: PinKind
  abstract val location: Location
  abstract val imageURL: URL
  abstract val authorId: Id

  /**
   * Data class representing a post pin on the map.
   *
   * @property isFriend Indicates whether the post author is a friend of the current user.
   */
  data class PostPin(
      override val id: Id,
      override val authorId: Id,
      override val location: Location,
      override val imageURL: URL,
      override val kind: PinKind = PinKind.POST,
      val isFriend: Boolean = false
  ) : MapPin()

  /**
   * Data class representing a report pin on the map.
   *
   * @property assigneeId The ID of the user assigned to handle the report, if any
   */
  data class ReportPin(
      override val id: Id,
      override val authorId: Id,
      override val location: Location,
      override val imageURL: URL,
      override val kind: PinKind = PinKind.REPORT,
      val assigneeId: Id?
  ) : MapPin()
}

/** Sealed interface representing detailed information for a map pin. */
sealed interface PinDetails {

  /**
   * Data class representing detailed information for a post pin.
   *
   * @property post The post associated with the pin.
   * @property author Simplified user information for the post author.
   * @property likedByMe Indicates whether the current user has liked this post.
   * @property animalName The name of the animal referenced in the post.
   */
  data class PostDetails(
      val post: Post,
      val author: SimpleUser?,
      val likedByMe: Boolean,
      val animalName: String = "animal"
  ) : PinDetails

  /**
   * Data class representing detailed information for a report pin.
   *
   * @property report The report associated with the pin.
   * @property author Simplified user information for the report author.
   * @property assignee Simplified user information for the report assignee (if any).
   */
  data class ReportDetails(val report: Report, val author: SimpleUser?, val assignee: SimpleUser?) :
      PinDetails
}
