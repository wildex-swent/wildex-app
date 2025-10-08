package com.android.wildex.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.wildex.R
import com.android.wildex.model.social.Post
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.theme.WildexTheme
import com.google.firebase.Timestamp

val testPost = Post(
    postId = "001",
    authorId = "002",
    pictureURL = "https://upload.wikimedia.org/wikipedia/commons/9/9f/Fennec_Fox_Vulpes_zerda.jpg",
    location = Location(0.0, 0.0, "EPFL"),
    date = Timestamp(0,0),
    animalId = "003",
    likesCount = 0,
    commentsCount = 0
)

@Composable
fun HomeScreen(){

}

@Preview
@Composable
fun ProfilUtilisateur() {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            // Image de profil
            AsyncImage(
                model = "https://randomuser.me/api/portraits/men/32.jpg",
                contentDescription = "Photo de profil",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Informations utilisateur
            Column {
                Text(
                    text = "Jean Dupont",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Développeur Android",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    WildexTheme { Surface(modifier = Modifier.fillMaxSize()) { HomeScreen() } }
}