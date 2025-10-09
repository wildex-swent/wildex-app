package com.android.wildex.ui.profile

import androidx.compose.runtime.Composable

@Composable
fun ProfileScreen(
    userId: String,
    onGoBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onTrophiesClick: (String) -> Unit
) {}