package com.android.wildex.ui.collection

import com.android.wildex.model.animal.Animal
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionScreenViewModelTest {
  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var userRepository: UserRepository

  private lateinit var userAnimalsRepository: UserAnimalsRepository

  private lateinit var animalsRepository: AnimalRepository

  private lateinit var viewModel: CollectionScreenViewModel

  private val u1 =
      User(
          userId = "currentUserId",
          username = "currentUsername",
          name = "John",
          surname = "Doe",
          bio = "This is a bio",
          profilePictureURL =
              "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "France",
          friendsCount = 3)

  private val u2 =
      User(
          userId = "otherUserId",
          username = "otherUsername",
          name = "Bob",
          surname = "Smith",
          bio = "This is my bob bio",
          profilePictureURL =
              "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "France",
          friendsCount = 3)

  private val su1 =
      SimpleUser(
          userId = "currentUserId",
          username = "currentUsername",
          profilePictureURL =
              "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png")

  private val su2 =
      SimpleUser(
          userId = "otherUserId",
          username = "otherUsername",
          profilePictureURL =
              "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png")

  private val defaultUser: SimpleUser =
      SimpleUser(
          userId = "defaultUserId",
          username = "defaultUsername",
          profilePictureURL = "",
      )

  private val a1 =
      Animal(
          animalId = "animalId-1",
          pictureURL =
              "https://media.istockphoto.com/id/1796374503/photo/the-lion-king.webp?b=1&s=612x612&w=0&k=20&c=wbgXbIrm_qtaLcDKF6_Ay8d4ECaYQ5t5UVVzYk1WNS4=",
          name = "Lion",
          species = "Panthera leo",
          description = "King of the Jungle")

  private val a2 =
      Animal(
          animalId = "animalId-2",
          pictureURL =
              "https://cdn.pixabay.com/photo/2016/02/19/15/46/labrador-retriever-1210559_1280.jpg",
          name = "Labrador",
          species = "Dog",
          description = "Man's best friend")

  private val a3 =
      Animal(
          animalId = "animalId-3",
          pictureURL =
              "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4d/Cat_November_2010-1a.jpg/250px-Cat_November_2010-1a.jpg",
          name = "Tabby Cat",
          species = "Cat",
          description = "Man's best frenemy")

  private val a4 =
      Animal(
          animalId = "animalId-4",
          pictureURL = "https://www.assuropoil.fr/wp-content/uploads/husky-de-siberie.jpg",
          name = "Husky",
          species = "Dog",
          description = "Biggest howler")

  private val a5 =
      Animal(
          animalId = "animalId-5",
          pictureURL =
              "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bb/Gorille_des_plaines_de_l%27ouest_à_l%27Espace_Zoologique.jpg/960px-Gorille_des_plaines_de_l%27ouest_à_l%27Espace_Zoologique.jpg",
          name = "Gorilla",
          species = "Monkey",
          description = "Donkey Kong's cousin")

  private val a6 =
      Animal(
          animalId = "animalId-6",
          pictureURL =
              "https://cdn.britannica.com/35/3635-050-96241EC1/Scarlet-macaw-ara-macao.jpg",
          name = "Ara Macao",
          species = "Bird",
          description = "Welcome to Rio de Janeiro!")

  private val a7 =
      Animal(
          animalId = "animalId-7",
          pictureURL = "https://realaquatics.co.uk/cdn/shop/articles/5.png?v=1634043062",
          name = "Blue Whale",
          species = "Cetacean",
          description = "Biggest mammal on Earth")

  @Before
  fun setUp() {
    userRepository = mockk()
    userAnimalsRepository = mockk()
    animalsRepository = mockk()
    viewModel =
        CollectionScreenViewModel(
            userRepository = userRepository,
            userAnimalsRepository = userAnimalsRepository,
            animalRepository = animalsRepository,
            currentUserId = "currentUserId")
    coEvery { userRepository.getUser("currentUserId") } returns u1
    coEvery { userRepository.getUser("otherUserId") } returns u2
    coEvery { userRepository.getSimpleUser("currentUserId") } returns su1
    coEvery { userRepository.getSimpleUser("otherUserId") } returns su2
    coEvery { animalsRepository.getAnimal("animalId-1") } returns a1
    coEvery { animalsRepository.getAnimal("animalId-2") } returns a2
    coEvery { animalsRepository.getAnimal("animalId-3") } returns a3
    coEvery { animalsRepository.getAnimal("animalId-4") } returns a4
    coEvery { animalsRepository.getAnimal("animalId-5") } returns a5
    coEvery { animalsRepository.getAnimal("animalId-6") } returns a6
    coEvery { animalsRepository.getAnimal("animalId-7") } returns a7
    coEvery { animalsRepository.getAllAnimals() } returns listOf(a1, a2, a3, a4, a5, a6, a7)
    coEvery { userAnimalsRepository.getAllAnimalsByUser("currentUserId") } returns
        mutableListOf(a1, a3, a5)
    coEvery { userAnimalsRepository.getAllAnimalsByUser("otherUserId") } returns
        mutableListOf(a2, a3, a6)
  }

  @Test
  fun viewModel_initializes_default_UI_state() {
    val initialState = viewModel.uiState.value
    Assert.assertEquals(initialState.user, defaultUser)
    Assert.assertFalse(initialState.isUserOwner)
    Assert.assertTrue(initialState.animals.isEmpty())
    Assert.assertFalse(initialState.isLoading)
    Assert.assertFalse(initialState.isError)
    Assert.assertNull(initialState.errorMsg)
  }

  @Test
  fun loadUIState_updates_UI_state_success() {
    mainDispatcherRule.runTest {
      val deferred = CompletableDeferred<List<Animal>>()
      coEvery { userAnimalsRepository.getAllAnimalsByUser("currentUserId") } coAnswers
          {
            deferred.await()
          }
      viewModel.loadUIState(userUid = "currentUserId")

      Assert.assertTrue(viewModel.uiState.value.isLoading)
      deferred.complete(listOf(a1, a3, a5))
      advanceUntilIdle()
      val expectedStates =
          listOf(
              AnimalState(
                  animalId = "animalId-1",
                  pictureURL = a1.pictureURL,
                  name = a1.name,
                  isUnlocked = true),
              AnimalState(
                  animalId = "animalId-3",
                  pictureURL = a3.pictureURL,
                  name = a3.name,
                  isUnlocked = true),
              AnimalState(
                  animalId = "animalId-5",
                  pictureURL = a5.pictureURL,
                  name = a5.name,
                  isUnlocked = true),
              AnimalState(
                  animalId = "animalId-2",
                  pictureURL = a2.pictureURL,
                  name = a2.name,
                  isUnlocked = false),
              AnimalState(
                  animalId = "animalId-4",
                  pictureURL = a4.pictureURL,
                  name = a4.name,
                  isUnlocked = false),
              AnimalState(
                  animalId = "animalId-6",
                  pictureURL = a6.pictureURL,
                  name = a6.name,
                  isUnlocked = false),
              AnimalState(
                  animalId = "animalId-7",
                  pictureURL = a7.pictureURL,
                  name = a7.name,
                  isUnlocked = false),
          )
      val updatedState = viewModel.uiState.value
      Assert.assertEquals(expectedStates, updatedState.animals)
      Assert.assertEquals(su1, updatedState.user)
      Assert.assertTrue(updatedState.isUserOwner)
      Assert.assertFalse(updatedState.isLoading)
      Assert.assertFalse(updatedState.isError)
      Assert.assertNull(updatedState.errorMsg)
    }
  }

  @Test
  fun animalList_of_another_user_contains_only_unlocked_animals() {
    mainDispatcherRule.runTest {
      viewModel.loadUIState(userUid = "otherUserId")
      advanceUntilIdle()
      val expectedStates =
          listOf(
              AnimalState(
                  animalId = "animalId-2",
                  pictureURL = a2.pictureURL,
                  name = a2.name,
                  isUnlocked = true),
              AnimalState(
                  animalId = "animalId-3",
                  pictureURL = a3.pictureURL,
                  name = a3.name,
                  isUnlocked = true),
              AnimalState(
                  animalId = "animalId-6",
                  pictureURL = a6.pictureURL,
                  name = a6.name,
                  isUnlocked = true),
          )
      val updatedState = viewModel.uiState.value
      Assert.assertEquals(expectedStates, updatedState.animals)
      Assert.assertEquals(su2, updatedState.user)
      Assert.assertFalse(updatedState.isUserOwner)
      Assert.assertFalse(updatedState.isLoading)
      Assert.assertFalse(updatedState.isError)
      Assert.assertNull(updatedState.errorMsg)
    }
  }

  @Test
  fun clearErrorMsg_resets_errorMsg_to_null() {
    mainDispatcherRule.runTest {
      coEvery { userRepository.getSimpleUser("currentUserId") } throws RuntimeException("boom")
      viewModel.loadUIState(userUid = "currentUserId")
      advanceUntilIdle()
      Assert.assertNotNull(viewModel.uiState.value.errorMsg)

      viewModel.clearErrorMsg()
      Assert.assertNull(viewModel.uiState.value.errorMsg)
    }
  }
}
