package com.android.wildex.model.animaldetector

import android.content.Context
import android.net.Uri
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

/**
 * Represents a repository that provides animal detection and information retrieval functionalities.
 */
interface AnimalInfoRepository {
  /**
   * Detects animals in the provided image and returns a list of detection results.
   *
   * @param context The Android context.
   * @param imageUri The URI of the image to be analyzed.
   * @param coroutineContext The coroutine context to use for the operation.
   * @return A list of [AnimalDetectResponse] containing detection results.
   */
  suspend fun detectAnimal(
      context: Context,
      imageUri: Uri,
      coroutineContext: CoroutineContext = Dispatchers.IO,
  ): List<AnimalDetectResponse>

  /**
   * Retrieves a description for the specified animal.
   *
   * @param animalName The name of the animal.
   * @param coroutineContext The coroutine context to use for the operation.
   * @return A string containing the description of the animal.
   */
  suspend fun getAnimalDescription(
      animalName: String,
      coroutineContext: CoroutineContext = Dispatchers.IO,
  ): String

  /**
   * Retrieves a picture URI for the specified animal.
   *
   * @param context The Android context.
   * @param animalName The name of the animal.
   * @param coroutineContext The coroutine context to use for the operation.
   * @return A [Uri] pointing to the picture of the animal.
   */
  suspend fun getAnimalPicture(
      context: Context,
      animalName: String,
      coroutineContext: CoroutineContext = Dispatchers.IO,
  ): Uri
}
