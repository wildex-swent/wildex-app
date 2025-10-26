package com.android.wildex.ui.profile

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.wildex.R
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

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
  const val SCROLL = "ProfileScreenScroll"
  const val ACHIEVEMENTS_CTA = "ProfileScreenAchievementsCTA"
  const val MAP_CTA = "ProfileScreenMapCTA"

  const val ACHIEVEMENTS_PREV = "ProfileScreenAchievementsPrev"
  const val ACHIEVEMENTS_NEXT = "ProfileScreenAchievementsNext"
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
  val context = LocalContext.current

  LaunchedEffect(Unit) { profileScreenViewModel.refreshUIState(userUid) }
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      profileScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = { ProfileTopBar(uiState.isUserOwner, onGoBack, onSettings) },
  ) { pd ->
    when {
      uiState.isLoading -> ProfileLoading(pd)
      uiState.user == null -> ProfileNotFound(pd)
      else -> {
        val swipeState = rememberSwipeRefreshState(isRefreshing = false)
        SwipeRefresh(
            state = swipeState,
            onRefresh = { profileScreenViewModel.refreshUIState(userUid) },
        ) {
          ProfileContent(
              pd = pd,
              user = uiState.user!!,
              ownerProfile = uiState.isUserOwner,
              achievements = uiState.achievements,
              onAchievements = onAchievements,
              animalCount = uiState.animalCount,
              onCollection = onCollection,
              onMap = onMap,
              onFriends = onFriends,
              onFriendRequest = onFriendRequest,
          )
        }
      }
    }
  }
}

@Composable
fun ProfileContent(
    pd: PaddingValues,
    user: User,
    ownerProfile: Boolean,
    achievements: List<Achievement> = emptyList(),
    animalCount: Int = 17,
    onAchievements: (Id) -> Unit,
    onCollection: (Id) -> Unit,
    onMap: (Id) -> Unit,
    onFriends: (Id) -> Unit,
    onFriendRequest: (Id) -> Unit,
) {
  val id = user.userId

  Column(
      modifier =
          Modifier.fillMaxSize()
              .padding(pd)
              .verticalScroll(rememberScrollState())
              .testTag(ProfileScreenTestTags.SCROLL)) {
        Spacer(Modifier.height(6.dp))
        ProfileImageAndName(
            name = user.name,
            surname = user.surname,
            username = user.username,
            profilePicture = user.profilePictureURL,
            country = user.country,
            isProfessional = user.userType == UserType.PROFESSIONAL,
        )

        Spacer(modifier = Modifier.height(10.dp))
        ProfileDescription(description = user.bio)

        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
          ProfileAnimals(
              modifier = Modifier.weight(1f).defaultMinSize(minHeight = 56.dp),
              id = id,
              onCollection = onCollection,
              ownerProfile = ownerProfile,
              animalCount = animalCount,
          )
          Spacer(modifier = Modifier.width(12.dp))
          ProfileFriends(
              modifier = Modifier.weight(1f).defaultMinSize(minHeight = 56.dp),
              id = id,
              onFriends = onFriends,
              ownerProfile = ownerProfile,
              friendCount = user.friendsCount,
          )
        }

        Spacer(modifier = Modifier.height(14.dp))
        ProfileAchievements(
            id = id,
            onAchievements = onAchievements,
            ownerProfile = ownerProfile,
            listAchievement = achievements,
        )

        Spacer(modifier = Modifier.height(14.dp))
        ProfileMap(id = id, onMap = onMap)

        Spacer(modifier = Modifier.height(24.dp))
        if (!ownerProfile) {
          ProfileFriendRequest(id = id, onFriendRequest = onFriendRequest)
        }
        Spacer(Modifier.height(12.dp))
      }
}

@Composable
fun ProfileImageAndName(
    name: String = "Name",
    surname: String = "Surname",
    username: String = "Username",
    profilePicture: String = "",
    country: String = "Country",
    isProfessional: Boolean = false,
) {
  val cs = colorScheme
  Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    val context = LocalContext.current

    Box(modifier = Modifier.size(92.dp)) {
      ElevatedCard(
          shape = RoundedCornerShape(100.dp),
          modifier =
              Modifier.fillMaxSize()
                  .shadow(
                      8.dp,
                      RoundedCornerShape(100.dp),
                      spotColor = cs.primary.copy(alpha = 0.25f),
                  ),
      ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(profilePicture).crossfade(true).build(),
            contentDescription = "Profile picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(100.dp)),
        )
      }

      if (isProfessional) {
        val badgeSize = 34.dp

        Box(
            modifier =
                Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp).size(badgeSize),
            contentAlignment = Alignment.Center,
        ) {
          Icon(
              imageVector = Icons.Filled.Pets,
              contentDescription = "Professional badge",
              tint = cs.tertiary,
              modifier = Modifier.fillMaxSize(),
          )

          val density = LocalDensity.current

          Icon(
              imageVector = Icons.Rounded.Add,
              contentDescription = null,
              tint = Color.White,
              modifier =
                  Modifier.size(14.dp).align(Alignment.Center).graphicsLayer {
                    translationY = with(density) { 6.dp.toPx() }
                  },
          )
        }
      }
    }

    Spacer(modifier = Modifier.width(14.dp))

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
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
      Text(
          modifier = Modifier.testTag(ProfileScreenTestTags.PROFILE_USERNAME),
          text = username,
          style =
              MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 18.sp,
              ),
          color = cs.onBackground.copy(alpha = 0.85f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
              Modifier.clip(RoundedCornerShape(20.dp))
                  .background(cs.secondary)
                  .padding(horizontal = 10.dp, vertical = 6.dp),
      ) {
        Icon(
            imageVector = Icons.Filled.Place,
            contentDescription = "Country Icon",
            tint = cs.onSecondary,
            modifier = Modifier.size(16.dp),
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
            color = cs.onSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun ProfileDescription(description: String = "Bio:...") {
  val cs = colorScheme
  ElevatedCard(
      modifier =
          Modifier.padding(horizontal = 16.dp)
              .fillMaxWidth()
              .defaultMinSize(minHeight = 94.dp)
              .testTag(ProfileScreenTestTags.PROFILE_DESCRIPTION),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = cs.background),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Info,
            contentDescription = "Bio",
            tint = cs.secondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Bio",
            color = cs.secondary,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
      }
      Spacer(Modifier.height(8.dp))
      Text(
          text =
              description.ifBlank { LocalContext.current.getString(R.string.default_description) },
          color = cs.onBackground,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
          style =
              MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Normal,
                  fontSize = 14.sp,
              ),
      )
    }
  }
}

@Composable
private fun ProfileStatCard(
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    icon: @Composable () -> Unit,
    title: String,
    value: String,
    onClick: () -> Unit,
    testTag: String,
) {
  val shape = RoundedCornerShape(12.dp)
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val scale by
      animateFloatAsState(
          targetValue = if (pressed) 0.98f else 1f,
          animationSpec = tween(120, easing = FastOutSlowInEasing),
          label = "statScale",
      )

  Surface(
      modifier =
          modifier
              .graphicsLayer {
                scaleX = scale
                scaleY = scale
              }
              .clip(shape)
              .testTag(testTag),
      color = containerColor,
      contentColor = contentColor,
      shape = shape,
      shadowElevation = 2.dp,
      tonalElevation = 0.dp,
  ) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable(
                    onClick = onClick,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      icon()
      Spacer(Modifier.width(10.dp))
      Column {
        Text(
            text = value,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            maxLines = 1,
        )
        Text(
            text = title,
            color = contentColor.copy(alpha = 0.95f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            maxLines = 1,
        )
      }
    }
  }
}

@Composable
fun ProfileAnimals(
    modifier: Modifier = Modifier,
    id: Id = "",
    onCollection: (Id) -> Unit = {},
    ownerProfile: Boolean,
    animalCount: Int = 17,
) {
  val cs = colorScheme
  ProfileStatCard(
      modifier = modifier,
      containerColor = cs.primary,
      contentColor = cs.onPrimary,
      icon = {
        Icon(
            imageVector = Icons.Filled.Pets,
            contentDescription = "Animals Icon",
            tint = cs.onPrimary,
            modifier = Modifier.size(32.dp),
        )
      },
      title = "Animals",
      value = "$animalCount",
      onClick = { if (ownerProfile) onCollection(id) },
      testTag = ProfileScreenTestTags.COLLECTION,
  )
}

@Composable
fun ProfileFriends(
    modifier: Modifier = Modifier,
    id: Id = "",
    onFriends: (Id) -> Unit = {},
    ownerProfile: Boolean,
    friendCount: Int = 42,
) {
  val cs = colorScheme
  ProfileStatCard(
      modifier = modifier,
      containerColor = cs.tertiary,
      contentColor = cs.onTertiary,
      icon = {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = "Friends Icon",
            tint = cs.onTertiary,
            modifier = Modifier.size(32.dp),
        )
      },
      title = "Friends",
      value = "$friendCount",
      onClick = { if (ownerProfile) onFriends(id) },
      testTag = ProfileScreenTestTags.FRIENDS,
  )
}

@Composable
fun ProfileFriendRequest(id: Id = "", onFriendRequest: (Id) -> Unit = {}) {
  val cs = colorScheme
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
      Text(text = LocalContext.current.getString(R.string.send_friend_request))
    }
  }
}
