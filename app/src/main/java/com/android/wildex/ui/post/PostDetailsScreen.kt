package com.android.wildex.ui.post

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.ui.theme.WildexTheme

// import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsScreen(
    postId: String,
    postDetailsScreenViewModel: PostDetailsScreenViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onProfile: () -> Unit = {},
) {
  val uiState by postDetailsScreenViewModel.uiState.collectAsState()

  Scaffold(
      topBar = {
        PostDetailsTopBar(
            onGoBack = onGoBack,
        )
      },
      content = { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              // TODO: Implement Post Details Screen Content

              Text(text = "Post Details Screen Content")

              // temp placeholder button to go to profile screen
              Button(
                  onClick = onProfile,
                  shape = CircleShape,
                  modifier = Modifier.size(64.dp).clip(CircleShape),
                  contentPadding = PaddingValues(0.dp),
              ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(40.dp))
              }

              /* example of a the better clickable small profile picture to be implemted

              AsyncImage(
                  model = "https://example.com/sample-profile.jpg",
                  contentDescription = "Profile picture",
                  modifier = Modifier
                      .size(64.dp)
                      .clip(CircleShape)
                      .clickable { onProfile() },
                  contentScale = ContentScale.Crop,
                  placeholder = painterResource(id = R.drawable.ic_default_avatar),
                  error = painterResource(id = R.drawable.ic_default_avatar)
              )

               */

            }
      })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsTopBar(onGoBack: () -> Unit) {
  TopAppBar(
      title = { Text(text = "Back to Homepage") },
      navigationIcon = {
        IconButton(onClick = onGoBack) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Homepage")
        }
      })
}

@Preview
@Composable
fun ProfileScreenPreview() {
  WildexTheme { Surface(modifier = Modifier.fillMaxSize()) { PostDetailsScreen("fakeId") } }
}
