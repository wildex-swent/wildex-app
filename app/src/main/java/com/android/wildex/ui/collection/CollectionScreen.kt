package com.android.wildex.ui.collection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.R
import com.android.wildex.model.utils.Id

/** Test tag constants used for UI testing of CollectionScreen components. */
object CollectionScreenTestTags {
  const val BOTTOM_BAR = "collection_screen_bottom_bar"
  const val GO_BACK_BUTTON = "collection_screen_go_back_button"
  const val NOTIFICATION_BUTTON = "collection_screen_notification_button"
  const val PROFILE_BUTTON = "collection_screen_profile_button"

  fun testTagForAnimal(animalId: Id) = "collection_screen_animal_$animalId"
}

/**
 * Entry point Composable for the Collection Screen.
 *
 * @param collectionScreenViewModel ViewModel managing the state for this screen.
 * @param userUid UID of the user whose collection is to be displayed.
 * @param onAnimal Callback invoked when an animal is selected.
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
    onAnimal: (Id) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onGoBack: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {}
) {
  val uiState by collectionScreenViewModel.uiState.collectAsState()

  Scaffold(bottomBar = { bottomBar() }) { innerPadding ->
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(innerPadding).fillMaxSize(),
    ) {
      Text(stringResource(R.string.not_implemented), textAlign = TextAlign.Center)
    }
  }
}
