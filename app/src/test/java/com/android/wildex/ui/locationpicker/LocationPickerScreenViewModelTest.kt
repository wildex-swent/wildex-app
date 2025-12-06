package com.android.wildex.ui.locationpicker

import com.android.wildex.model.location.GeocodingRepository
import com.android.wildex.model.utils.Location
import com.android.wildex.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationPickerScreenViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()
  private lateinit var geocodingRepository: GeocodingRepository
  private lateinit var viewModel: LocationPickerViewModel

  @Before
  fun setup() {
    geocodingRepository = mockk(relaxed = true)
    viewModel = LocationPickerViewModel(geocodingRepository)
  }

  @Test
  fun initialStateCorrect() {
    val state = viewModel.uiState.value
    assertEquals(46.5197, state.center.latitude, 0.0)
    assertEquals(6.6323, state.center.longitude, 0.0)
    assertEquals("Lausanne", state.center.name)
    assertNull(state.selected)
    assertEquals("", state.searchQuery)
    assertTrue(state.suggestions.isEmpty())
    assertFalse(state.isSearching)
    assertFalse(state.isLoading)
    assertNull(state.error)
    assertFalse(state.isError)
    assertFalse(state.showConfirmDialog)
    assertFalse(state.isLocationPermissionGranted)
  }

  @Test
  fun searchQueryChangedHandlesShortQueryDebounceAndCancellation() =
      mainDispatcherRule.runTest {
        viewModel.onSearchQueryChanged("la")
        var state = viewModel.uiState.value
        assertEquals("la", state.searchQuery)
        assertTrue(state.suggestions.isEmpty())
        assertFalse(state.isSearching)
        coVerify(exactly = 0) { geocodingRepository.searchSuggestions(any(), any()) }

        val lausanneResults =
            listOf(
                Location(46.52, 6.63, "Lausanne"),
                Location(48.85, 2.35, "Paris"),
            )
        coEvery { geocodingRepository.searchSuggestions("lausanne", 5) } returns lausanneResults
        coEvery { geocodingRepository.searchSuggestions("lau", 5) } returns emptyList()

        viewModel.onSearchQueryChanged("lau")
        viewModel.onSearchQueryChanged("lausanne")

        advanceTimeBy(300)
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("lausanne", state.searchQuery)
        assertFalse(state.isSearching)
        assertEquals(lausanneResults, state.suggestions)

        coVerify(exactly = 0) { geocodingRepository.searchSuggestions("lau", 5) }
        coVerify(exactly = 1) { geocodingRepository.searchSuggestions("lausanne", 5) }
      }

  @Test
  fun suggestionAndConfirmDialogFlowWorks() {
    val loc = Location(10.0, 20.0, "Place X")

    viewModel.onSuggestionClicked(loc)
    var state = viewModel.uiState.value
    assertEquals(loc, state.selected)
    assertEquals(loc, state.center)
    assertEquals(loc.name, state.searchQuery)
    assertTrue(state.suggestions.isEmpty())
    assertTrue(state.showConfirmDialog)
    assertNull(state.error)
    assertFalse(state.isError)

    viewModel.onConfirmDialogNo()
    state = viewModel.uiState.value
    assertFalse(state.showConfirmDialog)
  }

  @Test
  fun permissionAndUserLocationUpdatesStateCorrectly() {
    viewModel.onLocationPermissionResult(true)
    assertTrue(viewModel.uiState.value.isLocationPermissionGranted)
    viewModel.onLocationPermissionResult(false)
    assertFalse(viewModel.uiState.value.isLocationPermissionGranted)

    viewModel.onUserLocationAvailable(1.2, 3.4)
    var center = viewModel.uiState.value.center
    assertEquals(1.2, center.latitude, 0.0)
    assertEquals(3.4, center.longitude, 0.0)

    val selected = Location(10.0, 20.0, "Selected")
    viewModel.onSuggestionClicked(selected)
    viewModel.onUserLocationAvailable(9.9, 9.9)
    center = viewModel.uiState.value.center
    assertEquals(selected, center)
  }

  @Test
  fun mapClickedHandlesNamedAndUnknownLocations() =
      mainDispatcherRule.runTest {
        coEvery { geocodingRepository.reverseGeocode(5.0, 6.0) } returns "Found"
        viewModel.onMapClicked(5.0, 6.0)
        advanceUntilIdle()
        var state = viewModel.uiState.value
        var selected = state.selected!!
        assertEquals("Found", selected.name)
        assertEquals(5.0, selected.latitude, 0.0)
        assertEquals(6.0, selected.longitude, 0.0)
        assertEquals(selected, state.center)
        assertTrue(state.showConfirmDialog)
        assertFalse(state.isLoading)
        assertTrue(state.suggestions.isEmpty())

        coEvery { geocodingRepository.reverseGeocode(1.0, 2.0) } returns null
        viewModel.onMapClicked(1.0, 2.0)
        advanceUntilIdle()
        state = viewModel.uiState.value
        selected = state.selected!!
        assertEquals(1.0, selected.latitude, 0.0)
        assertEquals(2.0, selected.longitude, 0.0)
        assertEquals("Unknown Location at: (1.0, 2.0)", selected.name)
        assertFalse(state.isLoading)
      }

  @Test
  fun searchSubmittedHandlesBlankErrorSuccessAndClearError() =
      mainDispatcherRule.runTest {
        viewModel.onSearchSubmitted("   ")
        coVerify(exactly = 0) { geocodingRepository.forwardGeocode(any()) }

        coEvery { geocodingRepository.forwardGeocode("abc") } returns null
        viewModel.onSearchSubmitted("abc")
        advanceUntilIdle()
        var state = viewModel.uiState.value
        assertTrue(state.isError)
        assertEquals("No results for \"abc\"", state.error)
        assertFalse(state.isLoading)

        viewModel.clearError()
        state = viewModel.uiState.value
        assertNull(state.error)
        assertFalse(state.isError)

        val loc = Location(10.0, 20.0, "Found")
        coEvery { geocodingRepository.forwardGeocode("lausanne") } returns loc
        viewModel.onSearchSubmitted("lausanne")
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(loc, state.selected)
        assertEquals(loc, state.center)
        assertTrue(state.showConfirmDialog)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.isError)
      }

  @Test
  fun confirmDialogYesCoversNullSelectedAndEmitsEventWhenSelected() =
      mainDispatcherRule.runTest {
        viewModel.onConfirmDialogYes()
        assertNull(viewModel.uiState.value.selected)

        val loc = Location(10.0, 20.0, "Selected")
        viewModel.onSuggestionClicked(loc)

        val eventDeferred = async { viewModel.events.first() }
        viewModel.onConfirmDialogYes()

        val event = eventDeferred.await()
        assertTrue(event is LocationPickerEvent.Confirmed)
        assertEquals(loc, (event as LocationPickerEvent.Confirmed).location)
      }

  @Test
  fun useCurrentLocationNameAsQuery_setsErrorWhenReverseGeocodeFails() =
      mainDispatcherRule.runTest {
        viewModel.onLocationPermissionResult(true)
        viewModel.onUserLocationAvailable(10.0, 20.0)
        coEvery { geocodingRepository.reverseGeocode(10.0, 20.0) } returns null
        viewModel.useCurrentLocationNameAsQuery()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.isError)
        assertEquals("Couldn't get your current location name", state.error)
        assertEquals("", state.searchQuery)
      }
}
