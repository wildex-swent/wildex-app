package com.android.wildex.ui.utils

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.URL
import com.android.wildex.ui.utils.badges.ProfessionalBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClickableProfilePicture(
    modifier: Modifier = Modifier,
    profileId: String = "",
    profilePictureURL: URL = "",
    profileUserType: UserType = UserType.REGULAR,
    onProfile: (String) -> Unit = {},
) {
  Box(modifier = modifier.clickable { onProfile(profileId) }, contentAlignment = Alignment.Center) {
    val context = LocalContext.current

    // User profile picture
    ElevatedCard(
        shape = CircleShape,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.matchParentSize()) {
          AsyncImage(
              model = ImageRequest.Builder(context).data(profilePictureURL).crossfade(true).build(),
              contentDescription = "Profile picture",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().border(1.dp, colorScheme.primary, CircleShape))
        }

    // User badge
    Box(
        modifier =
            Modifier.align(Alignment.BottomEnd).fillMaxSize(0.45f).offset(x = 3.dp, y = 3.dp)) {
          when (profileUserType) {
            UserType.REGULAR -> {}
            UserType.PROFESSIONAL -> ProfessionalBadge()
          }
        }
  }
}
