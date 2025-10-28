package com.android.wildex.model.report

/** Represents a repository that manages Report items. */
interface ReportRepository {

  /** Generates and returns a new unique identifier for a Report item. */
  fun getNewReportId(): String

  /** Retrieves all Reports items from the repository. */
  suspend fun getAllReports(): List<Report>

  /** Retrieves all Reports items associated with a specific user. */
  suspend fun getAllReportsByUser(userId: String): List<Report>

  /** Retrieves a specific Report item by its unique identifier. */
  suspend fun getReport(reportId: String): Report

  /** Adds a new Report item to the repository. */
  suspend fun addReport(report: Report)

  /** Edits an existing Report item in the repository. */
  suspend fun editPost(reportId: String, newValue: Report)

  /** Deletes a Report item from the repository. */
  suspend fun deleteReport(reportId: String)
}
