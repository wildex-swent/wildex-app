package com.android.wildex.ui.notification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.wildex.R
import com.android.wildex.ui.profile.EditProfileScreenTestTags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationTopBar(onGoBack: () -> Unit){
    val cs = colorScheme
    CenterAlignedTopAppBar(
        title = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = LocalContext.current.getString(R.string.notification_title),
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onBackground,
                )
            }
        },
        navigationIcon = {
            IconButton(
                modifier = Modifier.testTag(EditProfileScreenTestTags.GO_BACK),
                onClick = { onGoBack() },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = cs.onBackground,
                )
            }
        },
        actions = {
            // Empty box to center the title
            Box(modifier = Modifier.size(48.dp))
        })
}