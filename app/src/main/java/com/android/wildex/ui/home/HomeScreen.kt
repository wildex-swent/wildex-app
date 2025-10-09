package com.android.wildex.ui.home

import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.home.HomeScreenTestTags.NOTIFICATION_BELL
import com.android.wildex.ui.home.HomeScreenTestTags.NO_POST
import com.android.wildex.ui.home.HomeScreenTestTags.POST_AUTHOR_PICTURE
import com.android.wildex.ui.home.HomeScreenTestTags.POST_COMMENT
import com.android.wildex.ui.home.HomeScreenTestTags.POST_LIKE
import com.android.wildex.ui.home.HomeScreenTestTags.POST_MORE_INFO
import com.android.wildex.ui.home.HomeScreenTestTags.PROFILE_PICTURE
import com.android.wildex.ui.theme.WildexTheme
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

// TODO: Remove those hardcoded values
val Crimson = Color(112, 38, 50)
val WildexGreen = Color(0xFF082C0B)
val user =
    User(
        userId = "<user>",
        username = "<username>",
        name = "<name>",
        surname = "<surname>",
        bio = "<bio>",
        profilePictureURL =
            "https://www.shutterstock.com/image-photo/" +
                "handsome-happy-african-american-bearded-600nw-2460702995.jpg",
        userType = UserType.REGULAR,
        creationDate = Timestamp.now(),
        country = "<country>",
        friendsCount = 0,
        animalsId = emptyList(),
        animalsCount = 0,
        achievementsId = emptyList(),
        achievementsCount = 0,
    )
val mockPost =
    Post(
        postId = "<post>",
        authorId = "<name>",
        pictureURL =
            "https://hips.hearstapps.com/hmg-prod/images/" +
                "cute-baby-animals-1558535060.jpg?crop=0.752xw:1.00xh;0.125xw,0&resize=640:*",
        location = Location(0.0, 0.0),
        date = Timestamp.now(),
        animalId = "<animal>",
        likesCount = 0,
        commentsCount = 0)
val postAuthor =
    User(
        userId = "",
        username = "<username>",
        name = "<name>",
        surname = "<surname>",
        bio = "<bio>",
        profilePictureURL =
            "https://paulhollandphotography.com/cdn/shop/articles/4713_Individual_" +
                "Outdoor_f930382f-c9d6-4e5b-b17d-9fe300ae169c.jpg?v=1743534144&width=1500",
        userType = UserType.REGULAR,
        creationDate = Timestamp.now(),
        country = "<country>",
        friendsCount = 0,
        animalsId = emptyList(),
        animalsCount = 0,
        achievementsId = emptyList(),
        achievementsCount = 0,
    )
val animal =
    Animal(
        animalId = "<animal>",
        pictureURL = "https://cdn.britannica.com/16/234216-050-C66F8665/beagle-hound-dog.jpg",
        name = "<name>",
        species = "<species>",
        description = "<description>",
    )

fun createMockPosts(size: Int): List<Post> {
  return List(size) { mockPost }
}
// TODO: End

object HomeScreenTestTags {
  const val NO_POST = "HomeScreenNoPost"
  const val NOTIFICATION_BELL = "HomeScreenNotificationBell"
  const val PROFILE_PICTURE = "HomeScreenProfilePicture"
  const val POST_AUTHOR_PICTURE = "HomeScreenPostAuthorPicture"
  const val POST_MORE_INFO = "HomeScreenPostMoreInfo"
  const val POST_LIKE = "HomeScreenPostLike"
  const val POST_COMMENT = "HomeScreenPostComment"
}

@Composable
fun HomeScreen(size: Int) {
  // TODO: import user
  // TODO: import posts
  val posts = createMockPosts(size)

  Scaffold(
      topBar = { WildexTopAppBar() },
      bottomBar = { /* TODO: BottomAppBar */},
      content = { pd ->
        if (posts.isEmpty()) {
          NoPostsView()
        } else {
          PostsView(posts = posts, pd = pd)
        }
      })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WildexTopAppBar() {
  TopAppBar(
      title = {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Text(
              text = "Wildex",
              style =
                  MaterialTheme.typography.titleLarge.copy(
                      fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 30.sp))
        }
      },
      modifier = Modifier.border(1.dp, WildexGreen).shadow(5.dp),
      navigationIcon = {
        IconButton(
            onClick = { /* TODO: Navigate to Notifications */},
            modifier = Modifier.testTag(NOTIFICATION_BELL)) {
              Icon(
                  painter = painterResource(R.drawable.notification_bell),
                  contentDescription = "Notifications",
                  modifier = Modifier.size(30.dp))
            }
      },
      actions = {
        IconButton(
            onClick = { /* TODO: Navigate to Profile */},
            modifier = Modifier.testTag(PROFILE_PICTURE)) {
              AsyncImage(
                  model = user.profilePictureURL,
                  contentDescription = "Profile picture",
                  modifier =
                      Modifier.size(40.dp).clip(CircleShape).border(1.dp, WildexGreen, CircleShape),
                  contentScale = ContentScale.Crop)
            }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              titleContentColor = WildexGreen, navigationIconContentColor = WildexGreen))
}

@Composable
fun NoPostsView() {
  Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Icon(
            painter = painterResource(R.drawable.nothing_found),
            contentDescription = "Nothing Found",
            tint = WildexGreen,
            modifier = Modifier.size(100.dp).testTag(NO_POST))
        Text(
            text = "No nearby posts.\n Start posting...",
            color = WildexGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp)
      }
}

@Composable
fun PostsView(posts: List<Post>, pd: PaddingValues) {
  LazyColumn(
      modifier = Modifier.fillMaxSize().padding(pd),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Top) {
        items(posts.size) { index ->
          // TODO: import postAuthor
          PostItem(post = posts[index])
        }
      }
}

@Composable
fun PostItem(post: Post) {
  Card(
      modifier = Modifier.padding(15.dp),
      shape = RoundedCornerShape(15.dp),
      colors =
          CardColors(
              containerColor = Color.White,
              contentColor = WildexGreen,
              disabledContainerColor = WildexGreen,
              disabledContentColor = WildexGreen),
      elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)) {
        Column {
          PostHeader(post)
          PostImage(post)
          PostActions(post)
        }
      }
}

@Composable
fun PostHeader(post: Post) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(15.dp),
      verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = postAuthor.profilePictureURL,
            contentDescription = "Author profile picture",
            modifier = Modifier.size(50.dp).clip(CircleShape).testTag(POST_AUTHOR_PICTURE))

        Spacer(modifier = Modifier.width(15.dp))

        Column {
          Text(
              text = "${postAuthor.name} saw ${animal.name}",
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp)
          Text(
              text =
                  SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                      .format(post.date.toDate()),
              fontWeight = FontWeight.SemiBold,
              fontSize = 15.sp,
              color = Crimson)
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = { /* TODO: Navigate to Post Details */},
            modifier = Modifier.testTag(POST_MORE_INFO)) {
              Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = "More Info",
                  tint = Crimson,
                  modifier = Modifier.size(40.dp))
            }
      }
}

@Composable
fun PostImage(post: Post) {
  AsyncImage(
      model = post.pictureURL,
      contentDescription = "Post picture",
      modifier =
          Modifier.fillMaxWidth()
              .height(200.dp)
              .clip(RoundedCornerShape(15.dp))
              .padding(horizontal = 15.dp),
      contentScale = ContentScale.Crop)
}

@Composable
fun PostActions(post: Post) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(15.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = { /* TODO: Like */}, modifier = Modifier.testTag(POST_LIKE)) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Likes",
                modifier = Modifier.size(30.dp),
                tint = Crimson)
          }
          Text(
              text = "${post.likesCount} likes",
              fontWeight = FontWeight.SemiBold,
              fontSize = 20.sp,
              color = Crimson)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = { /* TODO: Comments */}, modifier = Modifier.testTag(POST_COMMENT)) {
            Icon(
                painter = painterResource(R.drawable.comment_icon),
                contentDescription = "Comments",
                modifier = Modifier.size(30.dp),
                tint = Crimson)
          }
          Text(
              text = "${post.commentsCount} comments",
              fontWeight = FontWeight.SemiBold,
              fontSize = 20.sp,
              color = Crimson)
        }
      }
}

@Preview
@Composable
fun HomeScreenPreview() {
  WildexTheme { Surface(modifier = Modifier.fillMaxSize()) { HomeScreen(3) } }
}
