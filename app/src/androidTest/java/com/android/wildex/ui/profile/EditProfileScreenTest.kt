package com.android.wildex.ui.profile

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.utils.LocalRepositories
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class EditProfileScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private fun sampleUser() =
      User(
          userId = "uid-1",
          username = "jane_doe",
          name = "Jane",
          surname = "Doe",
          bio = "Bio of Jane",
          profilePictureURL = "https://example.com/pic.jpg",
          userType = UserType.REGULAR,
          creationDate = Timestamp(0, 0),
          country = "Switzerland",
          friendsCount = 10,
      )

  @Test
  fun initialState_loadsFromRepository_andShowsFields() {
    val userRepo = LocalRepositories.UserRepositoryImpl()
    runBlocking { userRepo.addUser(sampleUser()) }
    val vm =
        EditProfileViewModel(
            userRepository = userRepo,
            currentUserId = "uid-1",
        )

    composeRule.setContent { EditProfileScreen(editScreenViewModel = vm, isNewUser = true) }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(EditProfileScreenTestTags.INPUT_NAME).assertTextContains("Jane")
    composeRule.onNodeWithTag(EditProfileScreenTestTags.INPUT_SURNAME).assertTextContains("Doe")
    composeRule
        .onNodeWithTag(EditProfileScreenTestTags.INPUT_USERNAME)
        .assertTextContains("jane_doe")
    composeRule
        .onNodeWithTag(EditProfileScreenTestTags.INPUT_DESCRIPTION)
        .assertTextContains("Bio of Jane")
    composeRule.onNodeWithTag(EditProfileScreenTestTags.DROPDOWN_COUNTRY).assertIsDisplayed()
    composeRule.onNodeWithTag(EditProfileScreenTestTags.CHANGE_PROFILE_PICTURE).assertIsDisplayed()
  }

  @Test
  fun countryDropdown_opens_and_selects_country() {
    val userRepo = LocalRepositories.UserRepositoryImpl()
    runBlocking { userRepo.addUser(sampleUser()) }
    val vm =
        EditProfileViewModel(
            userRepository = userRepo,
            currentUserId = "uid-1",
        )

    composeRule.setContent { EditProfileScreen(editScreenViewModel = vm, isNewUser = true) }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(EditProfileScreenTestTags.DROPDOWN_COUNTRY).performClick()
    composeRule
        .onNodeWithText("United States", useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("United States", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun validationMessages_toggle_on_name_field() {
    val userRepo = LocalRepositories.UserRepositoryImpl()
    runBlocking { userRepo.addUser(sampleUser()) }
    val vm =
        EditProfileViewModel(
            userRepository = userRepo,
            currentUserId = "uid-1",
        )

    composeRule.setContent { EditProfileScreen(editScreenViewModel = vm, isNewUser = true) }
    composeRule.waitForIdle()

    val nameField = composeRule.onNodeWithTag(EditProfileScreenTestTags.INPUT_NAME)
    nameField.performTextClearance()
    nameField.performTextInput("")
    composeRule.waitForIdle()
    composeRule
        .onAllNodesWithTag(EditProfileScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertCountEquals(1)

    nameField.performTextInput("J")
    composeRule.waitForIdle()
    composeRule
        .onAllNodesWithTag(EditProfileScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertCountEquals(0)
  }

  @Test
  fun goBack_invokes_callback() {
    val userRepo = LocalRepositories.UserRepositoryImpl()
    runBlocking { userRepo.addUser(sampleUser()) }
    val vm =
        EditProfileViewModel(
            userRepository = userRepo,
            currentUserId = "uid-1",
        )

    var back = 0
    composeRule.setContent {
      EditProfileScreen(editScreenViewModel = vm, onGoBack = { back++ }, isNewUser = true)
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(EditProfileScreenTestTags.GO_BACK).performClick()
    Assert.assertEquals(1, back)
  }

  @Test
  fun save_click_invokes_onSave_when_isNewUser_true() {
    val userRepo = LocalRepositories.UserRepositoryImpl()
    runBlocking { userRepo.addUser(sampleUser()) }
    val vm =
        EditProfileViewModel(
            userRepository = userRepo,
            currentUserId = "uid-1",
        )

    var saved = 0
    composeRule.setContent {
      EditProfileScreen(editScreenViewModel = vm, isNewUser = true, onSave = { saved++ })
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Save").performClick()
    Assert.assertEquals(1, saved)
  }
}
