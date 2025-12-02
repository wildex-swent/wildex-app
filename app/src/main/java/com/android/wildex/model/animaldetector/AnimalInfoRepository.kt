package com.android.wildex.model.animaldetector

import android.content.Context
import android.net.Uri
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

interface AnimalInfoRepository {
  suspend fun detectAnimal(
      context: Context,
      imageUri: Uri,
      coroutineContext: CoroutineContext = Dispatchers.IO,
  ): List<AnimalDetectResponse>

  suspend fun getAnimalDescription(
      animalName: String,
      coroutineContext: CoroutineContext = Dispatchers.IO,
  ): String

  suspend fun getAnimalPicture(
      context: Context,
      animalName: String,
      coroutineContext: CoroutineContext = Dispatchers.IO,
  ): Uri
}
