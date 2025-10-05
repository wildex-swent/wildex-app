package com.android.wildex.model.animaldetector

/**
 * Simple data class representing the result of animal detection.
 *
 * @property animalType The predicted animal label (e.g., "Canis").
 * @property confidence The model's confidence in the prediction (0.0 - 1.0).
 */
data class AnimalDetectResponse(val animalType: String, val confidence: Float)
