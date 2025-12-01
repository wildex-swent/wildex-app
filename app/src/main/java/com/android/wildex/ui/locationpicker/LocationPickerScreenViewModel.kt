package com.android.wildex.ui.locationpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.location.GeocodingRepository
import com.android.wildex.model.utils.Location
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LocationPickerUiState(
    val center: Location = Location(46.5197, 6.6323, name = "Lausanne"),
    val selected: Location? = null,
    val searchQuery: String = "",
    val suggestions: List<Location> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isError: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val isLocationPermissionGranted: Boolean = false,
)

sealed interface LocationPickerEvent {
  data class Confirmed(val location: Location) : LocationPickerEvent
}

class LocationPickerViewModel(
    private val geocodingRepository: GeocodingRepository = RepositoryProvider.geocodingRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(LocationPickerUiState())
  val uiState: StateFlow<LocationPickerUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<LocationPickerEvent>()
  val events: SharedFlow<LocationPickerEvent> = _events.asSharedFlow()

  private var searchJob: Job? = null

  fun onSearchQueryChanged(query: String) {
    _uiState.value = _uiState.value.copy(searchQuery = query, error = null, isError = false)
    searchJob?.cancel()
    if (query.length < 3) {
      _uiState.value = _uiState.value.copy(suggestions = emptyList(), isSearching = false)
      return
    }
    searchJob =
        viewModelScope.launch {
          // Looks weird but this is just to avoid making too many requests while the user is typing
          delay(300)
          _uiState.value = _uiState.value.copy(isSearching = true)
          val results = geocodingRepository.searchSuggestions(query, limit = 5)
          _uiState.value = _uiState.value.copy(isSearching = false, suggestions = results)
        }
  }

  fun onSuggestionClicked(feature: Location) {
    _uiState.value =
        _uiState.value.copy(
            searchQuery = feature.name,
            suggestions = emptyList(),
            selected = feature,
            center = feature,
            showConfirmDialog = true,
            error = null,
            isError = false,
        )
  }

  fun onLocationPermissionResult(granted: Boolean) {
    _uiState.value = _uiState.value.copy(isLocationPermissionGranted = granted)
  }

  fun onUserLocationAvailable(latitude: Double, longitude: Double) {
    val current = _uiState.value
    if (current.selected == null) {
      _uiState.value = current.copy(center = Location(latitude = latitude, longitude = longitude))
    }
  }

  fun onMapClicked(lat: Double, lon: Double) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, error = null, isError = false)
      val name = geocodingRepository.reverseGeocode(lat, lon)
      val loc = Location(lat, lon, name ?: "Unknown Location at: ($lat, $lon)")
      _uiState.value =
          _uiState.value.copy(
              selected = loc,
              center = loc,
              isLoading = false,
              showConfirmDialog = true,
              suggestions = emptyList(),
          )
    }
  }

  fun onSearchSubmitted(query: String) {
    if (query.isBlank()) return
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, error = null, isError = false)
      val feature = geocodingRepository.forwardGeocode(query)
      if (feature == null) {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                error = "No results for \"$query\"",
                isError = true,
            )
        return@launch
      }
      _uiState.value =
          _uiState.value.copy(
              center = feature,
              selected = feature,
              isLoading = false,
              showConfirmDialog = true,
          )
    }
  }

  fun onConfirmDialogNo() {
    val current = _uiState.value
    _uiState.value =
        current.copy(
            showConfirmDialog = false,
        )
  }

  fun onConfirmDialogYes() {
    val selected = _uiState.value.selected ?: return
    viewModelScope.launch { _events.emit(LocationPickerEvent.Confirmed(selected)) }
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(error = null, isError = false)
  }
}
