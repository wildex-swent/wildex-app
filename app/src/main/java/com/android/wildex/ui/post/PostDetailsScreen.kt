package com.android.wildex.ui.post

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen

fun testTagForProfilePicture(profileId: String, role: String = ""): String {
  return if (role.isEmpty()) "ProfilePicture_$profileId" else "ProfilePicture_${role}_$profileId"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsScreen(
    postId: String,
    postDetailsScreenViewModel: PostDetailsScreenViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onProfile: (Id) -> Unit = {},
) {
  val uiState by postDetailsScreenViewModel.uiState.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(Unit) { postDetailsScreenViewModel.loadPostDetails(postId) }

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      postDetailsScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = { PostDetailsTopBar(onGoBack = onGoBack) },
      bottomBar = {
        // Pinned comment input – content scrolls behind it
        CommentInput(
            userId = uiState.currentUserId,
            userProfilePictureURL = uiState.currentUserProfilePictureURL,
            onProfile = onProfile,
            postDetailsScreenViewModel = postDetailsScreenViewModel,
        )
      },
  ) { pd ->
    val pullState = rememberPullToRefreshState()

    PullToRefreshBox(
        state = pullState,
        isRefreshing = false,
        modifier = Modifier.padding(pd),
        onRefresh = { postDetailsScreenViewModel.loadPostDetails(postId) },
    ) {
      when {
        uiState.isLoading -> {
          LoadingScreen(pd)
        }
        uiState.isError && uiState.postId.isBlank() -> {
          LoadingFail(pd)
        }
        else -> {

          Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
              // HERO IMAGE with soft gradient top and bottom
              item { PostPicture(uiState.pictureURL) }

              // CONTENT SHEET (rounded top), contains info + description + "Comments" header
              item {
                Surface(
                    color = colorScheme.background,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(Modifier.fillMaxWidth()) {
                    // make the sheet overlap a bit with the image to look continuous
                    Spacer(Modifier.height(8.dp))

                    LocationSpeciesLikeBar(
                        location = uiState.location,
                        species = uiState.animalSpecies,
                        likedByCurrentUser = uiState.likedByCurrentUser,
                        likesCount = uiState.likesCount,
                        onLike = { postDetailsScreenViewModel.addLike() },
                        onUnlike = { postDetailsScreenViewModel.removeLike() },
                    )

                    // INFO BAR
                    PostInfoBar(
                        authorId = uiState.authorId,
                        authorProfilePictureURL = uiState.authorProfilePictureURL,
                        authorUserName = uiState.authorUsername,
                        animalName = uiState.animalName,
                        date = uiState.date,
                        onProfile = onProfile,
                    )

                    // DESCRIPTION – clean card with subtle border
                    if (uiState.description.isNotBlank()) {
                      Card(
                          modifier =
                              Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                          shape = RoundedCornerShape(32.dp),
                          colors = CardDefaults.cardColors(containerColor = colorScheme.background),
                          border = BorderStroke(1.dp, colorScheme.tertiary),
                          elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                      ) {
                        Text(
                            text = uiState.description,
                            color = colorScheme.onBackground,
                            modifier = Modifier.padding(14.dp),
                            style = typography.bodyMedium,
                        )
                      }
                    }
                    // COMMENTS HEADER
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                      Text(
                          text =
                              if (uiState.commentsUI.size == 1) "1 Comment"
                              else "${uiState.commentsUI.size} Comments",
                          style = typography.titleSmall,
                          color = colorScheme.onBackground,
                      )
                    }
                  }
                }
              }

              // COMMENTS LIST – full-width, airy rows
              items(uiState.commentsUI) { commentUI ->
                Comment(commentUI = commentUI, onProfile = onProfile)
              }

              // Spacer so the last comment clears the bottom input
              item { Spacer(Modifier.height(96.dp)) }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PostPicture(pictureURL: URL) {
  Box(Modifier.fillMaxWidth()) {
    AsyncImage(
        model = pictureURL,
        contentDescription = "Post picture",
        contentScale = ContentScale.FillWidth,
        modifier = Modifier.fillMaxSize(),
    )
    // top black gradient overlay
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(72.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.7f),
                        1f to Color.Transparent,
                    )))
    // bottom gradient to transition into sheet
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(72.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to colorScheme.background,
                    )))
  }
}

// ---------- Info bar (author, date, location, like) ----------
@Composable
fun PostInfoBar(
    authorId: Id = "",
    authorProfilePictureURL: URL = "",
    authorUserName: String = "",
    animalName: Id = "",
    date: String = "",
    onProfile: (Id) -> Unit = {},
) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    ClickableProfilePicture(
        modifier = Modifier.size(48.dp),
        profileId = authorId,
        profilePictureURL = authorProfilePictureURL,
        role = "author",
        onProfile = onProfile,
    )

    Column(Modifier.weight(1f)) {
      Text(
          text =
              buildAnnotatedString {
                withStyle(SpanStyle(color = colorScheme.tertiary)) { append(authorUserName) }
                append(" saw ${if (animalName.startsWithVowel()) "an " else "a "}")
                withStyle(SpanStyle(color = colorScheme.primary)) { append("${animalName}!") }
              },
          style = typography.titleLarge,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
      )
      Text(text = date, color = colorScheme.tertiary, style = typography.labelMedium)

      Spacer(Modifier.height(5.dp))
    }
  }
}

@Composable
fun LocationSpeciesLikeBar(
    species: String = "",
    location: String = "",
    likedByCurrentUser: Boolean = false,
    likesCount: Int = 0,
    onLike: () -> Unit = {},
    onUnlike: () -> Unit = {},
) {
  // Location & Likes row
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    val iconSize = 28.dp
    val textStyle = typography.titleMedium
    val spacing = 6.dp
    if (location.isNotBlank()) {
      Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth(.33f),
      ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = "Location",
            tint = colorScheme.tertiary,
            modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.width(spacing))
        Text(
            text = location,
            style = textStyle,
            color = colorScheme.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      }
    }

    if (species.isNotBlank()) {
      Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth(.33f),
      ) {
        Icon(
            imageVector = Icons.Filled.Pets,
            contentDescription = "Species",
            tint = colorScheme.tertiary,
            modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.width(spacing))
        Text(
            text = species,
            style = textStyle,
            color = colorScheme.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(.33f),
    ) {
      Icon(
          imageVector =
              if (likedByCurrentUser) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
          contentDescription = "Like status",
          tint = colorScheme.tertiary,
          modifier =
              Modifier.size(iconSize).clickable {
                if (!likedByCurrentUser) onLike() else onUnlike()
              },
      )
      Text(
          text = likesCount.toString(),
          color = colorScheme.tertiary,
          style = textStyle,
      )
    }
  }
}

// ---------- One comment row ----------
@Composable
fun Comment(
    commentUI: CommentWithAuthorUI,
    onProfile: (Id) -> Unit = {},
) {
  Card(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
      shape = RoundedCornerShape(32.dp),
      colors = CardDefaults.cardColors(containerColor = colorScheme.background),
      border = BorderStroke(1.dp, colorScheme.primary),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
      ClickableProfilePicture(
          modifier = Modifier.fillMaxHeight(),
          profileId = commentUI.authorId,
          profilePictureURL = commentUI.authorProfilePictureUrl,
          role = "commenter",
          onProfile = onProfile,
      )

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = commentUI.authorUserName,
              style = typography.labelLarge,
              color = colorScheme.onBackground,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
              text = commentUI.date,
              style = typography.labelSmall,
              color = colorScheme.onBackground.copy(alpha = 0.7f),
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = commentUI.text,
            style = typography.bodyMedium,
            color = colorScheme.onBackground,
        )
      }
    }
  }
}

// ---------- Bottom input (pinned) ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentInput(
    userId: Id = "",
    userProfilePictureURL: URL = "",
    onProfile: (Id) -> Unit = {},
    postDetailsScreenViewModel: PostDetailsScreenViewModel,
) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .background(colorScheme.background)
              .border(
                  width = 1.dp,
                  color = colorScheme.onBackground.copy(alpha = 0.06f),
                  shape = RoundedCornerShape(0.dp),
              )
              .padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          ClickableProfilePicture(
              modifier = Modifier.size(44.dp),
              profileId = userId,
              profilePictureURL = userProfilePictureURL,
              role = "comment_input",
              onProfile = onProfile,
          )

          Spacer(modifier = Modifier.width(8.dp))

          var text by remember { mutableStateOf("") }

          OutlinedTextField(
              value = text,
              onValueChange = { text = it },
              placeholder = { Text("Add a comment …") },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(32.dp),
              singleLine = true,
              trailingIcon = {
                IconButton(
                    onClick = {
                      if (text.isNotBlank()) {
                        postDetailsScreenViewModel.addComment(text)
                        text = ""
                      }
                    }) {
                      Icon(
                          imageVector = Icons.AutoMirrored.Filled.Send,
                          contentDescription = "Send comment",
                          tint = colorScheme.primary,
                      )
                    }
              },
          )
        }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClickableProfilePicture(
    modifier: Modifier = Modifier,
    profileId: String = "",
    profilePictureURL: URL = "",
    role: String = "",
    onProfile: (Id) -> Unit = {},
) {
  IconButton(
      onClick = { onProfile(profileId) },
      modifier = modifier.testTag(testTagForProfilePicture(profileId, role)),
  ) {
    AsyncImage(
        model = profilePictureURL,
        contentDescription = "Profile picture",
        modifier = Modifier.clip(CircleShape).border(1.dp, colorScheme.primary, CircleShape),
        contentScale = ContentScale.Crop,
    )
  }
}

private fun String.startsWithVowel(): Boolean {
  val lower = this.lowercase()
  return lower.startsWith("a") ||
      lower.startsWith("e") ||
      lower.startsWith("i") ||
      lower.startsWith("o") ||
      lower.startsWith("u")
}
