package com.android.wildex.model.social

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

const val LIKE_COLLECTION_PATH = "likes"

class LikeRepositoryFirestore(private val db: FirebaseFirestore) : LikeRepository {

  override fun getNewLikeId(): String {
    return db.collection(LIKE_COLLECTION_PATH).document().id
  }

  override suspend fun getAllLikesByCurrentUser(): List<Like> {
    val currentUserId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("LikeRepositoryFirestore: User not logged in.")
    val collection =
        db.collection(LIKE_COLLECTION_PATH).whereEqualTo("userId", currentUserId).get().await()
    val docs = collection.documents
    return docs.mapNotNull { convertToLike(it) }
  }

  override suspend fun getLikesForPost(postId: String): List<Like> {
    val collection =
        db.collection(LIKE_COLLECTION_PATH).whereEqualTo("postId", postId).get().await()
    val docs = collection.documents
    return docs.mapNotNull { convertToLike(it) }
  }

  override suspend fun getLikeForPost(postId: String): Like? {
    val currentUserId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("LikeRepositoryFirestore: User not logged in.")
    val likeDocs =
        db.collection(LIKE_COLLECTION_PATH)
            .whereEqualTo("postId", postId)
            .whereEqualTo("userId", currentUserId)
            .get()
            .await()
    val docs = likeDocs.documents.mapNotNull { convertToLike(it) }
    return docs.firstOrNull()
  }

  override suspend fun addLike(like: Like) {
    val docRef = db.collection(LIKE_COLLECTION_PATH).document(like.likeId)
    val doc = docRef.get().await()
    if (doc.exists()) {
      throw IllegalArgumentException("A Like with likeId '${like.likeId}' already exists.")
    }
    docRef.set(like).await()
  }

  override suspend fun deleteLike(likeId: String) {
    val docRef = db.collection(LIKE_COLLECTION_PATH).document(likeId)
    val doc = docRef.get().await()

    if (!doc.exists()) {
      throw IllegalArgumentException("Like with given Id not found")
    }

    docRef.delete().await()
  }

  private fun convertToLike(doc: DocumentSnapshot): Like? {
    return try {
      val likeId = doc.id
      val postId = doc.getString("postId") ?: throwMissingFieldException("postId")
      val userId = doc.getString("userId") ?: throwMissingFieldException("userId")

      Like(likeId, postId, userId)
    } catch (e: Exception) {
      Log.e("LikeRepositoryFirestore", "Error converting document to Like", e)
      null
    }
  }

  private fun throwMissingFieldException(field: String): Nothing {
    throw IllegalArgumentException("Missing required field in LikeRepository: $field")
  }
}
