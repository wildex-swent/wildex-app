package com.android.wildex.model.report

import com.android.wildex.utils.FirestoreTest
import com.android.wildex.utils.offline.FakeReportCache
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test

private const val REPORTS_COLLECTION_PATH = "reports"

class ReportRepositoryFirestoreTest : FirestoreTest(REPORTS_COLLECTION_PATH) {
  private val reportCache = FakeReportCache()
  private var repository = ReportRepositoryFirestore(Firebase.firestore, reportCache)

  @Test
  fun getNewReportIdReturnsUniqueIds() = runTest {
    val numberIds = 100
    val reportIds = (0 until numberIds).toSet().map { repository.getNewReportId() }.toSet()

    assertEquals(reportIds.size, numberIds)
  }

  @Test
  fun getAllReportsWhenNoReportsExist() = runTest {
    val reports = repository.getAllReports()
    assertTrue(reports.isEmpty())
  }

  @Test
  fun getAllReportsWhenReportsExist() = runTest {
    repository.addReport(report1)
    repository.addReport(report2)

    val reports = repository.getAllReports()

    assertEquals(2, reports.size)
    assertTrue(reports[0] == report1)
    assertTrue(reports[1] == report2)
  }

  @Test
  fun getAllReportsByAuthorWhenNoReportsExist() = runTest {
    val authorId = report1.authorId
    val reports = repository.getAllReportsByAuthor(authorId)

    assertTrue(reports.isEmpty())
  }

  @Test
  fun getAllReportsByAuthorWhenNoCorrespondingAuthorsExist() = runTest {
    repository.addReport(report1)
    repository.addReport(report2)

    val authorId = "NoAuthor"
    val reports = repository.getAllReportsByAuthor(authorId)

    assertTrue(reports.isEmpty())
  }

  @Test
  fun getAllReportsByAuthorWhenCorrespondingAuthorsExist() = runTest {
    repository.addReport(report1)
    repository.addReport(report2)

    for (i in 1..2) {
      val authorId =
          when (i) {
            1 -> report1.authorId
            2 -> report2.authorId
            else -> throw IllegalStateException("Unexpected index")
          }
      val reports = repository.getAllReportsByAuthor(authorId)

      assertEquals(1, reports.size)
      assertTrue(reports[0].authorId == authorId)
    }
  }

  @Test
  fun getAllReportsByAssigneeWhenNoReportsExist() = runTest {
    val assigneeId = report1.assigneeId
    val reports = repository.getAllReportsByAssignee(assigneeId)

    assertTrue(reports.isEmpty())
  }

  @Test
  fun getAllReportsByAssigneeWhenNoCorrespondingAuthorsExist() = runTest {
    repository.addReport(report1)
    repository.addReport(report2)

    val assigneeId = "NoAssignee"
    val reports = repository.getAllReportsByAssignee(assigneeId)

    assertTrue(reports.isEmpty())
  }

  @Test
  fun getAllReportsByAssigneeWhenCorrespondingAuthorsExist() = runTest {
    repository.addReport(report1)
    repository.addReport(report2)

    for (i in 1..2) {
      val assigneeId =
          when (i) {
            1 -> report1.assigneeId
            2 -> report2.assigneeId
            else -> throw IllegalStateException("Unexpected index")
          }
      val reports = repository.getAllReportsByAssignee(assigneeId)

      assertEquals(1, reports.size)
      assertTrue(reports[0].assigneeId == assigneeId)
    }
  }

  @Test
  fun getReportWhenNoReportsExist() = runTest {
    val reportId = report1.reportId
    var exceptionThrown = false

    try {
      repository.getReport(reportId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals("Report not found", e.message)
    }

    assert(exceptionThrown)
  }

  @Test
  fun getReportWhenNoCorrespondingReportsExist() = runTest {
    repository.addReport(report1)

    val reportId = report2.reportId
    var exceptionThrown = false

    try {
      repository.getReport(reportId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals("Report not found", e.message)
    }

    assert(exceptionThrown)
  }

  @Test
  fun getReportWhenCorrespondingReportsExist() = runTest {
    repository.addReport(report1)
    repository.addReport(report2)

    val reportId = report1.reportId
    var exceptionThrown = false
    var report = report2

    try {
      report = repository.getReport(reportId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assert(!exceptionThrown)
    assertEquals(report1, report)
  }

  @Test
  fun addReportWhenIdDoesNotExist() = runTest {
    var exceptionThrown = false

    try {
      repository.addReport(report1)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assert(!exceptionThrown)

    val report = repository.getReport(report1.reportId)
    assertEquals(report1, report)
  }

  @Test
  fun addReportWhenIdAlreadyExists() = runTest {
    repository.addReport(report1)
    var exceptionThrown = false

    try {
      repository.addReport(report1)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals("A Report with reportId '${report1.reportId}' already exists.", e.message)
    }

    assert(exceptionThrown)
  }

  @Test
  fun editReportWhenIdDoesNotExist() = runTest {
    var exceptionThrown = false

    try {
      repository.editReport(report1.reportId, report2)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals("A Report with reportId '${report1.reportId}' does not exist.", e.message)
    }

    assert(exceptionThrown)
  }

  @Test
  fun editReportWhenIdAlreadyExists() = runTest {
    repository.addReport(report1)
    var exceptionThrown = false

    try {
      repository.editReport(report1.reportId, report2)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assert(!exceptionThrown)

    var report = repository.getReport(report1.reportId)
    assertEquals(report1.reportId, report.reportId)
    assertEquals(report2.imageURL, report.imageURL)
    assertEquals(report2.location, report.location)
    assertEquals(report2.date, report.date)
    assertEquals(report2.description, report.description)
    assertEquals(report2.authorId, report.authorId)
    assertEquals(report2.assigneeId, report.assigneeId)

    try {
      repository.getReport(report2.reportId)
    } catch (e: IllegalArgumentException) {
      assertEquals("Report not found", e.message)
    }
  }

  @Test
  fun deleteReportWhenIdDoesNotExist() = runTest {
    var exceptionThrown = false

    try {
      repository.deleteReport(report1.reportId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals("A Report with reportId '${report1.reportId}' does not exist.", e.message)
    }

    assert(exceptionThrown)
  }

  @Test
  fun deleteReportWhenIdExists() = runTest {
    repository.addReport(report1)
    var exceptionThrown = false

    try {
      repository.deleteReport(report1.reportId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assert(!exceptionThrown)

    try {
      repository.getReport(report1.reportId)
    } catch (e: IllegalArgumentException) {
      assertEquals("Report not found", e.message)
    }
  }

  @Test
  fun deleteReportsByUserDeletesCorrectReports() = runTest {
    val report1 = report1
    val report2 = report2
    val report3 = report2.copy(authorId = report1.authorId, reportId = "reportId3")

    repository.addReport(report1)
    repository.addReport(report2)
    repository.addReport(report3)

    repository.deleteReportsByUser(report1.authorId)

    val remainingReports = repository.getAllReports()
    assertEquals(1, remainingReports.size)
    assertTrue(remainingReports.all { it.authorId != report1.authorId })
    assertEquals(listOf(report2), remainingReports)
  }

  @Test
  fun getReportUsesCacheWhenAvailable() = runTest {
    // No Firestore documents added on purpose.
    // Fill only the cache.
    reportCache.saveReport(report1)

    val report = repository.getReport(report1.reportId)

    // If cache wasn't used, this would throw "Report not found"
    assertEquals(report1, report)
  }

  @Test
  fun getAllReportsUsesCacheWhenAvailable() = runTest {
    // Cache contains 2 reports; Firestore collection is empty.
    reportCache.saveReports(listOf(report1, report2))

    val reports = repository.getAllReports()

    assertEquals(2, reports.size)
    assertEquals(report1, reports[0])
    assertEquals(report2, reports[1])
  }

  @Test
  fun getAllReportsByAuthorUsesCacheWhenAvailable() = runTest {
    val authorId = "authorCache"
    val reportA = report1.copy(reportId = "reportA", authorId = authorId)
    val reportB = report2.copy(reportId = "reportB", authorId = "otherAuthor")

    // Only cache is populated
    reportCache.saveReports(listOf(reportA, reportB))

    val reports = repository.getAllReportsByAuthor(authorId)

    assertEquals(1, reports.size)
    assertEquals(authorId, reports[0].authorId)
    assertEquals(reportA, reports[0])
  }

  @Test
  fun getAllReportsByAssigneeUsesCacheWhenAvailable() = runTest {
    val assigneeId = "assigneeCache"
    val reportA = report1.copy(reportId = "reportA", assigneeId = assigneeId)
    val reportB = report2.copy(reportId = "reportB", assigneeId = "otherAssignee")

    reportCache.saveReports(listOf(reportA, reportB))

    val reports = repository.getAllReportsByAssignee(assigneeId)

    assertEquals(1, reports.size)
    assertEquals(assigneeId, reports[0].assigneeId)
    assertEquals(reportA, reports[0])
  }

  @Test
  fun addReportSavesReportInCache() = runTest {
    repository.addReport(report1)

    val cached = reportCache.getReport(report1.reportId)

    assertEquals(report1, cached)
  }

  @Test
  fun editReportUpdatesCache() = runTest {
    repository.addReport(report1)

    val updated = report1.copy(description = "updated description")
    repository.editReport(report1.reportId, updated)

    val cached = reportCache.getReport(report1.reportId)

    // reportId must stay the same, other fields updated
    assertEquals(report1.reportId, cached?.reportId)
    assertEquals("updated description", cached?.description)
  }

  @Test
  fun deleteReportRemovesFromCache() = runTest {
    repository.addReport(report1)

    // ensure it's cached
    assertEquals(report1, reportCache.getReport(report1.reportId))

    repository.deleteReport(report1.reportId)

    val cached = reportCache.getReport(report1.reportId)
    assertNull(cached)
  }

  @Test
  fun deleteReportsByUserAlsoDeletesFromCache() = runTest {
    val r1 = report1
    val r2 = report2
    val r3 = report2.copy(authorId = r1.authorId, reportId = "reportId3")

    repository.addReport(r1)
    repository.addReport(r2)
    repository.addReport(r3)

    // cache should now have 3 reports
    assertEquals(3, reportCache.getAllReports()!!.size)

    repository.deleteReportsByUser(r1.authorId)

    val cachedRemaining = reportCache.getAllReports()!!

    // only reports not authored by r1 remain in cache
    assertTrue(cachedRemaining.none { it.authorId == r1.authorId })
    assertEquals(1, cachedRemaining.size)
    assertEquals(r2, cachedRemaining[0])
  }

  @Test
  fun refreshCacheClearsAllCachedReports() = runTest {
    repository.addReport(report1)
    repository.addReport(report2)

    // cache should not be empty
    assertTrue(reportCache.getAllReports()!!.isNotEmpty())

    repository.refreshCache()

    val cached = reportCache.getAllReports()!!
    assertTrue(cached.isEmpty())
  }

  @Test
  fun documentToReportReturnsNullWhenRequiredFieldIsMissing() {
    runTest {
      val docRef = Firebase.firestore.collection(REPORTS_COLLECTION_PATH).document("badDoc")

      docRef
          .set(
              mapOf(
                  "imageURL" to "url",
                  "location" to mapOf("latitude" to 0.0, "longitude" to 0.0),
                  "date" to report1.date,
                  "authorId" to report1.authorId))
          .await()

      val snapshot = docRef.get().await()
      val result = repository.documentToReport(snapshot)
      assertNull(result)
    }
  }

  @Test
  fun documentToReportReturnsNullWhenLocationIsInvalid() {
    runTest {
      val docRef = Firebase.firestore.collection(REPORTS_COLLECTION_PATH).document("badLocation")

      docRef
          .set(
              mapOf(
                  "imageURL" to "url",
                  "location" to "notAMap",
                  "date" to report1.date,
                  "description" to "desc",
                  "authorId" to report1.authorId))
          .await()

      val snapshot = docRef.get().await()
      val result = repository.documentToReport(snapshot)

      assertNull(result)
    }
  }

  @Test
  fun getAllReportsByNullAssigneeFromFirestore() {
    runTest {
      val unassigned = report1.copy(assigneeId = null)
      repository.addReport(unassigned)
      val reports = repository.getAllReportsByAssignee(null)
      assertEquals(1, reports.size)
      assertNull(reports.first().assigneeId)
    }
  }

  @Test
  fun ensureDocumentExistsThrowsCorrectMessage() {
    runTest {
      val exception = runCatching { repository.deleteReport("nonExistingId") }.exceptionOrNull()

      assertTrue(exception is IllegalArgumentException)
      assertEquals("A Report with reportId 'nonExistingId' does not exist.", exception?.message)
    }
  }
}
