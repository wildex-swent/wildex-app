@file:OptIn(ExperimentalMaterial3Api::class)

package com.android.wildex.ui.home

/**
 * HomeScreen.kt
 *
 * Defines the main Home Screen UI for the Wildex Android app using Jetpack Compose. Displays user
 * posts, profile picture, and notifications. Handles post interactions such as likes and navigation
 * to details.
 */
import android.widget.Toast
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.utils.ClickableProfilePicture
import java.text.SimpleDateFormat
import java.util.Locale

/** Test tag constants used for UI testing of HomeScreen components. */
object HomeScreenTestTags {
  const val NO_POST_ICON = "HomeScreenNoPost"
  const val NOTIFICATION_BELL = "HomeScreenNotificationBell"
  const val PROFILE_PICTURE = "HomeScreenProfilePicture"
  const val TITLE = "HomeScreenTitle"
  const val POSTS_LIST = "HomeScreenPostsList"
  const val NO_POSTS = "HomeScreenEmpty"

  fun testTagForPost(postId: Id, element: String): String = "HomeScreenPost_${postId}_$element"

  fun likeTag(postId: Id): String = testTagForPost(postId, "LikeCount")

  fun commentTag(postId: Id): String = testTagForPost(postId, "CommentCount")

  fun locationTag(postId: Id): String = testTagForPost(postId, "Location")

  fun imageTag(postId: Id): String = testTagForPost(postId, "Image")

  fun authorPictureTag(postId: Id): String = testTagForPost(postId, "AuthorPicture")

  fun likeButtonTag(postId: Id): String = testTagForPost(postId, "LikeButton")
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
  val postStates = uiState.postStates

  val context = LocalContext.current

  LaunchedEffect(Unit) { homeScreenViewModel.loadUIState() }
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      homeScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = { HomeTopBar(user, onNotificationClick, onProfilePictureClick) },
      bottomBar = { bottomBar() },
      modifier = Modifier.testTag(NavigationTestTags.HOME_SCREEN)) { pd ->
        val pullState = rememberPullToRefreshState()

        PullToRefreshBox(
            state = pullState,
            isRefreshing = uiState.isRefreshing,
            modifier = Modifier.padding(pd),
            onRefresh = { homeScreenViewModel.refreshUIState() },
        ) {
          when {
            uiState.isError -> LoadingFail()
            uiState.isLoading -> LoadingScreen()
            postStates.isEmpty() -> NoPostsView()
            else ->
                PostsView(
                    postStates = postStates,
                    onProfilePictureClick = onProfilePictureClick,
                    onPostLike = homeScreenViewModel::toggleLike,
                    onPostClick = onPostClick,
                )
          }
        }
      }
}

/** Displays a placeholder view when there are no posts available. */
@Composable
fun NoPostsView() {
  Column(
      modifier = Modifier.fillMaxSize().padding(24.dp).testTag(HomeScreenTestTags.NO_POSTS),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Icon(
        painter = painterResource(R.drawable.nothing_found),
        contentDescription = "Nothing Found",
        tint = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.size(96.dp).testTag(HomeScreenTestTags.NO_POST_ICON),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = LocalContext.current.getString(R.string.no_nearby_posts),
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
  }
}

/**
 * Displays a scrollable list of posts.
 *
 * @param postStates List of PostState objects representing UI data for each post.
 * @param onPostLike Lambda invoked when a post like action occurs.
 * @param onPostClick Callback when a post is clicked.
 */
@Composable
fun PostsView(
    postStates: List<PostState>,
    onProfilePictureClick: (userId: Id) -> Unit = {},
    onPostLike: (Id) -> Unit,
    onPostClick: (Id) -> Unit,
) {
  LazyColumn(
      modifier = Modifier.fillMaxSize().testTag(HomeScreenTestTags.POSTS_LIST),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(vertical = 12.dp),
  ) {
    items(postStates.size) { index ->
      PostItem(
          postState = postStates[index],
          onProfilePictureClick = onProfilePictureClick,
          onPostLike = onPostLike,
          onPostClick = onPostClick,
      )
    }
  }
}

/**
 * Displays a single post card with image and information.
 *
 * @param postState Contains post, author, and UI state (like status).
 * @param onPostLike Lambda to call when the post is liked/unliked.
 * @param onPostClick Callback invoked when the post is selected.
 */
@Composable
fun PostItem(
    postState: PostState,
    onProfilePictureClick: (userId: Id) -> Unit = {},
    onPostLike: (Id) -> Unit,
    onPostClick: (Id) -> Unit
) {
  val colorScheme = MaterialTheme.colorScheme
  val post = postState.post
  val author = postState.author
  val animalName = postState.animalName

  // -------- Optimistic Like State (instant UI) --------
  var liked by remember(post.postId) { mutableStateOf(postState.isLiked) }
  var likeCount by remember(post.postId) { mutableIntStateOf(post.likesCount) }
  val heartScale by
      animateFloatAsState(
          targetValue = if (liked) 1.1f else 1f,
          animationSpec = tween(140, easing = FastOutSlowInEasing),
          label = "heartScale",
      )

  // Single place where we define like toggle logic
  val onToggleLike: () -> Unit = {
    liked = !liked
    likeCount = if (liked) likeCount + 1 else likeCount - 1
    onPostLike(post.postId)
  }

  Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colorScheme.background),
      border = BorderStroke(width = 1.dp, color = colorScheme.onBackground.copy(alpha = 0.28f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
  ) {
    // Header: avatar + title + date
    PostHeader(
        post = post,
        author = author,
        animalName = animalName,
        colorScheme = colorScheme,
        onProfilePictureClick = onProfilePictureClick,
    )

    // Image
    PostImage(
        post = post,
        liked = liked,
        heartScale = heartScale,
        colorScheme = colorScheme,
        onPostClick = { onPostClick(post.postId) },
        onToggleLike = onToggleLike,
    )

    // Actions: likes & comments & location
    PostActions(
        post = post,
        liked = liked,
        likeCount = likeCount,
        colorScheme = colorScheme,
        onToggleLike = onToggleLike,
        onPostClick = { onPostClick(post.postId) },
    )
  }
}

@Composable
private fun PostHeader(
    post: Post,
    author: SimpleUser,
    animalName: String,
    colorScheme: ColorScheme,
    onProfilePictureClick: (Id) -> Unit,
) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    ClickableProfilePicture(
        modifier = Modifier.size(40.dp).testTag(HomeScreenTestTags.authorPictureTag(post.postId)),
        profileId = author.userId,
        profilePictureURL = author.profilePictureURL,
        profileUserType = author.userType,
        onProfile = onProfilePictureClick,
    )
    Spacer(Modifier.width(10.dp))
    Column(Modifier.weight(1f)) {
      Text(
          text =
              "${author.username} saw ${if (animalName.startsWithVowel()) "an " else "a "}$animalName",
          style = typography.titleSmall,
          color = colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
            text =
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(post.date.toDate()),
            style = typography.labelSmall,
            color = colorScheme.tertiary,
        )

        if (post.location?.name?.isNotBlank() == true) {
          Row(
              modifier =
                  Modifier.fillMaxWidth(.4f).testTag(HomeScreenTestTags.locationTag(post.postId)),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                modifier = Modifier.size(13.dp).offset(y = (-1).dp),
                tint = colorScheme.tertiary,
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = post.location.name,
                style = typography.labelMedium,
                color = colorScheme.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PostImage(
    post: Post,
    liked: Boolean,
    heartScale: Float,
    colorScheme: ColorScheme,
    onPostClick: () -> Unit,
    onToggleLike: () -> Unit,
) {
  Box(
      modifier = Modifier.fillMaxWidth().clickable { onPostClick() },
  ) {
    AsyncImage(
        model = post.pictureURL,
        contentDescription = "Post picture",
        modifier =
            Modifier.fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(0.dp))
                .testTag(HomeScreenTestTags.imageTag(post.postId)),
        contentScale = ContentScale.Crop,
    )
    IconButton(
        onClick = onToggleLike,
        modifier =
            Modifier.align(Alignment.TopStart)
                .testTag(HomeScreenTestTags.likeButtonTag(post.postId)),
    ) {
      Icon(
          imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
          contentDescription = "Like button",
          modifier =
              Modifier.size(28.dp).graphicsLayer {
                scaleX = heartScale
                scaleY = heartScale
              },
          tint = if (liked) colorScheme.tertiary else colorScheme.onBackground,
      )
    }
  }
}

@Composable
private fun PostActions(
    post: Post,
    liked: Boolean,
    likeCount: Int,
    colorScheme: ColorScheme,
    onToggleLike: () -> Unit,
    onPostClick: () -> Unit,
) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    // Likes
    Row(
        modifier =
            Modifier.testTag(HomeScreenTestTags.likeTag(post.postId)).clickable { onToggleLike() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
          imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
          contentDescription = "Likes",
          modifier = Modifier.size(20.dp),
          tint = if (liked) colorScheme.tertiary else colorScheme.onBackground,
      )
      Spacer(Modifier.width(6.dp))
      Text(
          text = likeText(likeCount),
          style = typography.bodyMedium,
          color = colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(Modifier.width(15.dp))
    // Comments
    Row(
        modifier =
            Modifier.testTag(HomeScreenTestTags.commentTag(post.postId)).clickable {
              onPostClick()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
          imageVector = Icons.AutoMirrored.Filled.Comment,
          contentDescription = "Comments",
          modifier = Modifier.size(20.dp),
          tint = colorScheme.onBackground,
      )
      Spacer(Modifier.width(6.dp))
      Text(
          text = commentText(post.commentsCount),
          style = typography.bodyMedium,
          color = colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

/** Plural-friendly, single-line labels */
private fun likeText(count: Int): String = if (count == 1) "1 like" else "$count likes"

private fun commentText(count: Int): String = if (count == 1) "1 comment" else "$count comments"

private fun String.startsWithVowel(): Boolean {
  val lower = this.lowercase()
  return lower.startsWith("a") ||
      lower.startsWith("e") ||
      lower.startsWith("i") ||
      lower.startsWith("o") ||
      lower.startsWith("u")
}
