package com.android.wildex.model.social

/** Represents a repository that manages Comment items. */
interface CommentsRepository {

  /** Generates and returns a new unique identifier for a Comment item. */
  fun getNewCommentId(): String

  /** Retrieves all Comment items associated with a specific post. */
  suspend fun getAllCommentsByPost(postId: String): List<Comment>

  /** Adds a new Comment item to the repository. */
  suspend fun addComment(comment: Comment)

  /** Edits an existing Comment item in the repository. */
  suspend fun editComment(commentID: String, newValue: Comment)

  /** Deletes a Comment item from the repository. */
  suspend fun deleteComment(commentId: String)
}
