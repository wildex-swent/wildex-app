package com.android.wildex.ui.utils.badges

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalBadge(modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Icon(
        imageVector = Icons.Filled.Pets,
        contentDescription = "Professional badge",
        tint = colorScheme.primary,
        modifier = Modifier.fillMaxSize())
    Icon(
        imageVector = Icons.Rounded.Add,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.align(BiasAlignment(0f, 0.7f)).fillMaxSize(0.5f))
  }
}
