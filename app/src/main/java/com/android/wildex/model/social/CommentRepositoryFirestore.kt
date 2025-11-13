package com.android.wildex.model.social

import com.android.wildex.model.utils.Id
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val COMMENTS_COLLECTION_PATH = "comments"

private object CommentsFields {
  const val PARENT_ID = "parentId"
  const val AUTHOR_ID = "authorId"
  const val TEXT = "text"
  const val DATE = "date"
  const val TAG = "tag"
}

/** Represents a repository that manages Comment items. */
class CommentRepositoryFirestore(private val db: FirebaseFirestore) : CommentRepository {

  private val collection = db.collection(COMMENTS_COLLECTION_PATH)

  /**
   * Generates and returns a new unique identifier for a comment.
   *
   * @return A new unique [String] representing the comment ID.
   */
  override fun getNewCommentId(): String {
    return collection.document().id
  }

  /**
   * Retrieves all Comment items associated with a specific post.
   *
   * @param postId The ID of the post for which to retrieve comments.
   * @return A list of [Comment] items associated with the specified post.
   */
  override suspend fun getAllCommentsByPost(postId: String): List<Comment> {
    return collection
        .whereEqualTo(CommentsFields.PARENT_ID, postId)
        .whereEqualTo(CommentsFields.TAG, CommentTag.POST_COMMENT)
        .get()
        .await()
        .documents
        .mapNotNull { documentToComment(it) }
  }

  /**
   * Retrieves all Comment items associated with a specific report.
   *
   * @param reportId The ID of the report for which to retrieve comments.
   * @return A list of [Comment] items associated with the specified report.
   */
  override suspend fun getAllCommentsByReport(reportId: Id): List<Comment> {
    return collection
        .whereEqualTo(CommentsFields.PARENT_ID, reportId)
        .whereEqualTo(CommentsFields.TAG, CommentTag.REPORT_COMMENT)
        .get()
        .await()
        .documents
        .mapNotNull { documentToComment(it) }
  }

  /**
   * Adds a new Comment item to the repository.
   *
   * @param comment The [Comment] item to be added to the repository.
   */
  override suspend fun addComment(comment: Comment) {
    val docRef = collection.document(comment.commentId)
    ensureDocumentDoesNotExist(docRef, comment.commentId)
    docRef.set(comment).await()
  }

  /**
   * Edits an existing Comment item in the repository.
   *
   * @param commentId The ID of the comment to edit.
   * @param newValue The new value for the comment.
   */
  override suspend fun editComment(commentId: String, newValue: Comment) {
    val docRef = collection.document(commentId)
    ensureDocumentExists(docRef, commentId)
    docRef.set(newValue).await()
  }

  /**
   * Deletes all Comment items of a post from the repository.
   *
   * @param postId The ID of the parent whose comments are to be deleted.
   */
  override suspend fun deleteAllCommentsOfPost(postId: String) {
    getAllCommentsByPost(postId).forEach {
      if (it.tag == CommentTag.POST_COMMENT) {
        deleteComment(it.commentId)
      }
    }
  }

  /**
   * Deletes all Comment items of a report from the repository.
   *
   * @param reportId The ID of the report whose comments are to be deleted.
   */
  override suspend fun deleteAllCommentsOfReport(reportId: Id) {
    getAllCommentsByReport(reportId).forEach {
      if (it.tag == CommentTag.REPORT_COMMENT) {
        deleteComment(it.commentId)
      }
    }
  }

  /**
   * Deletes a Comment item from the repository.
   *
   * @param commentId The ID of the comment to delete.
   */
  override suspend fun deleteComment(commentId: String) {
    val docRef = collection.document(commentId)
    ensureDocumentExists(docRef, commentId)
    docRef.delete().await()
  }

  /**
   * Retrieves all Comment items authored by a specific user.
   *
   * @param userId The ID of the user whose comments are to be retrieved.
   * @return A list of [Comment] items authored by the specified user.
   */
  override suspend fun getCommentsByUser(userId: String): List<Comment> {
    return collection
        .whereEqualTo(CommentsFields.AUTHOR_ID, userId)
        .get()
        .await()
        .documents
        .mapNotNull { documentToComment(it) }
  }

  override suspend fun deleteCommentsByUser(userId: Id) {
    collection.whereEqualTo(CommentsFields.AUTHOR_ID, userId).get().await().documents.forEach {
      it.reference.delete().await()
    }
  }

  /**
   * Ensures no Comment item in the document reference has a specific commentId.
   *
   * @param docRef The document reference containing all comments.
   * @param commentId The ID that no comment should have.
   */
  private suspend fun ensureDocumentDoesNotExist(docRef: DocumentReference, commentId: String) {
    val doc = docRef.get().await()
    require(!doc.exists()) { "A Comment with commentId '${commentId}' already exists." }
  }

  /**
   * Ensures one Comment item in the document reference has a specific commentId.
   *
   * @param docRef The document reference containing all comments.
   * @param commentId The ID that one comment should have.
   */
  private suspend fun ensureDocumentExists(docRef: DocumentReference, commentId: String) {
    val doc = docRef.get().await()
    require(doc.exists()) { "A Comment with commentId '${commentId}' does not exist." }
  }

  /**
   * Converts a document reference to a comment.
   *
   * @param document The document to be converted into a comment.
   * @return The transformed [Comment] object.
   */
  private fun documentToComment(document: DocumentSnapshot): Comment? {
    return try {
      val commentId = document.id
      val parentId =
          document.getString(CommentsFields.PARENT_ID)
              ?: throwMissingFieldException(CommentsFields.PARENT_ID)
      val authorId =
          document.getString(CommentsFields.AUTHOR_ID)
              ?: throwMissingFieldException(CommentsFields.AUTHOR_ID)
      val text =
          document.getString(CommentsFields.TEXT) ?: throwMissingFieldException(CommentsFields.TEXT)
      val date =
          document.getTimestamp(CommentsFields.DATE)
              ?: throwMissingFieldException(CommentsFields.DATE)
      val tagData =
          document.getString(CommentsFields.TAG) ?: throwMissingFieldException(CommentsFields.TAG)
      val tag = CommentTag.valueOf(tagData.uppercase())

      Comment(
          commentId = commentId,
          parentId = parentId,
          authorId = authorId,
          text = text,
          date = date,
          tag = tag)
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
    throw IllegalArgumentException("Missing required field in Comment: $field")
  }
}
