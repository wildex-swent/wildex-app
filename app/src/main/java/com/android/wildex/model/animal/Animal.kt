package com.android.wildex.model.animal

import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL

/**
 * Represents an animal in the system.
 *
 * @property animalId The unique identifier for the animal.
 * @property pictureURL The URL of the animal's image.
 * @property name The name of the animal.
 * @property species The species of the animal.
 * @property description A description of the animal.
 */
data class Animal(
    val animalId: Id,
    val pictureURL: URL,
    val name: String,
    val species: String,
    val description: String,
)
