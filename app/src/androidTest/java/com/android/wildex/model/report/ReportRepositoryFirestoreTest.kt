package com.android.wildex.model.report

import com.android.wildex.utils.FirestoreTest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val REPORTS_COLLECTION_PATH = "reports"

class ReportRepositoryFirestoreTest : FirestoreTest(REPORTS_COLLECTION_PATH) {

  private var repository = ReportRepositoryFirestore(Firebase.firestore)

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
    assertEquals(report2.status, report.status)

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
}
