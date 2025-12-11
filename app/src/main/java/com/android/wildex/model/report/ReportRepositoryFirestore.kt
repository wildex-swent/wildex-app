package com.android.wildex.model.report

import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val REPORTS_COLLECTION_PATH = "reports"

private object ReportsFields {
  const val IMAGE_URL = "imageURL"
  const val LOCATION = "location"
  const val DATE = "date"
  const val DESCRIPTION = "description"
  const val AUTHOR_ID = "authorId"
  const val ASSIGNEE_ID = "assigneeId"
}

/** Represents a repository that manages Report items. */
class ReportRepositoryFirestore(private val db: FirebaseFirestore) : ReportRepository {

  private val collection = db.collection(REPORTS_COLLECTION_PATH)

  /**
   * Generates and returns a new unique identifier for a report.
   *
   * @return A new unique [String] representing the report ID.
   */
  override fun getNewReportId(): Id {
    return collection.document().id
  }

  /**
   * Retrieves all Reports items from the repository.
   *
   * @return A list of all [Report] items.
   */
  override suspend fun getAllReports(): List<Report> {
    return collection.get().await().documents.mapNotNull { documentToReport(it) }
  }

  /**
   * Retrieves all Reports items associated with a specific author.
   *
   * @param authorId The ID of the author of the reports to retrieve.
   * @return A list of [Report] items associated with the specified author.
   */
  override suspend fun getAllReportsByAuthor(authorId: Id): List<Report> {
    return collection
        .whereEqualTo(ReportsFields.AUTHOR_ID, authorId)
        .get()
        .await()
        .documents
        .mapNotNull { documentToReport(it) }
  }

  /**
   * Retrieves all Reports items associated with a specific assignee. If given a null assigneeId, it
   * retrieves all unassigned reports.
   *
   * @param assigneeId The ID of the assignee of the reports to retrieve.
   * @return A list of [Report] items associated with the specified assignee.
   */
  override suspend fun getAllReportsByAssignee(assigneeId: Id?): List<Report> {
    return collection
        .whereEqualTo(ReportsFields.ASSIGNEE_ID, assigneeId)
        .get()
        .await()
        .documents
        .mapNotNull { documentToReport(it) }
  }

  /**
   * Retrieves a specific Report item by its unique identifier.
   *
   * @param reportId The ID of the report to retrieve.
   * @return The [Report] item associated with the identifier.
   */
  override suspend fun getReport(reportId: Id): Report {
    return documentToReport(collection.document(reportId).get().await())
        ?: throw IllegalArgumentException("Report not found")
  }

  /**
   * Adds a new Report item to the repository.
   *
   * @param report The [Report] item to be added to the repository.
   */
  override suspend fun addReport(report: Report) {
    val docRef = collection.document(report.reportId)
    ensureDocumentDoesNotExist(docRef, report.reportId)
    docRef.set(report).await()
  }

  /**
   * Edits an existing Report item in the repository.
   *
   * @param reportId The ID of the report to edit.
   * @param newValue The new value for the report.
   */
  override suspend fun editReport(reportId: Id, newValue: Report) {
    val docRef = collection.document(reportId)
    ensureDocumentExists(docRef, reportId)
    docRef.set(newValue).await()
  }

  /**
   * Deletes a Report item from the repository.
   *
   * @param reportId The ID of the report to delete.
   */
  override suspend fun deleteReport(reportId: Id) {
    val docRef = collection.document(reportId)
    ensureDocumentExists(docRef, reportId)
    docRef.delete().await()
  }

  override suspend fun deleteReportsByUser(userId: Id) {
    collection.whereEqualTo("authorId", userId).get().await().documents.forEach {
      it.reference.delete().await()
    }
  }

  /**
   * Ensures no Report item in the document reference has a specific reportId.
   *
   * @param docRef The document reference containing all reports.
   * @param reportId The ID that no report should have.
   */
  private suspend fun ensureDocumentDoesNotExist(docRef: DocumentReference, reportId: String) {
    val doc = docRef.get().await()
    require(!doc.exists()) { "A Report with reportId '${reportId}' already exists." }
  }

  /**
   * Ensures one Report item in the document reference has a specific reportId.
   *
   * @param docRef The document reference containing all reports.
   * @param reportId The ID that one report should have.
   */
  private suspend fun ensureDocumentExists(docRef: DocumentReference, reportId: String) {
    val doc = docRef.get().await()
    require(doc.exists()) { "A Report with reportId '${reportId}' does not exist." }
  }

  /**
   * Converts a document reference to a report.
   *
   * @param document The document to be converted into a report.
   * @return The transformed [Report] object.
   */
  fun documentToReport(document: DocumentSnapshot): Report? {
    return try {
      val reportId = document.id
      val imageURL =
          document.getString(ReportsFields.IMAGE_URL)
              ?: throwMissingFieldException(ReportsFields.IMAGE_URL)
      val locationData =
          document[ReportsFields.LOCATION] as? Map<*, *>
              ?: throwMissingFieldException(ReportsFields.LOCATION)
      val location =
          locationData.let {
            Location(
                latitude = it["latitude"] as? Double ?: 0.0,
                longitude = it["longitude"] as? Double ?: 0.0,
                name = it["name"] as? String ?: "",
                specificName = it["specificName"] as? String ?: "",
                generalName = it["generalName"] as? String ?: "")
          }
      val date =
          document.getTimestamp(ReportsFields.DATE)
              ?: throwMissingFieldException(ReportsFields.DATE)
      val description =
          document.getString(ReportsFields.DESCRIPTION)
              ?: throwMissingFieldException(ReportsFields.DESCRIPTION)
      val authorId =
          document.getString(ReportsFields.AUTHOR_ID)
              ?: throwMissingFieldException(ReportsFields.AUTHOR_ID)
      val assigneeId = document.getString(ReportsFields.ASSIGNEE_ID)

      Report(
          reportId = reportId,
          imageURL = imageURL,
          location = location,
          date = date,
          description = description,
          authorId = authorId,
          assigneeId = assigneeId)
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Throws an error for the missing comment field.
   *
   * @param field The name of the missing field.
   * @throws IllegalArgumentException if the field is missing.
   */
  private fun throwMissingFieldException(field: String): Nothing {
    throw IllegalArgumentException("Missing required field in Report: $field")
  }
}
