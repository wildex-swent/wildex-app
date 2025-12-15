package com.android.wildex.model.cache.posts

import androidx.datastore.core.DataStore
import com.android.wildex.datastore.PostCacheStorage
import com.android.wildex.model.ConnectivityObserver
import com.android.wildex.model.social.Post
import com.android.wildex.model.utils.Id
import kotlin.collections.forEach
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private const val STALE_DURATION_MS = 10 * 60 * 1000L

class PostsCache(
    private val postDataStore: DataStore<PostCacheStorage>,
    private val connectivityObserver: ConnectivityObserver,
) : IPostsCache {

  private fun isStale(lastUpdated: Long): Boolean {
    val isOnline = connectivityObserver.isOnline.value
    val currentTime = System.currentTimeMillis()
    return isOnline && (currentTime - lastUpdated) > STALE_DURATION_MS
  }

  override suspend fun getPost(postId: Id): Post? {
    return postDataStore.data
        .map {
          val cached = it.postsMap[postId]
          if (cached != null && !isStale(cached.lastUpdated)) {
            cached.toPost()
          } else {
            null
          }
        }
        .firstOrNull()
  }

  override suspend fun getAllPosts(): List<Post>? {
    val posts =
        postDataStore.data
            .map { proto ->
              val posts = proto.postsMap.values
              if (posts.isNotEmpty() && posts.all { !isStale(it.lastUpdated) }) {
                posts.map { it.toPost() }
              } else {
                null
              }
            }
            .firstOrNull()
    return if (posts == null && !connectivityObserver.isOnline.value) {
      emptyList()
    } else posts
  }

  override suspend fun getAllPostsByAuthor(authorId: Id): List<Post>? {
    val posts =
        postDataStore.data
            .map { proto ->
              val posts = proto.postsMap.values.filter { it.authorId == authorId }
              if (posts.isNotEmpty() && posts.all { !isStale(it.lastUpdated) }) {
                posts.map { it.toPost() }
              } else {
                null
              }
            }
            .firstOrNull()
    return if (posts == null && !connectivityObserver.isOnline.value) {
      emptyList()
    } else posts
  }

  override suspend fun deletePostsByUser(userId: Id) {
    postDataStore.updateData {
      val builder = it.toBuilder()
      val postsToDelete = it.postsMap.values.filter { postProto -> postProto.authorId == userId }
      postsToDelete.forEach { postProto -> builder.removePosts(postProto.postId) }
      builder.build()
    }
  }

  override suspend fun savePost(post: Post) {
    postDataStore.updateData { it.toBuilder().putPosts(post.postId, post.toProto()).build() }
  }

  override suspend fun savePosts(posts: List<Post>) {
    postDataStore.updateData { current ->
      val builder = current.toBuilder()
      posts.forEach { post -> builder.putPosts(post.postId, post.toProto()) }
      builder.build()
    }
  }

  override suspend fun deletePost(postId: Id) {
    postDataStore.updateData { it.toBuilder().removePosts(postId).build() }
  }

  override suspend fun clearAll() {
    postDataStore.updateData { it.toBuilder().clearPosts().build() }
  }
}
