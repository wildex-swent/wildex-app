package com.android.wildex.ui.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.wildex.R
import kotlin.random.Random
import kotlin.random.nextInt

private val animalRes = listOf(
  R.drawable.icons8_cat,
  R.drawable.icons8_dog,
  R.drawable.icons8_ray,
  R.drawable.icons8_crab,
  R.drawable.icons8_fish,
  R.drawable.icons8_butterfly,
  R.drawable.icons8_camel,
  R.drawable.icons8_chinchilla,
  R.drawable.icons8_heron,
  R.drawable.icons8_horse,
  R.drawable.icons8_jaguar,
  R.drawable.icons8_mouse,
  R.drawable.icons8_panda,
  R.drawable.icons8_seagull
)

private val colors = listOf(
  Color(0xFFE26D5C),
  Color(0xFFFFB30F),
  Color(0xFFC84630),
  Color(0xFF995D81),
  Color(0xFF9381FF),
  Color(0xFFFF4000)
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
  val size: Float,
  val color: Color
)

private fun createRandomFlyingAnimal(screenWidth: Float, screenHeight: Float, animalSize: Float): FlyingAnimal{
  return FlyingAnimal(
    animal = animalRes.random(),
    positionX = (screenWidth - animalSize) * Random.nextFloat(),
    initialPositionY = screenHeight + animalSize,
    finalPositionY = -animalSize,
    initialRotation = Random.nextInt(-360..360).toFloat(),
    finalRotation = Random.nextInt(-360..360).toFloat(),
    speed = Random.nextInt(2000..5000),
    delay = Random.nextInt(0..5000),
    size = animalSize,
    color = colors.random()
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
      animation = tween(durationMillis = flyingAnimal.speed, delayMillis = 1000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
      initialStartOffset = StartOffset(flyingAnimal.delay)
    )
  )

  val rotation by infiniteTransition.animateFloat(
    initialValue = flyingAnimal.initialRotation,
    targetValue = flyingAnimal.finalRotation,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = flyingAnimal.speed, delayMillis = 1000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
      initialStartOffset = StartOffset(flyingAnimal.delay)
    )
  )

  Image(
    painter = painterResource(id = flyingAnimal.animal),
    colorFilter = ColorFilter.tint(flyingAnimal.color),
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
  (1..50).forEach{ _ ->
    flyingAnimals.add(createRandomFlyingAnimal(screenWidth, screenHeight, animalSizes.random()))
  }

  Box(
    modifier = Modifier.fillMaxSize().padding(PaddingValues(0.dp)),
  ) {
    WaterFillBackground()
    flyingAnimals.forEach {
      FlyingAnimalAnimation(it, it.size.dp)
    }
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ){
      Text(
        text = "Sharing your new post...",
        style = typography.titleLarge,
        color = colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 30.dp)
      )
    }
  }
}

