package com.android.wildex.ui.animal

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.utils.LocalRepositories
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnimalInformationScreenTest {
  private val animalRepository: AnimalRepository = LocalRepositories.animalRepository

  private lateinit var animalInformationScreenViewModel: AnimalInformationScreenViewModel

  @get:Rule val composeRule = createComposeRule()

  @Before
  fun setup() = runBlocking {
    val animal1 =
        Animal(
            animalId = "animalId1",
            pictureURL = "pictureURL1",
            name = "animalName1",
            species = "animalType1",
            description = "animalDescription1",
        )
    val animal2 =
        Animal(
            animalId = "animalId2",
            pictureURL = "pictureURL2",
            name = "animalName2",
            species = "animalType2",
            description = "animalDescription2",
        )
    animalRepository.addAnimal(animal1)
    animalRepository.addAnimal(animal2)
  }

  @After
  fun tearDown() {
    LocalRepositories.clearAll()
  }

  @Test
  fun topLeft_backButton_triggersCallback() {
    var backClicked = 0
    composeRule.setContent { AnimalInformationScreen("animalId1", onGoBack = { backClicked++ }) }
    composeRule.onNodeWithContentDescription("Back to Collection").performClick()
    Assert.assertEquals(1, backClicked)
  }
}
