package com.android.wildex.ui.authentication

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import com.android.wildex.ui.theme.WildexDarkGreen
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * Fullscreen water animation
 * - water fills from bottom to top over 4 seconds
 * - wave oscillates as it fills
 * - calls onFilled() when water is 80% full
 *
 * @param onFilled Callback invoked when the water fill animation is 80% complete.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun WaterFillBackground(onFilled: () -> Unit) {
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val level = remember { Animatable(0f) }
    val phase = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
      val fillDuration = 4000
      var triggered = false

      val fillJob = launch {
        level.animateTo(
            targetValue = 1f,
            animationSpec =
                tween(
                    durationMillis = fillDuration,
                    easing = FastOutSlowInEasing,
                ),
        ) {
          if (!triggered && this.value >= 0.8f) {
            triggered = true
            onFilled()
          }
        }
      }

      launch {
        phase.animateTo(
            targetValue = 3f,
            animationSpec =
                tween(
                    durationMillis = fillDuration,
                    easing = LinearEasing,
                ),
        )
      }

      fillJob.join()
      if (!triggered) onFilled()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      val amplitude = h * 0.03f
      val levelValue = level.value.coerceIn(0f, 1f)

      // baseY = surface level; subtract amplitude so wave overshoots top when full
      val baseY = h * (1f - levelValue) - amplitude

      val wavelength = w / 1.5f
      val currentPhase = phase.value * 2f * PI.toFloat()

      val wavePath =
          Path().apply {
            // start at bottom-left
            moveTo(0f, h)
            // go up to water surface at x=0
            lineTo(0f, baseY)

            val steps = 80
            for (i in 0..steps) {
              val x = i / steps.toFloat() * w
              val y = baseY + sin((x / wavelength) * 2f * PI.toFloat() + currentPhase) * amplitude
              lineTo(x, y)
            }

            // down to bottom-right and close
            lineTo(w, h)
            close()
          }

      // Water color is hardcoded since WildexDarkGreen is the official color of the logo and does
      // not change with themes
      val waterColor = WildexDarkGreen

      // Fill everything with water below the wave
      drawPath(
          path = wavePath,
          color = waterColor,
      )
    }
  }
}

/*
 * This water animation was taken from a stackoverflow answer by user 'Chris Spittles', that can be found here:
 * https://stackoverflow.com/questions/29738787/filling-water-animation
 * I used his second code suggestion as a base but since his used svg and html/css, I adapted the code to Kotlin/Compose
 * using ChatGPT.
 */
