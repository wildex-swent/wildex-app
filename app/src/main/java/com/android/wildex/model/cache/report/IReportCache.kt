package com.android.wildex.model.cache.report

import com.android.wildex.model.report.Report

/** Cache interface for report data. */
interface IReportCache {
  /**
   * Retrieves a report by its ID from the cache.
   *
   * @param reportId The ID of the report to retrieve.
   * @return The Report object if found, or null if not found.
   */
  suspend fun getReport(reportId: String): Report?

  /**
   * Retrieves all reports from the cache.
   *
   * @return A list of Report objects if any are found, or null if none are found.
   */
  suspend fun getAllReports(): List<Report>?

  /**
   * Retrieves all reports by a specific author from the cache.
   *
   * @param authorId The ID of the author whose reports to retrieve.
   * @return A list of Report objects if any are found, or null if none are found.
   */
  suspend fun getAllReportsByAuthor(authorId: String): List<Report>?

  /**
   * Retrieves all reports by a specific assignee from the cache. If assigneeId is null, retrieves
   * all unassigned reports.
   *
   * @param assigneeId The ID of the assignee whose reports to retrieve, or null for unassigned
   *   reports.
   * @return A list of Report objects if any are found, or null if none are found.
   */
  suspend fun getAllReportsByAssignee(assigneeId: String?): List<Report>?

  /**
   * Saves a report to the cache.
   *
   * @param report The Report object to save.
   */
  suspend fun saveReport(report: Report)

  /**
   * Saves multiple reports to the cache.
   *
   * @param reports A list of Report objects to save.
   */
  suspend fun saveReports(reports: List<Report>)

  /**
   * Edits an existing report in the cache.
   *
   * @param reportId The ID of the report to edit.
   * @param newValue The new Report object to replace the existing one.
   */
  //  suspend fun editReport(reportId: Id, newValue: Report)

  /**
   * Deletes a report from the cache by its ID.
   *
   * @param reportId The ID of the report to delete.
   */
  suspend fun deleteReport(reportId: String)

  /**
   * Deletes all reports from the cache by a specific author.
   *
   * @param authorId The ID of the author whose reports to delete.
   */
  suspend fun deleteReportByAuthor(authorId: String)

  /** Clears all report data from the cache. */
  suspend fun clearAll()
}
