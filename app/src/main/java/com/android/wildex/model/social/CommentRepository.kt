package com.android.wildex.model.social

import com.android.wildex.model.utils.Id

/** Represents a repository that manages Comment items. */
interface CommentRepository {

  /** Generates and returns a new unique identifier for a Comment item. */
  fun getNewCommentId(): Id

  /** Retrieves all Comment items associated with a specific post. */
  suspend fun getAllCommentsByPost(postId: Id): List<Comment>

  /** Retrieves all Comment items associated with a specific report. */
  suspend fun getAllCommentsByReport(reportId: Id): List<Comment>

  /** Adds a new Comment item to the repository. */
  suspend fun addComment(comment: Comment)

  /** Edits an existing Comment item in the repository. */
  suspend fun editComment(commentId: Id, newValue: Comment)

  /** Deletes all Comment items of a post from the repository. */
  suspend fun deleteAllCommentsOfPost(postId: Id)

  /** Deletes all Comment items of a report from the repository. */
  suspend fun deleteAllCommentsOfReport(reportId: Id)

  /** Deletes a Comment item from the repository. */
  suspend fun deleteComment(commentId: Id)

  /** Retrieves all Comment items authored by a specific user. */
  suspend fun getCommentsByUser(userId: Id): List<Comment>
}
