package com.android.wildex.ui.home

/**
 * HomeScreen.kt
 *
 * Defines the main Home Screen UI for the Wildex Android app using Jetpack Compose. Displays user
 * posts, profile picture, and notifications. Handles post interactions such as likes and navigation
 * to details.
 */
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
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

/**
 * Entry point composable for the home screen.
 *
 * @param homeScreenViewModel ViewModel managing UI state and data.
 * @param onPostClick Callback invoked when a post is selected.
 * @param onProfilePictureClick Callback invoked when the profile picture is selected.
 * @param onNotificationClick Callback invoked when the notification icon is clicked.
 */
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
      content = { pd ->
        if (posts.isEmpty()) NoPostsView()
        else
            PostsView(
                posts = posts,
                pd = pd,
                viewModel = homeScreenViewModel,
                onPostClick = onPostClick,
            )
      },
  )
}

/** Displays a placeholder view when there are no posts available. */
@Composable
fun NoPostsView() {
  Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Icon(
        painter = painterResource(R.drawable.nothing_found),
        contentDescription = "Nothing Found",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(100.dp).testTag(HomeScreenTestTags.NO_POST_ICON),
    )
    Text(
        text = "No nearby posts.\n Start posting...",
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
    )
  }
}

/**
 * Displays a scrollable list of posts.
 *
 * @param posts List of PostState objects representing UI data for each post.
 * @param pd Padding values from the Scaffold content.
 * @param viewModel Reference to HomeScreenViewModel for like actions.
 * @param onPostClick Callback when a post is clicked.
 */
@Composable
fun PostsView(
    posts: List<PostState>,
    pd: PaddingValues,
    viewModel: HomeScreenViewModel,
    onPostClick: (Id) -> Unit,
) {
  LazyColumn(
      modifier = Modifier.fillMaxSize().padding(pd),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Top,
  ) {
    items(posts.size) { index ->
      PostItem(postState = posts[index], viewModel = viewModel, onPostClick = onPostClick)
    }
  }
}

/**
 * Displays a single post card with image and information.
 *
 * @param postState Contains post, author, and UI state (like status).
 * @param viewModel Reference to the HomeScreenViewModel for handling likes.
 * @param onPostClick Callback invoked when the post is selected.
 */
@Composable
fun PostItem(postState: PostState, viewModel: HomeScreenViewModel, onPostClick: (Id) -> Unit) {
  Card(
      modifier = Modifier.padding(15.dp),
      shape = RoundedCornerShape(20.dp),
      colors =
          CardColors(
              containerColor = Color.White,
              contentColor = MaterialTheme.colorScheme.primary,
              disabledContainerColor = MaterialTheme.colorScheme.primary,
              disabledContentColor = MaterialTheme.colorScheme.primary,
          ),
      elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
  ) {
    Row {
      PostImage(postState, viewModel)
      PostInfo(postState, onPostClick = onPostClick)
    }
  }
}

/**
 * Displays detailed information for a post, including author, timestamp, location, likes, and
 * comments.
 *
 * @param postState State containing post and author information.
 * @param onPostClick Callback when the post is clicked.
 */
@Composable
fun PostInfo(postState: PostState, onPostClick: (postId: Id) -> Unit) {
  val post = postState.post
  val author = postState.author
  val animal = postState.animal

  Column(
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
          Modifier.padding(10.dp, 10.dp, 30.dp, 10.dp)
              .clickable(onClick = { onPostClick(post.postId) }),
  ) {
    Row {
      AsyncImage(
          model = author.profilePictureURL,
          contentDescription = "Author profile picture",
          modifier =
              Modifier.size(50.dp)
                  .clip(CircleShape)
                  .testTag(HomeScreenTestTags.POST_AUTHOR_PICTURE),
      )
      Spacer(modifier = Modifier.width(5.dp))
      Column(
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
            text = "${author.username} saw ${animal.name}",
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Left,
        )
        Text(
            text =
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(post.date.toDate()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
      }
    }
    Column(verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxSize()) {
      Spacer(modifier = Modifier.height(8.dp))
      Row(modifier = Modifier.testTag(HomeScreenTestTags.POST_LOCATION)) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Location",
            modifier = Modifier.size(25.dp),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = post.location?.name ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Row(modifier = Modifier.testTag(HomeScreenTestTags.POST_LIKE)) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = "Likes",
            modifier = Modifier.size(25.dp),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "${post.likesCount} likes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Row(modifier = Modifier.testTag(HomeScreenTestTags.POST_COMMENT)) {
        Icon(
            painter = painterResource(R.drawable.comment_icon),
            contentDescription = "Comments",
            modifier = Modifier.size(25.dp),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "${post.commentsCount} comments",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
      }
    }
  }
}

/**
 * Displays the post image and handles like toggling.
 *
 * @param postState State object containing post data and like status.
 * @param viewModel ViewModel to handle like interactions and state refresh.
 */
@Composable
fun PostImage(postState: PostState, viewModel: HomeScreenViewModel) {
  val post = postState.post
  val isLiked = postState.isLiked
  Box {
    AsyncImage(
        model = post.pictureURL,
        contentDescription = "Post picture",
        modifier =
            Modifier.fillMaxWidth(.5f)
                .height(200.dp)
                .clip(RoundedCornerShape(15.dp))
                .testTag(HomeScreenTestTags.POST_IMAGE),
        contentScale = ContentScale.Crop,
    )
    IconButton(
        onClick = {
          viewModel.handleLike(post.postId)
          viewModel.refreshUIState()
        },
        modifier = Modifier.align(Alignment.TopStart).testTag(HomeScreenTestTags.POST_LIKE_BUTTON),
    ) {
      Icon(
          imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
          contentDescription = "Like button",
          modifier = Modifier.size(30.dp),
      )
    }
  }
}
