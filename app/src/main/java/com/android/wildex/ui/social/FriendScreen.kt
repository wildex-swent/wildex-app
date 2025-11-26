package com.android.wildex.ui.social

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CancelScheduleSend
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.R
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.utils.ClickableProfilePicture

object FriendScreenTestTags {
  const val GO_BACK_BUTTON = "friend_screen_go_back_button"
  const val FRIENDS_TAB_BUTTON = "friends_tab_button"
  const val REQUESTS_TAB_BUTTON = "requests_tab_button"
  const val SCREEN_TITLE = "screen_title"
  const val NO_FRIENDS = "no_friends"
  const val NO_FRIENDS_TEXT = "no_friends_text"
  const val NO_SUGGESTIONS = "no_suggestions"
  const val NO_SENT_REQUESTS = "no_sent_requests"
  const val NO_RECEIVED_REQUESTS = "no_received_requests"

  fun testTagForTemplate(userId: Id) = "template_$userId"

  fun testTagForProfilePicture(userId: Id) = "profile_picture_${userId}"

  fun testTagForFollowButton(userId: Id) = "follow_button_$userId"

  fun testTagForUnfollowButton(userId: Id) = "unfollow_button_$userId"

  fun testTagForCancelSentRequestButton(userId: Id) = "cancel_sent_request_${userId}_button"

  fun testTagForAcceptReceivedRequestButton(userId: Id) = "accept_request_${userId}_button"

  fun testTagForDeclineReceivedRequestButton(userId: Id) = "decline_request_${userId}_button"
}

/**
 * Entry point Composable for the Friend Screen
 *
 * @param friendScreenViewModel ViewModel exposing the state to the screen and managing user actions
 * @param userId the user whose friend list we wish to access
 * @param onProfileClick callback function to be called when the user clicks on a profile picture
 * @param onGoBack callback function to be called when the user clicks on the go back button
 */
@Composable
fun FriendScreen(
    friendScreenViewModel: FriendScreenViewModel = viewModel(),
    userId: Id = "",
    onProfileClick: (Id) -> Unit = {},
    onGoBack: () -> Unit = {}
) {
  val uiState by friendScreenViewModel.uiState.collectAsState()
  val context = LocalContext.current
  val (selectedTab, setSelectedTab) =
      remember { mutableStateOf(context.getString(R.string.friends_tab_title)) }

  LaunchedEffect(Unit) { friendScreenViewModel.loadUIState(userId) }
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      friendScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize(), topBar = { FriendScreenTopBar(onGoBack = onGoBack) }) {
          paddingValues ->
        val pullState = rememberPullToRefreshState()

        PullToRefreshBox(
            state = pullState,
            isRefreshing = uiState.isRefreshing,
            modifier = Modifier
              .padding(paddingValues)
              .fillMaxSize(),
            onRefresh = { friendScreenViewModel.refreshUIState(userId) },
        ) {
          when {
            uiState.isError -> LoadingFail()
            uiState.isLoading -> LoadingScreen()
            else -> {
              Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.isCurrentUser) {
                  CurrentUserFriendScreenContent(selectedTab, setSelectedTab, friendScreenViewModel, uiState, onProfileClick)
                } else {
                  OtherUserFriendScreenContent(friendScreenViewModel, uiState, onProfileClick)
                }
              }
            }
          }
        }
      }
}

/**
 * Top bar Composable for the Friend Screen. It is very basic and consists only of a go back button
 * and the screen's title
 *
 * @param onGoBack callback function to be called when the user clicks on the go back button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendScreenTopBar(onGoBack: () -> Unit = {}) {
  CenterAlignedTopAppBar(
      title = {
        Text(
            text = LocalContext.current.getString(R.string.friend_screen_title),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag(FriendScreenTestTags.SCREEN_TITLE))
      },
      navigationIcon = {
        IconButton(
            onClick = { onGoBack() },
            modifier = Modifier.testTag(FriendScreenTestTags.GO_BACK_BUTTON)) {
              Icon(
                  imageVector = Icons.Filled.ChevronLeft,
                  contentDescription = "Go Back",
                  modifier = Modifier.fillMaxSize(0.8f))
            }
      })
}

/**
 * Tab selection Composable for the current user when viewing their own friend screen. It allows to
 * switch from the friends tab, where the user can see his friend list as well as new friend
 * suggestions, to the requests tab where the user can manage their sent requests and received
 * requests.
 *
 * @param selectedTab the initially selected tab
 * @param onTabSelected callback function to be called when the user wants to switch tabs
 */
@Composable
fun CurrentUserSelectionTab(selectedTab: String, onTabSelected: (String) -> Unit) {
  val tabs =
      listOf(
          LocalContext.current.getString(R.string.friends_tab_title),
          LocalContext.current.getString(R.string.requests_tab_title))
  val screenHeight = LocalWindowInfo.current.containerSize.height.dp
  Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(screenHeight / 55),
  ) {
    tabs.forEach { tab ->
      Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                  .testTag(
                    if (tab == LocalContext.current.getString(R.string.friends_tab_title))
                      FriendScreenTestTags.FRIENDS_TAB_BUTTON
                    else FriendScreenTestTags.REQUESTS_TAB_BUTTON
                  )
                  .fillMaxSize()
                  .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onTabSelected(tab) })) {
              Text(
                  text = tab,
                  fontWeight = FontWeight.SemiBold,
                  color =
                      if (tab == selectedTab) colorScheme.onBackground
                      else colorScheme.onBackground.copy(alpha = 0.5f),
              )
            }
        if (tab == selectedTab) {
          HorizontalDivider(
              thickness = 1.dp,
              color = colorScheme.onBackground,
              modifier = Modifier.fillMaxWidth(0.9f))
        }
      }
    }
  }
}

/**
 * Follow button Composable to be used in the template as an interactable element. This allows the
 * current user to send a friend request to any user that is not friend with them.
 *
 * @param onFollow callback function to be called when the user wants to send a friend request
 */
@Composable
fun FollowButton(onFollow: () -> Unit = {}, testTag: String) {
  Box(
      modifier =
          Modifier
            .testTag(testTag)
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
              onClick = onFollow
            )
            .background(color = colorScheme.primary, shape = RoundedCornerShape(5.dp))) {
        Text(
            text = LocalContext.current.getString(R.string.friend_screen_send_request),
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.background,
            modifier = Modifier.padding(horizontal = 23.dp, vertical = 7.dp))
      }
}

/**
 * Unfollow button Composable to be used in the template as an interactable element. This allows the
 * current user to revoke a friendship with any user they are friends with.
 *
 * @param onUnfollow callback function to be called when the user wants to revoke a friendship
 */
@Composable
fun UnfollowButton(onUnfollow: () -> Unit = {}, testTag: String) {
  Box(
      modifier =
          Modifier
            .testTag(testTag)
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
              onClick = onUnfollow
            )
            .background(color = colorScheme.surfaceVariant, shape = RoundedCornerShape(5.dp))) {
        Text(
            text = LocalContext.current.getString(R.string.friend_screen_remove_friend),
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 23.dp, vertical = 7.dp))
      }
}

/**
 * Interactable element Composable that is to be added to the template to manage a received request.
 * It allows to accept or decline an incoming friend request.
 *
 * @param onAccept callback function to be called when the user wants to accept the friend request
 * @param onDecline callback function to be called when the user wants to decline the friend request
 */
@Composable
fun ReceivedRequestInteractable(
    onAccept: () -> Unit = {},
    onDecline: () -> Unit = {},
    testTagAccept: String,
    testTagDecline: String
) {
  Row(modifier = Modifier
    .width(80.dp)
    .height(30.dp)) {
    RequestButton(
        onClick = onAccept,
        icon = Icons.Default.Check,
        contentDescription = "Accept Friend Request",
        backgroundColor = Color.Blue,
        iconColor = colorScheme.background,
        modifier = Modifier
          .weight(1f)
          .testTag(testTagAccept))
    RequestButton(
        onClick = onDecline,
        icon = Icons.Default.Close,
        contentDescription = "Decline Friend Request",
        backgroundColor = Color.Red,
        iconColor = colorScheme.background,
        modifier = Modifier
          .weight(1f)
          .testTag(testTagDecline))
  }
}

/**
 * Interactable element Composable to be added to the template when willing to manage a sent
 * request, in the current user's own friend screen. It allows to cancel a sent request in the
 * requests tab.
 *
 * @param onCancel callback function to be called when the user wants to cancel the sent friend
 *   request
 */
@Composable
fun CurrentUserSentRequestInteractable(onCancel: () -> Unit = {}, testTag: String) {
  RequestButton(
      onClick = onCancel,
      icon = Icons.Default.Close,
      contentDescription = "Cancel Friend Request",
      backgroundColor = Color(red = 0xD8, green = 0xD3, blue = 0xD3),
      iconColor = colorScheme.onBackground,
      modifier = Modifier
        .height(30.dp)
        .width(40.dp)
        .testTag(testTag))
}

/**
 * Interactable element Composable to be added to the template when willing to manage a sent friend
 * request, in another user's friend screen. This allows the current user to cancel a friend request
 * from the friend screen of another user.
 *
 * @param onCancel callback function to be called when the user wants to cancel the sent friend
 *   request
 */
@Composable
fun OtherUserSentRequestInteractable(onCancel: () -> Unit = {}, testTag: String) {
  Box(
      modifier =
          Modifier
            .testTag(testTag)
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
              onClick = onCancel
            )
            .background(color = colorScheme.surfaceVariant, shape = RoundedCornerShape(5.dp))) {
        Text(
            text =
                LocalContext.current.getString(R.string.friend_screen_pending_request_other_user),
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 23.dp, vertical = 7.dp))
      }
}

/**
 * Auxiliary Composable used in the friend request interactable, whether it is for the sent or
 * received requests. It defines an interactable button of circular shape with an icon in the middle
 * and a background color.
 *
 * @param onClick callback function to be called when the user wants to interact with this request
 *   button
 * @param icon the icon to display in the middle of the button
 * @param contentDescription description of the button's functionality
 * @param backgroundColor background color of the button's circular background
 * @param iconColor color of the icon
 * @param modifier modifier passed from the caller to be applied to the icon button as to apply
 *   padding or any other modifier to this button
 */
@Composable
fun RequestButton(
    onClick: () -> Unit = {},
    icon: ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier
) {
  IconButton(modifier = modifier, onClick = onClick) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = iconColor,
        modifier =
            Modifier
              .background(color = backgroundColor, shape = CircleShape)
              .size(23.dp)
              .padding(2.dp))
  }
}

/**
 * Composable defining a standard template for a friend in a friend list, a suggestion or a friend
 * request. It layouts the following elements in a Row: a user's clickable profile picture, a Column
 * with the user's username and an optional subtext, and an interactable element at the end of the
 * row to manage to subject of the Composable, whether it is a friend, a suggestion or a request.
 *
 * @param viewModel ViewModel needed to interact with the interactable element
 * @param isCurrentUser true if the screen is the current user's friend screen, false otherwise. It
 *   is needed to determine what type of sent friend request interactable element to display in case
 *   the subject is a sent friend request
 * @param user subject user of the friendship, suggestion or friend request
 * @param subtext optional text to display below the user's username. It is particularly useful in
 *   the case of a suggestion to display the suggestion reason
 * @param onProfileClick callback function to be called when the current user clicks on the profile
 *   picture of the subject user
 * @param friendStatus enum object defining which interactable element to display at the end of the
 *   row, or none if the subject user of this composable is the current user
 */
@Composable
fun FriendRequestSuggestionTemplate(
    viewModel: FriendScreenViewModel,
    isCurrentUser: Boolean,
    user: SimpleUser,
    subtext: String = "",
    onProfileClick: (Id) -> Unit = {},
    friendStatus: FriendStatus
) {
  Row(
      modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 25.dp)
            .height(70.dp)
            .testTag(FriendScreenTestTags.testTagForTemplate(user.userId)),
      verticalAlignment = Alignment.CenterVertically) {
        ClickableProfilePicture(
            modifier =
                Modifier
                  .size(45.dp)
                  .testTag(FriendScreenTestTags.testTagForProfilePicture(user.userId)),
            profileId = user.userId,
            profilePictureURL = user.profilePictureURL,
            profileUserType = user.userType,
            onProfile = onProfileClick)
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .padding(end = 10.dp),
            verticalArrangement = Arrangement.Center) {
              Spacer(modifier = Modifier.weight(1f))
              Text(
                  text = user.username,
                  maxLines = 1,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 16.sp,
              )
              if (subtext.isNotEmpty()) {
                Spacer(modifier = Modifier.weight(0.4f))
                Text(
                    text = subtext,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                )
              }
              Spacer(modifier = Modifier.weight(1f))
            }
        when (friendStatus) {
          FriendStatus.FRIEND ->
              UnfollowButton(
                  onUnfollow = { viewModel.unfollowUser(user.userId) },
                  testTag = FriendScreenTestTags.testTagForUnfollowButton(user.userId))
          FriendStatus.NOT_FRIEND ->
              FollowButton(
                  onFollow = { viewModel.sendRequestToUser(user.userId) },
                  testTag = FriendScreenTestTags.testTagForFollowButton(user.userId))
          FriendStatus.PENDING_RECEIVED ->
              ReceivedRequestInteractable(
                  onAccept = { viewModel.acceptReceivedRequest(user.userId) },
                  onDecline = { viewModel.declineReceivedRequest(user.userId) },
                  testTagAccept =
                      FriendScreenTestTags.testTagForAcceptReceivedRequestButton(user.userId),
                  testTagDecline =
                      FriendScreenTestTags.testTagForDeclineReceivedRequestButton(user.userId))
          FriendStatus.PENDING_SENT ->
              if (isCurrentUser) {
                CurrentUserSentRequestInteractable(
                    onCancel = { viewModel.cancelSentRequest(user.userId) },
                    testTag = FriendScreenTestTags.testTagForCancelSentRequestButton(user.userId))
              } else
                  OtherUserSentRequestInteractable(
                      onCancel = { viewModel.cancelSentRequest(user.userId) },
                      testTag = FriendScreenTestTags.testTagForCancelSentRequestButton(user.userId))
          FriendStatus.IS_CURRENT_USER -> Unit
        }
      }
}

/**
 * Placeholder Composable to display when the user whose friend screen is displayed doesn't have any
 * friends.
 *
 * @param text text to display whether we are viewing the current user's friend screen or another
 *   user's
 */
@Composable
fun NoFriends(text: String) {
  Column(
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .height(210.dp)
        .fillMaxWidth()
        .testTag(FriendScreenTestTags.NO_FRIENDS)) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(70.dp))
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            modifier =
                Modifier
                  .fillMaxWidth(0.6f)
                  .padding(top = 10.dp)
                  .testTag(FriendScreenTestTags.NO_FRIENDS_TEXT))
      }
}

/**
 * Placeholder Composable to display when no suggestions are available to display to the current
 * user
 */
@Composable
fun NoSuggestions() {
  Column(
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
          Modifier
            .height(210.dp)
            .fillMaxWidth()
            .testTag(FriendScreenTestTags.NO_SUGGESTIONS)) {
        Icon(
            imageVector = Icons.Default.AutoFixHigh,
            contentDescription = null,
            modifier = Modifier.size(60.dp))
        Text(
            text = LocalContext.current.getString(R.string.no_suggestions),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            modifier = Modifier
              .fillMaxWidth(0.6f)
              .padding(top = 10.dp))
      }
}

/**
 * Content of the friends tab when viewing the current user's friend screen. It has two sections,
 * the friends list of the current user and a suggestion section containing suggested users
 *
 * @param viewModel viewModel needed to interact with the friends list and the suggestions
 * @param state state needed to get the friends of the current user and the available suggestions
 * @param onProfileClick callback function to be called when the current user clicks on a profile
 *   picture.
 */
@Composable
fun FriendsTabContent(
    viewModel: FriendScreenViewModel,
    state: FriendsScreenUIState,
    onProfileClick: (Id) -> Unit
) {
  val friends = state.friends
  val suggestions = state.suggestions
  LazyColumn(modifier = Modifier.fillMaxSize()) {
    item {
      Text(
          text = LocalContext.current.getString(R.string.friends_section),
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.sp,
          color = colorScheme.onBackground,
          modifier =
              Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 4.dp))
    }
    if (friends.isEmpty()) {
      item { NoFriends(LocalContext.current.getString(R.string.no_friends_current_user)) }
    } else {
      items(friends.size) { index ->
        val friendState = friends[index]
        FriendRequestSuggestionTemplate(
            viewModel = viewModel,
            isCurrentUser = state.isCurrentUser,
            user = friendState.friend,
            onProfileClick = onProfileClick,
            friendStatus = friendState.status)
      }
    }
    item {
      HorizontalDivider(modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp), thickness = 1.dp)
      Text(
          text = LocalContext.current.getString(R.string.suggestions_section),
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.sp,
          color = colorScheme.onBackground,
          modifier =
              Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 4.dp))
    }
    if (suggestions.isEmpty()) {
      item { NoSuggestions() }
    } else {
      items(suggestions.size) { index ->
        val suggestion = suggestions[index]
        FriendRequestSuggestionTemplate(
            viewModel = viewModel,
            isCurrentUser = state.isCurrentUser,
            user = suggestion.user,
            subtext = suggestion.reason,
            onProfileClick = onProfileClick,
            friendStatus = FriendStatus.NOT_FRIEND)
      }
    }
  }
}

/**
 * Placeholder Composable to be displayed when viewing the current user's friend screen if they have
 * no sent friend requests.
 */
@Composable
fun NoSentRequests() {
  Column(
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
          Modifier
            .height(210.dp)
            .fillMaxWidth()
            .testTag(FriendScreenTestTags.NO_SENT_REQUESTS)) {
        Icon(
            imageVector = Icons.Default.CancelScheduleSend,
            contentDescription = null,
            modifier = Modifier.size(60.dp))
        Text(
            text = LocalContext.current.getString(R.string.no_sent_requests),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            modifier = Modifier
              .fillMaxWidth(0.6f)
              .padding(top = 10.dp))
      }
}

/**
 * Placeholder Composable to be displayed when viewing the current user's friend screen if they have
 * no incoming friend requests
 */
@Composable
fun NoReceivedRequests() {
  Column(
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
          Modifier
            .height(210.dp)
            .fillMaxWidth()
            .testTag(FriendScreenTestTags.NO_RECEIVED_REQUESTS)) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            modifier = Modifier.size(60.dp))
        Text(
            text = LocalContext.current.getString(R.string.no_received_requests),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            modifier = Modifier
              .fillMaxWidth(0.6f)
              .padding(top = 10.dp))
      }
}

/**
 * Content of the requests tab when viewing the current user's friend screen. It has two sections, a
 * received friend requests section where the current user can view and manage their incoming friend
 * requests, and a sent friend requests section where the current user can view and manage their
 * sent friend requests.
 *
 * @param viewModel viewModel needed to interact with the friend requests
 * @param state state needed to get the received and sent friend requests of the current user
 * @param onProfileClick callback function to be called when the current user clicks on a profile
 *   picture
 */
@Composable
fun RequestsTabContent(
    viewModel: FriendScreenViewModel,
    state: FriendsScreenUIState,
    onProfileClick: (Id) -> Unit
) {
  val receivedRequests = state.receivedRequests
  val sentRequests = state.sentRequests
  LazyColumn(modifier = Modifier.fillMaxSize()) {
    item {
      Text(
          text = LocalContext.current.getString(R.string.received_requests_section),
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.sp,
          color = colorScheme.onBackground,
          modifier =
              Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 4.dp))
    }
    if (receivedRequests.isEmpty()) {
      item { NoReceivedRequests() }
    } else {
      items(receivedRequests.size) { index ->
        val receivedRequest = receivedRequests[index]
        FriendRequestSuggestionTemplate(
            viewModel = viewModel,
            isCurrentUser = state.isCurrentUser,
            user = receivedRequest.user,
            onProfileClick = onProfileClick,
            friendStatus = FriendStatus.PENDING_RECEIVED)
      }
    }
    item {
      HorizontalDivider(modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp), thickness = 1.dp)
      Text(
          text = LocalContext.current.getString(R.string.sent_requests_section),
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.sp,
          color = colorScheme.onBackground,
          modifier =
              Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 4.dp))
    }
    if (sentRequests.isEmpty()) {
      item { NoSentRequests() }
    } else {
      items(sentRequests.size) { index ->
        val sentRequest = sentRequests[index]
        FriendRequestSuggestionTemplate(
            viewModel = viewModel,
            isCurrentUser = state.isCurrentUser,
            user = sentRequest.user,
            onProfileClick = onProfileClick,
            friendStatus = FriendStatus.PENDING_SENT)
      }
    }
  }
}

@Composable
fun CurrentUserFriendScreenContent(
  selectedTab: String,
  setSelectedTab: (String) -> Unit,
  friendScreenViewModel: FriendScreenViewModel,
  state: FriendsScreenUIState,
  onProfileClick: (Id) -> Unit
){
  CurrentUserSelectionTab(selectedTab = selectedTab, onTabSelected = setSelectedTab)
  if (selectedTab == LocalContext.current.getString(R.string.friends_tab_title)) {
    FriendsTabContent(friendScreenViewModel, state, onProfileClick)
  } else {
    RequestsTabContent(friendScreenViewModel, state, onProfileClick)
  }
}

/**
 * Content of the friend screen when viewing another user's friend screen. It has only one section:
 * the friend list, in which the current user can interact freely with users. If any user in the
 * list has sent a friend request to the current user, they can directly accept or decline it from
 * there. If the current user sent a friend request to a user in the list, they can see it and
 * cancel it directly from this screen.
 *
 * @param viewModel viewModel needed to interact with the friendships/requests with users in the
 *   friend list
 * @param state state needed to get the friends of the screen's user as well as their respective
 *   status relative to the current user
 * @param onProfileClick callback function to be called when the current user clicks on a profile
 *   picture
 */
@Composable
fun OtherUserFriendScreenContent(
    viewModel: FriendScreenViewModel,
    state: FriendsScreenUIState,
    onProfileClick: (Id) -> Unit
) {
  val friends = state.friends
  LazyColumn(modifier = Modifier.fillMaxSize()) {
    item {
      Text(
          text = LocalContext.current.getString(R.string.friends_section),
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.sp,
          color = colorScheme.onBackground,
          modifier =
              Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 4.dp))
    }
    if (friends.isEmpty()) {
      item { NoFriends(LocalContext.current.getString(R.string.no_friends_other_user)) }
    } else {
      items(friends.size) { index ->
        val friendState = friends[index]
        FriendRequestSuggestionTemplate(
            viewModel = viewModel,
            isCurrentUser = state.isCurrentUser,
            user = friendState.friend,
            onProfileClick = onProfileClick,
            friendStatus = friendState.status)
      }
    }
  }
}
