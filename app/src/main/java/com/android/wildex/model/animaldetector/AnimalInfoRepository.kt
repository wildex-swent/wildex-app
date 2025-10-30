package com.android.wildex.model.animaldetector

import android.content.Context
import android.net.Uri

interface AnimalInfoRepository {
  suspend fun detectAnimal(context: Context, imageUri: Uri): List<AnimalDetectResponse>

  suspend fun getAnimalDescription(animalName: String): String?
}
