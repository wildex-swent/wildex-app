package com.android.wildex.model.report

import com.android.wildex.model.utils.Location
import com.google.firebase.Timestamp

/**
 * Represents a report made by a user regarding an animal or post.
 *
 * @property reportId The unique identifier for the report.
 * @property image The URL/URI of the image being reported.
 * @property location The location where the report was made.
 * @property date The date and time when the report was created.
 * @property description A description of why the report was made.
 * @property authorId The ID of the user who made the report.
 * @property assigneeId The ID of the user assigned to handle the report.
 * @property status The status of the report, defined by the ReportStatus enum.
 */
data class Report(
    val reportId: String,
    val image: String,
    val location: Location,
    val date: Timestamp,
    val description: String,
    val authorId: String,
    val assigneeId: String,
    val status: ReportStatus,
)

/** Enum class representing the status of a report. */
enum class ReportStatus {
  PENDING,
  RESOLVED,
}
