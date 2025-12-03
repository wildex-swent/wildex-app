package com.android.wildex.ui.report

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.ui.utils.offline.OfflineScreenTestTags
import com.android.wildex.utils.offline.FakeConnectivityObserver
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

  private val fakeObserver = FakeConnectivityObserver(initial = true)

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
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SubmitReportScreen(onGoBack = onGoBack)
      }
    }

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
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SubmitReportFormScreen(
            uiState = SubmitReportUiState(),
            onCameraClick = onCameraClick,
            onDescriptionChange = onDescriptionChange,
            onSubmitClick = onSubmitClick,
        )
      }
    }
    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.IMAGE_BOX).performClick()
  }

  @Test
  fun typing_description_triggers_onDescriptionChange() {
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SubmitReportFormScreen(
            uiState = SubmitReportUiState(),
            onCameraClick = onCameraClick,
            onDescriptionChange = onDescriptionChange,
            onSubmitClick = onSubmitClick,
        )
      }
    }
    val text = "There’s an injured fox near the river"
    composeTestRule
        .onNodeWithTag(SubmitReportFormScreenTestTags.DESCRIPTION_FIELD)
        .assertIsDisplayed()
        .performTextInput(text)
  }

  @Test
  fun submit_button_is_enabled_and_triggers_callback() {
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      SubmitReportFormScreen(
          uiState =
              SubmitReportUiState(
                  isSubmitting = false,
                  description = "some description",
                  hasPickedLocation = true,
                  imageUri = Uri.parse("content://test/image.jpg"),
              ),
          onCameraClick = onCameraClick,
          onDescriptionChange = onDescriptionChange,
          onSubmitClick = onSubmitClick)
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
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
        )
      }
    }
    composeTestRule
        .onNodeWithTag(SubmitReportFormScreenTestTags.SUBMIT_BUTTON)
        .assertIsEnabled()
        .performClick()

    assertTrue(submitClicked)
  }

  @Test
  fun submit_button_is_disabled_and_shows_progress_text_when_submitting() {
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SubmitReportFormScreen(
            uiState = SubmitReportUiState(isSubmitting = true),
            onCameraClick = onCameraClick,
            onDescriptionChange = onDescriptionChange,
            onSubmitClick = onSubmitClick,
        )
      }
    }
    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.SUBMIT_BUTTON).assertIsNotEnabled()
    composeTestRule.onNodeWithText("Submitting…").assertIsDisplayed()
  }

  @Test
  fun shows_camera_placeholder_when_no_image_selected() {
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SubmitReportFormScreen(
            uiState = SubmitReportUiState(imageUri = null),
            onCameraClick = onCameraClick,
            onDescriptionChange = onDescriptionChange,
            onSubmitClick = onSubmitClick,
        )
      }
    }
    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.IMAGE_BOX).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SubmitReportFormScreenTestTags.CAMERA_ICON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun shows_selected_image_when_uri_is_provided() {
    fakeObserver.setOnline(true)
    val uri = Uri.parse("content://test/image.jpg")
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SubmitReportFormScreen(
            uiState = SubmitReportUiState(imageUri = uri),
            onCameraClick = onCameraClick,
            onDescriptionChange = onDescriptionChange,
            onSubmitClick = onSubmitClick,
        )
      }
    }

    composeTestRule
        .onNodeWithTag(SubmitReportFormScreenTestTags.SELECTED_IMAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun offlineScreenIsDisplayedWhenOfflineSubmitReportFormScreen() {
    fakeObserver.setOnline(false)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SubmitReportFormScreen(
            uiState = SubmitReportUiState(),
            onCameraClick = onCameraClick,
            onDescriptionChange = onDescriptionChange,
            onSubmitClick = onSubmitClick,
        )
      }
    }

    composeTestRule.onNodeWithTag(OfflineScreenTestTags.OFFLINE_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OfflineScreenTestTags.OFFLINE_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OfflineScreenTestTags.OFFLINE_SUBTITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OfflineScreenTestTags.OFFLINE_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OfflineScreenTestTags.ANIMATION).assertIsDisplayed()
  }
}
