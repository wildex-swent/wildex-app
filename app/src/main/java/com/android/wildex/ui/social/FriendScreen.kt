package com.android.wildex.ui.social

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.LinearGradient
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.home.HomeScreenTestTags.PROFILE_PICTURE
import com.android.wildex.ui.utils.ClickableProfilePicture
import com.google.firebase.Timestamp
import com.mapbox.maps.extension.style.expressions.dsl.generated.color
import com.mapbox.maps.extension.style.expressions.dsl.generated.mod

@Composable
fun FriendScreen (
  friendScreenViewModel: FriendScreenViewModel,
  userId: Id = "",
  onProfileClick: (Id) -> Unit = {},
  onGoBack: () -> Unit = {}
){
  val uiState by friendScreenViewModel.uiState.collectAsState()
  val context = LocalContext.current
  val (selectedTab, setSelectedTab) = remember { mutableStateOf("Friends") }

  LaunchedEffect(Unit) { friendScreenViewModel.loadUIState(userId) }
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      friendScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = { FriendScreenTopBar(onGoBack = onGoBack) }
  ) { paddingValues ->
    Column (
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ){
      if (uiState.isCurrentUser){
        CurrentUserSelectionTab(
          selectedTab = selectedTab,
          onTabSelected = setSelectedTab
        )
        if (selectedTab == "Friends"){
          FriendsTabContent(
            friendScreenViewModel,
            uiState,
            onProfileClick
          )
        } else {
          RequestsTabContent(
            friendScreenViewModel,
            uiState,
            onProfileClick
          )
        }
      } else {
        OtherUserFriendScreenContent(
          friendScreenViewModel,
          uiState,
          onProfileClick
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendScreenTopBar(
  onGoBack: () -> Unit = {}
){
  CenterAlignedTopAppBar(
    title = { Text(text = "Social", fontWeight = FontWeight.SemiBold) },
    navigationIcon = {
      IconButton(
        onClick = { onGoBack() },
      ) {
        Icon(
          imageVector = Icons.Filled.ChevronLeft,
          contentDescription = "Go Back",
          modifier = Modifier.fillMaxSize(0.8f)
        )
      }
    }
  )
}

@Composable
fun CurrentUserSelectionTab(
  selectedTab: String,
  onTabSelected: (String) -> Unit
){
  val tabs = listOf("Friends", "Requests")
  val screenHeight = LocalConfiguration.current.screenHeightDp.dp
  Row (
    modifier = Modifier
      .fillMaxWidth()
      .height(screenHeight / 24),
  ){
    tabs.forEach { tab ->
      Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .fillMaxSize()
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
              onClick = { onTabSelected(tab) }
            )
        ) {
          Text(
            text = tab,
            fontWeight = FontWeight.SemiBold,
            color = if (tab == selectedTab) colorScheme.onBackground else colorScheme.onBackground.copy(alpha = 0.5f),
          )
        }
        if (tab == selectedTab){
          HorizontalDivider(
            thickness = 1.dp,
            color = colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(0.9f)
          )
        }
      }
    }
  }
}

@Composable
fun FollowButton(
  onFollow: () -> Unit = {},
){
  Box(
    modifier = Modifier
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onFollow
      )
      .background(
        brush = Brush.linearGradient(
          colors = listOf(
            Color(red = 0xF5, green = 0, blue = 0x21, alpha = 255),
            Color(red = 0x26, green = 0, blue = 0xFE, alpha = 255)
          )
        ),
        shape = RoundedCornerShape(5.dp)
      )
  ){
    Text(
      text = "Add friend",
      fontWeight = FontWeight.SemiBold,
      color = colorScheme.background,
      modifier = Modifier.padding(horizontal = 23.dp, vertical = 7.dp)
    )
  }
}

@Composable
fun UnfollowButton(
  onUnfollow: () -> Unit = {},
){
  Box(
    modifier = Modifier
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onUnfollow
      )
      .background(
        color = colorScheme.surfaceVariant,
        shape = RoundedCornerShape(5.dp)
      )
  ){
    Text(
      text = "Delete",
      fontWeight = FontWeight.SemiBold,
      color = colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 23.dp, vertical = 7.dp)
    )
  }
}

@Composable
fun ReceivedRequestInteractable(
  onAccept: () -> Unit = {},
  onDecline: () -> Unit = {}
){
  Row(
    modifier = Modifier
      .width(80.dp)
      .height(30.dp)
  ){
    RequestButton(
      onClick = onAccept,
      icon = Icons.Default.Check,
      contentDescription = "Accept Friend Request",
      backgroundColor = Color(red = 36, green = 88, blue = 246),
      iconColor = colorScheme.background,
      modifier = Modifier.weight(1f)
    )
    RequestButton(
      onClick = onDecline,
      icon = Icons.Default.Close,
      contentDescription = "Decline Friend Request",
      backgroundColor = Color(red = 202, green = 69, blue = 62),
      iconColor = colorScheme.background,
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
fun CurrentUserSentRequestInteractable(
  onCancel: () -> Unit = {}
){
  RequestButton(
    onClick = onCancel,
    icon = Icons.Default.Close,
    contentDescription = "Cancel Friend Request",
    backgroundColor = Color(red = 0xD8, green = 0xD3, blue = 0xD3),
    iconColor = colorScheme.onBackground,
    modifier = Modifier
      .height(30.dp)
      .width(40.dp)
  )
}

@Composable
fun OtherUserSentRequestInteractable(
  onCancel: () -> Unit = {}
){
  Box(
    modifier = Modifier
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onCancel
      )
      .background(
        color = colorScheme.surfaceVariant,
        shape = RoundedCornerShape(5.dp)
      )
  ){
    Text(
      text = "Pending...",
      fontWeight = FontWeight.SemiBold,
      color = colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 23.dp, vertical = 7.dp)
    )
  }
}

@Composable
fun RequestButton(
  onClick : () -> Unit = {},
  icon : ImageVector,
  contentDescription: String,
  backgroundColor: Color,
  iconColor: Color,
  modifier: Modifier
){
  IconButton(
    modifier = modifier,
    onClick = onClick
  ){
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = iconColor,
      modifier = Modifier
        .background(color = backgroundColor, shape = CircleShape)
        .size(23.dp)
        .padding(2.dp)
    )
  }
}

@Composable
fun FriendRequestSuggestionTemplate(
  viewModel: FriendScreenViewModel,
  state: FriendsScreenUIState,
  user: SimpleUser,
  subtext: String,
  onProfileClick: (Id) -> Unit = {},
  friendStatus: FriendStatus
){
  Row (
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 25.dp)
      .height(70.dp),
    verticalAlignment = Alignment.CenterVertically
  ){
    ClickableProfilePicture(
      modifier = Modifier.size(45.dp),
      profileId = user.userId,
      profilePictureURL = user.profilePictureURL,
      profileUserType = user.userType,
      onProfile = onProfileClick
    )
    Spacer(modifier = Modifier.width(10.dp))
    Column (
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight(),
      verticalArrangement = Arrangement.Center
    ){
      Spacer(modifier = Modifier.weight(1f))
      Text(
        text = user.username,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        modifier = Modifier
      )
      Spacer(modifier = Modifier.weight(0.4f))
      Text(
        text = subtext,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        modifier = Modifier
      )
      Spacer(modifier = Modifier.weight(1f))
    }
    when (friendStatus) {
      FriendStatus.FRIEND -> UnfollowButton(onUnfollow = {viewModel.unfollowUser(user.userId)})
      FriendStatus.NOT_FRIEND -> FollowButton(onFollow = {viewModel.sendRequestToUser(user.userId)})
      FriendStatus.PENDING_RECEIVED -> ReceivedRequestInteractable(
        onAccept = {viewModel.acceptReceivedRequest(user.userId)},
        onDecline = {viewModel.declineReceivedRequest(user.userId)}
      )
      FriendStatus.PENDING_SENT -> if (state.isCurrentUser){
        CurrentUserSentRequestInteractable(onCancel = {viewModel.cancelSentRequest(user.userId)})
      } else OtherUserSentRequestInteractable(onCancel = {viewModel.cancelSentRequest(user.userId)})
      FriendStatus.IS_CURRENT_USER -> {}
    }
  }
}

@Composable
fun NoFriends(
  text: String
){
  Column (
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .height(210.dp)
      .fillMaxWidth()
  ){
    Icon(imageVector = Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(70.dp))
    Text(
      text = text,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
      fontSize = 12.sp,
      modifier = Modifier
        .fillMaxWidth(0.6f)
        .padding(top = 10.dp)
    )
  }
}

@Composable
fun NoSuggestions(){
  Column (
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .height(210.dp)
      .fillMaxWidth()
  ){
    Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(60.dp))
    Text(
      text = "We have no suggestions for you at the moment...",
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
      fontSize = 12.sp,
      modifier = Modifier
        .fillMaxWidth(0.6f)
        .padding(top = 10.dp)
    )
  }
}

@Composable
fun FriendsTabContent(
  viewModel: FriendScreenViewModel,
  state: FriendsScreenUIState,
  onProfileClick: (Id) -> Unit
){
  val friends = state.friends
  val suggestions = state.suggestions
  LazyColumn (
    modifier = Modifier.fillMaxSize()
  ){
    item {
      Text(
        text = "Friends",
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        color = colorScheme.onBackground,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(top = 12.dp, bottom = 4.dp)
      )
    }
    if (friends.isEmpty()){
      item {
        NoFriends("You don't have any friends yet. Look at our suggestions and discover new people!")
      }
    } else {
      items(friends.size) {index ->
        val friendState = friends[index]
        FriendRequestSuggestionTemplate(
          viewModel,
          state,
          friendState.friend,
          "",
          onProfileClick,
          friendState.status
        )
      }
    }
    item {
      HorizontalDivider(modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp), thickness = 1.dp)
      Text(
        text = "Suggestions",
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        color = colorScheme.onBackground,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(top = 12.dp, bottom = 4.dp)
      )
    }
    if (suggestions.isEmpty()){
      item {
        NoSuggestions()
      }
    } else {
      items(suggestions.size){ index ->
        val suggestion = suggestions[index]
        FriendRequestSuggestionTemplate(
          viewModel,
          state,
          suggestion.user,
          suggestion.reason,
          onProfileClick,
          FriendStatus.NOT_FRIEND
        )
      }
    }
  }
}

@Composable
fun NoSentRequests(){
  Column (
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .height(210.dp)
      .fillMaxWidth()
  ){
    Icon(imageVector = Icons.Default.CancelScheduleSend, contentDescription = null, modifier = Modifier.size(60.dp))
    Text(
      text = "You haven't sent any friend request yet. Look at our suggestions to discover new people!",
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
      fontSize = 12.sp,
      modifier = Modifier
        .fillMaxWidth(0.6f)
        .padding(top = 10.dp)
    )
  }
}

@Composable
fun NoReceivedRequests(){
  Column (
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .height(210.dp)
      .fillMaxWidth()
  ){
    Icon(imageVector = Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(60.dp))
    Text(
      text = "You have received no friend requests yet. Post to make your profile more attractive!",
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
      fontSize = 12.sp,
      modifier = Modifier
        .fillMaxWidth(0.6f)
        .padding(top = 10.dp)
    )
  }
}

@Composable
fun RequestsTabContent(
  viewModel: FriendScreenViewModel,
  state: FriendsScreenUIState,
  onProfileClick: (Id) -> Unit
){
  val receivedRequests = state.receivedRequests
  val sentRequests = state.sentRequests
  LazyColumn (
    modifier = Modifier.fillMaxSize()
  ){
    item {
      Text(
        text = "Received",
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        color = colorScheme.onBackground,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(top = 12.dp, bottom = 4.dp)
      )
    }
    if (receivedRequests.isEmpty()){
      item {
        NoReceivedRequests()
      }
    } else {
      items(receivedRequests.size){ index ->
        val receivedRequest = receivedRequests[index]
        FriendRequestSuggestionTemplate(
          viewModel = viewModel,
          state = state,
          user = receivedRequest.user,
          subtext = "",
          onProfileClick = onProfileClick,
          friendStatus = FriendStatus.PENDING_RECEIVED
        )
      }
    }
    item {
      HorizontalDivider(modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp), thickness = 1.dp)
      Text(
        text = "Sent",
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        color = colorScheme.onBackground,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(top = 12.dp, bottom = 4.dp)
      )
    }
    if (sentRequests.isEmpty()){
      item {
        NoSentRequests()
      }
    } else {
      items(sentRequests.size){ index ->
        val sentRequest = sentRequests[index]
        FriendRequestSuggestionTemplate(
          viewModel = viewModel,
          state = state,
          user = sentRequest.user,
          subtext = "",
          onProfileClick = onProfileClick,
          friendStatus = FriendStatus.PENDING_SENT
        )
      }
    }
  }
}

@Composable
fun OtherUserFriendScreenContent(
  viewModel: FriendScreenViewModel,
  state: FriendsScreenUIState,
  onProfileClick: (Id) -> Unit
){
  val friends = state.friends
  LazyColumn(
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Text(
        text = "Friends",
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        color = colorScheme.onBackground,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(top = 12.dp, bottom = 4.dp)
      )
    }
    if (friends.isEmpty()){
      item {
        NoFriends("This user has no friends... Ask them to become your friend!")
      }
    } else {
      items(friends.size) {index ->
        val friendState = friends[index]
        FriendRequestSuggestionTemplate(
          viewModel,
          state,
          friendState.friend,
          "",
          onProfileClick,
          friendState.status
        )
      }
    }
  }
}

@Preview
@Composable
fun FriendScreenPreview() {
  //FriendScreen()
}
