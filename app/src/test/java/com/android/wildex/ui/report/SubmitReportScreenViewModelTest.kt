package com.android.wildex.ui.report

import android.location.Location
import android.net.Uri
import com.android.wildex.model.location.GeocodingRepository
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.utils.MainDispatcherRule
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
  private lateinit var geocodingRepository: GeocodingRepository
  private lateinit var locationClient: FusedLocationProviderClient
  private lateinit var viewModel: SubmitReportScreenViewModel

  private val fakeImageUri = mockk<Uri>(relaxed = true)

  @Before
  fun setUp() {
    reportRepository = mockk()
    storageRepository = mockk()
    locationClient = mockk()
    geocodingRepository = mockk()
    viewModel =
        SubmitReportScreenViewModel(
            reportRepository = reportRepository,
            storageRepository = storageRepository,
            geocodingRepository = geocodingRepository,
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
        assertEquals("Location permission is required to submit a report.", state.errorMsg)
      }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun submitReport_success_resetsUiState() =
      mainDispatcherRule.runTest {
        viewModel.updateImage(fakeImageUri)
        viewModel.updateDescription("Some description")
        val fakeReportId = "r123"
        val fakeImageUrl = "https://fakeurl.com/img.jpg"

        // Mock the FusedLocationProviderClient to return an Android Location
        val androidLocation = mockk<Location>(relaxed = true)
        every { androidLocation.latitude } returns 12.0
        every { androidLocation.longitude } returns 13.0

        every { locationClient.lastLocation } returns Tasks.forResult(androidLocation)
        coEvery { geocodingRepository.reverseGeocode(any(), any()) } returns
            com.android.wildex.model.utils.Location(12.0, 13.0, "Test location")

        viewModel.fetchUserLocation(locationClient)
        advanceUntilIdle()

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

        // Mock Android location and Task to set the location via fetchUserLocation
        val androidLocation = mockk<Location>(relaxed = true)
        every { androidLocation.latitude } returns 1.0
        every { androidLocation.longitude } returns 2.0

        every { locationClient.lastLocation } returns Tasks.forResult(androidLocation)
        coEvery { geocodingRepository.reverseGeocode(any(), any()) } returns
            com.android.wildex.model.utils.Location(1.0, 2.0, "Test location")

        viewModel.fetchUserLocation(locationClient)
        advanceUntilIdle()

        coEvery { reportRepository.getNewReportId() } throws Exception("Network error")

        viewModel.submitReport {}
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.errorMsg!!.contains("Network error"))
        assertFalse(state.isSubmitting)
      }

  // New tests for fetchUserLocation
  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun fetchUserLocation_success_setsLocation() =
      mainDispatcherRule.runTest {
        val androidLocation = mockk<Location>(relaxed = true)
        every { androidLocation.latitude } returns 10.0
        every { androidLocation.longitude } returns 20.0

        every { locationClient.lastLocation } returns Tasks.forResult(androidLocation)

        viewModel.fetchUserLocation(locationClient)
        coEvery { geocodingRepository.reverseGeocode(any(), any()) } returns
            com.android.wildex.model.utils.Location(10.0, 20.0, "Test location")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.location)
        assertEquals(10.0, state.location!!.latitude)
        assertEquals(20.0, state.location!!.longitude)
      }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun fetchUserLocation_nullLocation_setsError() =
      mainDispatcherRule.runTest {
        every { locationClient.lastLocation } returns Tasks.forResult(null)

        viewModel.fetchUserLocation(locationClient)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.errorMsg!!.contains("Unable to fetch current location."))
      }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun fetchUserLocation_failure_setsError() =
      mainDispatcherRule.runTest {
        val task = mockk<Task<Location>>()
        val ex = Exception("GPS failed")
        every { task.addOnSuccessListener(any<OnSuccessListener<Location>>()) } answers { task }
        every { task.addOnFailureListener(any<OnFailureListener>()) } answers
            {
              val listener = it.invocation.args[0] as OnFailureListener
              listener.onFailure(ex)
              task
            }

        every { locationClient.lastLocation } returns task

        viewModel.fetchUserLocation(locationClient)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMsg)
      }
}
