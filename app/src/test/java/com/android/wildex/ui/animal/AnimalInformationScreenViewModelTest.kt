package com.android.wildex.ui.animal

import com.android.wildex.model.animal.Animal
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.utils.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnimalInformationScreenViewModelTest {
  @get:Rule val mainDispatcherRule = MainDispatcherRule()
  private lateinit var animalRepository: AnimalRepository
  private lateinit var viewModel: AnimalInformationScreenViewModel

  @Before
  fun setUp() {
    animalRepository = mockk()
    viewModel = AnimalInformationScreenViewModel(animalRepository = animalRepository)
    coEvery { animalRepository.getAnimal("animalId1") } returns animal1
    coEvery { animalRepository.getAnimal("animalId2") } returns animal2
    coEvery { animalRepository.getAllAnimals() } returns listOf(animal1, animal2)
    coEvery { animalRepository.addAnimal(any()) } just Runs
  }

  @Test
  fun viewModel_initializes_default_UI_state() {
    val initialState = viewModel.uiState.value
    assertEquals("", initialState.animalId)
    assertEquals("", initialState.pictureURL)
    assertEquals("", initialState.name)
    assertEquals("", initialState.species)
    assertEquals("", initialState.description)
    assertNull(initialState.errorMsg)
    assertFalse(initialState.isLoading)
    assertFalse(initialState.isError)
  }

  @Test
  fun loadAnimalInformation_updates_UI_state() {
    mainDispatcherRule.runTest {
      viewModel.loadAnimalInformation("animalId1")
      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertEquals("animalId1", state.animalId)
      assertEquals("pictureURL1", state.pictureURL)
      assertEquals("animalName1", state.name)
      assertEquals("animalType1", state.species)
      assertEquals("animalDescription1", state.description)
    }
  }

  @Test
  fun loadAnimalInformation_sets_error_on_exception() =
      mainDispatcherRule.runTest {
        coEvery { animalRepository.getAnimal("animalId1") } throws Exception("fail")

        viewModel.loadAnimalInformation("animalId1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.errorMsg!!.contains("fail"))
      }

  @Test
  fun clearErrorMsg_sets_errorMsg_to_null() =
      mainDispatcherRule.runTest {
        viewModel.loadAnimalInformation("animalId1")
        viewModel.clearErrorMsg()
        assertNull(viewModel.uiState.value.errorMsg)
      }

  @Test
  fun loadAnimalInformation_sets_isLoading_false_on_failure() =
      mainDispatcherRule.runTest {
        coEvery { animalRepository.getAnimal("animalId1") } throws Exception("boom")
        viewModel.loadAnimalInformation("animalId1")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.errorMsg)
      }

  private val animal1 =
      Animal(
          animalId = "animalId1",
          pictureURL = "pictureURL1",
          name = "animalName1",
          species = "animalType1",
          description = "animalDescription1",
      )

  private val animal2 =
      Animal(
          animalId = "animalId2",
          pictureURL = "pictureURL2",
          name = "animalName2",
          species = "animalType2",
          description = "animalDescription2",
      )
}
