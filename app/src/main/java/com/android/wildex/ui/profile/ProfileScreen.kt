package com.android.wildex.ui.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.wildex.ui.theme.WildexTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.R
//import coil.compose.AsyncImage

@Composable
fun ProfileScreen(
    profileScreenViewModel: ProfileScreenViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onSettings: () -> Unit = {}
) {
    val uiState by profileScreenViewModel.uiState.collectAsState()
    val user = uiState.user

    // Fetch todos when the screen is recomposed
    LaunchedEffect(Unit) { profileScreenViewModel.refreshUIState() }

    Scaffold(
        topBar = { /* TODO: Implement TopAppBar */
            Button(onClick = {onGoBack()}) {
                Text(text = "Go Back")
            }
        },
        content = { pd ->
            Column(
                modifier = Modifier.fillMaxSize().padding(pd)
            ) {
                Text(text = "Profile Screen Content")
                Button(onClick = {}) {
                    Text(text = "Settings")
                }
            }
        }
    )
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
        modifier = Modifier.border(1.dp, Color(0xFFD0BCFF) /*WildexGreen*/).shadow(5.dp),
        navigationIcon = {
            IconButton(onClick = { /* TODO: Navigate to Notifications */}) {
                /*Icon(
                    painter = painterResource(R.drawable.notification_bell),
                    contentDescription = "Notifications",
                    modifier = Modifier.size(30.dp))*/
            }
        },
        actions = {
            IconButton(onClick = { /* TODO: Navigate to Profile */}) {
                /*AsyncImage(
                    model = user.profilePictureURL,
                    contentDescription = "Profile picture",
                    modifier =
                        Modifier.size(40.dp).clip(CircleShape).border(1.dp, WildexGreen, CircleShape),
                    contentScale = ContentScale.Crop)*/
            }
        },
        /*colors =
            TopAppBarDefaults.topAppBarColors(
                titleContentColor = WildexGreen, navigationIconContentColor = WildexGreen)*/)
}
@Preview
@Composable
fun ProfileScreenPreview() {
    WildexTheme { Surface(modifier = Modifier.fillMaxSize()) { ProfileScreen() } }
}