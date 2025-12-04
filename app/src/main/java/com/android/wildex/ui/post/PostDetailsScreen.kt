package com.android.wildex.ui.post

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.R
import com.android.wildex.model.DefaultConnectivityObserver
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.post.PostDetailsScreenTestTags.testTagForProfilePicture
import com.android.wildex.ui.utils.ClickableProfilePicture
import com.android.wildex.ui.utils.buttons.AnimatedLikeButton
import com.android.wildex.ui.utils.images.ImageWithDoubleTapLike
import com.android.wildex.ui.utils.offline.OfflineScreen

object PostDetailsScreenTestTags {
  fun testTagForProfilePicture(profileId: String, role: String = ""): String {
    return if (role.isEmpty()) "ProfilePicture_$profileId" else "ProfilePicture_${role}_$profileId"
  }

  const val BACK_BUTTON = "backButton"
  const val DELETE_POST_DIALOG = "delete_post_dialog"
  const val DELETE_POST_CONFIRM_BUTTON = "delete_post_confirm_button"
  const val DELETE_POST_DISMISS_BUTTON = "delete_post_dismiss_button"
  const val DELETE_COMMENT_BUTTON = "delete_comment_button"
  const val IMAGE_BOX = "image_box"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsScreen(
    postId: String,
    postDetailsScreenViewModel: PostDetailsScreenViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onProfile: (Id) -> Unit = {},
) {
  val context = LocalContext.current
  val uiState by postDetailsScreenViewModel.uiState.collectAsState()
  val connectivityObserver = remember { DefaultConnectivityObserver(context) }
  val isOnlineObs by connectivityObserver.isOnline.collectAsState()
  val isOnline = isOnlineObs && LocalConnectivityObserver.current
  var showActionSheet by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) { postDetailsScreenViewModel.loadPostDetails(postId) }

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      postDetailsScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.testTag(NavigationTestTags.POST_DETAILS_SCREEN),
      topBar = {
        PostDetailsTopBar(
            onGoBack = onGoBack,
            onOpenActions = { showActionSheet = true },
        )
      },
      bottomBar = {
        // Pinned comment input – content scrolls behind it
        CommentInput(
            userId = uiState.currentUserId,
            userProfilePictureURL = uiState.currentUserProfilePictureURL,
            userUserType = uiState.currentUserUserType,
            onProfile = onProfile,
            postDetailsScreenViewModel = postDetailsScreenViewModel,
        )
      },
  ) { innerPadding ->
    if (isOnline) {
      PostDetailsScreenContent(
          innerPadding = innerPadding,
          uiState = uiState,
          postDetailsScreenViewModel = postDetailsScreenViewModel,
          postId = postId,
          onProfile = onProfile,
          onGoBack = onGoBack,
          showActionSheet = showActionSheet,
          onDismissActionSheet = { showActionSheet = false },
      )
    } else {
      OfflineScreen(innerPadding = innerPadding)
    }
  }
}

@Composable
fun PostDetailsScreenContent(
    innerPadding: PaddingValues,
    uiState: PostDetailsUIState,
    postDetailsScreenViewModel: PostDetailsScreenViewModel,
    postId: Id,
    onProfile: (Id) -> Unit,
    onGoBack: () -> Unit,
    showActionSheet: Boolean = false,
    onDismissActionSheet: () -> Unit,
) {
  val context = LocalContext.current
  val pullState = rememberPullToRefreshState()
  var showDeletionValidation by remember { mutableStateOf(false) }

  if (showActionSheet) {
    PostDetailsActions(
        postLocation = uiState.location,
        onDeletePressed = { showDeletionValidation = true },
        onDismissRequest = onDismissActionSheet,
        isAuthor = (uiState.currentUserId == uiState.authorId),
    )
  }

  PullToRefreshBox(
      state = pullState,
      isRefreshing = uiState.isRefreshing,
      modifier = Modifier.padding(innerPadding),
      onRefresh = { postDetailsScreenViewModel.refreshPostDetails(postId) },
  ) {
    when {
      uiState.isError -> LoadingFail()
      uiState.isLoading -> LoadingScreen()
      else -> {
        PostDetailsContent(
            uiState = uiState,
            postDetailsScreenViewModel = postDetailsScreenViewModel,
            onProfile = onProfile)
        if (showDeletionValidation) {
          AlertDialog(
              onDismissRequest = { showDeletionValidation = false },
              title = {
                Text(
                    text = context.getString(R.string.post_details_delete_post),
                    style = typography.titleLarge)
              },
              text = {
                Text(
                    text = context.getString(R.string.post_details_delete_post_confirmation),
                    style = typography.bodyMedium)
              },
              modifier = Modifier.testTag(PostDetailsScreenTestTags.DELETE_POST_DIALOG),
              confirmButton = {
                TextButton(
                    modifier =
                        Modifier.testTag(PostDetailsScreenTestTags.DELETE_POST_CONFIRM_BUTTON),
                    onClick = {
                      showDeletionValidation = false
                      postDetailsScreenViewModel.removePost(postId)
                      onGoBack()
                    },
                ) {
                  Text(
                      text = context.getString(R.string.post_details_final_delete_post),
                      color = colorScheme.error,
                      style = typography.bodyMedium)
                }
              },
              dismissButton = {
                TextButton(
                    modifier =
                        Modifier.testTag(PostDetailsScreenTestTags.DELETE_POST_DISMISS_BUTTON),
                    onClick = { showDeletionValidation = false },
                ) {
                  Text(
                      text = context.getString(R.string.post_details_cancel_delete_post),
                      style = typography.bodyMedium)
                }
              },
          )
        }
      }
    }
  }
}

@Composable
fun PostDetailsContent(
    uiState: PostDetailsUIState,
    postDetailsScreenViewModel: PostDetailsScreenViewModel,
    onProfile: (Id) -> Unit
) {
  Box(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
      // HERO IMAGE with soft gradient top and bottom
      item {
        PostPicture(
            pictureURL = uiState.pictureURL,
            likedByCurrentUser = uiState.likedByCurrentUser,
            onLike = { postDetailsScreenViewModel.addLike() },
        )
      }

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
                location = uiState.location?.name ?: "",
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
                authorUserType = uiState.authorUserType,
                animalName = uiState.animalName,
                date = uiState.date,
                onProfile = onProfile,
            )

            // DESCRIPTION – clean card with subtle border
            PostDescription(uiState)

            // COMMENTS HEADER
            PostCommentsHeader(uiState)
          }
        }
      }

      // COMMENTS LIST – full-width, airy rows
      items(uiState.commentsUI) { commentUI ->
        Comment(
            commentUI = commentUI,
            onProfile = onProfile,
            onDelete = { postDetailsScreenViewModel.removeComment(commentUI.commentId) },
            canDelete = (uiState.currentUserId == commentUI.authorId))
      }

      // Spacer so the last comment clears the bottom input
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
}

@Composable
private fun PostDescription(uiState: PostDetailsUIState) {
  if (uiState.description.isNotBlank()) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.background),
        border = BorderStroke(1.dp, colorScheme.onBackground),
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
}

@Composable
private fun PostCommentsHeader(uiState: PostDetailsUIState) {
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

@Composable
private fun PostPicture(
    pictureURL: URL = "",
    likedByCurrentUser: Boolean = false,
    onLike: () -> Unit = {},
) {
  Box(Modifier.fillMaxWidth()) {
    ImageWithDoubleTapLike(
        pictureURL = pictureURL,
        likedByCurrentUser = likedByCurrentUser,
        onLike = onLike,
        modifier = Modifier.fillMaxWidth().testTag(PostDetailsScreenTestTags.IMAGE_BOX))
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
    authorUserType: UserType = UserType.REGULAR,
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
        modifier =
            Modifier.size(48.dp)
                .testTag(testTagForProfilePicture(profileId = authorId, role = "author")),
        profileId = authorId,
        profilePictureURL = authorProfilePictureURL,
        profileUserType = authorUserType,
        onProfile = onProfile,
    )

    Column(Modifier.weight(1f)) {
      Text(
          text =
              buildAnnotatedString {
                withStyle(SpanStyle(color = colorScheme.onBackground)) { append(authorUserName) }
                append(" saw ${if (animalName.startsWithVowel()) "an " else "a "}")
                withStyle(SpanStyle(color = colorScheme.onBackground)) { append("${animalName}!") }
              },
          style = typography.titleLarge,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
      )
      Text(text = date, color = colorScheme.onBackground, style = typography.labelMedium)

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
            tint = colorScheme.onBackground,
            modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.width(spacing))
        Text(
            text = location,
            style = textStyle,
            color = colorScheme.onBackground,
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
            tint = colorScheme.onBackground,
            modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.width(spacing))
        Text(
            text = species,
            style = textStyle,
            color = colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      }
    }

    AnimatedLikeButton(
        likedByCurrentUser = likedByCurrentUser,
        likesCount = likesCount,
        onToggleLike = { if (!likedByCurrentUser) onLike() else onUnlike() },
        iconSize = iconSize,
        textStyle = textStyle,
    )
  }
}

// ---------- One comment row ----------
@Composable
fun Comment(
    commentUI: CommentWithAuthorUI,
    onProfile: (Id) -> Unit = {},
    onDelete: () -> Unit = {},
    canDelete: Boolean = false,
) {
  val context = LocalContext.current
  var showMenu by remember { mutableStateOf(false) }

  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 6.dp)
              .combinedClickable(
                  onClick = {},
                  onLongClick = { if (canDelete) showMenu = true },
              ),
      shape = RoundedCornerShape(32.dp),
      colors = CardDefaults.cardColors(containerColor = colorScheme.background),
      border = BorderStroke(1.dp, colorScheme.onBackground),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
      ClickableProfilePicture(
          modifier =
              Modifier.size(44.dp)
                  .testTag(
                      testTagForProfilePicture(profileId = commentUI.authorId, role = "commenter")),
          profileId = commentUI.authorId,
          profilePictureURL = commentUI.authorProfilePictureUrl,
          profileUserType = commentUI.authorUserType,
          onProfile = onProfile,
      )

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = commentUI.authorUserName,
              style = typography.labelLarge,
              color = colorScheme.onBackground,
              modifier = Modifier.weight(1f))
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

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        modifier = Modifier.testTag(PostDetailsScreenTestTags.DELETE_COMMENT_BUTTON)) {
          if (canDelete) {
            DropdownMenuItem(
                text = {
                  Text(
                      text = context.getString(R.string.post_details_delete_comment),
                      style = typography.bodyMedium,
                      color = colorScheme.error,
                  )
                },
                onClick = {
                  onDelete()
                  showMenu = false
                })
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
    userUserType: UserType = UserType.REGULAR,
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
              modifier =
                  Modifier.size(44.dp)
                      .testTag(
                          testTagForProfilePicture(profileId = userId, role = "comment_input")),
              profileId = userId,
              profilePictureURL = userProfilePictureURL,
              profileUserType = userUserType,
              onProfile = onProfile,
          )

          Spacer(modifier = Modifier.width(8.dp))

          var text by remember { mutableStateOf("") }

          OutlinedTextField(
              value = text,
              onValueChange = { text = it },
              placeholder = { Text(text = "Add a comment …", style = typography.bodyMedium) },
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
                          tint = colorScheme.onBackground,
                      )
                    }
              },
          )
        }
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
