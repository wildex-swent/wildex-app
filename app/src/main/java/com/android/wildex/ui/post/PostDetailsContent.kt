package com.android.wildex.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.android.wildex.R
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
import com.android.wildex.ui.post.PostDetailsScreenTestTags.testTagForProfilePicture
import com.android.wildex.ui.utils.ClickableProfilePicture
import com.android.wildex.ui.utils.buttons.AnimatedLikeButton
import com.android.wildex.ui.utils.expand.ExpandableTextCore
import com.android.wildex.ui.utils.images.ImageWithDoubleTapLike

object PostDetailsContentTestTags {
  const val DESCRIPTION_TEXT = "post_details_description_text"
  const val DESCRIPTION_TOGGLE = "post_details_description_toggle"
  const val IMAGE_BOX = "image_box"
  const val DELETE_COMMENT_BUTTON = "delete_comment_button"
  const val COMMENT_BODY = "post_details_comment_body"
  const val COMMENT_TOGGLE = "post_details_comment_toggle"
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
                location = uiState.location?.generalName ?: "",
                species = uiState.animalSpecies,
                likedByCurrentUser = uiState.likedByCurrentUser,
                likesCount = uiState.likesCount,
                onLike = { postDetailsScreenViewModel.addLike() },
                onUnlike = { postDetailsScreenViewModel.removeLike() },
            )
            Spacer(Modifier.height(10.dp))

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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = colorScheme.background,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
      Column(modifier = Modifier.padding(8.dp)) {
        ExpandableTextCore(
            text = uiState.description,
            collapsedLines = 4,
            bodyTag = PostDetailsContentTestTags.DESCRIPTION_TEXT,
            toggleTag = PostDetailsContentTestTags.DESCRIPTION_TOGGLE,
        )
      }
    }
  }
}

@Composable
private fun PostCommentsHeader(uiState: PostDetailsUIState) {
  Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp)) {
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
        modifier = Modifier.fillMaxWidth().testTag(PostDetailsContentTestTags.IMAGE_BOX))
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
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    ClickableProfilePicture(
        modifier =
            Modifier.size(64.dp)
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
                withStyle(SpanStyle(color = colorScheme.primary)) {
                  append("${animalName.replaceFirstChar { it.uppercase() }}!")
                }
              },
          style = typography.titleLarge,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
      )

      Spacer(Modifier.height(4.dp))

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
    val size = 1.plus(if (species.isNotBlank()) 1 else 0).plus(if (location.isNotBlank()) 1 else 0)
    val itemWeight = 1f / size

  // Location & Likes row
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    val iconSize = 30.dp
    val textStyle = typography.titleMedium
    val spacing = 8.dp
    if (location.isNotBlank()) {
      Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.weight(itemWeight),
      ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = "Location",
            tint = colorScheme.primary,
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
          modifier = Modifier.weight(itemWeight),
      ) {
        Icon(
            imageVector = Icons.Filled.Pets,
            contentDescription = "Species",
            tint = colorScheme.primary,
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
        modifier = Modifier.weight(itemWeight),
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
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .combinedClickable(
                  onClick = {},
                  onLongClick = { if (canDelete) showMenu = true },
              ),
      colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
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
        ExpandableTextCore(
            text = commentUI.text,
            collapsedLines = 3,
            bodyTag = PostDetailsContentTestTags.COMMENT_BODY,
            toggleTag = PostDetailsContentTestTags.COMMENT_TOGGLE,
        )
      }
    }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        containerColor = colorScheme.background,
        modifier = Modifier.testTag(PostDetailsContentTestTags.DELETE_COMMENT_BUTTON)) {
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

private fun String.startsWithVowel(): Boolean {
  val lower = this.lowercase()
  return lower.startsWith("a") ||
      lower.startsWith("e") ||
      lower.startsWith("i") ||
      lower.startsWith("o") ||
      lower.startsWith("u")
}
