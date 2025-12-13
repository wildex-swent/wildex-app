package com.android.wildex.model.cache.posts

import com.android.wildex.model.social.Post
import com.android.wildex.model.utils.Id

interface IPostsCache {
  /**
   * Retrieves a post by their ID from the cache.
   *
   * @param postId The ID of the post to retrieve.
   * @return The User object if found, or null if not found.
   */
  suspend fun getPost(postId: Id): Post?

  /**
   * Retrieves all posts from the cache.
   *
   * @return A list of Post objects if any are found, or null if none are found.
   */
  suspend fun getAllPosts(): List<Post>?

  /**
   * Retrieves all posts by a specific author from the cache.
   *
   * @param authorId The ID of the author whose posts to retrieve.
   * @return A list of Post objects if any are found, or null if none are found.
   */
  suspend fun getAllPostsByAuthor(authorId: Id): List<Post>?

  /**
   * Retrieves all posts by a specific author from the cache.
   *
   * @param authorId The ID of the author whose posts to retrieve.
   * @return A list of Post objects if any are found, or null if none are found.
   */
  suspend fun getAllPostsByGivenAuthor(authorId: Id): List<Post>?

  /**
   * Deletes all posts by a specific user from the cache.
   *
   * @param userId The ID of the user whose posts to delete.
   */
  suspend fun deletePostsByUser(userId: Id)

  /**
   * Saves a post to the cache.
   *
   * @param post The Post object to save.
   */
  suspend fun savePost(post: Post)

  /**
   * Saves multiple posts to the cache.
   *
   * @param posts A list of Post objects to save.
   */
  suspend fun savePosts(posts: List<Post>)

  /**
   * Deletes a post from the cache by their ID.
   *
   * @param postId The ID of the post to delete.
   */
  suspend fun deletePost(postId: Id)

  /** Clears all post data from the cache. */
  suspend fun clearAll()
}
