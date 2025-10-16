package com.android.wildex.ui.post

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
import com.android.wildex.ui.theme.WildexTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsScreen(
    postId: String,
    postDetailsScreenViewModel: PostDetailsScreenViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onProfile: (Id) -> Unit = {},
) {
  LaunchedEffect(postId) { postDetailsScreenViewModel.loadPostDetails(postId) }

  val postDetailsUIState by postDetailsScreenViewModel.uiState.collectAsState()
  val errorMsg = postDetailsUIState.errorMsg

  var showDropdown by remember { mutableStateOf(false) }

  val context = LocalContext.current

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      postDetailsScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
        PostDetailsTopBar(
            onGoBack = onGoBack,
        )
      },
      content = { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
              AsyncImage(
                  model = postDetailsUIState.pictureURL,
                  contentDescription = "Post picture",
                  modifier =
                      Modifier.size(40.dp)
                          .clip(CircleShape)
                          .border(2.dp, MaterialTheme.colorScheme.primary),
                  contentScale = ContentScale.Crop)

              PostInfoBar(
                  authorId = postDetailsUIState.authorId,
                  authorProfilePictureURL = postDetailsUIState.authorProfilePictureURL,
                  authorUserName = postDetailsUIState.authorUsername,
                  animalId = postDetailsUIState.animalId,
                  date = postDetailsUIState.date,
                  location = postDetailsUIState.location,
                  likedByCurrentUser = postDetailsUIState.likedByCurrentUser,
                  likesCount = postDetailsUIState.likesCount,
                  onProfile = onProfile,
                  postDetailsScreenViewModel = postDetailsScreenViewModel)

              Box(
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(8.dp)
                          .border(
                              2.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(12.dp))
                          .clip(RoundedCornerShape(24.dp))
                          .padding(8.dp)) {
                    Text(
                        text = postDetailsUIState.description,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                  }

              CommentSection(
                  commentsUI = postDetailsUIState.commentsUI,
                  userId = postDetailsUIState.currentUserId,
                  userProfilePictureURL = postDetailsUIState.currentUserProfilePictureURL,
                  onProfile = onProfile,
                  postDetailsScreenViewModel = postDetailsScreenViewModel)
            }
      })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsTopBar(onGoBack: () -> Unit) {
  TopAppBar(
      title = { Text(text = "Back to Homepage", color = MaterialTheme.colorScheme.primary) },
      navigationIcon = {
        IconButton(onClick = onGoBack) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Homepage",
              tint = MaterialTheme.colorScheme.primary)
        }
      })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostInfoBar(
    modifier: Modifier = Modifier,
    authorId: Id = "",
    authorProfilePictureURL: URL = "",
    authorUserName: String = "",
    animalId: Id = "",
    date: String = "",
    location: String = "",
    likedByCurrentUser: Boolean = false,
    likesCount: Int = 0,
    onProfile: (Id) -> Unit = {},
    postDetailsScreenViewModel: PostDetailsScreenViewModel
) {
  Row(
      modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        ClickableProfilePicture(
            modifier = Modifier.size(64.dp),
            profilePictureURL = authorProfilePictureURL,
            profileId = authorId,
            onProfile = onProfile)

        Column(
            modifier = Modifier.weight(2f).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                  text = "$authorUserName saw a",
              )
              Text(
                  text = animalId,
                  color = MaterialTheme.colorScheme.primary,
              )
              Text(
                  text = date,
                  color = MaterialTheme.colorScheme.tertiary,
              )
            }

        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                  imageVector = Icons.Default.LocationOn,
                  contentDescription = "Location",
                  modifier = Modifier.size(40.dp),
                  tint = MaterialTheme.colorScheme.tertiary)

              Text(
                  text = location,
                  color = MaterialTheme.colorScheme.tertiary,
              )
            }

        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
              IconButton(
                  onClick = {
                    if (likedByCurrentUser) postDetailsScreenViewModel.addLike()
                    else postDetailsScreenViewModel.removeLike()
                  }) {
                    Icon(
                        imageVector =
                            if (likedByCurrentUser) Icons.Filled.Favorite
                            else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (likedByCurrentUser) "Unlike" else "Like",
                        modifier = Modifier.size(40.dp),
                        tint =
                            if (likedByCurrentUser) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                  }

              Text(text = likesCount.toString(), color = MaterialTheme.colorScheme.tertiary)
            }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentSection(
    modifier: Modifier = Modifier,
    commentsUI: List<CommentWithAuthorUI> = emptyList(),
    userId: Id = "",
    userProfilePictureURL: URL = "",
    onProfile: (Id) -> Unit = {},
    postDetailsScreenViewModel: PostDetailsScreenViewModel
) {
  Box(
      modifier =
          Modifier.padding(8.dp)
              .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
              .clip(RoundedCornerShape(24.dp))) {
        Column(modifier = modifier.fillMaxWidth()) {
          LazyColumn(
              modifier = Modifier.weight(1f).fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(0.dp)) {
                items(commentsUI) { commentUI ->
                  Comment(commentUI = commentUI, onProfile = onProfile)
                }
              }

          CommentInput(
              userId = userId,
              userProfilePictureURL = userProfilePictureURL,
              onProfile = onProfile,
              postDetailsScreenViewModel = postDetailsScreenViewModel)
        }
      }
}

@Composable
fun Comment(
    commentUI: CommentWithAuthorUI,
    onProfile: (Id) -> Unit = {},
) {
  Box(modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary).padding(14.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
      ClickableProfilePicture(
          modifier = Modifier.size(48.dp),
          profileId = commentUI.authorId,
          profilePictureURL = commentUI.authorProfilePictureUrl,
          onProfile = onProfile)

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = commentUI.authorUserName,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
              text = commentUI.date,
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(text = commentUI.text, style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentInput(
    userId: Id = "",
    userProfilePictureURL: URL = "",
    onProfile: (Id) -> Unit = {},
    postDetailsScreenViewModel: PostDetailsScreenViewModel
) {
  Box(
      modifier =
          Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
              .padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
              ClickableProfilePicture(
                  modifier = Modifier.size(56.dp),
                  profileId = userId,
                  profilePictureURL = userProfilePictureURL,
                  onProfile = onProfile)

              Spacer(modifier = Modifier.width(8.dp))

              var text by remember { mutableStateOf("") }

              OutlinedTextField(
                  value = text,
                  onValueChange = { text = it },
                  placeholder = { Text("Add a comment...") },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(24.dp),
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
                              contentDescription = "Send comment")
                        }
                  })
            }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClickableProfilePicture(
    modifier: Modifier = Modifier,
    profileId: String = "",
    profilePictureURL: URL = "",
    onProfile: (Id) -> Unit = {}
) {
  IconButton(
      onClick = { onProfile(profileId) },
      modifier =
          modifier.background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)) {
        AsyncImage(
            model = profilePictureURL,
            contentDescription = "Profile picture",
            modifier =
                Modifier.size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentScale = ContentScale.Crop)
      }
}

@Preview
@Composable
fun ProfileScreenPreview() {
  WildexTheme { Surface(modifier = Modifier.fillMaxSize()) { PostDetailsScreen("post1") } }
}
