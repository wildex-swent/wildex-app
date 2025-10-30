package com.android.wildex.model.report

import com.android.wildex.model.utils.Id

/** Represents a repository that manages Report items. */
interface ReportRepository {

  /** Generates and returns a new unique identifier for a Report item. */
  fun getNewReportId(): Id

  /** Retrieves all Reports items from the repository. */
  suspend fun getAllReports(): List<Report>

  /** Retrieves all Reports items associated with a specific author. */
  suspend fun getAllReportsByAuthor(authorId: Id): List<Report>

  /** Retrieves all Reports items associated with a specific assignee. */
  suspend fun getAllReportsByAssignee(assigneeId: Id): List<Report>

  /** Retrieves a specific Report item by its unique identifier. */
  suspend fun getReport(reportId: Id): Report

  /** Adds a new Report item to the repository. */
  suspend fun addReport(report: Report)

  /** Edits an existing Report item in the repository. */
  suspend fun editPost(reportId: Id, newValue: Report)

  /** Deletes a Report item from the repository. */
  suspend fun deleteReport(reportId: Id)
}
