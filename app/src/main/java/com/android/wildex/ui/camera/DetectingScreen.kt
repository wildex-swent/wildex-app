package com.android.wildex.ui.camera

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.android.wildex.R

@Composable
fun DetectingScreen(photoUri: Uri, modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize()) {
    // Photo background
    AsyncImage(
        model = photoUri,
        contentDescription = "Captured photo",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
    )

    // Dark overlay
    Box(modifier = Modifier.fillMaxSize().background(colorScheme.surface.copy(alpha = 0.6f)))

    // Content
    Column(
        modifier = Modifier.align(Alignment.Center).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Clever phrase
      Text(
          text = "Detecting...",
          style = typography.headlineMedium,
          textAlign = TextAlign.Center,
      )

      // Lottie animation
      LottieAnimation(
          composition =
              rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loader_cat)).value,
          iterations = LottieConstants.IterateForever,
          modifier = Modifier.size(300.dp),
      )
    }
  }
}
