package com.android.wildex.ui.end2end

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.android.wildex.ui.navigation.NavigationTestUtils
import com.android.wildex.ui.profile.EditProfileScreenTestTags
import com.android.wildex.ui.profile.ProfileScreenTestTags
import com.android.wildex.ui.settings.SettingsScreenTestTags
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

class End2EndTest4 : NavigationTestUtils() {
  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun userFlow4() = runTest {
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
    composeRule.navigateToMyProfileScreenFromHome()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
    composeRule.navigateToSettingsScreenFromProfile()
    composeRule.waitForIdle()
    composeRule.checkSettingsScreenIsDisplayed()
    composeRule.checkFullSettingsScreenIsDisplayed()
    composeRule.navigateToEditProfileScreenFromSettings()
    composeRule.waitForIdle()
    composeRule.checkEditProfileScreenIsDisplayed()
    composeRule.editProfile()
    composeRule.navigateBackFromEditProfile()
    composeRule.waitForIdle()
    composeRule.checkSettingsScreenIsDisplayed()
    composeRule.navigateBackFromSettings()
    composeRule.waitForIdle()
    composeRule.checkProfileScreenIsDisplayed(userId)
    composeRule.checkProfileInformation()
    composeRule.navigateBackFromProfile()
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()
  }

  private fun ComposeTestRule.checkProfileInformation() {
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.PROFILE_NAME)
    onNodeWithTag(ProfileScreenTestTags.PROFILE_NAME)
        .assertTextEquals("ModifiedName ModifiedSurname")
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.PROFILE_USERNAME)
    onNodeWithTag(ProfileScreenTestTags.PROFILE_USERNAME).assertTextEquals("newUsername")
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.PROFILE_DESCRIPTION)
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.MAP)
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.MAP_CTA)
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.COLLECTION)
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.FRIENDS)
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.ACHIEVEMENTS)
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.ACHIEVEMENTS_CTA)
    onNodeWithTag(ProfileScreenTestTags.SETTINGS).assertIsDisplayed()
  }

  private fun ComposeTestRule.editProfile() {
    onNodeWithTag(EditProfileScreenTestTags.INPUT_NAME)
        .performScrollTo()
        .assertIsDisplayed()
        .performTextClearance()
    onNodeWithTag(EditProfileScreenTestTags.INPUT_NAME)
        .performScrollTo()
        .assertIsDisplayed()
        .performTextInput("ModifiedName")

    onNodeWithTag(EditProfileScreenTestTags.INPUT_SURNAME)
        .performScrollTo()
        .assertIsDisplayed()
        .performTextClearance()
    onNodeWithTag(EditProfileScreenTestTags.INPUT_SURNAME)
        .performScrollTo()
        .assertIsDisplayed()
        .performTextInput("ModifiedSurname")

    onNodeWithTag(EditProfileScreenTestTags.INPUT_USERNAME)
        .performScrollTo()
        .assertIsDisplayed()
        .performTextClearance()
    onNodeWithTag(EditProfileScreenTestTags.INPUT_USERNAME)
        .performScrollTo()
        .assertIsDisplayed()
        .performTextInput("newUsername")

    onNodeWithTag(EditProfileScreenTestTags.SAVE)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()
  }

  private fun ComposeTestRule.checkFullSettingsScreenIsDisplayed() {
    checkNodeWithTagGetsDisplayed(SettingsScreenTestTags.GO_BACK_BUTTON)
    checkNodeWithTagGetsDisplayed(SettingsScreenTestTags.SCREEN_TITLE)
    checkNodeWithTagGetsDisplayed(SettingsScreenTestTags.USER_TYPE_SETTING)
    checkNodeWithTagGetsDisplayed(SettingsScreenTestTags.PROFESSIONAL_USER_TYPE_BUTTON)
    checkNodeWithTagGetsDisplayed(SettingsScreenTestTags.REGULAR_USER_TYPE_BUTTON)
    checkNodeWithTagGetsDisplayed(SettingsScreenTestTags.APPEARANCE_MODE_SETTING)
    checkNodeWithTagGetsDisplayed(SettingsScreenTestTags.AUTOMATIC_MODE_BUTTON)
    checkNodeWithTagGetsDisplayed(SettingsScreenTestTags.DARK_MODE_BUTTON)
    checkNodeWithTagGetsDisplayed(SettingsScreenTestTags.LIGHT_MODE_BUTTON)
    checkNodeWithTagGetsDisplayed(SettingsScreenTestTags.NOTIFICATIONS_TOGGLE)
    checkNodeWithTagGetsDisplayed(SettingsScreenTestTags.NOTIFICATIONS_SETTING)
    checkNodeWithTagGetsDisplayed(SettingsScreenTestTags.EDIT_PROFILE_SETTING)
    checkNodeWithTagGetsDisplayed(SettingsScreenTestTags.EDIT_PROFILE_BUTTON)
    checkNodeWithTagGetsDisplayed(SettingsScreenTestTags.SIGN_OUT_BUTTON)
    checkNodeWithTagGetsDisplayed(SettingsScreenTestTags.DELETE_ACCOUNT_BUTTON)
  }
}
