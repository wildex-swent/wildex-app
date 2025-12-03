package com.android.wildex.ui.report

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.utils.Location
import com.android.wildex.ui.utils.offline.OfflineScreenTestTags
import com.android.wildex.utils.LocalRepositories
import com.android.wildex.utils.offline.FakeConnectivityObserver
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
  private val fakeObserver = FakeConnectivityObserver(initial = true)
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
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SubmitReportScreen(viewModel = viewModel, onSubmitted = onSubmitted, onGoBack = onGoBack)
      }
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
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SubmitReportScreen(viewModel = viewModel, onSubmitted = onSubmitted, onGoBack = onGoBack)
      }
    }

    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.BACK_BUTTON).performClick()
    assert(goBackClicked)
  }

  @Test
  fun submitButton_click_triggers_submitReport() {
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SubmitReportScreen(viewModel = viewModel, onSubmitted = onSubmitted, onGoBack = onGoBack)
      }
    }

    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.SUBMIT_BUTTON).assertIsNotEnabled()

    viewModel.updateDescription("some description")
    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.SUBMIT_BUTTON).assertIsNotEnabled()

    viewModel.onLocationPicked(Location(0.0, 0.0, "Paris"))
    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.SUBMIT_BUTTON).assertIsNotEnabled()

    viewModel.updateImage(Uri.parse("content://sample/image.jpg"))
    composeTestRule.onNodeWithTag(SubmitReportFormScreenTestTags.SUBMIT_BUTTON).assertIsEnabled()
  }

  @Test
  fun showsSelectedImage_whenImageUriPresent() {
    fakeObserver.setOnline(true)
    viewModel.updateImage(Uri.parse("content://sample/image.jpg"))
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SubmitReportScreen(viewModel = viewModel, onSubmitted = onSubmitted, onGoBack = onGoBack)
      }
    }

    composeTestRule
        .onNodeWithTag(SubmitReportFormScreenTestTags.SELECTED_IMAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun offlineScreenIsDisplayedWhenOfflineSubmitReportScreen() {
    fakeObserver.setOnline(false)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        SubmitReportScreen(viewModel = viewModel, onSubmitted = onSubmitted, onGoBack = onGoBack)
      }
    }
    composeTestRule.onNodeWithTag(OfflineScreenTestTags.OFFLINE_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OfflineScreenTestTags.OFFLINE_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OfflineScreenTestTags.OFFLINE_SUBTITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OfflineScreenTestTags.OFFLINE_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OfflineScreenTestTags.ANIMATION).assertIsDisplayed()
  }
}
