package com.android.wildex.ui.collection

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.utils.LocalRepositories
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
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
    collectionScreenVM =
        CollectionScreenViewModel(
            userAnimalsRepository = userAnimalsRepository,
            animalRepository = animalRepository,
            userRepository = userRepository,
            currentUserId = "currentUserId",
        )
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
            country = "France",
            friendsCount = 3))
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
            country = "France",
            friendsCount = 3))
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
    userAnimalsRepository.initializeUserAnimals("currentUserId")
    userAnimalsRepository.addUserAnimals("currentUserId", "animalId-1")
    userAnimalsRepository.addUserAnimals("currentUserId", "animalId-3")
    userAnimalsRepository.addUserAnimals("currentUserId", "animalId-7")
    userAnimalsRepository.initializeUserAnimals("otherUserId")
    userAnimalsRepository.addUserAnimals("otherUserId", "animalId-2")
    userAnimalsRepository.addUserAnimals("otherUserId", "animalId-4")
    userAnimalsRepository.addUserAnimals("otherUserId", "animalId-3")
    userAnimalsRepository.addUserAnimals("otherUserId", "animalId-6")
  }

  @After
  fun tearDown() {
    LocalRepositories.clearAll()
  }
}
