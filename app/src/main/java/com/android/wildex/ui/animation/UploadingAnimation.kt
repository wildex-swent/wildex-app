package com.android.wildex.ui.animation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.wildex.R
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val MAX_ROTATION_START = 90
private const val TIME_DELTA = 0.016f
private const val TIME_DELAY = 16L
private const val ROTATION_DELTA = 0.03f
private const val DELAY_RESPAWN = 150L
private const val DELAY_SPAWN = 300L
private const val NUMBER_ANIMAL_MAX = 8
private const val ANIMAL_SIZE = 120
private const val INITIAL_SPEED = 100

class FlyingAnimal(
  val animal: String,
  initialPositionX: Float,
  initialPositionY: Float,
  initialVelocity: Float
){
  var positionY by mutableFloatStateOf(initialPositionY)
  var positionX by mutableFloatStateOf(initialPositionX)
  private var velocity by mutableFloatStateOf(initialVelocity)
  private val rotationDelta =
    (Random.nextFloat() * MAX_ROTATION_START) - MAX_ROTATION_START / 2
  var rotation by mutableFloatStateOf(rotationDelta)

  fun setRandomAnimal(
    screenWidth: Float,
    screenHeight: Float,
    animalSize: Float,
  ) {
    val xPos = getXPos(screenWidth, animalSize)
    positionX = xPos
    positionY = screenHeight + animalSize
  }

  suspend fun fly(animalSize: Float, onFinished: () -> Unit) {
    while (positionY > -animalSize) {
      delay(TIME_DELAY)
      positionY -= velocity * TIME_DELTA
      rotation -= rotationDelta * ROTATION_DELTA
    }

    delay(DELAY_RESPAWN)
    onFinished()
  }
}

@Composable
fun UploadingAnimation(
  onFinish: () -> Unit,
  duration: Long = Long.MIN_VALUE
) {
  val screenSize = LocalWindowInfo.current.containerSize
  val screenWidth = 400.dp
  val screenHeight = 1080.dp
  val animalSize = ANIMAL_SIZE.dp

  val flyingAnimals = remember { mutableStateListOf<FlyingAnimal>() }

  if (duration != Long.MIN_VALUE) {
    LaunchedEffect(duration) {
      delay(duration)
      onFinish()
    }
  }

  LaunchedEffect(Unit) {
    while (flyingAnimals.size < NUMBER_ANIMAL_MAX) {
      delay(DELAY_SPAWN)

      val xPos = getXPos(screenWidth.value, animalSize.value)
      val newAnimal =
        FlyingAnimal(
          animal = "🦁",
          initialPositionX = xPos,
          initialPositionY = screenHeight.value + animalSize.value,
          initialVelocity = INITIAL_SPEED * Random.nextFloat())
      flyingAnimals.add(newAnimal)
    }
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    flyingAnimals.forEachIndexed { index, animal ->
      LaunchedEffect(animal) {
        while (true) {
          animal.fly(animalSize.value) {
            animal.setRandomAnimal(screenWidth.value, screenHeight.value, animalSize.value)
          }
        }
      }

      // Apply offset and rotation to each animal image
      Image(
        painter = painterResource(R.drawable.google_logo),
        contentDescription = "jid",
        modifier =
          Modifier.offset(x = animal.positionX.dp, y = animal.positionY.dp)
            .size(animalSize)
            .graphicsLayer(rotationZ = animal.rotation)
            .testTag("AnimalImage_$index"))
    }
  }
}

private fun getXPos(screenWidth: Float, animalSize: Float): Float {
  val padding = (animalSize / 2)
  val posAnimalCenter = (Random.nextFloat() * (screenWidth - animalSize))
  return padding + posAnimalCenter
}

@Preview(showBackground = true)
@Composable
fun PreviewAnimation(){
  Box(
    modifier = Modifier.height(1080.dp).width(400.dp)
  ){
    UploadingAnimation(onFinish = {})
  }
}

