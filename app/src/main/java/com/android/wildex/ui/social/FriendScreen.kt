package com.android.wildex.ui.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
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
import com.google.firebase.Timestamp
import com.mapbox.maps.extension.style.expressions.dsl.generated.color
import com.mapbox.maps.extension.style.expressions.dsl.generated.mod

@Composable
fun FriendScreen (
  userId: Id = "",
  onProfileClick: (Id) -> Unit = {},
  onGoBack: () -> Unit = {}
){
  val (selectedTab, setSelectedTab) = remember { mutableStateOf("Friends") }
  val user = User(
    userId = "userId",
    username = "johndoe",
    name = "John",
    surname = "Doe",
    bio = "",
    profilePictureURL = "https://example.com/profile.jpg",
    userType = UserType.REGULAR,
    creationDate = Timestamp.now(),
    country = "",
    friendsCount = 0,
  )

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = { FriendScreenTopBar(onGoBack = onGoBack) }
  ) { paddingValues ->
    Column (
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ){
      CurrentUserSelectionTab(
        selectedTab = selectedTab,
        onTabSelected = setSelectedTab
      )
      if (selectedTab == "Friends"){
        FriendsTabContent(
          user = user
        )
      } else {
        RequestsTabContent(user = user)
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
          modifier = Modifier.fillMaxSize().clickable(
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
      ).background(
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
      text = "Follow",
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
      ).background(
        color = colorScheme.surfaceVariant,
        shape = RoundedCornerShape(5.dp)
      )
  ){
    Text(
      text = "Unfollow",
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
    modifier = Modifier.width(80.dp).height(30.dp)
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
fun SentRequestInteractable(
  onCancel: () -> Unit = {}
){
  RequestButton(
    onClick = onCancel,
    icon = Icons.Default.Close,
    contentDescription = "Cancel Friend Request",
    backgroundColor = Color(red = 0xD8, green = 0xD3, blue = 0xD3),
    iconColor = colorScheme.onBackground,
    modifier = Modifier.height(30.dp).width(40.dp)
  )
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
  user: User,
  subtext: String,
  onProfileClick: (Id) -> Unit = {},
  interactableElement: @Composable () -> Unit = {}
){
  Row (
    modifier = Modifier.fillMaxWidth().padding(horizontal = 25.dp).height(70.dp),
    verticalAlignment = Alignment.CenterVertically
  ){
    IconButton(
      onClick = { onProfileClick(user.userId) },
      modifier = Modifier.size(45.dp)
    ) {
      AsyncImage(
        model = user.profilePictureURL,
        contentDescription = "Profile picture",
        modifier =
          Modifier.clip(CircleShape),
        contentScale = ContentScale.Crop,
      )
    }
    Spacer(modifier = Modifier.width(10.dp))
    Column (
      modifier = Modifier.weight(1f).fillMaxHeight(),
      verticalArrangement = Arrangement.Center
    ){
      Spacer(modifier = Modifier.weight(1f))
      Text(
        text = user.name + " " + user.surname,
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
    interactableElement()
  }
}

@Composable
fun FriendsTabContent(
  user: User
){
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
    item {
      FriendRequestSuggestionTemplate(
        user = user,
        subtext = user.username,
        interactableElement = {
          UnfollowButton()
        }
      )
    }
    item {
      FriendRequestSuggestionTemplate(
        user = user,
        subtext = user.username,
        interactableElement = {
          UnfollowButton()
        }
      )
    }
    item {
      FriendRequestSuggestionTemplate(
        user = user,
        subtext = user.username,
        interactableElement = {
          UnfollowButton()
        }
      )
    }
    item {
      HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), thickness = 1.dp)
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
    item {
      FriendRequestSuggestionTemplate(
        user = user,
        subtext = "posted 4 times near you",
        interactableElement = {
          FollowButton()
        }
      )
    }
    item {
      FriendRequestSuggestionTemplate(
        user = user,
        subtext = "shares 3 common friend with you",
        interactableElement = {
          FollowButton()
        }
      )
    }
    item {
      FriendRequestSuggestionTemplate(
        user = user,
        subtext = "is popular in Argentina",
        interactableElement = {
          FollowButton()
        }
      )
    }
    item {
      FriendRequestSuggestionTemplate(
        user = user,
        subtext = "shares 1 common friend with you",
        interactableElement = {
          FollowButton()
        }
      )
    }
  }
}

@Composable
fun RequestsTabContent(
  user: User
){
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
    item {
      FriendRequestSuggestionTemplate(
        user = user,
        subtext = user.username,
        interactableElement = {
          ReceivedRequestInteractable {  }
        }
      )
    }
    item {
      FriendRequestSuggestionTemplate(
        user = user,
        subtext = user.username,
        interactableElement = {
          ReceivedRequestInteractable {  }
        }
      )
    }
    item {
      FriendRequestSuggestionTemplate(
        user = user,
        subtext = user.username,
        interactableElement = {
          ReceivedRequestInteractable {  }
        }
      )
    }
    item {
      HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), thickness = 1.dp)
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
    item {
      FriendRequestSuggestionTemplate(
        user = user,
        subtext = user.username,
        interactableElement = {
          SentRequestInteractable {  }
        }
      )
    }
    item {
      FriendRequestSuggestionTemplate(
        user = user,
        subtext = user.username,
        interactableElement = {
          SentRequestInteractable {  }
        }
      )
    }
    item {
      FriendRequestSuggestionTemplate(
        user = user,
        subtext = user.username,
        interactableElement = {
          SentRequestInteractable {  }
        }
      )
    }
  }
}

@Preview
@Composable
fun FriendScreenPreview() {
  FriendScreen()
}
