package com.android.wildex.model.social

import android.util.Log
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

const val POST_COLLECTION_PATH = "posts"

class PostsRepositoryFirestore(private val db: FirebaseFirestore) : PostsRepository {

  private val ownerAttributeName = "authorId"

  override fun getNewPostId(): String {
    return db.collection(POST_COLLECTION_PATH).document().id
  }

  override suspend fun getAllPosts(): List<Post> {
    val collection = db.collection(POST_COLLECTION_PATH).get().await()
    val docs = collection.documents
    if (docs.isEmpty()) {
      Log.w("PostsRepositoryFirestore", "No posts found in the collection.")
      return emptyList()
    }

    return docs.mapNotNull { convertToPost(it) }
  }

  override suspend fun getAllPostsByAuthor(): List<Post> {
    val authorId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("PostsRepositoryFirestore: User not logged in.")
    val collection =
        db.collection(POST_COLLECTION_PATH).whereEqualTo(ownerAttributeName, authorId).get().await()
    val docs = collection.documents
    return docs.mapNotNull { convertToPost(it) }
  }

  override suspend fun getAllPostsByGivenAuthor(authorId: Id): List<Post> {
    val collection =
        db.collection(POST_COLLECTION_PATH).whereEqualTo(ownerAttributeName, authorId).get().await()
    val docs = collection.documents
    if (docs.isEmpty()) {
      Log.w("PostsRepositoryFirestore", "No posts found for authorId: $authorId")
      return emptyList()
    }
    return docs.mapNotNull { convertToPost(it) }
  }

  override suspend fun getPost(postId: Id): Post {
    val doc = db.collection(POST_COLLECTION_PATH).document(postId).get().await()
    require(doc.exists())
    return convertToPost(doc) ?: throw IllegalArgumentException("Post with given Id not found")
  }

  override suspend fun addPost(post: Post) {
    val docRef = db.collection(POST_COLLECTION_PATH).document(post.postId)
    val doc = docRef.get().await()
    require(!doc.exists())
    docRef.set(post).await()
  }

  override suspend fun editPost(postId: Id, newValue: Post) {
    val docRef = db.collection(POST_COLLECTION_PATH).document(postId)
    val doc = docRef.get().await()
    require(doc.exists())
    docRef.set(newValue).await()
  }

  override suspend fun deletePost(postId: String) {
    val docRef = db.collection(POST_COLLECTION_PATH).document(postId)
    val doc = docRef.get().await()
    require(doc.exists())
    docRef.delete().await()
  }

  override suspend fun deletePostsByUser(userId: Id) {
    db.collection(POST_COLLECTION_PATH)
      .whereEqualTo("authorId", userId)
      .get()
      .await()
      .documents
      .forEach { it.reference.delete().await() }
  }

  private fun convertToPost(doc: DocumentSnapshot): Post? {
    return try {
      val postId = doc.id
      val authorId = doc.getString("authorId") ?: throwMissingFieldException("authorId")
      val pictureURL = doc.getString("pictureURL") ?: throwMissingFieldException("pictureURL")
      val locationData = doc.get("location") as? Map<*, *>
      val location =
          locationData?.let {
            Location(
                latitude = it["latitude"] as? Double ?: 0.0,
                longitude = it["longitude"] as? Double ?: 0.0,
                name = it["name"] as? String ?: "",
            )
          }
      val date = doc.getTimestamp("date") ?: throwMissingFieldException("date")
      val animalId = doc.getString("animalId") ?: throwMissingFieldException("animalId")
      val description = doc.getString("description") ?: ""
      val likesCount = doc.getLong("likesCount")?.toInt() ?: 0
      val commentsCount = doc.getLong("commentsCount")?.toInt() ?: 0

      Post(
          postId = postId,
          authorId = authorId,
          pictureURL = pictureURL,
          location = location,
          description = description,
          date = date,
          animalId = animalId,
          likesCount = likesCount,
          commentsCount = commentsCount,
      )
    } catch (e: Exception) {
      Log.e("PostsRepositoryFirestore", "Error converting document to Post", e)
      null
    }
  }

  private fun throwMissingFieldException(field: String): Nothing {
    throw IllegalArgumentException("Missing required field in PostRepository: $field")
  }
}
