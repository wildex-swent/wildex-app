package com.android.wildex.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.utils.LocalRepositories
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val userRepository = LocalRepositories.userRepository

  private val userSettingsRepository = LocalRepositories.userSettingsRepository

  private lateinit var userSettingsScreenVM: SettingsScreenViewModel
}