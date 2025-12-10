package com.android.wildex.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.wildex.R
import kotlin.random.Random
import kotlin.random.nextInt

private val animalRes = listOf(
  R.drawable.gorilla,
  R.drawable.dog,
  R.drawable.dog_face,
  R.drawable.monkey,
  R.drawable.orangutan,
  R.drawable.poodle,
  R.drawable.wolf,
  R.drawable.baby_chick,
  R.drawable.crocodile,
  R.drawable.dolphin,
  R.drawable.eagle,
  R.drawable.duck,
  R.drawable.fox,
  R.drawable.giraffe,
  R.drawable.goat,
  R.drawable.kangaroo,
  R.drawable.lion,
  R.drawable.lizard,
  R.drawable.lobster,
  R.drawable.rooster,
  R.drawable.shark,
  R.drawable.snake,
  R.drawable.squid,
  R.drawable.tiger,
  R.drawable.tropical_fish,
  R.drawable.turtle,
  R.drawable.whale,
)

private data class FlyingAnimal(
  val animal: Int,
  val positionX: Float,
  val initialPositionY: Float,
  val initialRotation: Float,
  val finalPositionY: Float,
  val finalRotation: Float,
  val speed: Int,
  val delay: Int,
  val size: Float
)

private fun createRandomFlyingAnimal(screenWidth: Float, screenHeight: Float, animalSize: Float): FlyingAnimal{
  return FlyingAnimal(
    animal = animalRes.random(),
    positionX = (screenWidth - animalSize) * Random.nextFloat(),
    initialPositionY = screenHeight + animalSize,
    finalPositionY = -animalSize,
    initialRotation = Random.nextInt(-360..360).toFloat(),
    finalRotation = Random.nextInt(-360..360).toFloat(),
    speed = Random.nextInt(3000..7000),
    delay = Random.nextInt(0..20000),
    size = animalSize
  )
}

@Composable
private fun FlyingAnimalAnimation(
  flyingAnimal: FlyingAnimal,
  animalSize: Dp
){
  val infiniteTransition = rememberInfiniteTransition()
  val posY by infiniteTransition.animateFloat(
    initialValue = flyingAnimal.initialPositionY,
    targetValue = flyingAnimal.finalPositionY,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = flyingAnimal.speed, delayMillis = flyingAnimal.delay, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    )
  )

  val rotation by infiniteTransition.animateFloat(
    initialValue = flyingAnimal.initialRotation,
    targetValue = flyingAnimal.finalRotation,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = flyingAnimal.speed, delayMillis = flyingAnimal.delay, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    )
  )

  Image(
    painter = painterResource(id = flyingAnimal.animal),
    contentDescription = null,
    modifier = Modifier
      .offset(x = flyingAnimal.positionX.dp, y = posY.dp)
      .graphicsLayer(
        rotationZ = rotation
      )
      .size(animalSize)
  )
}

@Composable
fun UploadingAnimation() {
  val screenWidth = LocalConfiguration.current.screenWidthDp.dp.value
  val screenHeight = LocalConfiguration.current.screenHeightDp.dp.value
  val animalSizes = listOf(screenWidth / 7, screenWidth / 8, screenWidth / 9, screenWidth / 10)
  val flyingAnimals = mutableListOf<FlyingAnimal>()
  (1..200).forEach{ _ ->
    flyingAnimals.add(createRandomFlyingAnimal(screenWidth, screenHeight, animalSizes.random()))
  }

  Box(
    modifier = Modifier.fillMaxSize()
  ) {
    flyingAnimals.forEach {
      FlyingAnimalAnimation(it, it.size.dp)
    }
  }
}

