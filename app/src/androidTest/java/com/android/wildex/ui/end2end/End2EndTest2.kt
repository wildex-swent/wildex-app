package com.android.wildex.ui.end2end

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.report.Report
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.LoadingScreenTestTags
import com.android.wildex.ui.camera.CameraPermissionScreenTestTags
import com.android.wildex.ui.navigation.DEFAULT_TIMEOUT
import com.android.wildex.ui.navigation.NavigationTestUtils
import com.android.wildex.ui.profile.EditProfileScreenTestTags
import com.android.wildex.ui.profile.ProfileScreenTestTags
import com.android.wildex.ui.report.ReportScreenTestTags
import com.android.wildex.ui.report.SubmitReportFormScreenTestTags
import com.android.wildex.ui.settings.SettingsScreenTestTags
import com.android.wildex.utils.FirebaseEmulator
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test

@Ignore("Succeeding locally but failing on CI")
class End2EndTest2 : NavigationTestUtils() {

  @Test
  fun userFlow() {
    /* User authenticates, creates his profile, checks his profile, checks his settings,
    switches appearance to Dark mode and status to professional,
    goes back to Profile and then to Home Screen, check the camera tab,
    checks the report tab, sees all reports, tries to submit a form,
    backs out and goes back to report tab, check his profile, goes to settings and logs out.*/
    val user0 =
        User(
            "author0",
            "name0",
            "surname0",
            "username0",
            "regular user 0",
            "",
            UserType.REGULAR,
            Timestamp.now(),
            "country 0",
            0,
        )
    val user1 =
        User(
            "author1",
            "name1",
            "surname1",
            "username1",
            "regular user 1",
            "",
            UserType.REGULAR,
            Timestamp.now(),
            "country 1",
            0,
        )
    val report0 =
        Report(
            reportId = "report0",
            authorId = "author0",
            imageURL = "",
            location = Location(10.0, 10.0, "location0"),
            description = "description0",
            assigneeId = null,
            date = Timestamp.now(),
        )
    val report1 =
        Report(
            reportId = "report1",
            authorId = "author1",
            imageURL = "",
            location = Location(20.0, 20.0, "location1"),
            description = "description1",
            assigneeId = null,
            date = Timestamp.now(),
        )

    runBlocking {
      FirebaseEmulator.auth.signOut()
      initUser(user0)
      initUser(user1)
      RepositoryProvider.reportRepository.addReport(report0)
      RepositoryProvider.reportRepository.addReport(report1)
    }

    composeRule.waitForIdle()
    composeRule.checkAuthScreenIsDisplayed()
    composeRule.navigateFromAuth()
    composeRule.waitForIdle()
    composeRule.checkEditProfileScreenIsDisplayed(isNewUser = true)
    val nameNode = composeRule.onNodeWithTag(EditProfileScreenTestTags.INPUT_NAME)
    val surnameNode = composeRule.onNodeWithTag(EditProfileScreenTestTags.INPUT_SURNAME)
    val usernameNode = composeRule.onNodeWithTag(EditProfileScreenTestTags.INPUT_USERNAME)
    val saveNode = composeRule.onNodeWithTag(EditProfileScreenTestTags.SAVE)
    composeRule.waitUntil(DEFAULT_TIMEOUT) {
      nameNode.isDisplayed() ||
          surnameNode.isDisplayed() ||
          usernameNode.isDisplayed() ||
          saveNode.isDisplayed()
    }
    nameNode.performScrollTo().performClick().performTextInput("Name1")
    surnameNode.performScrollTo().performClick().performTextInput("Surname1")
    usernameNode.performScrollTo().performClick().performTextInput("Username1")
    saveNode.performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()

    composeRule.waitUntilAfterLoadingScreen()
    composeRule.navigateToMyProfileScreenFromHome()
    composeRule.waitUntilAfterLoadingScreen()
    composeRule.waitForIdle()
    composeRule.checkMyProfileScreenIsDisplayed()
    composeRule.waitUntilAfterLoadingScreen()
    composeRule.navigateToSettingsScreenFromProfile()
    composeRule.waitUntilAfterLoadingScreen()
    composeRule.waitForIdle()
    composeRule.switchAndCheckProfileSettings()

    composeRule.navigateBackFromSettings()
    composeRule.waitForIdle()
    composeRule.navigateBackFromProfile()
    composeRule.waitForIdle()

    composeRule.navigateToCameraScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.checkCameraScreenIsDisplayed()
    composeRule.checkFullCameraPermissionScreenIsDisplayed()

    composeRule.navigateToReportScreenFromBottomBar()
    composeRule.waitForIdle()
    composeRule.waitUntilAfterLoadingScreen()
    composeRule.checkFullReportScreenIsDisplayed(listOf(report0, report1))

    composeRule.navigateToSubmitReportScreenFromReport()
    composeRule.waitForIdle()
    composeRule.checkSubmitReportScreenIsDisplayed()
    composeRule.checkFullSubmitScreenIsDisplayed()
    composeRule.navigateBackFromSubmitReport()
    composeRule.waitForIdle()

    composeRule.checkReportScreenIsDisplayed()
    composeRule.waitUntilAfterLoadingScreen()
    composeRule.navigateToMyProfileScreenFromReport(Firebase.auth.uid!!)
    composeRule.waitUntilAfterLoadingScreen()
    composeRule.waitForIdle()

    composeRule.checkMyProfileScreenIsDisplayed()
    composeRule.waitUntilAfterLoadingScreen()
    composeRule.navigateToSettingsScreenFromProfile()
    composeRule.waitUntilAfterLoadingScreen()

    composeRule.waitForIdle()
    composeRule.navigateFromSettingsScreen_LogOut()
    composeRule.waitForIdle()
    composeRule.checkAuthScreenIsDisplayed()
  }

  private suspend fun initUser(user: User) {
    RepositoryProvider.userRepository.addUser(user)
    RepositoryProvider.userSettingsRepository.initializeUserSettings(user.userId)
    RepositoryProvider.userAchievementsRepository.initializeUserAchievements(user.userId)
    RepositoryProvider.userAnimalsRepository.initializeUserAnimals(user.userId)
  }

  private fun ComposeTestRule.checkMyProfileScreenIsDisplayed() {
    checkProfileScreenIsDisplayed(FirebaseEmulator.auth.uid!!)

    waitUntil { onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN).isNotDisplayed() }
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.PROFILE_NAME)
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.PROFILE_USERNAME)
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.PROFILE_DESCRIPTION)
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.MAP)
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.MAP_CTA)
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.COLLECTION)
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.FRIENDS)
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.ACHIEVEMENTS)
    checkNodeWithTagScrollAndDisplay(ProfileScreenTestTags.ACHIEVEMENTS_CTA)
    onNodeWithTag(ProfileScreenTestTags.SETTINGS).assertIsDisplayed()
  }

  private fun ComposeTestRule.checkNodeWithTagScrollAndDisplay(tag: String) {
    onNodeWithTag(tag, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
  }

  private fun ComposeTestRule.waitUntilAfterLoadingScreen() {
    waitUntil { onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN).isNotDisplayed() }
  }

  private fun ComposeTestRule.switchAndCheckProfileSettings() {
    onNodeWithTag(SettingsScreenTestTags.AUTOMATIC_MODE_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertIsSelected()
    onNodeWithTag(SettingsScreenTestTags.DARK_MODE_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()
    onNodeWithTag(SettingsScreenTestTags.DARK_MODE_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertIsSelected()

    onNodeWithTag(SettingsScreenTestTags.NOTIFICATIONS_TOGGLE, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertIsToggleable()

    onNodeWithTag(SettingsScreenTestTags.REGULAR_USER_STATUS_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertIsSelected()
    onNodeWithTag(SettingsScreenTestTags.PROFESSIONAL_USER_STATUS_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()
    waitUntil {
      onNodeWithTag(SettingsScreenTestTags.PROFESSIONAL_USER_STATUS_BUTTON, useUnmergedTree = true)
          .isDisplayed()
    }
    onNodeWithTag(SettingsScreenTestTags.PROFESSIONAL_USER_STATUS_BUTTON, useUnmergedTree = true)
        .assertIsSelected()
  }

  private fun ComposeTestRule.checkFullCameraPermissionScreenIsDisplayed() {
    onNodeWithTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_UPLOAD_BUTTON)
        .assertIsDisplayed()
    onNodeWithTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_MESSAGE_1).assertIsDisplayed()
    onNodeWithTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_MESSAGE_2).assertIsDisplayed()
    onNodeWithTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_BUTTON).assertIsDisplayed()
    onNodeWithTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_CAMERA_ICON).assertIsDisplayed()
  }

  private fun ComposeTestRule.checkFullReportScreenIsDisplayed(reports: List<Report>) {
    onNodeWithTag(ReportScreenTestTags.SUBMIT_REPORT, useUnmergedTree = true).assertIsDisplayed()
    onNodeWithTag(ReportScreenTestTags.SCREEN_TITLE, useUnmergedTree = true).assertIsDisplayed()
    onNodeWithTag(ReportScreenTestTags.REPORT_LIST, useUnmergedTree = true).assertIsDisplayed()
    reports.forEach {
      onNodeWithTag(
              ReportScreenTestTags.testTagForReport(it.reportId, "full"),
              useUnmergedTree = true,
          )
          .assertIsDisplayed()
      onNodeWithTag(
              ReportScreenTestTags.testTagForProfilePicture(it.authorId, "author"),
              useUnmergedTree = true,
          )
          .assertIsDisplayed()
    }
  }

  private fun ComposeTestRule.checkFullSubmitScreenIsDisplayed() {
    onNodeWithTag(SubmitReportFormScreenTestTags.CAMERA_ICON, useUnmergedTree = true)
        .assertIsDisplayed()
    onNodeWithTag(SubmitReportFormScreenTestTags.DESCRIPTION_FIELD, useUnmergedTree = true)
        .assertIsDisplayed()
    onNodeWithTag(SubmitReportFormScreenTestTags.SUBMIT_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
    onNodeWithTag(SubmitReportFormScreenTestTags.IMAGE_BOX, useUnmergedTree = true)
        .assertIsDisplayed()
    onNodeWithTag(SubmitReportFormScreenTestTags.TOP_APP_BAR_TEXT, useUnmergedTree = true)
        .assertIsDisplayed()
    onNodeWithTag(SubmitReportFormScreenTestTags.TOP_APP_BAR, useUnmergedTree = true)
        .assertIsDisplayed()
    onNodeWithTag(SubmitReportFormScreenTestTags.IMAGE_BOX, useUnmergedTree = true).performClick()
    checkFullCameraPermissionScreenIsDisplayed()
  }
}
