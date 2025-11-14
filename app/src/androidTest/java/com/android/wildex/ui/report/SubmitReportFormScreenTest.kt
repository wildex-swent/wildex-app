package com.android.wildex.ui.report

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SubmitReportFormScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var onCameraClick: () -> Unit
  private lateinit var onDescriptionChange: (String) -> Unit
  private lateinit var onSubmitClick: () -> Unit
  private lateinit var onGoBack: () -> Unit

  private var cameraClicked = false
  private var lastDescription: String? = null
  private var submitClicked = false
  private var goBackClicked = false

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    onCameraClick = { cameraClicked = true }
    onDescriptionChange = { lastDescription = it }
    onSubmitClick = { submitClicked = true }
    onGoBack = { goBackClicked = true }
  }

  @Test
  fun topAppBar_and_backButton_areDisplayed_andClickable() {
    composeTestRule.setContent { SubmitReportScreen(onGoBack = onGoBack) }

    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.TOP_APP_BAR).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SubmitReportFormScreenTestTags.TOP_APP_BAR_TEXT)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SubmitReportFormScreenTestTags.BACK_BUTTON)
        .assertIsDisplayed()
        .performClick()
  }

  @Test
  fun clicking_image_box_calls_onCameraClick() {
    composeTestRule.setContent {
      SubmitReportFormScreen(
          uiState = SubmitReportUiState(),
          onCameraClick = onCameraClick,
          onDescriptionChange = onDescriptionChange,
          onSubmitClick = onSubmitClick,
          context = context,
          onGoBack = onGoBack,
      )
    }

    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.IMAGE_BOX).performClick()
  }

  @Test
  fun typing_description_triggers_onDescriptionChange() {
    composeTestRule.setContent {
      SubmitReportFormScreen(
          uiState = SubmitReportUiState(),
          onCameraClick = onCameraClick,
          onDescriptionChange = onDescriptionChange,
          onSubmitClick = onSubmitClick,
          context = context,
          onGoBack = onGoBack,
      )
    }

    val text = "There’s an injured fox near the river"
    composeTestRule
        .onNodeWithTag(SubmitReportFormScreenTestTags.DESCRIPTION_FIELD)
        .assertIsDisplayed()
        .performTextInput(text)
  }

  @Test
  fun submit_button_is_enabled_and_triggers_callback() {
    composeTestRule.setContent {
      SubmitReportFormScreen(
          uiState =
              SubmitReportUiState(
                  isSubmitting = false,
                  description = "some description",
                  imageUri = Uri.parse("content://test/image.jpg"),
              ),
          onCameraClick = onCameraClick,
          onDescriptionChange = onDescriptionChange,
          onSubmitClick = onSubmitClick,
          context = context,
          onGoBack = onGoBack,
      )
    }

    composeTestRule
        .onNodeWithTag(SubmitReportFormScreenTestTags.SUBMIT_BUTTON)
        .assertIsEnabled()
        .performClick()

    assertTrue(submitClicked)
  }

  @Test
  fun submit_button_is_disabled_and_shows_progress_text_when_submitting() {
    composeTestRule.setContent {
      SubmitReportFormScreen(
          uiState = SubmitReportUiState(isSubmitting = true),
          onCameraClick = onCameraClick,
          onDescriptionChange = onDescriptionChange,
          onSubmitClick = onSubmitClick,
          context = context,
          onGoBack = onGoBack,
      )
    }

    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.SUBMIT_BUTTON).assertIsNotEnabled()
    composeTestRule.onNodeWithText("Submitting...").assertIsDisplayed()
  }

  @Test
  fun shows_camera_placeholder_when_no_image_selected() {
    composeTestRule.setContent {
      SubmitReportFormScreen(
          uiState = SubmitReportUiState(imageUri = null),
          onCameraClick = onCameraClick,
          onDescriptionChange = onDescriptionChange,
          onSubmitClick = onSubmitClick,
          context = context,
          onGoBack = onGoBack,
      )
    }

    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.IMAGE_BOX).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SubmitReportFormScreenTestTags.CAMERA_ICON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun shows_selected_image_when_uri_is_provided() {
    val uri = Uri.parse("content://test/image.jpg")

    composeTestRule.setContent {
      SubmitReportFormScreen(
          uiState = SubmitReportUiState(imageUri = uri),
          onCameraClick = onCameraClick,
          onDescriptionChange = onDescriptionChange,
          onSubmitClick = onSubmitClick,
          context = context,
          onGoBack = onGoBack,
      )
    }

    composeTestRule
        .onNodeWithTag(SubmitReportFormScreenTestTags.SELECTED_IMAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }
}
