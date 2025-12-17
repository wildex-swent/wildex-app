package com.android.wildex.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.user.OnBoardingStage
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.usecase.user.DeleteUserUseCase
import com.android.wildex.utils.FakeAuthRepository
import com.android.wildex.utils.LocalRepositories
import com.android.wildex.utils.offline.FakeConnectivityObserver
import com.google.firebase.Timestamp
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val userRepository = LocalRepositories.userRepository

  private val userSettingsRepository = LocalRepositories.userSettingsRepository

  private val userAnimalsRepository = LocalRepositories.userAnimalsRepository

  private val userAchievementsRepository = LocalRepositories.userAchievementsRepository
  private val userFriendsRepository = LocalRepositories.userFriendsRepository
  private val userTokensRepository = LocalRepositories.userTokensRepository
  private val friendRequestRepository = LocalRepositories.friendRequestRepository
  private val postsRepository = LocalRepositories.postsRepository
  private val reportsRepository = LocalRepositories.reportRepository
  private val commentsRepository = LocalRepositories.commentRepository
  private val likesRepository = LocalRepositories.likeRepository
  private val notificationRepository = LocalRepositories.notificationRepository
  private val fakeObserver = FakeConnectivityObserver(initial = true)
  private val authRepository = FakeAuthRepository()

  private lateinit var userSettingsScreenVM: SettingsScreenViewModel

  @Before
  fun setup() = runBlocking {
    userRepository.addUser(
        User(
            userId = "currentUserId",
            username = "currentUsername",
            name = "John",
            surname = "Doe",
            bio = "This is a bio",
            profilePictureURL =
                "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
            userType = UserType.PROFESSIONAL,
            creationDate = Timestamp.now(),
            country = "France",
            onBoardingStage = OnBoardingStage.COMPLETE))
    userSettingsRepository.initializeUserSettings("currentUserId")
    userAnimalsRepository.initializeUserAnimals("currentUserId")
    userAchievementsRepository.initializeUserAchievements("currentUserId")
    userFriendsRepository.initializeUserFriends("currentUserId")
    userTokensRepository.initializeUserTokens("currentUserId")

    userSettingsScreenVM =
        SettingsScreenViewModel(
            authRepository = authRepository,
            userRepository = userRepository,
            userSettingsRepository = userSettingsRepository,
            userTokensRepository = userTokensRepository,
            reportRepository = reportsRepository,
            currentUserId = "currentUserId",
            deleteUserUseCase =
                DeleteUserUseCase(
                    authRepository = authRepository,
                    userRepository = userRepository,
                    userSettingsRepository = userSettingsRepository,
                    userAnimalsRepository = userAnimalsRepository,
                    userAchievementsRepository = userAchievementsRepository,
                    userFriendsRepository = userFriendsRepository,
                    userTokensRepository = userTokensRepository,
                    friendRequestRepository = friendRequestRepository,
                    postsRepository = postsRepository,
                    likeRepository = likesRepository,
                    commentRepository = commentsRepository,
                    reportRepository = reportsRepository,
                    notificationRepository = notificationRepository,
                ),
        )
  }

  @After
  fun tearDown() {
    LocalRepositories.clearAll()
  }

  @Test
  fun initialState_displaysCorrectly() {
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SettingsScreen(
            settingsScreenViewModel = userSettingsScreenVM,
            onGoBack = {},
            onEditProfileClick = {},
            onAccountDeleteOrSignOut = {},
        )
      }
    }
    composeTestRule.onNodeWithTag(SettingsScreenTestTags.EDIT_PROFILE_SETTING).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SettingsScreenTestTags.NOTIFICATIONS_SETTING).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SettingsScreenTestTags.USER_TYPE_SETTING).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.APPEARANCE_MODE_SETTING)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(SettingsScreenTestTags.DELETE_ACCOUNT_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.DELETE_ACCOUNT_DIALOG)
        .assertIsNotDisplayed()
  }

  @Test
  fun goBackClick_invokesCallback() {
    fakeObserver.setOnline(true)
    var goBackInvoked = false

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SettingsScreen(
            settingsScreenViewModel = userSettingsScreenVM,
            onGoBack = { goBackInvoked = true },
            onEditProfileClick = {},
            onAccountDeleteOrSignOut = {},
        )
      }
    }

    composeTestRule.onNodeWithTag(SettingsScreenTestTags.GO_BACK_BUTTON).performClick()
    assert(goBackInvoked)
  }

  @Test
  fun editProfileClick_invokesCallback() {
    fakeObserver.setOnline(true)
    var editProfileInvoked = false

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SettingsScreen(
            settingsScreenViewModel = userSettingsScreenVM,
            onGoBack = {},
            onEditProfileClick = { editProfileInvoked = true },
            onAccountDeleteOrSignOut = {},
        )
      }
    }

    composeTestRule.onNodeWithTag(SettingsScreenTestTags.EDIT_PROFILE_BUTTON).performClick()
    assert(editProfileInvoked)
  }

  @Test
  fun notificationToggle_invokesNotificationStateChange() {
    fakeObserver.setOnline(true)
    val initialState = userSettingsScreenVM.uiState.value.notificationsEnabled

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SettingsScreen(
            settingsScreenViewModel = userSettingsScreenVM,
            onGoBack = {},
            onEditProfileClick = {},
            onAccountDeleteOrSignOut = {},
        )
      }
    }

    composeTestRule.onNodeWithTag(SettingsScreenTestTags.NOTIFICATIONS_TOGGLE).performClick()
    val newState = userSettingsScreenVM.uiState.value.notificationsEnabled
    assert(newState != initialState)
  }

  @Test
  fun userStatusChange_invokesUserStatusChange() {
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SettingsScreen(
            settingsScreenViewModel = userSettingsScreenVM,
            onGoBack = {},
            onEditProfileClick = {},
            onAccountDeleteOrSignOut = {},
        )
      }
    }
    composeTestRule.waitForIdle()

    val initialType = userSettingsScreenVM.uiState.value.userType
    assert(initialType == UserType.PROFESSIONAL)

    composeTestRule.onNodeWithTag(SettingsScreenTestTags.REGULAR_USER_TYPE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(SettingsScreenTestTags.USER_TYPE_DIALOG).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.USER_TYPE_DIALOG_CANCEL)
        .assertIsDisplayed()
        .performClick()
    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.USER_TYPE_DIALOG_CONFIRM)
        .assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(SettingsScreenTestTags.REGULAR_USER_TYPE_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.USER_TYPE_DIALOG_CONFIRM)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.onNodeWithTag(SettingsScreenTestTags.USER_TYPE_DIALOG).assertIsNotDisplayed()

    val newType = userSettingsScreenVM.uiState.value.userType
    assert(initialType != newType)
  }

  @Test
  fun appearanceModeChange_invokesAppearanceModeModification() {
    fakeObserver.setOnline(true)
    val initialState = userSettingsScreenVM.uiState.value.appearanceMode

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SettingsScreen(
            settingsScreenViewModel = userSettingsScreenVM,
            onGoBack = {},
            onEditProfileClick = {},
            onAccountDeleteOrSignOut = {},
        )
      }
    }

    composeTestRule.onNodeWithTag(SettingsScreenTestTags.LIGHT_MODE_BUTTON).performClick()
    val newState = userSettingsScreenVM.uiState.value.appearanceMode

    composeTestRule.onNodeWithTag(SettingsScreenTestTags.DARK_MODE_BUTTON).performClick()
    val anotherState = userSettingsScreenVM.uiState.value.appearanceMode

    assert(
        initialState != newState &&
            initialState != anotherState &&
            newState == AppearanceMode.LIGHT &&
            anotherState == AppearanceMode.DARK)
  }

  @Test
  fun signOutClick_invokesCallback() {
    fakeObserver.setOnline(true)
    var signOutInvoked = false

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SettingsScreen(
            settingsScreenViewModel = userSettingsScreenVM,
            onGoBack = {},
            onEditProfileClick = {},
            onAccountDeleteOrSignOut = { signOutInvoked = true },
        )
      }
    }

    composeTestRule.onNodeWithTag(SettingsScreenTestTags.SIGN_OUT_BUTTON).performClick()
    assert(signOutInvoked)
  }

  @Test
  fun deleteAccountClick_displaysPopup() {
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SettingsScreen(
            settingsScreenViewModel = userSettingsScreenVM,
            onGoBack = {},
            onEditProfileClick = {},
            onAccountDeleteOrSignOut = {},
        )
      }
    }

    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.DELETE_ACCOUNT_DIALOG)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.DELETE_ACCOUNT_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.onNodeWithTag(SettingsScreenTestTags.DELETE_ACCOUNT_DIALOG).assertIsDisplayed()
  }

  @Test
  fun confirmDeleteAccount_invokesCallback() {
    fakeObserver.setOnline(true)
    var accountDeletionInvoked = false

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SettingsScreen(
            settingsScreenViewModel = userSettingsScreenVM,
            onGoBack = {},
            onEditProfileClick = {},
            onAccountDeleteOrSignOut = { accountDeletionInvoked = true },
        )
      }
    }

    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.DELETE_ACCOUNT_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.onNodeWithTag(SettingsScreenTestTags.DELETE_ACCOUNT_DIALOG).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.DELETE_ACCOUNT_CONFIRM_BUTTON)
        .assertIsDisplayed()
        .performClick()
    assert(accountDeletionInvoked)
  }

  @Test
  fun cancelDeleteAccount_hidesPopup_and_doesNotInvokeCallback() {
    fakeObserver.setOnline(true)
    var accountDeletionInvoked = false
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SettingsScreen(
            settingsScreenViewModel = userSettingsScreenVM,
            onGoBack = {},
            onEditProfileClick = {},
            onAccountDeleteOrSignOut = { accountDeletionInvoked = true },
        )
      }
    }
    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.DELETE_ACCOUNT_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.onNodeWithTag(SettingsScreenTestTags.DELETE_ACCOUNT_DIALOG).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.DELETE_ACCOUNT_DISMISS_BUTTON)
        .assertIsDisplayed()
        .performClick()
    assert(!accountDeletionInvoked)
  }

  @Test
  fun settingsDialog_dismisses_afterPermissionGranted() {
    var count = 0
    val fakeContext = spyk(composeTestRule.activity.applicationContext)
    every { fakeContext.startActivity(any()) } just Runs
    composeTestRule.setContent {
      CompositionLocalProvider(LocalContext provides fakeContext) {
        SettingsPermissionDialog { ++count }
      }
    }
    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.NOTIFICATIONS_SETTING_DIALOG)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.NOTIFICATIONS_SETTING_DIALOG_CANCEL)
        .assertIsDisplayed()
        .performClick()
    assert(count == 1)

    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.NOTIFICATIONS_SETTING_DIALOG_CONFIRM)
        .assertIsDisplayed()
        .performClick()
    assert(count == 2)
  }

  @Test
  fun onGoBackEnabledOffline() {
    fakeObserver.setOnline(false)
    var goBackInvoked = false
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SettingsScreen(
            settingsScreenViewModel = userSettingsScreenVM,
            onGoBack = { goBackInvoked = true },
        )
      }
    }
    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.GO_BACK_BUTTON)
        .assertIsDisplayed()
        .performClick()
    assertTrue(goBackInvoked)
  }

  @Test
  fun signOutEnabledOffline() {
    fakeObserver.setOnline(false)
    var signOutInvoked = false
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SettingsScreen(
            settingsScreenViewModel = userSettingsScreenVM,
            onAccountDeleteOrSignOut = { signOutInvoked = true },
        )
      }
    }
    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.SIGN_OUT_BUTTON)
        .assertIsDisplayed()
        .performClick()
    assertTrue(signOutInvoked)
  }

  @Test
  fun deleteAccountDisabledOffline() {
    fakeObserver.setOnline(false)
    var deleteAccountInvoked = false
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SettingsScreen(
            settingsScreenViewModel = userSettingsScreenVM,
            onAccountDeleteOrSignOut = { deleteAccountInvoked = true },
        )
      }
    }
    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.DELETE_ACCOUNT_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.DELETE_ACCOUNT_DIALOG)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.DELETE_ACCOUNT_CONFIRM_BUTTON)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.DELETE_ACCOUNT_DISMISS_BUTTON)
        .assertIsNotDisplayed()
    assertFalse(deleteAccountInvoked)
  }

  @Test
  fun editProfileDisabledOffline() {
    fakeObserver.setOnline(false)
    var editProfileInvoked = false
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SettingsScreen(
            settingsScreenViewModel = userSettingsScreenVM,
            onEditProfileClick = { editProfileInvoked = true })
      }
    }
    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.EDIT_PROFILE_BUTTON)
        .assertIsDisplayed()
        .performClick()
    assertFalse(editProfileInvoked)
  }

  @Test
  fun notificationsToggleDisabledOffline() {
    fakeObserver.setOnline(false)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SettingsScreen(settingsScreenViewModel = userSettingsScreenVM)
      }
    }
    val oldState = userSettingsScreenVM.uiState.value.notificationsEnabled
    composeTestRule.onNodeWithTag(SettingsScreenTestTags.NOTIFICATIONS_TOGGLE).performClick()
    val newState = userSettingsScreenVM.uiState.value.notificationsEnabled
    assertEquals(oldState, newState)
  }

  @Test
  fun userStatusChangeDisabledOffline() {
    fakeObserver.setOnline(false)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SettingsScreen(settingsScreenViewModel = userSettingsScreenVM)
      }
    }
    val oldState = userSettingsScreenVM.uiState.value.userType
    when (oldState) {
      UserType.REGULAR -> {
        composeTestRule
            .onNodeWithTag(SettingsScreenTestTags.PROFESSIONAL_USER_TYPE_BUTTON)
            .performClick()
      }
      UserType.PROFESSIONAL -> {
        composeTestRule
            .onNodeWithTag(SettingsScreenTestTags.REGULAR_USER_TYPE_BUTTON)
            .performClick()
      }
    }
    val newState = userSettingsScreenVM.uiState.value.userType
    assertEquals(oldState, newState)
  }

  @Test
  fun appearanceChangeDisabledOffline() {
    fakeObserver.setOnline(false)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SettingsScreen(settingsScreenViewModel = userSettingsScreenVM)
      }
    }
    val oldState = userSettingsScreenVM.uiState.value.appearanceMode
    when (oldState) {
      AppearanceMode.AUTOMATIC -> {
        composeTestRule.onNodeWithTag(SettingsScreenTestTags.LIGHT_MODE_BUTTON).performClick()
      }
      AppearanceMode.LIGHT -> {
        composeTestRule.onNodeWithTag(SettingsScreenTestTags.DARK_MODE_BUTTON).performClick()
      }
      AppearanceMode.DARK -> {
        composeTestRule.onNodeWithTag(SettingsScreenTestTags.AUTOMATIC_MODE_BUTTON).performClick()
      }
    }
    val newState = userSettingsScreenVM.uiState.value.appearanceMode
    assertEquals(oldState, newState)
  }
}
