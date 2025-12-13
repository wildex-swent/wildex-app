package com.android.wildex.model.report

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.android.wildex.datastore.ReportCacheStorage
import com.android.wildex.datastore.ReportProto
import com.android.wildex.model.cache.report.ReportCache
import com.android.wildex.model.cache.report.ReportCacheSerializer
import com.android.wildex.model.cache.report.reportDataStore
import com.android.wildex.model.cache.report.toProto
import com.android.wildex.model.cache.report.toReport
import com.android.wildex.model.utils.Location
import com.android.wildex.utils.FirebaseEmulator
import com.android.wildex.utils.FirestoreTest
import com.android.wildex.utils.offline.FakeConnectivityObserver
import com.android.wildex.utils.offline.TestContext
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Date

private const val REPORTS_COLLECTION_PATH = "reports"

@OptIn(ExperimentalCoroutinesApi::class)
class ReportCacheTest : FirestoreTest(REPORTS_COLLECTION_PATH) {
  private lateinit var dataStore: DataStore<ReportCacheStorage>
  private lateinit var context: Context
  private lateinit var connectivityObserver: FakeConnectivityObserver
  private lateinit var cache: ReportCache
  private lateinit var db: FirebaseFirestore
  private lateinit var reportRepository: ReportRepositoryFirestore
  private val testScope = TestScope(UnconfinedTestDispatcher())
  private val staleTime = System.currentTimeMillis() - (20 * 60 * 1000L)

  private val testReport =
      Report(
          reportId = "reportId1",
          imageURL = "imageURL1",
          location = Location(0.3, 0.3),
          date = Timestamp(Date(0)),
          description = "description1",
          authorId = "authorId1",
          assigneeId = "assigneeId1",
      )

  @Before
  override fun setUp() {
    super.setUp()
    val appContext = ApplicationProvider.getApplicationContext<Context>()
    dataStore =
        DataStoreFactory.create(serializer = ReportCacheSerializer, scope = testScope) {
          File.createTempFile("reportcache", ".pb")
        }
    context = TestContext(dataStore, appContext)
    connectivityObserver = FakeConnectivityObserver(initial = true)
    cache = ReportCache(dataStore, connectivityObserver)
    db = FirebaseEmulator.firestore
    reportRepository = ReportRepositoryFirestore(db, cache)
  }

  @Test
  fun offlineReadsFromReportCache() {
    runTest {
      connectivityObserver.setOnline(false)
      reportRepository.addReport(testReport)
      db.collection(REPORTS_COLLECTION_PATH)
          .document(testReport.reportId)
          .update("description", "tampered")
          .await()
      val result = reportRepository.getReport(testReport.reportId)
      assertEquals(testReport.reportId, result.reportId)
      assertEquals(testReport, result)
    }
  }

  @Test
  fun onlineAndStaleReportFetchesFromFirestore() {
    runTest {
      connectivityObserver.setOnline(true)
      reportRepository.addReport(testReport)

      db.collection(REPORTS_COLLECTION_PATH)
          .document(testReport.reportId)
          .update("description", "FIRESTORE_VALUE")
          .await()

      val staleProto =
          testReport
              .copy(description = "CACHED_OLD_VALUE")
              .toProto()
              .toBuilder()
              .setLastUpdated(staleTime)
              .build()

      dataStore.updateData { it.toBuilder().putReports(testReport.reportId, staleProto).build() }
      assertNull(cache.getReport(testReport.reportId))

      val result = reportRepository.getReport(testReport.reportId)
      assertEquals(testReport.reportId, result.reportId)
      assertEquals("FIRESTORE_VALUE", result.description)
    }
  }

  @Test
  fun onlineButFreshReportReadsFromCache() {
    runTest {
      connectivityObserver.setOnline(true)
      reportRepository.addReport(testReport)

      val freshTime = System.currentTimeMillis() - (5 * 60 * 1000L)
      val freshProto = testReport.toProto().toBuilder().setLastUpdated(freshTime).build()

      context.reportDataStore.updateData {
        it.toBuilder().putReports(testReport.reportId, freshProto).build()
      }

      assertEquals(testReport, cache.getReport(testReport.reportId))
      assertEquals(testReport, reportRepository.getReport(testReport.reportId))
    }
  }

  @Test
  fun offlineAndEmptyCacheReturnsEmptyList() {
    runTest {
      connectivityObserver.setOnline(false)
      cache.clearAll()

      assertEquals(emptyList<Report>(), cache.getAllReports())
    }
  }

  @Test
  fun onlineAndStaleCacheReturnsNull() {
    runTest {
      connectivityObserver.setOnline(true)
      val staleProto = testReport.toProto().toBuilder().setLastUpdated(staleTime).build()
      dataStore.updateData { it.toBuilder().putReports(testReport.reportId, staleProto).build() }

      assertNull(cache.getAllReports())
    }
  }

  @Test
  fun getAllReportsByAuthorReturnsOnlyMatchingReports() {
    runTest {
      connectivityObserver.setOnline(true)
      val otherReport = testReport.copy(reportId = "r2", authorId = "authorId2")
      cache.saveReports(listOf(testReport, otherReport))

      val result = cache.getAllReportsByAuthor("authorId1")
      assertEquals(1, result?.size)
      assertEquals(testReport, result?.first())
    }
  }

  @Test
  fun getAllReportsByInexistentAssigneeReturnsNull() {
    runTest {
      connectivityObserver.setOnline(true)
      val unassigned = testReport.copy(reportId = "r2", assigneeId = null)
      cache.saveReports(listOf(testReport, unassigned))

      assertNull(cache.getAllReportsByAssignee("me"))
    }
  }

  @Test
  fun getAllReportsByNullAssigneeReturnsEmptyList() {
    runTest {
      connectivityObserver.setOnline(false)
      val unassigned = testReport.copy(reportId = "r2", assigneeId = null)
      cache.saveReports(listOf(testReport, unassigned))

      val result = cache.getAllReportsByAssignee(null)
      assertEquals(1, result?.size)
      assertEquals(unassigned, result?.first())
    }
  }

  @Test
  fun deleteReportRemovesItFromCache() {
    runTest {
      connectivityObserver.setOnline(true)
      cache.saveReport(testReport)
      cache.deleteReport(testReport.reportId)

      assertNull(cache.getReport(testReport.reportId))
    }
  }

  @Test
  fun deleteReportByAuthorRemovesAllMatchingReports() {
    runTest {
      val otherAuthor = testReport.copy(reportId = "r2", authorId = "other")
      val otherAuthor2 = otherAuthor.copy(reportId = "r3")
      cache.saveReports(listOf(testReport, otherAuthor, otherAuthor2))
      cache.deleteReportByAuthor("other")

      val remaining = cache.getAllReports()
      assertEquals(1, remaining?.size)
      assertEquals("authorId1", remaining?.first()?.authorId)
    }
  }

  @Test
  fun clearAllRemovesEverything() {
    runTest {
      cache.saveReports(listOf(testReport, report1.copy(reportId = "id3"), report2))
      cache.clearAll()

      assertNull(cache.getAllReports())
    }
  }
}
