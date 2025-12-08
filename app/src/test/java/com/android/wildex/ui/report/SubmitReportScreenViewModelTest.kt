package com.android.wildex.ui.report

import android.net.Uri
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.utils.Location
import com.android.wildex.utils.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SubmitReportScreenViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var reportRepository: ReportRepository
  private lateinit var storageRepository: StorageRepository
  private lateinit var viewModel: SubmitReportScreenViewModel

  private val fakeImageUri = mockk<Uri>(relaxed = true)

  @Before
  fun setUp() {
    reportRepository = mockk()
    storageRepository = mockk()
    viewModel =
        SubmitReportScreenViewModel(
            reportRepository = reportRepository,
            storageRepository = storageRepository,
            currentUserId = "testUser",
        )
  }

  @Test
  fun initialState_isDefault() {
    val state = viewModel.uiState.value
    assertNull(state.imageUri)
    assertEquals("", state.description)
    assertNull(state.location)
    assertFalse(state.isSubmitting)
    assertNull(state.errorMsg)
  }

  @Test
  fun updateDescription_updatesStateCorrectly() {
    viewModel.updateDescription("Proper description")
    assertEquals("Proper description", viewModel.uiState.value.description)
  }

  @Test
  fun updateImage_updatesStateCorrectly() {
    viewModel.updateImage(fakeImageUri)
    assertEquals(fakeImageUri, viewModel.uiState.value.imageUri)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun clearErrorMsg_setsErrorToNull() =
      mainDispatcherRule.runTest {
        viewModel.updateDescription("")
        viewModel.updateImage(mockk(relaxed = true))
        viewModel.submitReport {}
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMsg)
        viewModel.clearErrorMsg()
        assertNull(viewModel.uiState.value.errorMsg)
      }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun submitReport_setsError_whenImageMissing() =
      mainDispatcherRule.runTest {
        viewModel.updateDescription("Some description")
        viewModel.updateImage(null)
        viewModel.updateDescription("Test report")
        viewModel.updateImage(null)
        viewModel.updateDescription("Test")
        viewModel.updateDescription("desc")

        viewModel.submitReport {}

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("Please provide an image for the report.", state.errorMsg)
      }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun submitReport_setsError_whenDescriptionBlank() =
      mainDispatcherRule.runTest {
        viewModel.updateImage(fakeImageUri)
        viewModel.updateDescription("")
        viewModel.submitReport {}

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("Please provide a description for the report.", state.errorMsg)
      }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun submitReport_setsError_whenLocationMissing() =
      mainDispatcherRule.runTest {
        viewModel.updateImage(fakeImageUri)
        viewModel.updateDescription("A proper description")
        viewModel.submitReport {}

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("Location is required to submit a report.", state.errorMsg)
      }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun submitReport_success_resetsUiState() =
      mainDispatcherRule.runTest {
        viewModel.updateImage(fakeImageUri)
        viewModel.updateDescription("Some description")
        viewModel.updateLocation(Location(46.5197, 6.6323, name = "Lausanne"))
        val fakeReportId = "r123"
        val fakeImageUrl = "https://fakeurl.com/img.jpg"

        coEvery { reportRepository.getNewReportId() } returns fakeReportId
        coEvery { storageRepository.uploadReportImage(fakeReportId, fakeImageUri) } returns
            fakeImageUrl
        coEvery { reportRepository.addReport(any()) } just Runs

        viewModel.submitReport {}
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMsg)
        assertFalse(state.isSubmitting)
        assertNull(state.imageUri)
        assertEquals("", state.description)
        assertNull(state.location)

        coVerify(exactly = 1) { reportRepository.addReport(any()) }
        coVerify { storageRepository.uploadReportImage(fakeReportId, fakeImageUri) }
      }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun submitReport_setsError_onException() =
      mainDispatcherRule.runTest {
        viewModel.updateImage(fakeImageUri)
        viewModel.updateDescription("Test desc")
        viewModel.updateLocation(Location(46.5197, 6.6323, name = "Lausanne"))

        coEvery { reportRepository.getNewReportId() } throws Exception("Network error")

        viewModel.submitReport {}
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.errorMsg!!.contains("Network error"))
        assertFalse(state.isSubmitting)
      }
}
