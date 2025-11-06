package com.android.wildex.model.animaldetector

/**
 * Simple data class representing the result of animal detection.
 *
 * @property animalType The predicted animal label (e.g., "Canis").
 * @property confidence The model's confidence in the prediction (0.0 - 1.0).
 */
data class AnimalDetectResponse(
    val animalType: String,
    val confidence: Float,
    val boundingBox: BoundingBox,
    val taxonomy: Taxonomy,
)

data class BoundingBox(val x: Float, val y: Float, val width: Float, val height: Float)

data class Taxonomy(
    val id: String,
    val animalClass: String = "",
    val order: String = "",
    val family: String = "",
    val genus: String = "",
    val species: String = "",
)
