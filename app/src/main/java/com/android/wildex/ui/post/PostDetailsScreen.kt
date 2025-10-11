package com.android.wildex.ui.post


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.ui.theme.WildexTheme


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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TODO: Implement Post Details Screen Content
                Text(text = "Post Details Screen Content")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsTopBar(
    onGoBack: () -> Unit
) {
    TopAppBar(
        title = { Text(text = "Back to Homepage") },
        navigationIcon = {
            IconButton(onClick = onGoBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Homepage"
                )
            }
        }
    )
}



@Preview
@Composable
fun ProfileScreenPreview() {
    WildexTheme { Surface(modifier = Modifier.fillMaxSize()) { PostDetailsScreen("fakeId") } }
}