package com.android.wildex.model.social

/** Represents a repository that manages likes on posts */
interface LikeRepository {

  /** Generates and returns a new unique identifier for a Like item. */
  fun getNewLikeId(): String

  /** Retrieves all Like items made by the current user. */
  suspend fun getAllLikesByCurrentUser(): List<Like>

  /** Retrieves all Like items for a given Post */
  suspend fun getLikesForPost(postId: String): List<Like>

  /** Retrieves a Like item authored by the current user on a specific post if it exists */
  suspend fun getLikeForPost(postId: String): Like?

  /** Adds a new Like item to the repository. */
  suspend fun addLike(like: Like)

  /** Deletes a Like item from the repository. */
  suspend fun deleteLike(likeId: String)
}
