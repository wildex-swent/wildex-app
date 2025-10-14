package com.android.wildex.model.social

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val COMMENTS_COLLECTION_PATH = "comments"

private object CommentsFields {
  const val POST_ID = "postId"
  const val AUTHOR_ID = "authorId"
  const val TEXT = "text"
  const val DATE = "date"
}

class CommentsRepositoryFirestore(private val db: FirebaseFirestore) : CommentsRepository {

  private val collection = db.collection(COMMENTS_COLLECTION_PATH)

  override fun getNewCommentId(): String {
    return collection.document().id
  }

  override suspend fun getAllCommentsByPost(postId: String): List<Comment> {
    return collection
        .whereEqualTo(CommentsFields.POST_ID, postId)
        .get()
        .await()
        .documents
        .mapNotNull { documentToComment(it) }
  }

  override suspend fun addComment(comment: Comment) {
    val docRef = collection.document(comment.commentId)
    ensureDocumentDoesNotExist(docRef, comment.commentId)
    docRef.set(comment).await()
  }

  override suspend fun editComment(commentId: String, newValue: Comment) {
    val docRef = collection.document(commentId)
    ensureDocumentExists(docRef, commentId)
    docRef.set(newValue).await()
  }

  override suspend fun deleteComment(commentId: String) {
    val docRef = collection.document(commentId)
    ensureDocumentExists(docRef, commentId)
    docRef.delete().await()
  }

  private suspend fun ensureDocumentDoesNotExist(docRef: DocumentReference, commentId: String) {
    val doc = docRef.get().await()
    if (doc.exists()) {
      throw IllegalArgumentException("A Comment with commentId '${commentId}' already exists.")
    }
  }

  private suspend fun ensureDocumentExists(docRef: DocumentReference, commentId: String) {
    val doc = docRef.get().await()
    if (!doc.exists()) {
      throw IllegalArgumentException("A Comment with commentId '${commentId}' does not exist.")
    }
  }

  private fun documentToComment(document: DocumentSnapshot): Comment? {
    return try {
      val commentId = document.id
      val postId =
          document.getString(CommentsFields.POST_ID)
              ?: throwMissingFieldException(CommentsFields.POST_ID)
      val authorId =
          document.getString(CommentsFields.AUTHOR_ID)
              ?: throwMissingFieldException(CommentsFields.AUTHOR_ID)
      val text =
          document.getString(CommentsFields.TEXT) ?: throwMissingFieldException(CommentsFields.TEXT)
      val date =
          document.getTimestamp(CommentsFields.DATE)
              ?: throwMissingFieldException(CommentsFields.DATE)

      Comment(commentId = commentId, postId = postId, authorId = authorId, text = text, date = date)
    } catch (e: Exception) {
      Log.e(
          "CommentsRepositoryFirestore",
          "documentToComment: error converting document ${document.id} to Comment",
          e)

      null
    }
  }

  private fun throwMissingFieldException(field: String): Nothing {
    throw IllegalArgumentException("Missing required field in Comment: $field")
  }
}
