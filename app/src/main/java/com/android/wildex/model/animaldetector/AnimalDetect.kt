package com.android.wildex.model.animaldetector

/**
 * Simple data class representing the result of animal detection.
 *
 * @property animalType The predicted animal label (e.g., "Canis").
 * @property confidence The model's confidence in the prediction (0.0 - 1.0).
 * @property taxonomy Additional taxonomy information if available.
 */
data class AnimalDetectResponse(
    val animalType: String,
    val confidence: Float,
    val taxonomy: Taxonomy,
)

/**
 * Data class representing the taxonomy details of an animal.
 *
 * @property id The unique identifier for the taxonomy entry.
 * @property animalClass The class of the animal (e.g., "Mammalia").
 * @property order The order of the animal (e.g., "Carnivora").
 * @property family The family of the animal (e.g., "Canidae").
 * @property genus The genus of the animal (e.g., "Canis").
 * @property species The species of the animal (e.g., "Canis lupus").
 */
data class Taxonomy(
    val id: String,
    val animalClass: String = "",
    val order: String = "",
    val family: String = "",
    val genus: String = "",
    val species: String = "",
)
