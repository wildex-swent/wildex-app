package com.android.wildex.ui.report

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.ui.camera.CameraPermissionScreenTestTags
import com.android.wildex.ui.camera.CameraPreviewScreenTestTags
import com.android.wildex.utils.LocalRepositories
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SubmitReportScreenTest {

  @get:Rule val composeTestRule = createComposeRule()
  private lateinit var context: Context
  private lateinit var reportRepository: ReportRepository
  private lateinit var storageRepository: StorageRepository
  private lateinit var viewModel: SubmitReportScreenViewModel

  private lateinit var onSubmitted: () -> Unit
  private lateinit var onGoBack: () -> Unit
  private lateinit var fakeStateFlow: MutableStateFlow<SubmitReportUiState>
  private var submitClicked = false
  private var goBackClicked = false

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    reportRepository = LocalRepositories.reportRepository
    storageRepository = LocalRepositories.storageRepository
    viewModel =
        SubmitReportScreenViewModel(
            reportRepository = reportRepository,
            storageRepository = storageRepository,
            currentUserId = "testUser",
        )
    onSubmitted = { submitClicked = true }
    onGoBack = { goBackClicked = true }
    fakeStateFlow = MutableStateFlow(SubmitReportUiState())
  }

  @Test
  fun displaysFormScreen_whenLaunched() {
    composeTestRule.setContent {
      SubmitReportScreen(viewModel = viewModel, onSubmitted = onSubmitted, onGoBack = onGoBack)
    }

    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.TOP_APP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.IMAGE_BOX).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SubmitReportFormScreenTestTags.DESCRIPTION_FIELD)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.SUBMIT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun clickingBackButton_calls_onNavigateBack() {
    composeTestRule.setContent {
      SubmitReportScreen(viewModel = viewModel, onSubmitted = onSubmitted, onGoBack = onGoBack)
    }

    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.BACK_BUTTON).performClick()
    assert(goBackClicked)
  }

  @Test
  fun submitButton_click_triggers_submitReport() {
    composeTestRule.setContent {
      SubmitReportScreen(viewModel = viewModel, onSubmitted = onSubmitted, onGoBack = onGoBack)
    }

    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.SUBMIT_BUTTON).assertIsNotEnabled()

    viewModel.updateDescription("some description")
    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.SUBMIT_BUTTON).assertIsNotEnabled()

    viewModel.updateImage(Uri.parse("content://sample/image.jpg"))
    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.SUBMIT_BUTTON).assertIsEnabled()
  }

  @Test
  fun showsSelectedImage_whenImageUriPresent() {
    viewModel.updateImage(Uri.parse("content://sample/image.jpg"))
    composeTestRule.setContent {
      SubmitReportScreen(viewModel = viewModel, onSubmitted = onSubmitted, onGoBack = onGoBack)
    }

    composeTestRule
        .onNodeWithTag(SubmitReportFormScreenTestTags.SELECTED_IMAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun showsCameraPermission_whenCameraClicked() {
    composeTestRule.setContent {
      SubmitReportScreen(
          viewModel = viewModel,
          onSubmitted = onSubmitted,
          onGoBack = onGoBack,
      )
    }

    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.IMAGE_BOX).performClick()
    if (!hasPermission()) {
      assertCameraPermissionScreenIsDisplayed()
    } else {
      assertCameraPreviewScreenIsDisplayed()
    }
  }

  private fun assertCameraPermissionScreenIsDisplayed() {
    composeTestRule
        .onNodeWithTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_CAMERA_ICON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_MESSAGE_1)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_MESSAGE_2)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_UPLOAD_BUTTON)
        .assertIsDisplayed()
  }

  private fun assertCameraPreviewScreenIsDisplayed() {
    composeTestRule.onNodeWithTag(CameraPreviewScreenTestTags.CAMERA_VIEWFINDER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraPreviewScreenTestTags.UPLOAD_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraPreviewScreenTestTags.CAPTURE_BUTTON).assertIsDisplayed()
  }

  private fun hasPermission(): Boolean {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
  }
}
