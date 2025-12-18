package com.android.wildex.ui.end2end

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.report.Report
import com.android.wildex.model.user.OnBoardingStage
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.LoadingScreenTestTags
import com.android.wildex.ui.authentication.NamingScreenTestTags
import com.android.wildex.ui.authentication.OptionalInfoScreenTestTags
import com.android.wildex.ui.authentication.SignInScreenTestTags
import com.android.wildex.ui.authentication.UserTypeScreenTestTags
import com.android.wildex.ui.camera.CameraPermissionScreenTestTags
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.navigation.NavigationTestUtils
import com.android.wildex.ui.profile.ProfileScreenTestTags
import com.android.wildex.ui.report.ReportScreenTestTags
import com.android.wildex.ui.report.SubmitReportFormScreenTestTags
import com.android.wildex.ui.settings.SettingsScreenTestTags
import com.android.wildex.utils.FirebaseEmulator
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.runBlocking
import org.junit.Test

class End2EndTest2 : NavigationTestUtils() {

  @Test
  fun userFlow2() {
    val user0 =
        User(
            userId = "author0",
            username = "name0",
            name = "surname0",
            surname = "username0",
            bio = "regular user 0",
            profilePictureURL = "",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "country 0",
            onBoardingStage = OnBoardingStage.COMPLETE,
        )
    val user1 =
        User(
            userId = "author1",
            username = "name1",
            name = "surname1",
            surname = "username1",
            bio = "regular user 1",
            profilePictureURL = "",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "country 1",
            onBoardingStage = OnBoardingStage.COMPLETE,
        )
    val report0 =
        Report(
            reportId = "report0",
            authorId = "author0",
            imageURL = "",
            location = Location(10.0, 10.0, "location0", "address0", "country0"),
            description = "description0",
            assigneeId = null,
            date = Timestamp.now(),
        )
    val report1 =
        Report(
            reportId = "report1",
            authorId = "author1",
            imageURL = "",
            location = Location(20.0, 20.0, "location1", "address1", "country1"),
            description = "description1",
            assigneeId = null,
            date = Timestamp.now(),
        )

    runBlocking {
      initUser(user0)
      initUser(user1)
      RepositoryProvider.reportRepository.addReport(report0)
      RepositoryProvider.reportRepository.addReport(report1)
    }

    composeRule.waitForIdle()
    composeRule.checkHomeScreenIsDisplayed()

    composeRule.navigateToMyProfileScreenFromHome()

    composeRule.waitForIdle()
    composeRule.checkMyProfileScreenIsDisplayed()

    composeRule.navigateToSettingsScreenFromProfile()
    composeRule.waitForIdle()
    composeRule.checkSettingsScreenIsDisplayed()
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
    composeRule.checkReportScreenIsDisplayed()
    composeRule.checkFullReportScreenIsDisplayed(listOf(report0, report1))

    composeRule.navigateToSubmitReportScreenFromReport()
    composeRule.waitForIdle()
    composeRule.checkSubmitReportScreenIsDisplayed()
    composeRule.checkFullSubmitScreenIsDisplayed()
    composeRule.navigateBackFromSubmitReport()
    composeRule.waitForIdle()

    composeRule.checkReportScreenIsDisplayed()

    composeRule.navigateToMyProfileScreenFromReport()

    composeRule.waitForIdle()

    composeRule.checkMyProfileScreenIsDisplayed()

    composeRule.navigateToSettingsScreenFromProfile()

    composeRule.waitForIdle()
    composeRule.navigateFromSettingsScreenToAuthScreen_LogOut()
    composeRule.waitForIdle()
    composeRule.checkAuthScreenIsDisplayed()
  }

  private suspend fun initUser(user: User) {
    RepositoryProvider.userRepository.addUser(user)
    RepositoryProvider.userSettingsRepository.initializeUserSettings(user.userId)
    RepositoryProvider.userAchievementsRepository.initializeUserAchievements(user.userId)
    RepositoryProvider.userAnimalsRepository.initializeUserAnimals(user.userId)
    RepositoryProvider.userFriendsRepository.initializeUserFriends(user.userId)
    RepositoryProvider.userTokensRepository.initializeUserTokens(user.userId)
  }

  private fun ComposeTestRule.checkMyProfileScreenIsDisplayed() {
    checkProfileScreenIsDisplayed(FirebaseEmulator.auth.uid!!)

    waitUntil { onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN).isNotDisplayed() }
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.PROFILE_NAME)
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.PROFILE_USERNAME)
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.PROFILE_DESCRIPTION)
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.MAP)
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.MAP_CTA)
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.COLLECTION)
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.FRIENDS)
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.ACHIEVEMENTS)
    checkNodeWithTagGetsDisplayed(ProfileScreenTestTags.ACHIEVEMENTS_CTA)
    onNodeWithTag(ProfileScreenTestTags.SETTINGS).assertIsDisplayed()
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

    onNodeWithTag(SettingsScreenTestTags.REGULAR_USER_TYPE_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
    onNodeWithTag(SettingsScreenTestTags.PROFESSIONAL_USER_TYPE_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
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
    onNodeWithTag(ReportScreenTestTags.MORE_ACTIONS_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()
    onNodeWithTag(ReportScreenTestTags.SUBMIT_REPORT_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
    onNodeWithTag(ReportScreenTestTags.MORE_ACTIONS_BUTTON, useUnmergedTree = true).performClick()
    onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE, useUnmergedTree = true).assertIsDisplayed()
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
