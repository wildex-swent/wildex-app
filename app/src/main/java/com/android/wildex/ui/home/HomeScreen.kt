package com.android.wildex.ui.home

/**
 * HomeScreen.kt
 *
 * Modernized Home Screen for Wildex using Jetpack Compose.
 * - Vertical, feed-like cards (no cramped side-by-side layout)
 * - Optimistic like toggle (instant UI) + background sync
 * - Single-line counts with proper pluralization (no broken words)
 * - Keeps all test tags intact
 */
import android.R.attr.scaleY
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.utils.Id
import java.text.SimpleDateFormat
import java.util.Locale

/** Test tag constants used for UI testing of HomeScreen components. */
object HomeScreenTestTags {
  const val NO_POST_ICON = "HomeScreenNoPost"
  const val NOTIFICATION_BELL = "HomeScreenNotificationBell"
  const val PROFILE_PICTURE = "HomeScreenProfilePicture"
  const val POST_AUTHOR_PICTURE = "HomeScreenPostAuthorPicture"
  const val POST_LIKE = "HomeScreenPostLike"
  const val POST_COMMENT = "HomeScreenPostComment"
  const val POST_IMAGE = "HomeScreenPostImage"
  const val POST_LOCATION = "HomeScreenPostLocation"
  const val POST_LIKE_BUTTON = "HomeScreenPostLikeButton"
}

@Composable
fun HomeScreen(
    homeScreenViewModel: HomeScreenViewModel = viewModel(),
    bottomBar: @Composable () -> Unit = {},
    onPostClick: (postId: Id) -> Unit = {},
    onProfilePictureClick: (userId: Id) -> Unit = {},
    onNotificationClick: () -> Unit = {},
) {
  val uiState by homeScreenViewModel.uiState.collectAsState()
  val user = uiState.currentUser
  val posts = uiState.posts

  LaunchedEffect(Unit) { homeScreenViewModel.refreshUIState() }

  Scaffold(
      topBar = { WildexHomeTopAppBar(user, onNotificationClick, onProfilePictureClick) },
      bottomBar = { bottomBar() },
  ) { pd ->
    if (posts.isEmpty()) {
      NoPostsView()
    } else {
      PostsView(
          posts = posts,
          pd = pd,
          viewModel = homeScreenViewModel,
          onPostClick = onPostClick,
      )
    }
  }
}

/** Placeholder when there are no posts. */
@Composable
fun NoPostsView() {
  Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Icon(
        painter = painterResource(R.drawable.nothing_found),
        contentDescription = "Nothing Found",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(96.dp).testTag(HomeScreenTestTags.NO_POST_ICON),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = "No nearby posts.\nStart posting…",
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis)
  }
}

/** Scrollable list of posts. */
@Composable
fun PostsView(
    posts: List<PostState>,
    pd: PaddingValues,
    viewModel: HomeScreenViewModel,
    onPostClick: (Id) -> Unit,
) {
  LazyColumn(
      modifier = Modifier.fillMaxSize().padding(pd),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(vertical = 12.dp)) {
        items(posts.size) { index ->
          PostItem(postState = posts[index], viewModel = viewModel, onPostClick = onPostClick)
        }
      }
}

/** One modern post card. */
@Composable
fun PostItem(postState: PostState, viewModel: HomeScreenViewModel, onPostClick: (Id) -> Unit) {
  val cs = MaterialTheme.colorScheme
  val post = postState.post
  val author = postState.author
  val animal = postState.animal

  // -------- Optimistic Like State (instant UI) --------
  var liked by remember(post.postId) { mutableStateOf(postState.isLiked) }
  var likeCount by remember(post.postId) { mutableIntStateOf(post.likesCount) }
  val heartScale by
      animateFloatAsState(
          targetValue = if (liked) 1.1f else 1f,
          animationSpec = tween(140, easing = FastOutSlowInEasing),
          label = "heartScale")
  // -----------------------------------------------------

  Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = cs.background),
      border = BorderStroke(width = 1.dp, color = cs.primary.copy(alpha = 0.28f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
  ) {
    // Header: avatar + title + date
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
          AsyncImage(
              model = author.profilePictureURL,
              contentDescription = "Author profile picture",
              modifier =
                  Modifier.size(40.dp)
                      .clip(CircleShape)
                      .testTag(HomeScreenTestTags.POST_AUTHOR_PICTURE),
              contentScale = ContentScale.Crop)
          Spacer(Modifier.width(10.dp))
          Column(Modifier.weight(1f)) {
            Text(
                text = "${author.username} saw ${animal.name}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = cs.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(
                text =
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(post.date.toDate()),
                style = MaterialTheme.typography.labelSmall,
                color = cs.tertiary)
          }
        }

    // Image
    Box(modifier = Modifier.fillMaxWidth().clickable { onPostClick(post.postId) }) {
      AsyncImage(
          model = post.pictureURL,
          contentDescription = "Post picture",
          modifier =
              Modifier.fillMaxWidth()
                  .height(220.dp)
                  .clip(RoundedCornerShape(0.dp))
                  .testTag(HomeScreenTestTags.POST_IMAGE),
          contentScale = ContentScale.Crop,
      )
      IconButton(
          onClick = {
            liked = !liked
            likeCount = if (liked) likeCount + 1 else likeCount - 1
            viewModel.handleLike(post.postId)
          },
          modifier =
              Modifier.align(Alignment.TopStart).testTag(HomeScreenTestTags.POST_LIKE_BUTTON)) {
            Icon(
                imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Like button",
                modifier =
                    Modifier.size(28.dp).graphicsLayer {
                      scaleX = heartScale
                      scaleY = heartScale
                    },
                tint = if (liked) cs.tertiary else cs.onBackground)
          }
    }

    // Meta: location
    if (post.location?.name?.isNotBlank() == true) {
      Row(
          modifier =
              Modifier.fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp)
                  .testTag(HomeScreenTestTags.POST_LOCATION),
          verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                modifier = Modifier.size(20.dp),
                tint = cs.tertiary,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = post.location!!.name,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
          }
    }

    Divider(Modifier.padding(horizontal = 12.dp))

    // Actions: likes & comments
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
          // Likes
          Row(
              modifier =
                  Modifier.testTag(HomeScreenTestTags.POST_LIKE).clickable {
                    liked = !liked
                    likeCount = if (liked) likeCount + 1 else likeCount - 1
                    viewModel.handleLike(post.postId)
                  },
              verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector =
                        if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Likes",
                    modifier = Modifier.size(20.dp),
                    tint = if (liked) cs.tertiary else cs.onBackground,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = likeText(likeCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
              }

          Spacer(Modifier.width(18.dp))

          // Comments
          Row(
              modifier = Modifier.testTag(HomeScreenTestTags.POST_COMMENT),
              verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.comment_icon),
                    contentDescription = "Comments",
                    modifier = Modifier.size(20.dp),
                    tint = cs.onBackground,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = commentText(post.commentsCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
              }
        }
  }
}

/** Plural-friendly, single-line labels */
private fun likeText(count: Int): String = if (count == 1) "1 like" else "$count likes"

private fun commentText(count: Int): String = if (count == 1) "1 comment" else "$count comments"
