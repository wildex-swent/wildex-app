package com.android.wildex.model.comment

import com.android.wildex.model.social.CommentRepositoryFirestore
import com.android.wildex.utils.FirestoreTest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val COMMENTS_COLLECTION_PATH = "comments"

class CommentRepositoryFirestoreTest : FirestoreTest(COMMENTS_COLLECTION_PATH) {

  private var repository = CommentRepositoryFirestore(Firebase.firestore)

  @Test
  fun getNewCommentIdReturnsUniqueIDs() = runTest {
    val numberIds = 100
    val postIds = (0 until numberIds).toSet().map { repository.getNewCommentId() }.toSet()

    assertEquals(postIds.size, numberIds)
  }

  @Test
  fun getAllCommentsByPostWhenNoPostsExist() = runTest {
    val postId = comment1.postId
    val comments = repository.getAllCommentsByPost(postId)

    assertTrue(comments.isEmpty())
  }

  @Test
  fun getAllCommentsByPostWhenNoCorrespondingPostsExist() = runTest {
    repository.addComment(comment1)
    repository.addComment(comment2)

    val postId = "noPost"
    val comments = repository.getAllCommentsByPost(postId)

    assertTrue(comments.isEmpty())
  }

  @Test
  fun getAllCommentsByPostWhenCorrespondingPostsExist() = runTest {
    repository.addComment(comment1)
    repository.addComment(comment2)

    for (i in 1..2) {
      val postId =
          when (i) {
            1 -> comment1.postId
            2 -> comment2.postId
            else -> throw IllegalStateException("Unexpected comment index")
          }
      val comments = repository.getAllCommentsByPost(postId)

      assertEquals(1, comments.size)
      assertTrue(comments.all { it.postId == postId })
    }
  }

  @Test
  fun addCommentWhenIdDoesNotExist() = runTest {
    var exceptionThrown = false

    try {
      repository.addComment(comment1)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assert(!exceptionThrown)

    val comments = repository.getAllCommentsByPost(comment1.postId)
    assertEquals(1, comments.size)
    assertEquals(comment1, comments[0])
  }

  @Test
  fun addCommentWhenIdAlreadyExists() = runTest {
    repository.addComment(comment1)
    var exceptionThrown = false

    try {
      repository.addComment(comment1)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals("A Comment with commentId '${comment1.commentId}' already exists.", e.message)
    }

    assert(exceptionThrown)
  }

  @Test
  fun editCommentWhenIdDoesNotExist() = runTest {
    var exceptionThrown = false

    try {
      repository.editComment(comment1.commentId, comment2)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals("A Comment with commentId '${comment1.commentId}' does not exist.", e.message)
    }

    assert(exceptionThrown)
  }

  @Test
  fun editCommentWhenIdAlreadyExists() = runTest {
    repository.addComment(comment1)
    var exceptionThrown = false

    try {
      repository.editComment(comment1.commentId, comment2)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assert(!exceptionThrown)

    var comments = repository.getAllCommentsByPost(comment1.postId)
    assertTrue(comments.isEmpty())

    comments = repository.getAllCommentsByPost(comment2.postId)
    assertEquals(1, comments.size)
    assertEquals(comment1.commentId, comments[0].commentId)
    assertEquals(comment2.postId, comments[0].postId)
    assertEquals(comment2.authorId, comments[0].authorId)
    assertEquals(comment2.text, comments[0].text)
    assertEquals(comment2.date, comments[0].date)
  }

  @Test
  fun deleteCommentWhenIdDoesNotExist() = runTest {
    var exceptionThrown = false

    try {
      repository.deleteComment(comment1.commentId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals("A Comment with commentId '${comment1.commentId}' does not exist.", e.message)
    }

    assert(exceptionThrown)
  }

  @Test
  fun deleteCommentWhenIdExists() = runTest {
    repository.addComment(comment1)
    var exceptionThrown = false

    try {
      repository.deleteComment(comment1.commentId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assert(!exceptionThrown)

    val comments = repository.getAllCommentsByPost(comment1.postId)
    assertTrue(comments.isEmpty())
  }

  @Test
  fun getCommentByUserWhenNoCommentsExist() = runTest {
    val userId = "noUser"
    val comments = repository.getCommentByUser(userId)

    assertTrue(comments.isEmpty())
  }

  @Test
  fun getCommentByUserReturnsOnlyRequestedAuthorComments() = runTest {
    repository.addComment(comment1)
    repository.addComment(comment2)

    val targetUserId = comment1.authorId
    val expectedCount = listOf(comment1, comment2).count { it.authorId == targetUserId }

    val comments = repository.getCommentByUser(targetUserId)

    assertEquals(expectedCount, comments.size)
    assertTrue(comments.all { it.authorId == targetUserId })
  }
}
