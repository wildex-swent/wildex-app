package com.android.wildex.model.social

import com.android.wildex.model.utils.Id

/** Represents a repository that manages Post items. */
interface PostsRepository {

  /** Generates and returns a new unique identifier for a Post item. */
  fun getNewPostId(): String

  /** Retrieves all Post items from the repository. */
  suspend fun getAllPosts(): List<Post>

  /** Retrieves all Post items made by a specific author. */
  suspend fun getAllPostsByAuthor(): List<Post>

  /** Retrieves all Post items made by a given author. */
  suspend fun getAllPostsByGivenAuthor(authorId: Id): List<Post>

  /** Retrieves a specific Post item by its unique identifier. */
  suspend fun getPost(postId: Id): Post

  /** Adds a new Post item to the repository. */
  suspend fun addPost(post: Post)

  /** Edits an existing Post item in the repository. */
  suspend fun editPost(postId: Id, newValue: Post)

  /** Deletes a Post item from the repository. */
  suspend fun deletePost(postId: Id)
}
