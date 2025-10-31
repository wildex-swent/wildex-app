package com.android.wildex.ui.collection

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.animal.FakeAnimalRepository
import com.android.wildex.model.user.FakeUserAnimalsRepository
import com.android.wildex.model.user.FakeUserRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.theme.WildexTheme
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import kotlin.math.ceil

/** Test tag constants used for UI testing of CollectionScreen components. */
object CollectionScreenTestTags {
  const val GO_BACK_BUTTON = "collection_screen_go_back_button"
  const val NOTIFICATION_BUTTON = "collection_screen_notification_button"
  const val PROFILE_BUTTON = "collection_screen_profile_button"
  const val ANIMAL_LIST = "collection_screen_animal_list"

  fun testTagForAnimal(animalId: Id) = "collection_screen_animal_$animalId"
}

/**
 * Entry point Composable for the Collection Screen.
 *
 * @param collectionScreenViewModel ViewModel managing the state for this screen.
 * @param userUid UID of the user whose collection is to be displayed.
 * @param onAnimalClick Callback invoked when an animal is selected.
 * @param onProfileClick Callback invoked when the profile button is clicked, only use when we
 *   display the current user's collection.
 * @param onNotificationClick Callback invoked when the notification button is clicked, only use
 *   when we display the current user's collection.
 * @param onGoBack Callback invoked when the go back button is clicked, only use when we display the
 *   collection of another user than the current logged one.
 * @param bottomBar Composable lambda for rendering the bottom navigation bar when we display the
 *   current user's collection.
 */
@Composable
fun CollectionScreen(
    collectionScreenViewModel: CollectionScreenViewModel = viewModel(),
    userUid: String = "",
    onAnimalClick: (Id) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onGoBack: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {}
) {
  val uiState by collectionScreenViewModel.uiState.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(Unit) { collectionScreenViewModel.loadUIState(userUid) }
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      collectionScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      bottomBar = { bottomBar() },
      topBar = {
        CollectionTopBar(
            uiState.isUserOwner,
            uiState.user.profilePictureURL,
            onGoBack,
            onProfileClick,
            onNotificationClick)
      }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
          when {
            uiState.isError -> LoadingFail()
            uiState.isLoading -> LoadingScreen()
            uiState.animals.isEmpty() -> NoAnimalsView()
            else -> AnimalsView(animalsStates = uiState.animals, onAnimalClick = onAnimalClick)
          }
        }
      }
}

/**
 * Composable for the top app bar in the Collection Screen.
 *
 * @param isUserOwner Boolean indicating if the displayed collection belongs to the current user.
 * @param userProfilePictureURL URL of the user's profile picture, used when displaying the current
 *   user's collection.
 * @param onGoBack Callback invoked when the go back button is clicked.
 * @param onProfileClick Callback invoked when the profile button is clicked.
 * @param onNotificationClick Callback invoked when the notification button is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionTopBar(
    isUserOwner: Boolean,
    userProfilePictureURL: String = "",
    onGoBack: () -> Unit,
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
  TopAppBar(
      title = {
        Text(
          modifier = Modifier.fillMaxWidth(),
          text = if (isUserOwner) LocalContext.current.getString(R.string.collection) else "",
          fontWeight = FontWeight.SemiBold,
          color = colorScheme.onBackground,
          textAlign = TextAlign.Center
        )
      },
      navigationIcon = {
        IconButton(
            modifier =
                Modifier.testTag(
                    if (isUserOwner) CollectionScreenTestTags.NOTIFICATION_BUTTON
                    else CollectionScreenTestTags.GO_BACK_BUTTON),
            onClick = { if (isUserOwner) onNotificationClick() else onGoBack() },
        ) {
          Icon(
              imageVector =
                  if (isUserOwner) Icons.Default.Notifications else Icons.Default.ArrowBack,
              contentDescription = if (isUserOwner) "Notifications" else "Back",
              tint = colorScheme.onBackground,
          )
        }
      },
      actions = {
        if (isUserOwner) {
          IconButton(
              onClick = { onProfileClick() },
              modifier = Modifier.testTag(CollectionScreenTestTags.PROFILE_BUTTON),
          ) {
            AsyncImage(
                model = userProfilePictureURL,
                contentDescription = "Profile picture",
                modifier =
                    Modifier.size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, colorScheme.primary, CircleShape),
                contentScale = ContentScale.Crop,
            )
          }
        }
      })
}

/**
 * Composable displaying a list of animals in a two-column grid layout.
 *
 * @param animalsStates List of AnimalState objects representing the animals to display.
 * @param onAnimalClick Callback invoked when an animal is selected.
 */
@Composable
fun AnimalsView(animalsStates: List<AnimalState>, onAnimalClick: (Id) -> Unit) {
  LazyColumn(modifier = Modifier.fillMaxSize().testTag(CollectionScreenTestTags.ANIMAL_LIST)) {
    val nbRows = ceil(animalsStates.size / 2.0).toInt()
    items(nbRows) { index ->
      val rowStartIndex = index * 2
      Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
      ) {
        AnimalView(animalsStates[rowStartIndex], onAnimalClick, modifier = Modifier.weight(1f))
        // Check if there is another animal to display when we are at the last row
        if (rowStartIndex + 1 <= animalsStates.size - 1) {
          AnimalView(animalsStates[rowStartIndex + 1], onAnimalClick, modifier = Modifier.weight(1f))
        } else {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }
}

/**
 * Composable representing a single animal item in the collection.
 *
 * @param animalState AnimalState object representing the animal to display.
 * @param onAnimalClick Callback invoked when the animal is selected.
 */
@Composable
fun AnimalView(animalState: AnimalState, onAnimalClick: (Id) -> Unit, modifier: Modifier) {
  val animalName = animalState.name
  val animalPictureURL = animalState.pictureURL
  Card(
      onClick = { if (animalState.isUnlocked) onAnimalClick(animalState.animalId) },
      shape = RoundedCornerShape(16.dp),
      enabled = animalState.isUnlocked,
      modifier = modifier
        .testTag(CollectionScreenTestTags.testTagForAnimal(animalState.animalId))
  ) {
        AsyncImage(
          model = animalPictureURL,
          contentDescription = animalName,
          modifier = Modifier.size(180.dp).clip(RoundedCornerShape(8.dp)),
          contentScale = ContentScale.Crop,
        )
        Text(
          text = animalName,
          fontWeight = FontWeight.Medium,
          color = colorScheme.background,
          textAlign = TextAlign.Center,
          fontSize = 17.sp,
          style = TextStyle(lineHeight = 2.2.em,
            lineHeightStyle = LineHeightStyle(
              alignment = LineHeightStyle.Alignment.Center,
              trim = LineHeightStyle.Trim.None
            )
          ),
          modifier =
              Modifier.fillMaxSize()
                  .background(color = colorScheme.primary)
        )
  }
}

/** Composable displayed when the user's collection is empty. */
@Composable
fun NoAnimalsView() {
  Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
    Text(
        text = LocalContext.current.getString(R.string.empty_collection),
        color = colorScheme.onBackground,
      textAlign = TextAlign.Center)
  }
}

@Preview
@Composable
fun CollectionScreenPreview() {
  val animalRepository = FakeAnimalRepository()
  val userAnimalsRepository = FakeUserAnimalsRepository(animalRepository)
  val userRepository = FakeUserRepository()
  runBlocking {
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
        friendsCount = 3
      )
    )
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
        friendsCount = 3
      )
    )
    animalRepository.addAnimal(
      Animal(
        animalId = "animalId-1",
        pictureURL =
          "https://media.istockphoto.com/id/1796374503/photo/the-lion-king.webp?b=1&s=612x612&w=0&k=20&c=wbgXbIrm_qtaLcDKF6_Ay8d4ECaYQ5t5UVVzYk1WNS4=",
        name = "Lion",
        species = "Panthera leo",
        description = "King of the Jungle"
      )
    )
    animalRepository.addAnimal(
      Animal(
        animalId = "animalId-2",
        pictureURL =
          "https://cdn.pixabay.com/photo/2016/02/19/15/46/labrador-retriever-1210559_1280.jpg",
        name = "Labrador",
        species = "Dog",
        description = "Man's best friend"
      )
    )
    animalRepository.addAnimal(
      Animal(
        animalId = "animalId-3",
        pictureURL =
          "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4d/Cat_November_2010-1a.jpg/250px-Cat_November_2010-1a.jpg",
        name = "Tabby Cat",
        species = "Cat",
        description = "Man's best frenemy"
      )
    )
    animalRepository.addAnimal(
      Animal(
        animalId = "animalId-4",
        pictureURL = "https://www.assuropoil.fr/wp-content/uploads/husky-de-siberie.jpg",
        name = "Husky",
        species = "Dog",
        description = "Biggest howler"
      )
    )
    animalRepository.addAnimal(
      Animal(
        animalId = "animalId-5",
        pictureURL =
          "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bb/Gorille_des_plaines_de_l%27ouest_à_l%27Espace_Zoologique.jpg/960px-Gorille_des_plaines_de_l%27ouest_à_l%27Espace_Zoologique.jpg",
        name = "Gorilla",
        species = "Monkey",
        description = "Donkey Kong's cousin"
      )
    )
    animalRepository.addAnimal(
      Animal(
        animalId = "animalId-6",
        pictureURL =
          "https://cdn.britannica.com/35/3635-050-96241EC1/Scarlet-macaw-ara-macao.jpg",
        name = "Ara Macao",
        species = "Bird",
        description = "Welcome to Rio de Janeiro!"
      )
    )
    animalRepository.addAnimal(
      Animal(
        animalId = "animalId-7",
        pictureURL = "https://realaquatics.co.uk/cdn/shop/articles/5.png?v=1634043062",
        name = "Blue Whale",
        species = "Cetacean",
        description = "Biggest mammal on Earth"
      )
    )

    userAnimalsRepository.initializeUserAnimals("otherUserId")
    userAnimalsRepository.initializeUserAnimals("currentUserId")
    userAnimalsRepository.addAnimalToUserAnimals("currentUserId", "animalId-1")
    userAnimalsRepository.addAnimalToUserAnimals("currentUserId", "animalId-3")
    userAnimalsRepository.addAnimalToUserAnimals("currentUserId", "animalId-7")
    userAnimalsRepository.addAnimalToUserAnimals("otherUserId", "animalId-2")
    userAnimalsRepository.addAnimalToUserAnimals("otherUserId", "animalId-4")
    userAnimalsRepository.addAnimalToUserAnimals("otherUserId", "animalId-3")
    userAnimalsRepository.addAnimalToUserAnimals("otherUserId", "animalId-6")
  }

  WildexTheme { Surface(modifier = Modifier.fillMaxSize()){CollectionScreen(
    userUid = "currentUserId",
    collectionScreenViewModel = CollectionScreenViewModel(animalRepository = animalRepository,
      userAnimalsRepository = userAnimalsRepository,
      userRepository = userRepository,
      currentUserId = "currentUserId"
    )
  )} }
}
