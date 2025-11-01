package com.android.wildex.ui.camera

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.android.wildex.R
import kotlinx.coroutines.delay

object DetectingScreenTestTags {
  const val DETECTING_SCREEN_ANIMATION = "detecting_screen_animation"
  const val DETECTING_SCREEN_LOADING_BAR = "detecting_screen_loading_bar"
  const val DETECTING_SCREEN_PHRASE_1 = "detecting_screen_phrase_1"
  const val DETECTING_SCREEN_PHRASE_2 = "detecting_screen_phrase_2"
  const val DETECTING_SCREEN_IMAGE = "detecting_screen_image"
}

@Composable
fun DetectingScreen(photoUri: Uri, modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize()) {
    val animationIds =
        listOf(
            R.raw.turtle,
            R.raw.cock,
            R.raw.blue_whale,
            R.raw.elephant,
            R.raw.lion,
            R.raw.squirrel,
            R.raw.snake,
        )

    var currentAnimationId by remember { mutableIntStateOf(animationIds.random()) }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(currentAnimationId))
    val progress by
        animateLottieCompositionAsState(
            composition,
            iterations = 1, // play once, then switch
            restartOnPlay = false,
        )

    // When animation finishes, pick another one randomly
    LaunchedEffect(progress) {
      if (progress >= 1f) {
        currentAnimationId = animationIds.random()
      }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val loadingProgress by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "loadingProgress",
        )

    // Photo background
    AsyncImage(
        model = photoUri,
        contentDescription = "Captured photo",
        modifier = Modifier.fillMaxSize().testTag(DetectingScreenTestTags.DETECTING_SCREEN_IMAGE),
        contentScale = ContentScale.Crop,
    )

    // Dark overlay
    Box(modifier = Modifier.fillMaxSize().background(colorScheme.surfaceVariant.copy(alpha = 0.7f)))

    // Content
    Column(
        modifier =
            Modifier.align(Alignment.Center)
                .background(
                    colorScheme.primaryContainer.copy(alpha = 0.6f),
                    RoundedCornerShape(20.dp),
                )
                .padding(bottom = 20.dp, start = 30.dp, end = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

      // Lottie animation
      RandomLottieAnimationsFromRaw(
          lottieResIds = animationIds,
          modifier =
              Modifier.size(150.dp).testTag(DetectingScreenTestTags.DETECTING_SCREEN_ANIMATION),
      )
      // Clever phrase
      Text(
          text = "🐾  Analyzing Wildlife  🐾",
          style = typography.headlineSmall,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag(DetectingScreenTestTags.DETECTING_SCREEN_PHRASE_1),
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
          text = "Identifying species and characteristics...",
          style = typography.titleMedium,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag(DetectingScreenTestTags.DETECTING_SCREEN_PHRASE_2),
      )

      Spacer(modifier = Modifier.height(10.dp))
      // Infinite loading bar
      Box(
          modifier =
              Modifier.fillMaxWidth(.94f)
                  .height(6.dp)
                  .clip(RoundedCornerShape(3.dp))
                  .background(colorScheme.primaryContainer.copy(alpha = 0.5f))
                  .testTag(DetectingScreenTestTags.DETECTING_SCREEN_LOADING_BAR)) {
            Box(
                modifier =
                    Modifier.fillMaxHeight()
                        .fillMaxWidth(0.6f)
                        .offset(x = ((loadingProgress - 0.3f) * 1.6f * 300).dp)
                        .background(
                            Brush.horizontalGradient(
                                colors =
                                    listOf(
                                        colorScheme.primaryContainer,
                                        colorScheme.primary,
                                        colorScheme.onPrimaryContainer,
                                        colorScheme.primary,
                                        colorScheme.primaryContainer,
                                    )),
                            shape = RoundedCornerShape(3.dp),
                        ))
          }
    }
  }
}

@Composable
fun RandomLottieAnimationsFromRaw(lottieResIds: List<Int>, modifier: Modifier = Modifier) {
  var currentIndex by remember { mutableIntStateOf((lottieResIds.indices).random()) }
  var isPlaying by remember { mutableStateOf(true) }

  val composition by
      rememberLottieComposition(LottieCompositionSpec.RawRes(lottieResIds[currentIndex]))

  val progress by
      animateLottieCompositionAsState(
          composition = composition,
          isPlaying = isPlaying,
          iterations = 1,
          speed = 1f,
      )

  LaunchedEffect(composition, progress) {
    if (composition != null && progress >= 1f) {
      isPlaying = false
      delay(100)
      // Pick next random animation
      val nextIndex = (lottieResIds.indices).random()
      currentIndex = nextIndex
      isPlaying = true
    }
  }

  LottieAnimation(composition = composition, progress = { progress }, modifier = modifier)
}
