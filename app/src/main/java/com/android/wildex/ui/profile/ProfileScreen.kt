package com.android.wildex.ui.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.wildex.R
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.social.FriendStatus
import com.android.wildex.ui.utils.badges.ProfessionalBadge

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
  const val ANIMAL_COUNT = "ProfileScreenAnimalCount"
  const val FRIENDS_COUNT = "ProfileScreenFriendsCount"
  const val FOLLOW_BUTTON = "ProfileScreenFollowButton"
  const val UNFOLLOW_BUTTON = "ProfileScreenUnfollowButton"
  const val CANCEL_REQUEST_BUTTON = "ProfileScreenCancelRequestButton"
  const val ACCEPT_REQUEST_BUTTON = "ProfileScreenAcceptRequestButton"
  const val DECLINE_REQUEST_BUTTON = "ProfileScreenDeclineRequestButton"
  const val PULL_TO_REFRESH = "ProfileScreenPullToRefresh"
}

/** Profile Screen Composable */
@OptIn(ExperimentalMaterial3Api::class)
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
) {
  val context = LocalContext.current
  val uiState by profileScreenViewModel.uiState.collectAsState()
  val connectivityObserver = LocalConnectivityObserver.current
  val isOnline by connectivityObserver.isOnline.collectAsState()

  var showMap by remember { mutableStateOf(true) }

  BackHandler {
    showMap = false
    onGoBack()
  }

  LaunchedEffect(Unit) { profileScreenViewModel.loadUIState(userUid) }
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      profileScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(NavigationTestTags.PROFILE_SCREEN),
      topBar = {
        ProfileTopBar(
            ownerProfile = uiState.isUserOwner,
            onGoBack = {
              showMap = false
              onGoBack()
            },
            onSettings = onSettings,
        )
      },
  ) { pd ->
    val pullState = rememberPullToRefreshState()

    PullToRefreshBox(
        state = pullState,
        isRefreshing = uiState.isRefreshing,
        modifier = Modifier.padding(pd).testTag(ProfileScreenTestTags.PULL_TO_REFRESH),
        onRefresh = {
          if (isOnline) profileScreenViewModel.refreshUIState(userUid)
          else profileScreenViewModel.refreshOffline()
        },
    ) {
      when {
        uiState.isError -> LoadingFail()
        uiState.isLoading -> LoadingScreen()
        else -> {
          ProfileContent(
              user = uiState.user,
              viewModel = profileScreenViewModel,
              state = uiState,
              onAchievements = onAchievements,
              onCollection = onCollection,
              onMap = onMap,
              onFriends = onFriends,
              showMap = showMap,
          )
        }
      }
    }
  }
}

/** Profile Content Composable */
@Composable
fun ProfileContent(
    user: User,
    viewModel: ProfileScreenViewModel,
    state: ProfileUIState,
    onAchievements: (Id) -> Unit,
    onCollection: (Id) -> Unit,
    onMap: (Id) -> Unit,
    onFriends: (Id) -> Unit,
    showMap: Boolean = true,
) {
  val id = user.userId

  Column(
      modifier =
          Modifier.fillMaxSize()
              .verticalScroll(rememberScrollState())
              .testTag(ProfileScreenTestTags.SCROLL)) {
        Spacer(Modifier.height(6.dp))
        ProfileImageAndName(viewModel = viewModel, state = state)

        Spacer(modifier = Modifier.height(10.dp))
        ProfileDescription(description = user.bio)

        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
          ProfileAnimals(
              modifier = Modifier.weight(1f).defaultMinSize(minHeight = 56.dp),
              id = id,
              onCollection = onCollection,
              animalCount = state.animalCount,
          )
          Spacer(modifier = Modifier.width(12.dp))
          ProfileFriends(
              modifier = Modifier.weight(1f).defaultMinSize(minHeight = 56.dp),
              id = id,
              onFriends = onFriends,
              friendCount = state.friendsCount,
          )
        }

        Spacer(modifier = Modifier.height(14.dp))
        ProfileAchievements(
            id = id,
            onAchievements = onAchievements,
            ownerProfile = state.isUserOwner,
            listAchievement = state.achievements,
        )

        Spacer(modifier = Modifier.height(14.dp))
        if (showMap) {
          ProfileMap(id = id, onMap = onMap, pins = state.recentPins)
        }
        Spacer(Modifier.height(12.dp))
      }
}

/** Profile Image And Name Composable */
@Composable
fun ProfileImageAndName(viewModel: ProfileScreenViewModel, state: ProfileUIState) {
  val name = state.user.name
  val surname = state.user.surname
  val username = state.user.username
  val profilePicture = state.user.profilePictureURL
  val country = state.user.country
  val friendStatus = state.friendStatus
  val userType = state.user.userType
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

      Box(
          modifier =
              Modifier.align(Alignment.BottomEnd).fillMaxSize(0.45f).offset(x = 3.dp, y = 3.dp)) {
            when (userType) {
              UserType.REGULAR -> Unit
              UserType.PROFESSIONAL -> ProfessionalBadge()
            }
          }
    }

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
          modifier = Modifier.testTag(ProfileScreenTestTags.PROFILE_NAME),
          text = "$name $surname",
          style = typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
          color = cs.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
      Text(
          modifier = Modifier.testTag(ProfileScreenTestTags.PROFILE_USERNAME),
          text = username,
          style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = cs.primary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.clip(RoundedCornerShape(20.dp))
                    .background(cs.onBackground)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
          Icon(
              imageVector = Icons.Filled.Place,
              contentDescription = "Country Icon",
              tint = cs.background,
              modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
              modifier = Modifier.testTag(ProfileScreenTestTags.PROFILE_COUNTRY),
              text = country,
              style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              color = cs.background,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
          )
        }
        ProfileFriendInteractable(viewModel, friendStatus, Modifier.weight(1f))
      }
    }
  }
}

/** Profile Description Composable */
@Composable
fun ProfileDescription(description: String = "Bio:...") {
  val cs = colorScheme
  ElevatedCard(
      modifier =
          Modifier.padding(horizontal = 16.dp)
              .fillMaxWidth()
              .defaultMinSize(minHeight = 94.dp)
              .border(
                  1.dp,
                  cs.onBackground.copy(alpha = 0.08f),
                  shape = RoundedCornerShape(14.dp),
              )
              .testTag(ProfileScreenTestTags.PROFILE_DESCRIPTION),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = cs.background),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Info,
            contentDescription = "Bio",
            tint = cs.onBackground,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Bio",
            color = cs.onBackground,
            style = typography.labelLarge,
        )
      }
      Spacer(Modifier.height(8.dp))
      Text(
          text =
              description.ifBlank { LocalContext.current.getString(R.string.default_description) },
          color = cs.onBackground,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
          style = typography.bodyMedium,
      )
    }
  }
}

/** Profile Stat Card Composable */
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
            style = typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            modifier =
                Modifier.testTag(
                    if (title == "Friends") ProfileScreenTestTags.FRIENDS_COUNT
                    else ProfileScreenTestTags.ANIMAL_COUNT),
        )
        Text(
            text = title,
            color = contentColor.copy(alpha = 0.95f),
            style = typography.labelLarge.copy(fontSize = 18.sp),
            maxLines = 1,
        )
      }
    }
  }
}

/** Profile Animals Composable */
@Composable
fun ProfileAnimals(
    modifier: Modifier = Modifier,
    id: Id = "",
    onCollection: (Id) -> Unit = {},
    animalCount: Int = 0,
) {
  val cs = colorScheme
  ProfileStatCard(
      modifier = modifier,
      containerColor = cs.primary,
      contentColor = cs.background,
      icon = {
        Icon(
            imageVector = Icons.Filled.Pets,
            contentDescription = "Animals Icon",
            tint = cs.background,
            modifier = Modifier.size(32.dp),
        )
      },
      title = "Animals",
      value = "$animalCount",
      onClick = { onCollection(id) },
      testTag = ProfileScreenTestTags.COLLECTION,
  )
}

/** Profile Friends Composable */
@Composable
fun ProfileFriends(
    modifier: Modifier = Modifier,
    id: Id = "",
    onFriends: (Id) -> Unit = {},
    friendCount: Int = 0,
) {
  val cs = colorScheme
  ProfileStatCard(
      modifier = modifier,
      containerColor = cs.primary,
      contentColor = cs.background,
      icon = {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = "Friends Icon",
            tint = cs.background,
            modifier = Modifier.size(32.dp),
        )
      },
      title = "Friends",
      value = "$friendCount",
      onClick = { onFriends(id) },
      testTag = ProfileScreenTestTags.FRIENDS,
  )
}

/** Remove friend button interactable element */
@Composable
fun UnfollowButton(onUnfollow: () -> Unit = {}, testTag: String) {
  Box(
      modifier =
          Modifier.testTag(testTag)
              .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                  onClick = onUnfollow,
              )
              .background(color = colorScheme.onSurface, shape = RoundedCornerShape(20.dp))) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
          Icon(
              imageVector = Icons.Filled.DeleteForever,
              contentDescription = "Delete friend icon",
              tint = colorScheme.surface,
              modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
              text = LocalContext.current.getString(R.string.friend_screen_remove_friend),
              style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              color = colorScheme.surface,
          )
        }
      }
}

/** Send friend request interactable element */
@Composable
fun FollowButton(onFollow: () -> Unit = {}, testTag: String) {
  Box(
      modifier =
          Modifier.testTag(testTag)
              .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                  onClick = onFollow,
              )
              .background(color = colorScheme.primary, shape = RoundedCornerShape(20.dp))) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
          Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = "Send friend request icon",
              tint = colorScheme.background,
              modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
              text = LocalContext.current.getString(R.string.friend_screen_send_request),
              style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              color = colorScheme.background,
          )
        }
      }
}

/** Cancel sent request interactable element */
@Composable
fun SentRequestInteractable(onCancel: () -> Unit = {}, testTag: String) {
  Box(
      modifier =
          Modifier.testTag(testTag)
              .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                  onClick = onCancel,
              )
              .background(color = colorScheme.onSurface, shape = RoundedCornerShape(20.dp))) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
          Text(
              text =
                  LocalContext.current.getString(R.string.friend_screen_pending_request_other_user),
              style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              color = colorScheme.surface,
          )
        }
      }
}

/** Received friend request interactable element */
@Composable
fun ReceivedRequestInteractable(
    onAccept: () -> Unit = {},
    onDecline: () -> Unit = {},
    testTagAccept: String,
    testTagDecline: String,
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
    Box(
        modifier =
            Modifier.testTag(testTagAccept)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAccept,
                )
                .background(color = colorScheme.primary, shape = RoundedCornerShape(50.dp))) {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
          ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Accept friend request icon",
                tint = colorScheme.background,
                modifier = Modifier.size(16.dp),
            )
          }
        }
    Spacer(modifier = Modifier.width(20.dp))
    Box(
        modifier =
            Modifier.testTag(testTagDecline)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDecline,
                )
                .background(color = colorScheme.onSurface, shape = RoundedCornerShape(50.dp))) {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
          ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Decline friend request icon",
                tint = colorScheme.surface,
                modifier = Modifier.size(16.dp),
            )
          }
        }
  }
}

/** Friend Request Button Composable For now, not connected to the backend. */
@Composable
fun ProfileFriendInteractable(
    viewModel: ProfileScreenViewModel,
    friendStatus: FriendStatus,
    modifier: Modifier,
) {
  Box(modifier = modifier, contentAlignment = Alignment.CenterEnd) {
    when (friendStatus) {
      FriendStatus.FRIEND ->
          UnfollowButton(
              onUnfollow = { viewModel.unfollowUser() },
              testTag = ProfileScreenTestTags.UNFOLLOW_BUTTON,
          )
      FriendStatus.NOT_FRIEND ->
          FollowButton(
              onFollow = { viewModel.sendRequestToUser() },
              testTag = ProfileScreenTestTags.FOLLOW_BUTTON,
          )
      FriendStatus.PENDING_RECEIVED ->
          ReceivedRequestInteractable(
              onAccept = { viewModel.acceptReceivedRequest() },
              onDecline = { viewModel.declineReceivedRequest() },
              testTagAccept = ProfileScreenTestTags.ACCEPT_REQUEST_BUTTON,
              testTagDecline = ProfileScreenTestTags.DECLINE_REQUEST_BUTTON,
          )
      FriendStatus.PENDING_SENT ->
          SentRequestInteractable(
              onCancel = { viewModel.cancelSentRequestToUser() },
              testTag = ProfileScreenTestTags.CANCEL_REQUEST_BUTTON,
          )
      FriendStatus.IS_CURRENT_USER -> Unit
    }
  }
}
