package com.android.wildex.ui.collection

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.navigation.TopLevelTopBar
import kotlin.math.ceil

/** Test tag constants used for UI testing of CollectionScreen components. */
object CollectionScreenTestTags {
  const val GO_BACK_BUTTON = "collection_screen_go_back_button"
  const val NO_ANIMAL_TEXT = "no_animal_text"
  const val ANIMAL_LIST = "collection_screen_animal_list"

  fun testTagForAnimal(animalId: Id, isUnlocked: Boolean) =
      if (isUnlocked) "collection_screen_animal_${animalId}_unlocked"
      else "collection_screen_animal_${animalId}_locked"
}

/**
 * Entry point Composable for the Collection Screen.
 *
 * @param collectionScreenViewModel ViewModel managing the state for this screen.
 * @param userUid UID of the user whose collection is to be displayed.
 * @param onAnimalClick Callback invoked when an animal is selected.
 * @param onProfilePictureClick callback invoked when the current user owns the screen and clicks on
 *   their profile picture
 * @param onNotificationClick Callback invoked when the notification button is clicked, only use
 *   when we display the current user's collection.
 * @param onGoBack Callback invoked when the go back button is clicked, only use when we display the
 *   collection of another user than the current logged one.
 * @param bottomBar Composable lambda for rendering the bottom navigation bar when we display the
 *   current user's collection.
 * @param currentUserTopBar boolean for rendering the correct top bar when the current user owns the
 *   screen
 */
@Composable
fun CollectionScreen(
    collectionScreenViewModel: CollectionScreenViewModel = viewModel(),
    userUid: String = "",
    onAnimalClick: (Id) -> Unit = {},
    onProfilePictureClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onGoBack: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    currentUserTopBar: Boolean = true
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
      modifier = Modifier.fillMaxSize().testTag(NavigationTestTags.COLLECTION_SCREEN),
      bottomBar = bottomBar,
      topBar = {
        if (currentUserTopBar)
            TopLevelTopBar(
                currentUser = uiState.user,
                title = context.getString(R.string.collection),
                onNotificationClick = onNotificationClick,
                onProfilePictureClick = onProfilePictureClick)
        else OtherUserCollectionTopBar(onGoBack = onGoBack)
      }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
          when {
            uiState.isError -> LoadingFail()
            uiState.isLoading -> LoadingScreen()
            uiState.animals.isEmpty() -> NoAnimalsView(uiState.isUserOwner)
            else -> AnimalsView(animalsStates = uiState.animals, onAnimalClick = onAnimalClick)
          }
        }
      }
}

/**
 * Composable for the top app bar in the Collection Screen.
 *
 * @param onGoBack Callback invoked when the go back button is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserCollectionTopBar(onGoBack: () -> Unit) {
  TopAppBar(
      title = {},
      navigationIcon = {
        IconButton(
            modifier = Modifier.testTag(CollectionScreenTestTags.GO_BACK_BUTTON),
            onClick = onGoBack,
        ) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = colorScheme.tertiary,
              modifier = Modifier.size(30.dp),
          )
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
  val screenHeight = LocalWindowInfo.current.containerSize.height.dp
  LazyColumn(modifier = Modifier.fillMaxSize().testTag(CollectionScreenTestTags.ANIMAL_LIST)) {
    val nbRows = ceil(animalsStates.size / 2.0).toInt()
    val rowHeight = (screenHeight) / 12
    items(nbRows) { index ->
      val rowStartIndex = index * 2
      Row(
          horizontalArrangement = Arrangement.spacedBy(20.dp),
          modifier =
              Modifier.height(rowHeight)
                  .fillMaxWidth()
                  .padding(horizontal = 20.dp, vertical = 10.dp),
      ) {
        AnimalView(animalsStates[rowStartIndex], onAnimalClick, modifier = Modifier.weight(1f))
        // Check if there is another animal to display when we are at the last row
        if (rowStartIndex + 1 <= animalsStates.size - 1) {
          AnimalView(
              animalsStates[rowStartIndex + 1], onAnimalClick, modifier = Modifier.weight(1f))
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
  Box(modifier = modifier.clip(RoundedCornerShape(16.dp))) {
    Card(
        onClick = { if (animalState.isUnlocked) onAnimalClick(animalState.animalId) },
        enabled = animalState.isUnlocked,
        modifier =
            modifier.testTag(
                CollectionScreenTestTags.testTagForAnimal(
                    animalState.animalId, animalState.isUnlocked))) {
          Column(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = animalPictureURL,
                contentDescription = animalName,
                modifier = Modifier.fillMaxSize().weight(0.82f),
                contentScale = ContentScale.Crop)
            Box(
                modifier =
                    Modifier.fillMaxWidth().weight(0.18f).background(color = colorScheme.primary),
                contentAlignment = Alignment.Center) {
                  Text(
                      text = if (animalState.isUnlocked) animalName else "???",
                      color = colorScheme.background,
                      textAlign = TextAlign.Center,
                      style = typography.titleMedium,
                      modifier = Modifier.fillMaxWidth())
                }
          }
        }
    if (!animalState.isUnlocked) {
      Box(
          modifier =
              Modifier.matchParentSize()
                  .background(color = colorScheme.background.copy(alpha = 0.4f)),
          contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.lock),
                contentDescription = "Locked animal",
                modifier = Modifier.size(90.dp))
          }
    }
  }
}

/** Composable displayed when the user's collection is empty. */
@Composable
fun NoAnimalsView(isUserOwner: Boolean) {
  Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
    Text(
        text =
            LocalContext.current.getString(
                if (isUserOwner) R.string.empty_current_collection
                else R.string.empty_other_collection),
        color = colorScheme.onBackground,
        style = typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag(CollectionScreenTestTags.NO_ANIMAL_TEXT))
  }
}
