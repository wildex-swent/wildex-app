package com.android.wildex.ui.collection

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.utils.LocalRepositories
import org.junit.After
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val animalRepository = LocalRepositories.animalRepository
  private val userAnimalsRepository = LocalRepositories.userAnimalsRepository
  private val userRepository = LocalRepositories.userRepository

  @After
  fun tearDown() {
    LocalRepositories.clearAll()
  }
}
