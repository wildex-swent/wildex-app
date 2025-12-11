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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.AppTheme
import com.android.wildex.R
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.profile.StaticMiniMap
import com.android.wildex.ui.utils.ClickableProfilePicture
import com.mapbox.geojson.Point
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
  const val PULL_TO_REFRESH = "HomeScreenPullToRefresh"

  fun testTagForPost(postId: Id, element: String): String = "HomeScreenPost_${postId}_$element"

  fun sliderTag(postId: Id): String = testTagForPost(postId, "Slider")

  fun mapPreviewTag(postId: Id): String = testTagForPost(postId, "MapPreview")

  fun mapPreviewButtonTag(postId: Id): String = testTagForPost(postId, "MapPreviewButton")

  fun mapLocationTag(postId: Id): String = testTagForPost(postId, "MapLocation")

  fun sliderStateTag(postId: Id): String = testTagForPost(postId, "SliderState")

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
  val connectivityObserver = LocalConnectivityObserver.current
  val isOnline by connectivityObserver.isOnline.collectAsState()

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
      modifier = Modifier.testTag(NavigationTestTags.HOME_SCREEN),
  ) { pd ->
    val pullState = rememberPullToRefreshState()

    PullToRefreshBox(
        state = pullState,
        isRefreshing = uiState.isRefreshing,
        modifier = Modifier.padding(pd).testTag(HomeScreenTestTags.PULL_TO_REFRESH),
        onRefresh = {
          if (isOnline) homeScreenViewModel.refreshUIState()
          else homeScreenViewModel.refreshOffline()
        },
    ) {
      when {
        uiState.isError -> LoadingFail()
        uiState.isLoading -> LoadingScreen()
        postStates.isEmpty() -> NoPostsView()
        else -> {
          val filteredPostStates = homeScreenViewModel.filterPosts(postStates = postStates)

          PostsView(
              postStates = filteredPostStates,
              onProfilePictureClick = onProfilePictureClick,
              onPostLike = homeScreenViewModel::toggleLike,
              onPostClick = onPostClick,
          )
        }
      }
    }

    OpenFiltersButton(homeScreenViewModel = homeScreenViewModel)
  }
}

/**
 * Displays a FloatingActionButton that opens the Filters Manager when clicked
 *
 * @param homeScreenViewModel the view model of the screen
 */
@Composable
fun OpenFiltersButton(
    homeScreenViewModel: HomeScreenViewModel = viewModel(),
) {
  val uiState by homeScreenViewModel.uiState.collectAsState()
  val cs = colorScheme

  var showFilters by remember { mutableStateOf(false) }

  var fromAuthor: String? by remember { mutableStateOf(null) }
  var fromPlace: String? by remember { mutableStateOf(null) }
  var ofAnimal: String? by remember { mutableStateOf(null) }
  var onlyFriendsPosts by remember { mutableStateOf(false) }
  var onlyMyPosts by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize()) {
    FloatingActionButton(
        onClick = { showFilters = true },
        modifier =
            Modifier.align(Alignment.BottomEnd)
                .padding(bottom = 75.dp, end = 15.dp)
                .clip(shape = RoundedCornerShape(100)),
        containerColor = cs.background,
        contentColor = cs.onBackground,
    ) {
      Icon(
          imageVector = Icons.Default.Search,
          contentDescription = "Filter",
          modifier = Modifier.size(25.dp),
      )
    }

    if (showFilters) {
      FiltersManager(
          postsFilters =
              PostsFilters(
                  fromAuthor = fromAuthor,
                  fromPlace = fromPlace,
                  ofAnimal = ofAnimal,
                  onlyFriendsPosts = onlyFriendsPosts,
                  onlyMyPosts = onlyMyPosts,
              ),
          onFromAuthorChange = { fromAuthor = it },
          onFromPlaceChange = { fromPlace = it },
          onOfAnimalChange = { ofAnimal = it },
          onOnlyFriendsPostsChange = {
            onlyFriendsPosts = it
            if (onlyFriendsPosts && onlyMyPosts) {
              onlyMyPosts = false
            }
          },
          onOnlyMyPostsChange = {
            onlyMyPosts = it
            if (onlyFriendsPosts && onlyMyPosts) {
              onlyFriendsPosts = false
            }
          },
          onDismissRequest = {
            fromAuthor = uiState.postsFilters.fromAuthor
            fromPlace = uiState.postsFilters.fromPlace
            ofAnimal = uiState.postsFilters.ofAnimal
            onlyFriendsPosts = uiState.postsFilters.onlyFriendsPosts
            onlyMyPosts = uiState.postsFilters.onlyMyPosts

            showFilters = false
          },
          onApply = {
            if (ofAnimal == "") {
              ofAnimal = null
            }
            if (fromPlace == "") {
              fromPlace = null
            }
            if (fromAuthor == "") {
              fromAuthor = null
            }

            homeScreenViewModel.setPostsFilter(
                fromPlace = fromPlace,
                fromAuthor = fromAuthor,
                ofAnimal = ofAnimal,
                onlyFriendsPosts = onlyFriendsPosts,
                onlyMyPosts = onlyMyPosts,
            )

            showFilters = false
          },
          onReset = {
            fromAuthor = null
            fromPlace = null
            ofAnimal = null
            onlyFriendsPosts = false
            onlyMyPosts = false

            homeScreenViewModel.setPostsFilter(
                fromAuthor = null,
                fromPlace = null,
                ofAnimal = null,
                onlyFriendsPosts = false,
                onlyMyPosts = false,
            )

            showFilters = false
          },
      )
    }
  }
}

/**
 * Displays the Filters Manager to interact with the 4 filters: fromAuthor, fromPlace, ofAnimal,
 * onlyFriendsPosts and onlyMyPosts.
 *
 * @param postsFilters the values of the filters
 * @param onFromAuthorChange the function to apply when the value of the fromAuthor filter is
 *   modified
 * @param onFromPlaceChange the function to apply when the value of the fromPlace filter is modified
 * @param onOfAnimalChange the function to apply when the value of the ofAnimal filter is modified
 * @param onOnlyFriendsPostsChange the function to apply when the value of the onlyFriendsPosts
 *   filter is modified
 * @param onOnlyFriendsPostsChange the function to apply when the value of the onlyFriendsPosts
 *   filter is modified
 * @param onDismissRequest the function to apply when the user quits the Filters Managers
 * @param onApply the function to apply when the user applies new filters
 * @param onReset the function to apply when the user resets the filters
 */
@Composable
private fun FiltersManager(
    postsFilters: PostsFilters,
    onFromAuthorChange: (String?) -> Unit,
    onFromPlaceChange: (String?) -> Unit,
    onOfAnimalChange: (String?) -> Unit,
    onOnlyFriendsPostsChange: (Boolean) -> Unit,
    onOnlyMyPostsChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
) {
  val cs = colorScheme

  AlertDialog(
      containerColor = cs.background,
      iconContentColor = cs.onBackground,
      titleContentColor = cs.onBackground,
      textContentColor = cs.onBackground,
      onDismissRequest = onDismissRequest,
      title = {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
          Text("Filters Manager")
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          FilterTextField(
              value = postsFilters.fromAuthor,
              onValueChange = onFromAuthorChange,
              filterName = "Author",
          )

          FilterTextField(
              value = postsFilters.fromPlace,
              onValueChange = onFromPlaceChange,
              filterName = "Location",
          )

          FilterTextField(
              value = postsFilters.ofAnimal,
              onValueChange = onOfAnimalChange,
              filterName = "Animal",
          )

          Row(
              modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("See only my friends posts")
            Switch(
                checked = postsFilters.onlyFriendsPosts,
                onCheckedChange = onOnlyFriendsPostsChange,
            )
          }

          Row(
              modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("See only my posts")
            Switch(
                checked = postsFilters.onlyMyPosts,
                onCheckedChange = onOnlyMyPostsChange,
            )
          }
        }
      },
      confirmButton = { TextButton(onClick = onApply) { Text("Apply") } },
      dismissButton = { TextButton(onClick = onReset) { Text("Reset") } },
  )
}

/**
 * Displays a mutable text field for a filter
 *
 * @param value the value of the filter
 * @param onValueChange the function to apply when the value of the filter is modified
 * @param filterName the name of the filter
 */
@Composable
private fun FilterTextField(
    value: String?,
    onValueChange: (String?) -> Unit,
    filterName: String,
) {
  OutlinedTextField(
      value = value ?: "",
      onValueChange = { onValueChange(it) },
      label = { Text("$filterName Name") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
      trailingIcon = {
        if (value != null) {
          IconButton(onClick = { onValueChange(null) }) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear $filterName",
            )
          }
        }
      },
  )
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
        tint = colorScheme.onBackground,
        modifier = Modifier.size(96.dp).testTag(HomeScreenTestTags.NO_POST_ICON),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = LocalContext.current.getString(R.string.no_nearby_posts),
        color = colorScheme.onBackground,
        style = typography.titleLarge,
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
      verticalArrangement = Arrangement.spacedBy(2.dp),
      contentPadding = PaddingValues(vertical = 2.dp),
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
  val post = postState.post
  val author = postState.author
  val animalName = postState.animalName
  val pagerState = rememberPagerState(pageCount = { if (post.location != null) 2 else 1 })

  // -------- Optimistic Like State (instant UI) --------
  var liked by remember(post.postId) { mutableStateOf(postState.isLiked) }
  var likeCount by remember(post.postId) { mutableIntStateOf(postState.likeCount) }
  var commentCount by remember(post.postId) { mutableIntStateOf(postState.commentsCount) }

  val onToggleLike: () -> Unit = {
    liked = !liked
    likeCount = if (liked) likeCount + 1 else likeCount - 1
    onPostLike(post.postId)
  }

  Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colorScheme.background),
      modifier = Modifier.fillMaxWidth(),
  ) {
    // Header: avatar + title + date + location
    PostHeader(
        post = post,
        author = author,
        animalName = animalName,
        colorScheme = colorScheme,
        onProfilePictureClick = onProfilePictureClick)

    // Image
    PostSlider(post = post, onPostClick = { onPostClick(post.postId) }, pagerState)

    // Actions: likes & comments & location
    PostActions(
        post = post,
        liked = liked,
        likeCount = likeCount,
        commentCount = commentCount,
        onToggleLike = onToggleLike,
        onPostClick = { onPostClick(post.postId) },
        pagerState = pagerState)
  }
}

/**
 * Displays a post's header.
 *
 * @param post The post whose header is to be displayed.
 * @param author The author of the post.
 * @param animalName The name of the animal appearing on the post.
 * @param colorScheme The colorscheme to follow.
 * @param onProfilePictureClick The action when the user clicks on the profile picture of the post's
 *   author.
 */
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
      Row {
        Text(
            text = "${author.username} saw ${if (animalName.startsWithVowel()) "an " else "a "}",
            style = typography.titleMedium,
            color = colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = animalName.replaceFirstChar { it.uppercase() },
            style = typography.titleMedium,
            color = colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
          text =
              SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(post.date.toDate()),
          style = typography.labelSmall,
          color = colorScheme.onBackground,
      )
    }
    if (post.location?.name?.isNotBlank() == true) {
      Row(
          modifier = Modifier.testTag(HomeScreenTestTags.locationTag(post.postId)),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Location",
            modifier = Modifier.size(13.dp).offset(y = (-1).dp),
            tint = colorScheme.onBackground,
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = post.location.name,
            style = typography.labelMedium,
            color = colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

/**
 * Displays a post's image.
 *
 * @param post The post whose image is to be displayed.
 * @param onPostClick The action when the user clicks on the post, to see its details.
 */
@Composable
private fun PostSlider(post: Post, onPostClick: () -> Unit, pagerState: PagerState) {
  HorizontalPager(
      state = pagerState,
      modifier =
          Modifier.fillMaxWidth()
              .height(LocalWindowInfo.current.containerSize.height.dp / 6)
              .testTag(HomeScreenTestTags.sliderTag(post.postId))) { page ->
        when (page) {
          0 -> {
            AsyncImage(
                model = post.pictureURL,
                contentDescription = "Post picture",
                modifier =
                    Modifier.fillMaxSize()
                        .testTag(HomeScreenTestTags.imageTag(post.postId))
                        .clickable { onPostClick() },
                contentScale = ContentScale.Crop,
            )
          }
          1 -> {
            val loc = post.location!!
            val context = LocalContext.current
            val isDark =
                when (AppTheme.appearanceMode) {
                  AppearanceMode.DARK -> true
                  AppearanceMode.LIGHT -> false
                  AppearanceMode.AUTOMATIC -> isSystemInDarkTheme()
                }
            Box(
                modifier =
                    Modifier.fillMaxSize().testTag(HomeScreenTestTags.mapPreviewTag(post.postId))) {
                  StaticMiniMap(
                      modifier = Modifier.matchParentSize(),
                      pins = listOf(Point.fromLngLat(loc.longitude, loc.latitude)),
                      styleUri = context.getString(R.string.map_style),
                      styleImportId = context.getString(R.string.map_standard_import),
                      isDark = isDark,
                      fallbackZoom = 2.0,
                      context = context)
                  Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier =
                          Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                              .clip(RoundedCornerShape(20.dp))
                              .background(colorScheme.onBackground)
                              .padding(horizontal = 10.dp, vertical = 8.dp),
                  ) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = "Country Icon",
                        tint = colorScheme.background,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        modifier = Modifier.testTag(HomeScreenTestTags.mapLocationTag(post.postId)),
                        text = post.location.name,
                        style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.background,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                  }
                  Box(
                      modifier =
                          Modifier.matchParentSize()
                              .clickable { onPostClick() }
                              .background(Color.Transparent)
                              .testTag(HomeScreenTestTags.mapPreviewButtonTag(post.postId)))
                }
          }
        }
      }
}

@Composable
private fun SlideState(slideIndex: Int, currentPage: Int) {
  Box(
      modifier =
          Modifier.size(if (currentPage == slideIndex) 7.dp else 5.dp)
              .clip(RoundedCornerShape(50.dp))
              .background(
                  color =
                      colorScheme.onBackground.copy(
                          alpha = if (currentPage == slideIndex) 0.9f else 0.6f)))
}

/**
 * Displays a post's actions.
 *
 * @param post The post whose actions are to be displayed.
 * @param liked True if the post is liked by the user, false otherwise.
 * @param likeCount the number of likes the post has.
 * @param colorScheme The colorscheme to follow.
 * @param onToggleLike The action when the user clicks on the heart, to like it.
 * @param onPostClick The action when the user clicks on the post, to see its details.
 */
@Composable
private fun PostActions(
    post: Post,
    liked: Boolean,
    likeCount: Int,
    commentCount: Int,
    onToggleLike: () -> Unit,
    onPostClick: () -> Unit,
    pagerState: PagerState
) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    // Likes
    Row(
        modifier =
            Modifier.testTag(HomeScreenTestTags.likeButtonTag(post.postId)).clickable {
              onToggleLike()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
          imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
          contentDescription = "Likes",
          modifier = Modifier.size(30.dp),
          tint = if (liked) colorScheme.primary else colorScheme.onBackground,
      )
      Spacer(Modifier.width(6.dp))
      Text(
          text = "$likeCount",
          style = typography.bodyLarge,
          fontWeight = FontWeight.SemiBold,
          color = colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
    }

    // Page selected
    if (pagerState.pageCount > 1) {
      Box(
          modifier =
              Modifier.fillMaxHeight().testTag(HomeScreenTestTags.sliderStateTag(post.postId)),
          contentAlignment = Alignment.TopCenter) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 5.dp)) {
                  SlideState(0, pagerState.currentPage)
                  Spacer(modifier = Modifier.width(5.dp))
                  SlideState(1, pagerState.currentPage)
                }
          }
    }

    // Comments
    Row(
        modifier =
            Modifier.testTag(HomeScreenTestTags.commentTag(post.postId)).clickable {
              onPostClick()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
          imageVector = Icons.AutoMirrored.Outlined.Chat,
          contentDescription = "Comments",
          modifier = Modifier.size(25.dp),
          tint = colorScheme.onBackground,
      )
      Spacer(Modifier.width(6.dp))
      Text(
          text = "$commentCount",
          style = typography.bodyLarge,
          fontWeight = FontWeight.SemiBold,
          color = colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
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
