package com.android.wildex.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.user.User
import com.android.wildex.model.utils.Id

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
    userUid: String = "",
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
  val ownerProfile: Boolean = uiState.isUserOwner
  // val achievements = uiState.achievements

  LaunchedEffect(Unit) { profileScreenViewModel.refreshUIState(userUid) }

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = { ProfileTopAppBar(ownerProfile, onGoBack, onSettings) },
      content = { pd ->
        ProfileContent(
            pd = pd,
            user = user ?: profileScreenViewModel.defaultUser,
            ownerProfile = ownerProfile,
            onAchievements = onAchievements,
            onCollection = onCollection,
            onMap = onMap,
            onFriends = onFriends,
            onFriendRequest = onFriendRequest,
        )
      },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopAppBar(ownerProfile: Boolean = true, onGoBack: () -> Unit, onSettings: () -> Unit) {
  val cs = MaterialTheme.colorScheme
  TopAppBar(
      title = {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Text(
              text = if (ownerProfile) LocalContext.current.getString(R.string.profile) else "",
          )
        }
      },
      navigationIcon = {
        IconButton(
            modifier = Modifier.testTag(ProfileScreenTestTags.GO_BACK),
            onClick = { onGoBack() },
        ) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = cs.onBackground,
          )
        }
      },
      actions = {
        if (ownerProfile) {
          IconButton(
              modifier = Modifier.testTag(ProfileScreenTestTags.SETTINGS),
              onClick = { onSettings() },
          ) {
            Icon(
                // Using Close for the M1, replace with Icons.Filled.Settings afterwards
                imageVector = Icons.Filled.Close,
                contentDescription = "Settings",
                tint = cs.onBackground,
            )
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
    ProfileImageAndName(
        name = user.name,
        surname = user.surname,
        username = user.username,
        profilePicture = user.profilePictureURL,
        country = user.country,
    )
    Spacer(modifier = Modifier.height(8.dp))
    ProfileDescription(description = user.bio)
    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
      ProfileAnimals(id = id, onCollection = onCollection, ownerProfile = ownerProfile)
      Spacer(modifier = Modifier.width(12.dp))
      ProfileFriends(id = id, onFriends = onFriends, ownerProfile = ownerProfile)
    }
    Spacer(modifier = Modifier.height(12.dp))
    ProfileAchievements(id = id, onAchievements = onAchievements, ownerProfile = ownerProfile)
    Spacer(modifier = Modifier.height(12.dp))
    ProfileMap(id = id, onMap = onMap)
    Spacer(modifier = Modifier.height(24.dp))
    if (!ownerProfile) {
      ProfileFriendRequest(id = id, onFriendRequest = onFriendRequest)
    }
  }
}

@Composable
fun ProfileImageAndName(
    name: String = "Name",
    surname: String = "Surname",
    username: String = "Username",
    profilePicture: String = "",
    country: String = "Country",
) {
  val cs = MaterialTheme.colorScheme
  Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    AsyncImage(
        model = profilePicture,
        contentDescription = "Profile picture",
        contentScale = ContentScale.Crop,
        modifier =
            Modifier.size(88.dp)
                .clip(RoundedCornerShape(100.dp))
                .border(
                    0.dp,
                    cs.onBackground.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(100.dp),
                ),
    )

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
          modifier = Modifier.testTag(ProfileScreenTestTags.PROFILE_NAME),
          text = "$name $surname",
          style =
              MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 24.sp,
              ),
          color = cs.onBackground,
      )
      Text(
          modifier = Modifier.testTag(ProfileScreenTestTags.PROFILE_USERNAME),
          text = username,
          style =
              MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp,
              ),
          color = cs.onBackground,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Place,
            contentDescription = "Country Icon",
            tint = cs.secondary,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            modifier = Modifier.testTag(ProfileScreenTestTags.PROFILE_COUNTRY),
            text = country,
            style =
                MaterialTheme.typography.bodySmall.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            color = cs.secondary,
        )
      }
    }
  }
}

@Composable
fun ProfileDescription(description: String = "Bio:...") {
  val cs = MaterialTheme.colorScheme
  Box(
      modifier =
          Modifier.padding(horizontal = 16.dp)
              .width(371.dp)
              .height(94.dp)
              .background(color = cs.secondary, shape = RoundedCornerShape(8.dp))
              .fillMaxWidth()
              .padding(12.dp)
              .testTag(ProfileScreenTestTags.PROFILE_DESCRIPTION)
  ) {
    Text(
        text = description.ifBlank { " " },
        color = cs.onSecondary,
        style =
            MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            ),
    )
  }
}

@Composable
fun ProfileAnimals(id: Id = "", onCollection: (Id) -> Unit = {}, ownerProfile: Boolean) {
  val cs = MaterialTheme.colorScheme
  Button(
      modifier = Modifier.height(64.dp).width(176.dp).testTag(ProfileScreenTestTags.COLLECTION),
      onClick = { if (ownerProfile) onCollection(id) },
      colors =
          ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary),
      shape = RoundedCornerShape(8.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
          painter = painterResource(R.drawable.animal_icon),
          contentDescription = "Animals Icon",
          tint = cs.onPrimary,
          modifier = Modifier.size(54.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(text = "15", color = cs.onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 24.sp)
        Text(
            text = "Animals",
            color = cs.onPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
        )
      }
    }
  }
}

@Composable
fun ProfileFriends(id: Id = "", onFriends: (Id) -> Unit = {}, ownerProfile: Boolean) {
  val cs = MaterialTheme.colorScheme
  Button(
      modifier = Modifier.height(64.dp).width(176.dp).testTag(ProfileScreenTestTags.FRIENDS),
      onClick = { if (ownerProfile) onFriends(id) },
      colors =
          ButtonDefaults.buttonColors(containerColor = cs.tertiary, contentColor = cs.onTertiary),
      shape = RoundedCornerShape(8.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
          imageVector = Icons.Filled.Person,
          contentDescription = "Friends Icon",
          tint = cs.onTertiary,
          modifier = Modifier.size(54.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(text = "34", color = cs.onTertiary, fontWeight = FontWeight.SemiBold, fontSize = 24.sp)
        Text(
            text = "Friends",
            color = cs.onTertiary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
        )
      }
    }
  }
}

@Composable
fun ProfileAchievements(id: Id = "", onAchievements: (Id) -> Unit = {}, ownerProfile: Boolean) {
  val cs = MaterialTheme.colorScheme
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp)
              .border(1.dp, cs.background, shape = RoundedCornerShape(12.dp))
              .padding(12.dp)
              .testTag(ProfileScreenTestTags.ACHIEVEMENTS)
  ) {
    if (ownerProfile) {
      Button(
          onClick = { onAchievements(id) },
          modifier = Modifier.align(Alignment.End),
          colors =
              ButtonDefaults.buttonColors(
                  containerColor = cs.background,
                  contentColor = cs.onBackground,
              ),
      ) {
        Text(text = "View all achievements →")
      }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      IconButton(onClick = { /* left */ }) {
        Icon(
            painter = painterResource(R.drawable.chevron_left),
            contentDescription = "Prev",
            tint = cs.secondary,
            modifier = Modifier.size(32.dp),
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Box(modifier = Modifier.weight(1f).height(80.dp).background(cs.background)) {
        Text(
            text = "Achievements Placeholder",
            modifier = Modifier.align(Alignment.Center).padding(8.dp),
            color = cs.onBackground,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      IconButton(onClick = { /* right */ }) {
        Icon(
            painter = painterResource(R.drawable.chevron_right),
            contentDescription = "Next",
            tint = cs.secondary,
            modifier = Modifier.size(32.dp),
        )
      }
    }
  }
}

@Composable
fun ProfileMap(id: Id = "", onMap: (Id) -> Unit = {}, ownerProfile: Boolean = true) {
  val cs = MaterialTheme.colorScheme
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp)
              .border(1.dp, cs.onBackground.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp))
              .padding(12.dp)
              .testTag(ProfileScreenTestTags.MAP)
  ) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(cs.background)
    ) {
      Text(
          text = "Map Placeholder",
          modifier = Modifier.align(Alignment.Center),
          color = cs.onBackground,
      )
    }

    Button(
        onClick = { onMap(id) },
        modifier = Modifier.align(Alignment.Start).padding(top = 8.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = cs.secondary,
                contentColor = cs.onSecondary,
            ),
    ) {
      Text(text = "View in full screen →")
    }
  }
}

@Composable
fun ProfileFriendRequest(id: Id = "", onFriendRequest: (Id) -> Unit = {}) {
  val cs = MaterialTheme.colorScheme
  Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
    Button(
        modifier =
            Modifier.testTag(ProfileScreenTestTags.FRIEND_REQUEST)
                .height(48.dp)
                .width(183.dp)
                .align(Alignment.Center),
        onClick = { onFriendRequest(id) },
        colors =
            ButtonDefaults.buttonColors(
                containerColor = cs.secondary,
                contentColor = cs.onSecondary,
            ),
        shape = RoundedCornerShape(10.dp),
    ) {
      Text(text = "Send Friend Request")
    }
  }
}
