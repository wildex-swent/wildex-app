package com.android.wildex.utils.offline

import com.android.wildex.model.cache.posts.IPostsCache
import com.android.wildex.model.social.Post
import com.android.wildex.model.utils.Id

class FakePostsCache : IPostsCache {

  private val cache = mutableMapOf<Id, Post>()

  override suspend fun getPost(postId: Id): Post? {
    return cache[postId]
  }

  override suspend fun getAllPosts(): List<Post>? {
    return if (cache.isEmpty()) null else cache.values.toList()
  }

  override suspend fun getAllPostsByAuthor(authorId: Id): List<Post>? {
    val posts = cache.values.filter { it.authorId == authorId }
    return posts.ifEmpty { null }
  }

  override suspend fun getAllPostsByGivenAuthor(authorId: Id): List<Post>? {
    // Assuming this has the same meaning in your interface
    val posts = cache.values.filter { it.authorId == authorId }
    return posts.ifEmpty { null }
  }

  override suspend fun deletePostsByUser(userId: Id) {
    cache.entries.removeIf { it.value.authorId == userId }
  }

  override suspend fun savePost(post: Post) {
    cache[post.postId] = post
  }

  override suspend fun savePosts(posts: List<Post>) {
    posts.forEach { cache[it.postId] = it }
  }

  override suspend fun deletePost(postId: Id) {
    cache.remove(postId)
  }

  override suspend fun clearAll() {
    cache.clear()
  }
}
