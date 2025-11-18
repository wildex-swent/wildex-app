package com.android.wildex.ui.notification

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun NotificationScreen(
    onGoBack: () -> Unit = {},
    notificationScreenViewModel: NotificationScreenViewModel = viewModel(),
    ) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            NotificationTopBar(onGoBack = onGoBack)
        },
    ) { pd ->
        NotificationView(pd)
    }
}
@Composable
fun NotificationView(pd: PaddingValues){}