package com.android.wildex.model.social

import android.util.Log
import com.android.wildex.model.cache.posts.IPostsCache
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

const val POST_COLLECTION_PATH = "posts"

class PostsRepositoryFirestore(private val db: FirebaseFirestore, private val cache: IPostsCache) :
    PostsRepository {

  private val ownerAttributeName = "authorId"

  override fun getNewPostId(): String {
    return db.collection(POST_COLLECTION_PATH).document().id
  }

  override suspend fun getAllPosts(): List<Post> {
    cache.getAllPosts()?.let {
      return it
    }
    val collection = db.collection(POST_COLLECTION_PATH).get().await()
    val docs = collection.documents
    if (docs.isEmpty()) {
      Log.w("PostsRepositoryFirestore", "No posts found in the collection.")
      return emptyList()
    }

    val posts = docs.mapNotNull { convertToPost(it) }
    cache.savePosts(posts)
    return posts
  }

  override suspend fun getAllPostsByAuthor(): List<Post> {
    val authorId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("PostsRepositoryFirestore: User not logged in.")
    cache.getAllPostsByAuthor(authorId)?.let {
      return it
    }
    val collection =
        db.collection(POST_COLLECTION_PATH).whereEqualTo(ownerAttributeName, authorId).get().await()
    val docs = collection.documents
    val posts = docs.mapNotNull { convertToPost(it) }
    cache.savePosts(posts)
    return posts
  }

  override suspend fun getAllPostsByGivenAuthor(authorId: Id): List<Post> {
    cache.getAllPostsByAuthor(authorId)?.let {
      return it
    }
    val collection =
        db.collection(POST_COLLECTION_PATH).whereEqualTo(ownerAttributeName, authorId).get().await()
    val docs = collection.documents
    if (docs.isEmpty()) {
      Log.w("PostsRepositoryFirestore", "No posts found for authorId: $authorId")
      return emptyList()
    }
    val posts = docs.mapNotNull { convertToPost(it) }
    cache.savePosts(posts)
    return posts
  }

  override suspend fun getPost(postId: Id): Post {
    cache.getPost(postId)?.let {
      return it
    }
    val doc = db.collection(POST_COLLECTION_PATH).document(postId).get().await()
    require(doc.exists())
    val post = convertToPost(doc) ?: throw IllegalArgumentException("Post with given Id not found")
    cache.savePost(post)
    return post
  }

  override suspend fun addPost(post: Post) {
    val docRef = db.collection(POST_COLLECTION_PATH).document(post.postId)
    val doc = docRef.get().await()
    require(!doc.exists())
    docRef.set(post).await()
    cache.savePost(post)
  }

  override suspend fun editPost(postId: Id, newValue: Post) {
    val docRef = db.collection(POST_COLLECTION_PATH).document(postId)
    val doc = docRef.get().await()
    require(doc.exists())
    docRef.set(newValue).await()
    cache.savePost(newValue)
  }

  override suspend fun deletePost(postId: String) {
    val docRef = db.collection(POST_COLLECTION_PATH).document(postId)
    val doc = docRef.get().await()
    require(doc.exists())
    docRef.delete().await()
    cache.deletePost(postId)
  }

  override suspend fun deletePostsByUser(userId: Id) {
    db.collection(POST_COLLECTION_PATH)
        .whereEqualTo("authorId", userId)
        .get()
        .await()
        .documents
        .forEach { it.reference.delete().await() }
    cache.deletePostsByUser(userId)
  }

  override suspend fun refreshCache() {
    cache.clearAll()
  }

  private fun convertToPost(doc: DocumentSnapshot): Post? {
    return try {
      val postId = doc.id
      val authorId = doc.getString("authorId") ?: throwMissingFieldException("authorId")
      val pictureURL = doc.getString("pictureURL") ?: throwMissingFieldException("pictureURL")
      val locationData = doc["location"] as? Map<*, *>
      val location =
          locationData?.let {
            Location(
                latitude = it["latitude"] as? Double ?: 0.0,
                longitude = it["longitude"] as? Double ?: 0.0,
                name = it["name"] as? String ?: "",
                specificName = it["specificName"] as? String ?: "",
                generalName = it["generalName"] as? String ?: "",
            )
          }
      val date = doc.getTimestamp("date") ?: throwMissingFieldException("date")
      val animalId = doc.getString("animalId") ?: throwMissingFieldException("animalId")
      val description = doc.getString("description") ?: ""

      Post(
          postId = postId,
          authorId = authorId,
          pictureURL = pictureURL,
          location = location,
          description = description,
          date = date,
          animalId = animalId,
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
