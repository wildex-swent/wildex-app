package com.android.wildex.model.cache.report

import androidx.datastore.core.DataStore
import com.android.wildex.datastore.ReportCacheStorage
import com.android.wildex.model.ConnectivityObserver
import com.android.wildex.model.report.Report
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private const val STALE_DURATION_MS = 10 * 60 * 1000L // 10 minutes

class ReportCache(
    private val reportDataStore: DataStore<ReportCacheStorage>,
    private val connectivityObserver: ConnectivityObserver
) : IReportCache {
  private fun isStale(lastUpdated: Long): Boolean {
    val isOnline = connectivityObserver.isOnline.value
    val currentTime = System.currentTimeMillis()
    return isOnline && (currentTime - lastUpdated) > STALE_DURATION_MS
  }

  override suspend fun getReport(reportId: String): Report? {
    return reportDataStore.data
        .map {
          val cached = it.reportsMap[reportId]
          if (cached != null && !isStale(cached.lastUpdated)) {
            cached.toReport()
          } else {
            null
          }
        }
        .firstOrNull()
  }

  override suspend fun getAllReports(): List<Report>? {
    val reports =
        reportDataStore.data
            .map { cacheStorage ->
              val reports = cacheStorage.reportsMap.values
              if (reports.isNotEmpty() && reports.all { !isStale(it.lastUpdated) }) {
                reports.map { it.toReport() }
              } else {
                null
              }
            }
            .firstOrNull()
    return if (reports == null && !connectivityObserver.isOnline.value) {
      emptyList()
    } else {
      reports
    }
  }

  override suspend fun getAllReportsByAuthor(authorId: String): List<Report>? {
    val reports =
        reportDataStore.data
            .map { cacheStorage ->
              val reports = cacheStorage.reportsMap.values.filter { it.authorId == authorId }
              if (reports.isNotEmpty() && reports.all { !isStale(it.lastUpdated) }) {
                reports.map { it.toReport() }
              } else {
                null
              }
            }
            .firstOrNull()
    return if (reports == null && !connectivityObserver.isOnline.value) {
      emptyList()
    } else {
      reports
    }
  }

  override suspend fun getAllReportsByAssignee(assigneeId: String?): List<Report>? {
    val reports =
        reportDataStore.data
            .map { cacheStorage ->
              val reports =
                  cacheStorage.reportsMap.values.filter {
                    when {
                      assigneeId == null -> it.assigneeId.isBlank()
                      else -> it.assigneeId == assigneeId
                    }
                  }
              if (reports.isNotEmpty() && reports.all { !isStale(it.lastUpdated) }) {
                reports.map { it.toReport() }
              } else {
                null
              }
            }
            .firstOrNull()
    return if (reports == null && !connectivityObserver.isOnline.value) {
      emptyList()
    } else {
      reports
    }
  }

  override suspend fun saveReport(report: Report) {
    reportDataStore.updateData {
      it.toBuilder().putReports(report.reportId, report.toProto()).build()
    }
  }

  override suspend fun saveReports(reports: List<Report>) {
    reports.forEach { saveReport(it) }
  }

  override suspend fun deleteReport(reportId: String) {
    reportDataStore.updateData { it.toBuilder().removeReports(reportId).build() }
  }

  override suspend fun deleteReportByAuthor(authorId: String) {
    reportDataStore.updateData { cacheStorage ->
      val builder = cacheStorage.toBuilder()
      val reports = cacheStorage.reportsMap.values.filter { it.authorId == authorId }
      reports.forEach { builder.removeReports(it.reportId) }
      builder.build()
    }
  }

  override suspend fun clearAll() {
    reportDataStore.updateData { it.toBuilder().clearReports().build() }
  }
}
