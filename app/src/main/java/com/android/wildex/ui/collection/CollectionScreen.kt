package com.android.wildex.ui.collection

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import kotlin.math.ceil

/** Test tag constants used for UI testing of CollectionScreen components. */
object CollectionScreenTestTags {
  const val GO_BACK_BUTTON = "collection_screen_go_back_button"
  const val NOTIFICATION_BUTTON = "collection_screen_notification_button"
  const val PROFILE_BUTTON = "collection_screen_profile_button"
  const val NO_ANIMAL_TEXT = "no_animal_text"
  const val SCREEN_TITLE = "collection_screen_title"
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
        if (!uiState.isLoading) {
          CollectionTopBar(
              uiState.isUserOwner,
              uiState.user.profilePictureURL,
              onGoBack,
              onProfileClick,
              onNotificationClick)
        }
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
        if (isUserOwner) {
          Text(
              modifier = Modifier.fillMaxWidth().testTag(CollectionScreenTestTags.SCREEN_TITLE),
              text = LocalContext.current.getString(R.string.collection),
              fontWeight = FontWeight.SemiBold,
              color = colorScheme.onBackground,
              textAlign = TextAlign.Center)
        }
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
                  if (isUserOwner) Icons.Default.Notifications
                  else Icons.AutoMirrored.Filled.ArrowBack,
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
  val screenHeight = LocalConfiguration.current.screenHeightDp.dp
  LazyColumn(modifier = Modifier.fillMaxSize().testTag(CollectionScreenTestTags.ANIMAL_LIST)) {
    val nbRows = ceil(animalsStates.size / 2.0).toInt()
    val rowHeight = (screenHeight) / 4
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
                modifier = Modifier.fillMaxSize().weight(0.8f),
                contentScale = ContentScale.Crop)

            Text(
                text = if (animalState.isUnlocked) animalName else "???",
                fontWeight = FontWeight.Medium,
                color = colorScheme.background,
                textAlign = TextAlign.Center,
                fontSize = 17.sp,
                style =
                    TextStyle(
                        lineHeight = 2.2.em,
                        lineHeightStyle =
                            LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.None)),
                modifier =
                    Modifier.fillMaxWidth().weight(0.2f).background(color = colorScheme.primary),
            )
          }
        }
    if (!animalState.isUnlocked) {
      Box(
          modifier =
              Modifier.matchParentSize()
                  .background(color = colorScheme.onBackground.copy(alpha = 0.4f)),
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
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag(CollectionScreenTestTags.NO_ANIMAL_TEXT))
  }
}
