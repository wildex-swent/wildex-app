package com.android.wildex.ui.collection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.ui.navigation.BottomNavigationMenu
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.navigation.Tab
import com.android.wildex.utils.LocalRepositories
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val animalRepository = LocalRepositories.animalRepository
  private val userAnimalsRepository = LocalRepositories.userAnimalsRepository
  private val userRepository = LocalRepositories.userRepository

  private lateinit var collectionScreenVM: CollectionScreenViewModel

  @Before
  fun setup() = runBlocking {
    userRepository.addUser(
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
            country = "France"))
    userRepository.addUser(
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
            country = "France"))
    userAnimalsRepository.initializeUserAnimals("currentUserId")
    userAnimalsRepository.initializeUserAnimals("otherUserId")

    populateRepos()

    collectionScreenVM =
        CollectionScreenViewModel(
            userAnimalsRepository = userAnimalsRepository,
            animalRepository = animalRepository,
            userRepository = userRepository,
            currentUserId = "currentUserId",
        )
  }

  fun populateRepos() {
    runBlocking {
      animalRepository.addAnimal(
          Animal(
              animalId = "animalId-1",
              pictureURL =
                  "https://media.istockphoto.com/id/1796374503/photo/the-lion-king.webp?b=1&s=612x612&w=0&k=20&c=wbgXbIrm_qtaLcDKF6_Ay8d4ECaYQ5t5UVVzYk1WNS4=",
              name = "Lion",
              species = "Panthera leo",
              description = "King of the Jungle"))
      animalRepository.addAnimal(
          Animal(
              animalId = "animalId-2",
              pictureURL =
                  "https://cdn.pixabay.com/photo/2016/02/19/15/46/labrador-retriever-1210559_1280.jpg",
              name = "Labrador",
              species = "Dog",
              description = "Man's best friend"))
      animalRepository.addAnimal(
          Animal(
              animalId = "animalId-3",
              pictureURL =
                  "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4d/Cat_November_2010-1a.jpg/250px-Cat_November_2010-1a.jpg",
              name = "Tabby Cat",
              species = "Cat",
              description = "Man's best frenemy"))
      animalRepository.addAnimal(
          Animal(
              animalId = "animalId-4",
              pictureURL = "https://www.assuropoil.fr/wp-content/uploads/husky-de-siberie.jpg",
              name = "Husky",
              species = "Dog",
              description = "Biggest howler"))
      animalRepository.addAnimal(
          Animal(
              animalId = "animalId-5",
              pictureURL =
                  "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bb/Gorille_des_plaines_de_l%27ouest_à_l%27Espace_Zoologique.jpg/960px-Gorille_des_plaines_de_l%27ouest_à_l%27Espace_Zoologique.jpg",
              name = "Gorilla",
              species = "Monkey",
              description = "Donkey Kong's cousin"))
      animalRepository.addAnimal(
          Animal(
              animalId = "animalId-6",
              pictureURL =
                  "https://cdn.britannica.com/35/3635-050-96241EC1/Scarlet-macaw-ara-macao.jpg",
              name = "Ara Macao",
              species = "Bird",
              description = "Welcome to Rio de Janeiro!"))
      animalRepository.addAnimal(
          Animal(
              animalId = "animalId-7",
              pictureURL = "https://realaquatics.co.uk/cdn/shop/articles/5.png?v=1634043062",
              name = "Blue Whale",
              species = "Cetacean",
              description = "Biggest mammal on Earth"))

      userAnimalsRepository.addAnimalToUserAnimals("currentUserId", "animalId-1")
      userAnimalsRepository.addAnimalToUserAnimals("currentUserId", "animalId-3")
      userAnimalsRepository.addAnimalToUserAnimals("currentUserId", "animalId-7")
      userAnimalsRepository.addAnimalToUserAnimals("otherUserId", "animalId-2")
      userAnimalsRepository.addAnimalToUserAnimals("otherUserId", "animalId-4")
      userAnimalsRepository.addAnimalToUserAnimals("otherUserId", "animalId-3")
      userAnimalsRepository.addAnimalToUserAnimals("otherUserId", "animalId-6")
    }
  }

  fun clearRepos() {
    runBlocking { LocalRepositories.clearUserAnimalsAndAnimals() }
  }

  @After
  fun tearDown() {
    LocalRepositories.clearAll()
  }

  @Test
  fun testTagsAreCorrectlySetWhenNoAnimalAndCurrentUser() {
    clearRepos()
    composeTestRule.setContent {
      CollectionScreen(
          collectionScreenVM,
          userUid = "currentUserId",
          bottomBar = { BottomNavigationMenu(selectedTab = Tab.Collection) })
    }
    composeTestRule
        .onNodeWithTag(CollectionScreenTestTags.NO_ANIMAL_TEXT)
        .assertIsDisplayed()
        .assertTextEquals("No animals in your collection yet.\nStart exploring…")
    composeTestRule.onNodeWithTag(CollectionScreenTestTags.PROFILE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CollectionScreenTestTags.NOTIFICATION_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CollectionScreenTestTags.GO_BACK_BUTTON).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(CollectionScreenTestTags.ANIMAL_LIST).assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(CollectionScreenTestTags.SCREEN_TITLE)
        .assertIsDisplayed()
        .assertTextEquals("My Collection")
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.COLLECTION_TAB).assertIsSelected()
    populateRepos()
  }

  @Test
  fun testTagsAreCorrectlySetWhenNoAnimalAndOtherUser() {
    clearRepos()
    composeTestRule.setContent { CollectionScreen(collectionScreenVM, userUid = "otherUserId") }
    composeTestRule
        .onNodeWithTag(CollectionScreenTestTags.NO_ANIMAL_TEXT)
        .assertIsDisplayed()
        .assertTextEquals("No animals in this collection.")
    composeTestRule.onNodeWithTag(CollectionScreenTestTags.PROFILE_BUTTON).assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(CollectionScreenTestTags.NOTIFICATION_BUTTON)
        .assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(CollectionScreenTestTags.GO_BACK_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CollectionScreenTestTags.ANIMAL_LIST).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(CollectionScreenTestTags.SCREEN_TITLE).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsNotDisplayed()
    populateRepos()
  }

  @Test
  fun profilePictureClick_invokesCallback() {
    var profileClicked = false

    composeTestRule.setContent {
      CollectionScreen(
          collectionScreenVM, userUid = "currentUserId", onProfileClick = { profileClicked = true })
    }

    composeTestRule.onNodeWithTag(CollectionScreenTestTags.PROFILE_BUTTON).performClick()
    assert(profileClicked)
  }

  @Test
  fun notificationClick_invokesCallback() {
    var notificationClicked = false

    composeTestRule.setContent {
      CollectionScreen(
          collectionScreenVM,
          userUid = "currentUserId",
          onNotificationClick = { notificationClicked = true })
    }

    composeTestRule.onNodeWithTag(CollectionScreenTestTags.NOTIFICATION_BUTTON).performClick()
    assert(notificationClicked)
  }

  @Test
  fun goBackClick_invokesCallback() {
    var goBackClicked = false

    composeTestRule.setContent {
      CollectionScreen(
          collectionScreenVM, userUid = "otherUserId", onGoBack = { goBackClicked = true })
    }

    composeTestRule.onNodeWithTag(CollectionScreenTestTags.GO_BACK_BUTTON).performClick()
    assert(goBackClicked)
  }

  @Test
  fun testTagsAreCorrectlySetWhenAnimalsAndCurrentUser() {

    composeTestRule.setContent {
      CollectionScreen(
          collectionScreenVM,
          userUid = "currentUserId",
          bottomBar = { BottomNavigationMenu(selectedTab = Tab.Collection) })
    }

    composeTestRule.onNodeWithTag(CollectionScreenTestTags.ANIMAL_LIST).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(CollectionScreenTestTags.testTagForAnimal("animalId-1", true))
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(CollectionScreenTestTags.ANIMAL_LIST).performScrollToIndex(3)

    composeTestRule
        .onNodeWithTag(CollectionScreenTestTags.testTagForAnimal("animalId-6", false))
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(CollectionScreenTestTags.PROFILE_BUTTON).assertIsDisplayed()

    composeTestRule.onNodeWithTag(CollectionScreenTestTags.NOTIFICATION_BUTTON).assertIsDisplayed()

    composeTestRule.onNodeWithTag(CollectionScreenTestTags.GO_BACK_BUTTON).assertIsNotDisplayed()

    composeTestRule
        .onNodeWithTag(CollectionScreenTestTags.SCREEN_TITLE)
        .assertIsDisplayed()
        .assertTextEquals("My Collection")

    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()

    composeTestRule.onNodeWithTag(NavigationTestTags.COLLECTION_TAB).assertIsSelected()
  }

  @Test
  fun testTagsAreCorrectlySetWhenAnimalsAndOtherUser() {
    composeTestRule.setContent { CollectionScreen(collectionScreenVM, userUid = "otherUserId") }

    composeTestRule.onNodeWithTag(CollectionScreenTestTags.ANIMAL_LIST).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(CollectionScreenTestTags.testTagForAnimal("animalId-6", true))
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(CollectionScreenTestTags.ANIMAL_LIST).performScrollToIndex(1)

    composeTestRule
        .onNodeWithTag(CollectionScreenTestTags.testTagForAnimal("animalId-1", false))
        .assertIsNotDisplayed()

    composeTestRule.onNodeWithTag(CollectionScreenTestTags.PROFILE_BUTTON).assertIsNotDisplayed()

    composeTestRule
        .onNodeWithTag(CollectionScreenTestTags.NOTIFICATION_BUTTON)
        .assertIsNotDisplayed()

    composeTestRule.onNodeWithTag(CollectionScreenTestTags.GO_BACK_BUTTON).assertIsDisplayed()

    composeTestRule.onNodeWithTag(CollectionScreenTestTags.SCREEN_TITLE).assertIsNotDisplayed()

    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsNotDisplayed()
  }

  @Test
  fun unlockedAnimalClick_invokesCallback() {
    var clickedAnimalId: String? = null

    composeTestRule.setContent {
      CollectionScreen(
          collectionScreenVM,
          userUid = "currentUserId",
          onAnimalClick = { clickedAnimalId = it },
          bottomBar = { BottomNavigationMenu(selectedTab = Tab.Collection) })
    }

    composeTestRule
        .onNodeWithTag(CollectionScreenTestTags.testTagForAnimal("animalId-1", true))
        .performClick()

    assert(clickedAnimalId == "animalId-1")
  }

  @Test
  fun lockedAnimalClick_doesNotInvokeCallback() {
    var clickedAnimalId: String? = null

    composeTestRule.setContent {
      CollectionScreen(
          collectionScreenVM,
          userUid = "currentUserId",
          onAnimalClick = { clickedAnimalId = it },
          bottomBar = { BottomNavigationMenu(selectedTab = Tab.Collection) })
    }

    composeTestRule.onNodeWithTag(CollectionScreenTestTags.ANIMAL_LIST).performScrollToIndex(3)

    composeTestRule
        .onNodeWithTag(CollectionScreenTestTags.testTagForAnimal("animalId-6", false))
        .assertIsDisplayed()
        .performClick()

    assert(clickedAnimalId == null)
  }
}
