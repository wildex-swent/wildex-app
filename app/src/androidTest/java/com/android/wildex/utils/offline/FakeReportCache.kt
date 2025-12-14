package com.android.wildex.utils.offline

import com.android.wildex.model.cache.report.IReportCache
import com.android.wildex.model.report.Report

class FakeReportCache : IReportCache {
  val cache = mutableMapOf<String, Report>()

  init {
    cache.clear()
  }

  override suspend fun getReport(reportId: String): Report? {
    return cache[reportId]
  }

  override suspend fun getAllReports(): List<Report>? {
    return cache.values.toList()
  }

  override suspend fun getAllReportsByAuthor(authorId: String): List<Report>? {
    return cache.values.filter { it.authorId == authorId }.toList()
  }

  override suspend fun getAllReportsByAssignee(assigneeId: String?): List<Report>? {
    return cache.values.filter { it.assigneeId == assigneeId }.toList()
  }

  override suspend fun saveReport(report: Report) {
    cache.put(report.reportId, report)
  }

  override suspend fun saveReports(reports: List<Report>) {
    reports.forEach { saveReport(it) }
  }

  override suspend fun deleteReport(reportId: String) {
    cache.remove(reportId)
  }

  override suspend fun deleteReportByAuthor(authorId: String) {
    val reportsToDelete = cache.values.filter { it.authorId == authorId }
    reportsToDelete.forEach { cache.remove(it.reportId) }
  }

  override suspend fun clearAll() {
    cache.clear()
  }
}
