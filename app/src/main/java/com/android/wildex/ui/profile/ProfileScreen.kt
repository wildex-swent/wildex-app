package com.android.wildex.ui.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.google.firebase.Timestamp

val defaultUser: User =
    User(
        userId = "0",
        username = "defaultUser",
        name = "Name",
        surname = "Surname",
        bio = "This is a default user bio.",
        profilePictureURL =
            "https://paulhollandphotography.com/cdn/shop/articles" +
                "/4713_Individual_Outdoor_f930382f-c9d6-4e5b-b17d-9fe300ae169c" +
                ".jpg?v=1743534144&width=1500",
        userType = UserType.REGULAR,
        creationDate = Timestamp.now(),
        country = "Country",
        friendsCount = 12)

object ProfileScreenTestTags {
  const val GO_BACK = "ProfileScreenGoBack"
  const val SETTINGS = "ProfileScreenSettings"
  const val ACHIEVEMENTS = "ProfileScreenAchievements"
  const val COLLECTION = "ProfileScreenCollection"
  const val MAP = "ProfileScreenMap"
  const val FRIENDS = "ProfileScreenFriends"
  const val PROFILE_NAME = "ProfileScreenName"
  const val PROFILE_USERNAME = "ProfileScreenSurname"
  const val PROFILE_COUNTRY = "ProfileScreenCountry"
  const val PROFILE_DESCRIPTION = "ProfileScreenBio"
  const val FRIEND_REQUEST = "ProfileScreenFriendRequest"
}

@Composable
fun ProfileScreen(
    profileScreenViewModel: ProfileScreenViewModel = viewModel(),
    userUid: String,
    onGoBack: () -> Unit = {},
    onSettings: () -> Unit = {},
    onCollection: (Id) -> Unit = {},
    onAchievements: (Id) -> Unit = {},
    onFriends: (Id) -> Unit = {},
    onMap: (Id) -> Unit = {},
    onFriendRequest: (Id) -> Unit = {},
) {
  val uiState by profileScreenViewModel.uiState.collectAsState()
  val user = uiState.user
  val achievements = uiState.achievements
  val ownerProfile: Boolean = uiState.isUserOwner

  // Fetch user infos when the screen is recomposed
  LaunchedEffect(Unit) { profileScreenViewModel.refreshUIState(userUid) }

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = { ProfileTopAppBar(ownerProfile, onGoBack, onSettings) },
      content = { pd ->
        ProfileContent(
            pd, user, ownerProfile, onAchievements, onCollection, onMap, onFriends, onFriendRequest)
      })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopAppBar(ownerProfile: Boolean = true, onGoBack: () -> Unit, onSettings: () -> Unit) {
  TopAppBar(
      title = {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Text(
              text =
                  if (ownerProfile) LocalContext.current.getString(R.string.profile)
                  else LocalContext.current.getString(R.string.user_profile),
              style =
                  MaterialTheme.typography.titleLarge.copy(
                      fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 30.sp))
        }
      },
      modifier = Modifier.border(1.dp, Color(0xFFD0BCFF) /*WildexGreen*/).shadow(5.dp),
      navigationIcon = {
        IconButton(
            modifier = Modifier.testTag(ProfileScreenTestTags.GO_BACK), onClick = { onGoBack() }) {
              Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
      },
      actions = {
        if (ownerProfile) {
          IconButton(
              modifier = Modifier.testTag(ProfileScreenTestTags.SETTINGS),
              onClick = { onSettings() }) {
                Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
              }
        }
      },
  )
}

@Composable
fun ProfileContent(
    pd: PaddingValues,
    user: User,
    ownerProfile: Boolean,
    onAchievements: (Id) -> Unit,
    onCollection: (Id) -> Unit,
    onMap: (Id) -> Unit,
    onFriends: (Id) -> Unit,
    onFriendRequest: (Id) -> Unit,
) {
  val id = user.userId
  Column(modifier = Modifier.fillMaxSize().padding(pd)) {
    Text(text = "Profile Screen Content")
    ProfileImageAndName(
        name = user.name,
        surname = user.surname,
        username = user.username,
        profilePicture = user.profilePictureURL,
        country = user.country)
    ProfileDescription(description = user.bio)
    Row {
      ProfileAnimals(id, onCollection, ownerProfile)
      ProfileFriends(id, onFriends, ownerProfile)
    }
    ProfileAchievements(id, onAchievements)
    ProfileMap(id, onMap)
    if (!ownerProfile) {
      ProfileFriendRequest(id, onFriendRequest)
    }
  }
}

@Composable
fun ProfileImageAndName(
    name: String = "Name",
    surname: String = "Surname",
    username: String = "Username",
    profilePicture: String = "",
    country: String = "Country"
) {
  Box() {
    Row {
      AsyncImage(
          model = profilePicture,
          contentDescription = "Profile picture",
          modifier =
              Modifier.padding(16.dp)
                  .fillMaxWidth(0.33f)
                  .aspectRatio(1f)
                  .clip(MaterialTheme.shapes.medium)
                  .border(2.dp, Color.Gray, shape = MaterialTheme.shapes.medium)
                  .padding(4.dp))
      Column {
        Text(
            modifier = Modifier.testTag(ProfileScreenTestTags.PROFILE_NAME),
            text = "$name $surname")
        Text(modifier = Modifier.testTag(ProfileScreenTestTags.PROFILE_USERNAME), text = username)
        Row {
          Icon(
              imageVector = Icons.Filled.Place,
              contentDescription = "Country Icon",
              modifier = Modifier.padding(end = 4.dp))
          Text(modifier = Modifier.testTag(ProfileScreenTestTags.PROFILE_COUNTRY), text = country)
        }
      }
    }
  }
}

@Composable
fun ProfileDescription(description: String = "Bio:...") {
  Column(
      modifier =
          Modifier.padding(16.dp)
              .fillMaxWidth()
              .testTag(ProfileScreenTestTags.PROFILE_DESCRIPTION)) {
        Text(text = description)
      }
}

@Composable
fun ProfileAnimals(id: Id = "", onCollection: (Id) -> Unit = {}, ownerProfile: Boolean) {
  Button(
      modifier = Modifier.testTag(ProfileScreenTestTags.COLLECTION),
      onClick = { if (ownerProfile) onCollection(id) }) {
        Row {
          Icon(
              imageVector = Icons.Filled.Share,
              contentDescription = "Animals Icon",
              modifier = Modifier.padding(end = 4.dp))
          Column {
            Text(text = "9")
            Text(text = "Animals")
          }
        }
      }
}

@Composable
fun ProfileFriends(id: Id = "", onFriends: (Id) -> Unit = {}, ownerProfile: Boolean) {
  Button(
      modifier = Modifier.testTag(ProfileScreenTestTags.FRIENDS),
      onClick = { if (ownerProfile) onFriends(id) }) {
        Row {
          Icon(
              imageVector = Icons.Filled.Person,
              contentDescription = "Friends Icon",
              modifier = Modifier.padding(end = 4.dp))
          Column {
            Text(text = "12")
            Text(text = "Friends")
          }
        }
      }
}

@Composable
fun ProfileAchievements(id: Id = "", onAchievements: (Id) -> Unit = {}) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .padding(16.dp)
              .border(1.dp, Color.Gray)
              .testTag(ProfileScreenTestTags.ACHIEVEMENTS)) {
        Text(text = "Achievements Placeholder", modifier = Modifier.padding(16.dp))
        Button(onClick = { onAchievements(id) }) { Text(text = "Achievements") }
      }
}

@Composable
fun ProfileMap(id: Id = "", onMap: (Id) -> Unit = {}) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .padding(16.dp)
              .border(1.dp, Color.Gray)
              .testTag(ProfileScreenTestTags.MAP)) {
        Text(text = "Map Placeholder", modifier = Modifier.padding(16.dp))
        Button(onClick = { onMap(id) }) { Text(text = "Map") }
      }
}

@Composable
fun ProfileFriendRequest(id: Id = "", onFriendRequest: (Id) -> Unit = {}) {
  Button(
      modifier = Modifier.testTag(ProfileScreenTestTags.FRIEND_REQUEST),
      onClick = { onFriendRequest(id) }) {
        Text(text = "Send Friend Request")
      }
}
