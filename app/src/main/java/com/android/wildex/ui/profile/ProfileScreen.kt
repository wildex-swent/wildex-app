package com.android.wildex.ui.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.wildex.R
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.user.User
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.theme.FontSizes

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
  val achievements = uiState.achievements
  val animalsCount = uiState.animalCount

  LaunchedEffect(Unit) { profileScreenViewModel.refreshUIState(userUid) }

  // ---- Hardcoded test achievements (used if VM list is empty) ----
  val fakeAchievements = remember {
    listOf(
        Achievement(
            "1",
            "https://upload.wikimedia.org/wikipedia/commons/9/99/Star_icon_stylized.svg",
            "First achievement!",
            "Explorer") {
              false
            },
        Achievement(
            "2",
            "https://upload.wikimedia.org/wikipedia/commons/2/29/Gold_medal_icon.svg",
            "Second achievement!",
            "Wildlife Hero") {
              false
            },
        Achievement(
            "3",
            "https://upload.wikimedia.org/wikipedia/commons/7/7c/Medal_icon.svg",
            "Third achievement!",
            "Trailblazer") {
              false
            },
        Achievement(
            "4",
            "https://upload.wikimedia.org/wikipedia/commons/4/4e/Trophy_icon.svg",
            "Fourth achievement!",
            "Champion") {
              false
            },
    )
  }
  val displayAchievements = if (achievements.isEmpty()) fakeAchievements else achievements
  // ----------------------------------------------------------------

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = { ProfileTopAppBar(ownerProfile, onGoBack, onSettings) },
  ) { pd ->
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.background,
                        1f to MaterialTheme.colorScheme.background))) {
          ProfileContent(
              pd = pd,
              user = user,
              ownerProfile = ownerProfile,
              achievements = displayAchievements,
              onAchievements = onAchievements,
              animalCount = animalsCount,
              onCollection = onCollection,
              onMap = onMap,
              onFriends = onFriends,
              onFriendRequest = onFriendRequest,
          )
        }
  }
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
              fontWeight = FontWeight.SemiBold,
              color = cs.onBackground)
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
                imageVector = Icons.Filled.Settings,
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
    user: User?,
    ownerProfile: Boolean,
    achievements: List<Achievement> = emptyList(),
    animalCount: Int = 17,
    onAchievements: (Id) -> Unit,
    onCollection: (Id) -> Unit,
    onMap: (Id) -> Unit,
    onFriends: (Id) -> Unit,
    onFriendRequest: (Id) -> Unit,
) {
  if (user == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(
          text = "User not found :(. Who are you?",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onBackground,
      )
    }
    return
  }
  val id = user.userId

  Column(modifier = Modifier.fillMaxSize().padding(pd).verticalScroll(rememberScrollState())) {
    Spacer(Modifier.height(6.dp))
    ProfileImageAndName(
        name = user.name,
        surname = user.surname,
        username = user.username,
        profilePicture = user.profilePictureURL,
        country = user.country,
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
) {
  val cs = MaterialTheme.colorScheme
  Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    val context = LocalContext.current
    ElevatedCard(
        shape = RoundedCornerShape(100.dp),
        modifier =
            Modifier.size(92.dp)
                .shadow(
                    8.dp, RoundedCornerShape(100.dp), spotColor = cs.primary.copy(alpha = 0.25f))) {
          AsyncImage(
              model = ImageRequest.Builder(context).data(profilePicture).crossfade(true).build(),
              contentDescription = "Profile picture",
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(92.dp).clip(RoundedCornerShape(100.dp)))
        }

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
          modifier = Modifier.testTag(ProfileScreenTestTags.PROFILE_NAME),
          text = "$name $surname",
          style =
              MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = FontSizes.ProfileName,
              ),
          color = cs.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis)
      Text(
          modifier = Modifier.testTag(ProfileScreenTestTags.PROFILE_USERNAME),
          text = username,
          style =
              MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  fontSize = FontSizes.ProfileUsername,
              ),
          color = cs.onBackground.copy(alpha = 0.85f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis)
      Spacer(modifier = Modifier.height(8.dp))
      Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
              Modifier.clip(RoundedCornerShape(20.dp))
                  .background(cs.secondary)
                  .padding(horizontal = 10.dp, vertical = 6.dp)) {
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = "Country Icon",
                tint = cs.onSecondary,
                modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                modifier = Modifier.testTag(ProfileScreenTestTags.PROFILE_COUNTRY),
                text = country,
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontSize = FontSizes.BodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = cs.onSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
          }
    }
  }
}

/* --------- Redesigned description (Bio) block: full-width clean card --------- */
@Composable
fun ProfileDescription(description: String = "Bio:...") {
  val cs = MaterialTheme.colorScheme
  ElevatedCard(
      modifier =
          Modifier.padding(horizontal = 16.dp)
              .fillMaxWidth()
              .defaultMinSize(minHeight = 94.dp)
              .testTag(ProfileScreenTestTags.PROFILE_DESCRIPTION),
      shape = RoundedCornerShape(14.dp),
      colors =
          CardDefaults.elevatedCardColors(containerColor = cs.background) // <-- unify background
      ) {
        Column(
            modifier =
                Modifier.fillMaxWidth() // still good practice
                    .padding(horizontal = 16.dp, vertical = 14.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = "Bio",
                    tint = cs.secondary,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Bio",
                    color = cs.secondary,
                    style =
                        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
              }
              Spacer(Modifier.height(8.dp))
              Text(
                  text = description.ifBlank { " " },
                  color = cs.onBackground,
                  maxLines = 4,
                  overflow = TextOverflow.Ellipsis,
                  style =
                      MaterialTheme.typography.bodyMedium.copy(
                          fontWeight = FontWeight.Normal,
                          fontSize = FontSizes.BodyMedium,
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
          label = "statScale")

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
      tonalElevation = 0.dp) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable(
                        interactionSource = interaction,
                        indication =
                            rememberRipple(
                                bounded = true, color = contentColor.copy(alpha = 0.25f)),
                        onClick = onClick),
            verticalAlignment = Alignment.CenterVertically) {
              icon()
              Spacer(Modifier.width(10.dp))
              Column {
                Text(
                    text = value,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = FontSizes.StatLarge,
                    maxLines = 1)
                Text(
                    text = title,
                    color = contentColor.copy(alpha = 0.95f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = FontSizes.StatLabel,
                    maxLines = 1)
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
  val cs = MaterialTheme.colorScheme
  ProfileStatCard(
      modifier = modifier,
      containerColor = cs.primary,
      contentColor = cs.onPrimary,
      icon = {
        Icon(
            painter = painterResource(R.drawable.animal_icon),
            contentDescription = "Animals Icon",
            tint = cs.onPrimary,
            modifier = Modifier.size(32.dp),
        )
      },
      title = "Animals",
      value = "$animalCount",
      onClick = { if (ownerProfile) onCollection(id) },
      testTag = ProfileScreenTestTags.COLLECTION)
}

@Composable
fun ProfileFriends(
    modifier: Modifier = Modifier,
    id: Id = "",
    onFriends: (Id) -> Unit = {},
    ownerProfile: Boolean,
    friendCount: Int = 42,
) {
  val cs = MaterialTheme.colorScheme
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
      testTag = ProfileScreenTestTags.FRIENDS)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ProfileAchievements(
    id: Id = "",
    onAchievements: (Id) -> Unit = {},
    ownerProfile: Boolean,
    listAchievement: List<Achievement> = emptyList(),
) {
  val cs = MaterialTheme.colorScheme

  if (listAchievement.isEmpty()) {
    ElevatedCard(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag(ProfileScreenTestTags.ACHIEVEMENTS),
        shape = RoundedCornerShape(14.dp)) {
          Column(
              modifier =
                  Modifier.border(1.dp, cs.background, shape = RoundedCornerShape(14.dp))
                      .padding(12.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            if (ownerProfile) {
              Button(
                  onClick = { onAchievements(id) },
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = cs.background,
                          contentColor = cs.onBackground,
                      ),
                  modifier = Modifier.align(Alignment.End),
              ) {
                Text("View all achievements →")
              }
            }
            Spacer(Modifier.height(8.dp))
            Text("No achievements yet!", color = cs.onBackground)
          }
        }
    return
  }

  var startIndex by rememberSaveable { mutableIntStateOf(0) }
  var navDirection by rememberSaveable { mutableIntStateOf(0) } // -1 left, +1 right
  fun wrap(i: Int) = (i % listAchievement.size + listAchievement.size) % listAchievement.size
  val windowSize = minOf(3, listAchievement.size)
  val visible =
      remember(startIndex, listAchievement) {
        List(windowSize) { k -> listAchievement[wrap(startIndex + k)] }
      }

  ElevatedCard(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp)
              .testTag(ProfileScreenTestTags.ACHIEVEMENTS),
      shape = RoundedCornerShape(14.dp)) {
        Column(
            modifier =
                Modifier.border(1.dp, cs.background, shape = RoundedCornerShape(14.dp))
                    .padding(12.dp)) {
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
                  Text("View all achievements →")
                }
              }

              Spacer(Modifier.height(8.dp))

              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()) {
                    ArrowButton(isLeft = true, tint = cs.secondary) {
                      navDirection = -1
                      startIndex = wrap(startIndex - 1)
                    }

                    Spacer(Modifier.width(6.dp))

                    AnimatedContent(
                        targetState = visible,
                        transitionSpec = {
                          val duration = 220
                          if (navDirection >= 0) {
                            (slideInHorizontally(
                                animationSpec = tween(duration, easing = FastOutSlowInEasing),
                                initialOffsetX = { it / 2 }) + fadeIn(tween(duration))) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(duration, easing = FastOutSlowInEasing),
                                    targetOffsetX = { -it / 2 }) + fadeOut(tween(duration)))
                          } else {
                            (slideInHorizontally(
                                animationSpec = tween(duration, easing = FastOutSlowInEasing),
                                initialOffsetX = { -it / 2 }) +
                                fadeIn(tween(duration))) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(duration, easing = FastOutSlowInEasing),
                                    targetOffsetX = { it / 2 }) + fadeOut(tween(duration)))
                          }
                        },
                        modifier = Modifier.weight(1f).height(124.dp)) { trio ->
                          Row(
                              modifier = Modifier.fillMaxWidth(),
                              horizontalArrangement = Arrangement.spacedBy(8.dp),
                              verticalAlignment = Alignment.CenterVertically) {
                                trio.forEach { a ->
                                  Box(
                                      modifier = Modifier.weight(1f),
                                      contentAlignment = Alignment.Center) {
                                        AchievementChip(a)
                                      }
                                }
                              }
                        }

                    Spacer(Modifier.width(6.dp))

                    ArrowButton(isLeft = false, tint = cs.secondary) {
                      navDirection = +1
                      startIndex = wrap(startIndex + 1)
                    }
                  }
            }
      }
}

@Composable
private fun AchievementChip(a: Achievement) {
  val cs = MaterialTheme.colorScheme
  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(92.dp)) {
    ElevatedCard(shape = RoundedCornerShape(12.dp)) {
      Box(
          modifier = Modifier.size(72.dp).background(cs.background),
          contentAlignment = Alignment.Center) {
            AsyncImage(
                model = a.pictureURL,
                contentDescription = a.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)))
          }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = a.name,
        color = cs.onBackground,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun ArrowButton(isLeft: Boolean, tint: Color, onClick: () -> Unit) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val scale by
      animateFloatAsState(
          targetValue = if (pressed) 0.94f else 1f,
          animationSpec = tween(100, easing = FastOutSlowInEasing),
          label = "arrowScale")
  IconButton(
      onClick = onClick,
      interactionSource = interaction,
      modifier =
          Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
          }) {
        Icon(
            painter =
                painterResource(if (isLeft) R.drawable.chevron_left else R.drawable.chevron_right),
            contentDescription = if (isLeft) "Prev" else "Next",
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
      }
}

@Composable
fun ProfileMap(id: Id = "", onMap: (Id) -> Unit = {}, ownerProfile: Boolean = true) {
  val cs = MaterialTheme.colorScheme
  ElevatedCard(
      modifier =
          Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag(ProfileScreenTestTags.MAP),
      shape = RoundedCornerShape(14.dp)) {
        Column(
            modifier =
                Modifier.border(
                        1.dp,
                        cs.onBackground.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp))
                    .padding(12.dp)) {
              Box(
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(160.dp)
                          .clip(RoundedCornerShape(12.dp))
                          .background(cs.background)) {
                    Text(
                        text = "Map Placeholder",
                        modifier = Modifier.align(Alignment.Center),
                        color = cs.onBackground,
                    )
                  }

              Button(
                  onClick = { onMap(id) },
                  modifier = Modifier.padding(top = 10.dp),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = cs.primary,
                          contentColor = cs.onPrimary,
                      ),
              ) {
                Text(text = "View in full screen →")
              }
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
                containerColor = cs.primary,
                contentColor = cs.onPrimary,
            ),
        shape = RoundedCornerShape(10.dp),
    ) {
      Text(text = "Send Friend Request")
    }
  }
}
