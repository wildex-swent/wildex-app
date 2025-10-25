package com.android.wildex.model.map

import com.android.wildex.model.report.Report
import com.android.wildex.model.report.ReportStatus
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.android.wildex.model.utils.URL

enum class PinKind {
  POST,
  REPORT
}

sealed class MapPin {
  abstract val id: Id // post/report id
  abstract val kind: PinKind
  abstract val location: Location
  abstract val imageURL: URL
  abstract val authorId: Id

  data class PostPin(
      override val id: Id,
      override val authorId: Id,
      override val location: Location,
      override val imageURL: URL,
  ) : MapPin() {
    override val kind = PinKind.POST
  }

  data class ReportPin(
      override val id: Id,
      override val authorId: Id,
      override val location: Location,
      override val imageURL: URL,
      val status: ReportStatus,
      val assigneeId: Id?
  ) : MapPin() {
    override val kind = PinKind.REPORT
  }
}

sealed interface PinDetails {
  data class PostDetails(val post: Post, val author: SimpleUser?, val likedByMe: Boolean) :
      PinDetails

  data class ReportDetails(val report: Report, val author: SimpleUser?, val assignee: SimpleUser?) :
      PinDetails
}
