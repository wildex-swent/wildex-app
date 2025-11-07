package com.android.wildex.ui.settings

import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserSettings
import com.android.wildex.model.user.UserSettingsRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsScreenViewModelTest {
  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var userRepository: UserRepository
  private lateinit var userSettingsRepository: UserSettingsRepository
  private lateinit var viewModel: SettingsScreenViewModel

  private val u1 =
    User(
      userId = "currentUserId",
      username = "currentUsername",
      name = "John",
      surname = "Doe",
      bio = "This is a bio",
      profilePictureURL =
        "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
      userType = UserType.REGULAR,
      creationDate = Timestamp.now(),
      country = "France",
      friendsCount = 3)

  private val u2 =
    User(
      userId = "otherUserId",
      username = "otherUsername",
      name = "Bob",
      surname = "Smith",
      bio = "This is my bob bio",
      profilePictureURL =
        "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
      userType = UserType.REGULAR,
      creationDate = Timestamp.now(),
      country = "France",
      friendsCount = 3)

  private val settingsUser1 =
    UserSettings(
      userId = "currentUserId",
      appearanceMode = AppearanceMode.DARK,
      enableNotifications = false
    )

  private val settingsUser2 =
    UserSettings(
      userId = "otherUserId",
      appearanceMode = AppearanceMode.LIGHT,
      enableNotifications = true
    )

  @Before
  fun setUp() {
    userRepository = mockk()
    userSettingsRepository = mockk()
    viewModel = SettingsScreenViewModel(
      userSettingsRepository = userSettingsRepository,
      userRepository = userRepository,
      currentUserId = "currentUserId"
    )
  }
}