package com.android.wildex.model.storage

import android.net.Uri
import com.android.wildex.model.utils.URL

/**
 * Interface for managing storage operations such as uploading and deleting images.
 * This interface abstracts the storage layer, allowing for different implementations
 * (e.g., Firebase Storage, local storage, etc.).
 */
interface StorageRepository {

    /**
     * Uploads a user's profile picture to the storage.
     *
     * @param userId The unique identifier of the user.
     * @param imageUri The local URI of the profile picture to be uploaded.
     * @return The download URL of the uploaded profile picture, or null if the upload fails.
     */
    suspend fun uploadUserProfilePicture(userId: String, imageUri: Uri): URL?

    /**
     * Uploads an image associated with a post to the storage.
     *
     * @param postId The unique identifier of the post.
     * @param imageUri The local URI of the post image to be uploaded.
     * @return The download URL of the uploaded post image, or null if the upload fails.
     */
    suspend fun uploadPostImage(postId: String, imageUri: Uri): URL?

    /**
     * Uploads an image associated with an animal to the storage.
     *
     * @param animalId The unique identifier of the animal.
     * @param imageUri The local URI of the animal picture to be uploaded.
     * @return The download URL of the uploaded animal picture, or null if the upload fails.
     */
    suspend fun uploadAnimalPicture(animalId: String, imageUri: Uri): URL?

    /**
     * Deletes a user's profile picture from the storage.
     *
     * @param userId The unique identifier of the user whose profile picture is to be deleted.
     */
    suspend fun deleteUserProfilePicture(userId: String)

    /**
     * Deletes an image associated with a post from the storage.
     *
     * @param postId The unique identifier of the post whose image is to be deleted.
     */
    suspend fun deletePostImage(postId: String)

    /**
     * Deletes an image associated with an animal from the storage.
     *
     * @param animalId The unique identifier of the animal whose picture is to be deleted.
     */
    suspend fun deleteAnimalPicture(animalId: String)
}
