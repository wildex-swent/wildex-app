package com.android.wildex.model.storage

import android.net.Uri
import android.util.Log
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
import com.google.firebase.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await

/**
 * Firebase Storage implementation for handling image uploads and deletions.
 *
 * Storage structure:
 * - users/{userId}.jpg - User profile pictures
 * - posts/{postId}.jpg - Post images
 * - animals/{animalId}.jpg - Animal pictures
 *
 * @property storage The Firebase Storage instance
 */
class StorageRepositoryFirebase(private val storage: FirebaseStorage = Firebase.storage) :
    StorageRepository {

  override suspend fun uploadUserProfilePicture(userId: Id, imageUri: Uri): URL? {
    val path = getUserProfilePicturePath(userId)
    return uploadImageToStorage(imageUri, path)
  }

  override suspend fun uploadPostImage(postId: Id, imageUri: Uri): URL? {
    val path = getPostImagePath(postId)
    return uploadImageToStorage(imageUri, path)
  }

  override suspend fun uploadAnimalPicture(animalId: Id, imageUri: Uri): URL? {
    val path = getAnimalPicturePath(animalId)
    return uploadImageToStorage(imageUri, path)
  }

  override suspend fun deleteUserProfilePicture(userId: Id) {
    val path = getUserProfilePicturePath(userId)
    deleteImageFromStorage(path)
  }

  override suspend fun deletePostImage(postId: Id) {
    val path = getPostImagePath(postId)
    deleteImageFromStorage(path)
  }

  override suspend fun deleteAnimalPicture(animalId: Id) {
    val path = getAnimalPicturePath(animalId)
    deleteImageFromStorage(path)
  }

  companion object {
    private const val TAG = "StorageRepositoryFirebase"
    private const val USERS_PATH = "users"
    private const val POSTS_PATH = "posts"
    private const val ANIMALS_PATH = "animals"
  }

  /**
   * Uploads an image to Firebase Storage at the specified path.
   *
   * @param imageUri The local URI of the image to upload
   * @param path The storage path where the image will be saved (e.g., "users/userId.jpg")
   * @return The download URL of the uploaded image, or null if the upload fails
   */
  private suspend fun uploadImageToStorage(imageUri: Uri, path: String): URL? {
    try {
      val storageRef: StorageReference = storage.reference.child(path)
      storageRef.putFile(imageUri).await()
      val downloadUrl = storageRef.downloadUrl.await()
      return downloadUrl.toString()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to upload image: ${e.message}", e)
      return null
    }
  }

  /**
   * Deletes an image from Firebase Storage at the specified path.
   *
   * @param path The storage path of the image to delete (e.g., "users/userId.jpg")
   */
  private suspend fun deleteImageFromStorage(path: String) {
    try {
      val storageRef: StorageReference = storage.reference.child(path)
      storageRef.delete().await()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to delete image: ${e.message}", e)
    }
  }

  /**
   * Helper function to generate a storage path for a user's profile picture.
   *
   * @param userId The user's ID
   * @return The storage path (e.g., "users/userId.jpg")
   */
  private fun getUserProfilePicturePath(userId: Id): String = "$USERS_PATH/$userId.jpg"

  /**
   * Helper function to generate a storage path for a post image.
   *
   * @param postId The post's ID
   * @return The storage path (e.g., "posts/postId.jpg")
   */
  private fun getPostImagePath(postId: Id): String = "$POSTS_PATH/$postId.jpg"

  /**
   * Helper function to generate a storage path for an animal picture.
   *
   * @param animalId The animal's ID
   * @return The storage path (e.g., "animals/animalId.jpg")
   */
  private fun getAnimalPicturePath(animalId: Id): String = "$ANIMALS_PATH/$animalId.jpg"
}
