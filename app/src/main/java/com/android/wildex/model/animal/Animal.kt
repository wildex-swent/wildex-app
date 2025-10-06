package com.android.wildex.model.animal

/**
 * Represents an animal in the system.
 *
 * @property animalId The unique identifier for the animal.
 * @property picture The URL/URI of the animal's image.
 * @property name The name of the animal.
 * @property species The species of the animal.
 * @property description A description of the animal.
 */
data class Animal(
    val animalId: String,
    val picture: String,
    val name: String,
    val species: String,
    val description: String,
)
