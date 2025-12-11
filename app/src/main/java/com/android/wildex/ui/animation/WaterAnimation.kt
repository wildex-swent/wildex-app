package com.android.wildex.ui.animation

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * Fullscreen water animation
 * - water fills from bottom to top over 4 seconds
 * - wave oscillates as it fills
 * - calls onFilled() when water is 80% full
 *
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun WaterFillBackground() {
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val infiniteTransition = rememberInfiniteTransition()
    val fillDuration = 3000

    var wave by remember { mutableStateOf(true) }
    val level = remember { Animatable(-0.2f) }

    LaunchedEffect(Unit) {
      while (true) {
        level.animateTo(
          targetValue = 1.2f,
          animationSpec = tween(durationMillis = fillDuration, easing = LinearEasing)
        )

        level.snapTo(-0.2f)

        wave = !wave
      }
    }

    val phase by infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 3f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = fillDuration, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
      )
    )

    val waterColor = if (wave) colorScheme.primary else colorScheme.background
    val backgroundColor = if (wave) colorScheme.background else colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      val amplitude = h * 0.03f

      val baseY = h * (1f - level.value) - amplitude

      val wavelength = w / 1.5f
      val currentPhase = phase * 2f * PI.toFloat()

      val wavePath =
        Path().apply {
          moveTo(0f, h)
          lineTo(0f, baseY)

          val steps = 80
          for (i in 0..steps) {
            val x = i / steps.toFloat() * w
            val y = baseY + sin((x / wavelength) * 2f * PI.toFloat() + currentPhase) * amplitude
            lineTo(x, y)
          }

          lineTo(w, h)
          close()
        }

      val backgroundPath = Path().apply {
        moveTo(0f, h)
        lineTo(0f, 0f)
        lineTo(w, 0f)
        lineTo(w, h)
        close()
      }

      drawPath(
        path = backgroundPath,
        color = backgroundColor,
      )

      // Fill everything with water below the wave
      drawPath(
        path = wavePath,
        color = waterColor,
      )
    }
  }
}