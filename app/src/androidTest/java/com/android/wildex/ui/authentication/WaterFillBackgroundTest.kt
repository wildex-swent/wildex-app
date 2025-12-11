package com.android.wildex.ui.authentication

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class WaterFillBackgroundTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun waterFillBackground_callsOnFilledOnceByEndOfAnimation() {
    var callbackCount = 0
    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      WaterFillBackground(
          onFilled = { callbackCount++ },
      )
    }
    Assert.assertEquals(0, callbackCount)
    composeTestRule.mainClock.advanceTimeBy(1000L)
    composeTestRule.mainClock.advanceTimeBy(4000L)
    composeTestRule.waitForIdle()
    Assert.assertTrue("onFilled should be called at least once", callbackCount >= 1)
    Assert.assertEquals("onFilled should be called exactly once", 1, callbackCount)
  }

  @Test
  fun waterFillBackground_callsOnFilledDuringAnimation_notOnlyAtEnd() {
    var callbackCount = 0
    var filledDuringAnimation = false
    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      WaterFillBackground(
          onFilled = {
            callbackCount++
            if (composeTestRule.mainClock.currentTime < 4000L) filledDuringAnimation = true
          })
    }
    composeTestRule.mainClock.advanceTimeBy(2500L)
    composeTestRule.mainClock.advanceTimeBy(2000L)
    composeTestRule.waitForIdle()
    Assert.assertEquals(1, callbackCount)
    Assert.assertTrue(filledDuringAnimation)
  }
}
