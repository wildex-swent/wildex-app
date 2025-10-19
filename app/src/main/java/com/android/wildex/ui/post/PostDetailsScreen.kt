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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import coil.compose.AsyncImage
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL

// ---------- Test tag util (unchanged) ----------
fun testTagForProfilePicture(profileId: String, role: String = ""): String {
  return if (role.isEmpty()) "ProfilePicture_$profileId" else "ProfilePicture_${role}_$profileId"
}

// ---------- Screen ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsScreen(
    postId: String,
    postDetailsScreenViewModel: PostDetailsScreenViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(),
    onGoBack: () -> Unit = {},
    onProfile: (Id) -> Unit = {},
) {
  LaunchedEffect(postId) { postDetailsScreenViewModel.loadPostDetails(postId) }

  val ui by postDetailsScreenViewModel.uiState.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(ui.errorMsg) {
    ui.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      postDetailsScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = { PostDetailsTopBar(onGoBack) },
      bottomBar = {
        // Pinned comment input – content scrolls behind it
        CommentInput(
            userId = ui.currentUserId,
            userProfilePictureURL = ui.currentUserProfilePictureURL,
            onProfile = onProfile,
            postDetailsScreenViewModel = postDetailsScreenViewModel,
        )
      },
  ) { pd ->
    Box(Modifier.fillMaxSize().padding(bottom = pd.calculateBottomPadding())) {
      // Full scroll: hero image + content sheet + comments
      LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(0.dp),
      ) {
        // HERO IMAGE with soft gradient bottom
        item {
          Box(Modifier.fillMaxWidth()) {
            AsyncImage(
                model = ui.pictureURL,
                contentDescription = "Post picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(320.dp),
            )
            // bottom gradient to transition into sheet
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(96.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.background)))
          }
        }

        // CONTENT SHEET (rounded top), contains info + description + "Comments" header
        item {
          Surface(
              color = MaterialTheme.colorScheme.background,
              tonalElevation = 0.dp,
              shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
              modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                  // make the sheet overlap a bit with the image to look continuous
                  Spacer(Modifier.height(8.dp))

                  // INFO BAR
                  PostInfoBar(
                      authorId = ui.authorId,
                      authorProfilePictureURL = ui.authorProfilePictureURL,
                      authorUserName = ui.authorUsername,
                      animalName = ui.animalName,
                      date = ui.date,
                      location = ui.location,
                      likedByCurrentUser = ui.likedByCurrentUser,
                      likesCount = ui.likesCount,
                      onProfile = onProfile,
                      postDetailsScreenViewModel = postDetailsScreenViewModel,
                      modifier =
                          Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                  )

                  // DESCRIPTION – clean card with subtle border
                  if (ui.description.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.background),
                        border =
                            androidx.compose.foundation.BorderStroke(
                                1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                          Text(
                              text = ui.description,
                              color = MaterialTheme.colorScheme.onBackground,
                              modifier = Modifier.padding(14.dp),
                              style = MaterialTheme.typography.bodyMedium,
                          )
                        }
                  }

                  // COMMENTS HEADER
                  Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text =
                            if (ui.commentsUI.size == 1) "1 Comment"
                            else "${ui.commentsUI.size} Comments",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                  }
                }
              }
        }

        // COMMENTS LIST – full-width, airy rows
        itemsIndexed(ui.commentsUI) { idx, commentUI ->
          Comment(
              commentUI = commentUI,
              onProfile = onProfile,
          )
          if (idx < ui.commentsUI.lastIndex) {
            Divider(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
            )
          }
        }

        // Spacer so the last comment clears the bottom input
        item { Spacer(Modifier.height(96.dp)) }
      }
    }
  }
}

// ---------- Top bar ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsTopBar(onGoBack: () -> Unit) {
  TopAppBar(
      title = {
        Text(
            text = "Back to Homepage",
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      },
      navigationIcon = {
        IconButton(onClick = onGoBack) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Homepage",
              tint = MaterialTheme.colorScheme.primary,
          )
        }
      },
  )
}

// ---------- Info bar (author, date, location, like) ----------
@Composable
fun PostInfoBar(
    modifier: Modifier = Modifier,
    authorId: Id = "",
    authorProfilePictureURL: URL = "",
    authorUserName: String = "",
    animalName: Id = "",
    date: String = "",
    location: String = "",
    likedByCurrentUser: Boolean = false,
    likesCount: Int = 0,
    onProfile: (Id) -> Unit = {},
    postDetailsScreenViewModel: PostDetailsScreenViewModel,
) {
  val cs = MaterialTheme.colorScheme

  Row(
      modifier = modifier,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    ClickableProfilePicture(
        modifier = Modifier.size(52.dp),
        profileId = authorId,
        profilePictureURL = authorProfilePictureURL,
        role = "author",
        onProfile = onProfile,
    )

    Column(Modifier.weight(1f)) {
      Text(
          text =
              buildAnnotatedString {
                append("$authorUserName saw ")
                withStyle(SpanStyle(color = cs.primary)) { append("an animal!") }
              },
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
      Text(
          text = date,
          color = cs.tertiary,
          style = MaterialTheme.typography.labelSmall,
      )

      Spacer(Modifier.height(8.dp))

      // Location & Likes row (compact chips)
      Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        if (location.isNotBlank()) {
          Row(
              modifier =
                  Modifier.clip(RoundedCornerShape(20.dp))
                      .border(1.dp, cs.onBackground.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                      .padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Location",
                tint = cs.tertiary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = location,
                style = MaterialTheme.typography.labelMedium,
                color = cs.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(
            modifier =
                Modifier.clip(RoundedCornerShape(20.dp))
                    .border(1.dp, cs.onBackground.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
              onClick = {
                if (!likedByCurrentUser) postDetailsScreenViewModel.addLike()
                else postDetailsScreenViewModel.removeLike()
              },
              modifier = Modifier.size(24.dp),
          ) {
            Icon(
                imageVector =
                    if (likedByCurrentUser) Icons.Filled.Favorite
                    else Icons.Outlined.FavoriteBorder,
                contentDescription = "Like status",
                tint = cs.tertiary,
                modifier = Modifier.size(18.dp),
            )
          }
          Text(
              text = likesCount.toString(),
              color = cs.tertiary,
              style = MaterialTheme.typography.labelMedium,
          )
        }
      }
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
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
      border =
          androidx.compose.foundation.BorderStroke(
              1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
      ClickableProfilePicture(
          modifier = Modifier.size(42.dp),
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
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onBackground,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
              text = commentUI.date,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = commentUI.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
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
  val cs = MaterialTheme.colorScheme

  Box(
      modifier =
          Modifier.fillMaxWidth()
              .background(cs.background)
              .border(
                  width = 1.dp,
                  color = cs.onBackground.copy(alpha = 0.06f),
                  shape = RoundedCornerShape(0.dp))
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
              shape = RoundedCornerShape(20.dp),
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
                          tint = cs.primary,
                      )
                    }
              },
          )
        }
      }
}

// ---------- Reusable profile image (keeps your test tags) ----------
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
      modifier = modifier.height(56.dp).testTag(testTagForProfilePicture(profileId, role)),
  ) {
    AsyncImage(
        model = profilePictureURL,
        contentDescription = "Profile picture",
        modifier =
            Modifier.clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
        contentScale = ContentScale.Crop,
    )
  }
}
