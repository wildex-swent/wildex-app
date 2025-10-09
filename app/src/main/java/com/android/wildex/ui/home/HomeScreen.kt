package com.android.wildex.ui.home

import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.social.Post
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.theme.WildexTheme
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

// TODO: Remove those hardcoded values
val Crimson = Color(112, 38, 50)
val post = Post("1", "Alice", "", Location(.0, .0), Timestamp.now(), "a Dog", 12, 3)
val posts: MutableList<Post> = mutableListOf()
val messageTextStyle = TextStyle(color = Crimson, fontWeight = FontWeight.Bold, fontSize = 25.sp)
val profilePicture =
    "https://www.shutterstock.com/image-photo/" +
        "handsome-happy-african-american-bearded-600nw-2460702995.jpg"
val authorPicture =
    "https://paulhollandphotography.com/cdn/shop/articles/" +
        "4713_Individual_Outdoor_f930382f-c9d6-4e5b-b17d-9fe300ae169c.jpg?v=1743534144&width=1500"
val picture =
    "https://hips.hearstapps.com/hmg-prod/images/" +
        "cute-baby-animals-1558535060.jpg?crop=0.752xw:1.00xh;0.125xw,0&resize=640:*"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
  // TODO: Remove when viewModel implemented
  for (i in 1..5) {
    posts.add(post)
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Wildex",
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
              }
            },
            navigationIcon = {
              IconButton(onClick = { /*TODO: Implement Notifications Screen*/}) {
                Icon(
                    painter = painterResource(R.drawable.notification_bell),
                    contentDescription = "Notifications",
                    modifier = Modifier.size(30.dp),
                )
              }
            },
            actions = {
              IconButton(onClick = { /*TODO: Implement Profile Screen*/}) {
                AsyncImage(
                    model = profilePicture,
                    contentDescription = "Profile picture",
                    modifier =
                        Modifier.size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop)
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF082C0B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White))
      },
      bottomBar = {
        // TODO: BottomAppBar() when implementing navigation
      },
      content = { pd ->
        if (posts.isEmpty()) {
          Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
              modifier = Modifier.fillMaxSize().padding(pd)) {
                Icon(
                    painter = painterResource(R.drawable.nothing_found),
                    contentDescription = "My custom icon",
                    tint = Crimson,
                    modifier = Modifier.size(100.dp))
                Text(text = "No nearby posts.", style = messageTextStyle)
                Text(text = "Start posting...", style = messageTextStyle)
              }
        } else {
          // TODO: Posts
          LazyColumn(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Top,
              modifier = Modifier.fillMaxSize().padding(pd)) {
                items(posts.size) { index -> PostItem(posts[index]) }
              }
        }
      })
}

@Composable
fun PostItem(post: Post) {
  Card(
      modifier = Modifier.padding(15.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
      shape = RoundedCornerShape(15.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
          Row(
              modifier = Modifier.fillMaxWidth().padding(15.dp),
              verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { /*TODO: Implement Profile Screen*/}) {
                  AsyncImage(
                      model = authorPicture,
                      contentDescription = "Author profile picture",
                      modifier = Modifier.size(50.dp).clip(CircleShape))
                }

                Spacer(modifier = Modifier.width(15.dp))

                Column {
                  Text(
                      text = "${post.authorId} saw ${post.animalId}",
                      fontWeight = FontWeight.Bold,
                      fontSize = 20.sp)
                  Text(
                      text =
                          SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                              .format(post.date.toDate()),
                      fontSize = 15.sp,
                      color = Crimson)
                }

                Spacer(modifier = Modifier.width(15.dp))

                IconButton(onClick = { /*TODO: Implement Post Screen*/}) {
                  Icon(
                      imageVector = Icons.Default.ArrowForward,
                      contentDescription = "More Info",
                      modifier = Modifier.fillMaxSize().size(40.dp),
                      tint = Crimson)
                }
              }

          AsyncImage(
              model = picture,
              contentDescription = "Post picture",
              modifier =
                  Modifier.fillMaxWidth()
                      .height(200.dp)
                      .clip(RoundedCornerShape(15.dp))
                      .padding(horizontal = 15.dp))

          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth().padding(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(onClick = { /*TODO: Implement Like feature*/}) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Likes",
                        tint = Crimson)
                  }

                  Text(
                      text = "${post.likesCount} likes",
                      fontWeight = FontWeight.Bold,
                      fontSize = 15.sp,
                      color = Crimson)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(onClick = { /*TODO: Implement Comment Screen*/}) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Comments",
                        tint = Crimson)
                  }

                  Text(
                      text = "${post.commentsCount} comments",
                      fontWeight = FontWeight.Bold,
                      fontSize = 15.sp,
                      color = Crimson)
                }
              }
        }
      }
}

@Preview
@Composable
fun HomeScreenPreview() {
  WildexTheme { Surface(modifier = Modifier.fillMaxSize()) { HomeScreen() } }
}
