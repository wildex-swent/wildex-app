package com.android.wildex.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.usecase.user.DeleteUserUseCase
import com.android.wildex.utils.FakeAuthRepository
import com.android.wildex.utils.LocalRepositories
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val userRepository = LocalRepositories.userRepository

  private val userSettingsRepository = LocalRepositories.userSettingsRepository

  private val userAnimalsRepository = LocalRepositories.userAnimalsRepository

  private val userAchievementsRepository = LocalRepositories.userAchievementsRepository
  private val postsRepository = LocalRepositories.postsRepository
  private val reportsRepository = LocalRepositories.reportRepository
  private val commentsRepository = LocalRepositories.commentRepository
  private val likesRepository = LocalRepositories.likeRepository

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
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "France",
            friendsCount = 3,
        )
    )
    userSettingsRepository.initializeUserSettings("currentUserId")
    userAnimalsRepository.initializeUserAnimals("currentUserId")
    userAchievementsRepository.initializeUserAchievements("currentUserId")

    userSettingsScreenVM =
        SettingsScreenViewModel(
            authRepository = authRepository,
            userRepository = userRepository,
            userSettingsRepository = userSettingsRepository,
            currentUserId = "currentUserId",
            deleteUserUseCase =
                DeleteUserUseCase(
                    authRepository = authRepository,
                    userRepository = userRepository,
                    userSettingsRepository = userSettingsRepository,
                    userAnimalsRepository = userAnimalsRepository,
                    userAchievementsRepository = userAchievementsRepository,
                    postsRepository = postsRepository,
                    likeRepository = likesRepository,
                    commentRepository = commentsRepository,
                    reportRepository = reportsRepository,
                ),
        )
  }

  @After
  fun tearDown() {
    LocalRepositories.clearAll()
  }

  @Test
  fun initialState_displaysCorrectly() {
    composeTestRule.setContent {
      SettingsScreen(
          settingsScreenViewModel = userSettingsScreenVM,
          onGoBack = {},
          onEditProfileClick = {},
          onAccountDeleteOrSignOut = {},
      )
    }
    composeTestRule.onNodeWithTag(SettingsScreenTestTags.EDIT_PROFILE_SETTING).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SettingsScreenTestTags.NOTIFICATIONS_SETTING).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SettingsScreenTestTags.USER_STATUS_SETTING).assertIsDisplayed()
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
    var goBackInvoked = false

    composeTestRule.setContent {
      SettingsScreen(
          settingsScreenViewModel = userSettingsScreenVM,
          onGoBack = { goBackInvoked = true },
          onEditProfileClick = {},
          onAccountDeleteOrSignOut = {},
      )
    }

    composeTestRule.onNodeWithTag(SettingsScreenTestTags.GO_BACK_BUTTON).performClick()
    assert(goBackInvoked)
  }

  @Test
  fun editProfileClick_invokesCallback() {
    var editProfileInvoked = false

    composeTestRule.setContent {
      SettingsScreen(
          settingsScreenViewModel = userSettingsScreenVM,
          onGoBack = {},
          onEditProfileClick = { editProfileInvoked = true },
          onAccountDeleteOrSignOut = {},
      )
    }

    composeTestRule.onNodeWithTag(SettingsScreenTestTags.EDIT_PROFILE_BUTTON).performClick()
    assert(editProfileInvoked)
  }

  @Test
  fun notificationToggle_invokesNotificationStateChange() {
    val initialState = userSettingsScreenVM.uiState.value.notificationsEnabled

    composeTestRule.setContent {
      SettingsScreen(
          settingsScreenViewModel = userSettingsScreenVM,
          onGoBack = {},
          onEditProfileClick = {},
          onAccountDeleteOrSignOut = {},
      )
    }

    composeTestRule.onNodeWithTag(SettingsScreenTestTags.NOTIFICATIONS_TOGGLE).performClick()
    val newState = userSettingsScreenVM.uiState.value.notificationsEnabled
    assert(newState != initialState)
  }

  @Test
  fun userStatusChange_invokesUserStatusChange() {
    val initialStatus = userSettingsScreenVM.uiState.value.userType

    composeTestRule.setContent {
      SettingsScreen(
          settingsScreenViewModel = userSettingsScreenVM,
          onGoBack = {},
          onEditProfileClick = {},
          onAccountDeleteOrSignOut = {},
      )
    }

    composeTestRule
        .onNodeWithTag(SettingsScreenTestTags.PROFESSIONAL_USER_STATUS_BUTTON)
        .performClick()
    val newStatus = userSettingsScreenVM.uiState.value.userType
    assert(initialStatus != newStatus)
  }

  @Test
  fun appearanceModeChange_invokesAppearanceModeModification() {
    val initialState = userSettingsScreenVM.uiState.value.appearanceMode

    composeTestRule.setContent {
      SettingsScreen(
          settingsScreenViewModel = userSettingsScreenVM,
          onGoBack = {},
          onEditProfileClick = {},
          onAccountDeleteOrSignOut = {},
      )
    }

    composeTestRule.onNodeWithTag(SettingsScreenTestTags.LIGHT_MODE_BUTTON).performClick()
    val newState = userSettingsScreenVM.uiState.value.appearanceMode

    composeTestRule.onNodeWithTag(SettingsScreenTestTags.DARK_MODE_BUTTON).performClick()
    val anotherState = userSettingsScreenVM.uiState.value.appearanceMode

    assert(
        initialState != newState &&
            initialState != anotherState &&
            newState == AppearanceMode.LIGHT &&
            anotherState == AppearanceMode.DARK
    )
  }

  @Test
  fun signOutClick_invokesCallback() {
    var signOutInvoked = false

    composeTestRule.setContent {
      SettingsScreen(
          settingsScreenViewModel = userSettingsScreenVM,
          onGoBack = {},
          onEditProfileClick = {},
          onAccountDeleteOrSignOut = { signOutInvoked = true },
      )
    }

    composeTestRule.onNodeWithTag(SettingsScreenTestTags.SIGN_OUT_BUTTON).performClick()
    assert(signOutInvoked)
  }

  @Test
  fun deleteAccountClick_displaysPopup() {
    composeTestRule.setContent {
      SettingsScreen(
          settingsScreenViewModel = userSettingsScreenVM,
          onGoBack = {},
          onEditProfileClick = {},
          onAccountDeleteOrSignOut = {},
      )
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
    var accountDeletionInvoked = false

    composeTestRule.setContent {
      SettingsScreen(
          settingsScreenViewModel = userSettingsScreenVM,
          onGoBack = {},
          onEditProfileClick = {},
          onAccountDeleteOrSignOut = { accountDeletionInvoked = true },
      )
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
    var accountDeletionInvoked = false

    composeTestRule.setContent {
      SettingsScreen(
          settingsScreenViewModel = userSettingsScreenVM,
          onGoBack = {},
          onEditProfileClick = {},
          onAccountDeleteOrSignOut = { accountDeletionInvoked = true },
      )
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
}
