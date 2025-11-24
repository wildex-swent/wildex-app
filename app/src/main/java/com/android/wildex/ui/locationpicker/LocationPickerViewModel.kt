package com.android.wildex.ui.locationpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.location.GeocodingFeature
import com.android.wildex.model.location.GeocodingRepository
import com.android.wildex.model.location.PickedLocation
import com.android.wildex.model.utils.Location
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LocationPickerUiState(
    val center: Location = Location(46.5197, 6.6323, name = "Lausanne"),
    val selected: Location? = null,
    val searchQuery: String = "",
    val suggestions: List<GeocodingFeature> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showConfirmDialog: Boolean = false,
    val isLocationPermissionGranted: Boolean = false,
)

sealed interface LocationPickerEvent {
  data class Confirmed(val location: PickedLocation) : LocationPickerEvent
}

class LocationPickerViewModel(
    private val geocodingRepository: GeocodingRepository = RepositoryProvider.geocodingRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(LocationPickerUiState())
  val uiState: StateFlow<LocationPickerUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<LocationPickerEvent>()
  val events: SharedFlow<LocationPickerEvent> = _events

  private var searchJob: Job? = null

  fun onSearchQueryChanged(query: String) {
    _uiState.value =
        _uiState.value.copy(
            searchQuery = query,
        )
    searchJob?.cancel()
    if (query.length < 3) {
      _uiState.value = _uiState.value.copy(suggestions = emptyList())
      return
    }

    searchJob =
        viewModelScope.launch {
          delay(300)
          _uiState.value = _uiState.value.copy(isSearching = true)
          val results = geocodingRepository.searchSuggestions(query, limit = 5)
          _uiState.value =
              _uiState.value.copy(
                  isSearching = false,
                  suggestions = results,
              )
        }
  }

  fun onSuggestionClicked(feature: GeocodingFeature) {
    _uiState.value =
        _uiState.value.copy(
            searchQuery = feature.placeName ?: "",
            suggestions = emptyList(),
            selected = feature.toLocation(),
            center = feature.toLocation(),
            showConfirmDialog = true,
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
      _uiState.value = _uiState.value.copy(isLoading = true)
      val name = runCatching { geocodingRepository.reverseGeocode(lat, lon) }.getOrElse { null }
      val loc = Location(lat, lon, name ?: "($lat, $lon)")
      _uiState.value =
          _uiState.value.copy(
              selected = loc, center = loc, isLoading = false, showConfirmDialog = true)
    }
  }

  fun onSearchSubmitted(query: String) {
    if (query.isBlank()) return
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      val feature = runCatching { geocodingRepository.forwardGeocode(query) }.getOrElse { null }
      if (feature == null) {
        _uiState.value = _uiState.value.copy(isLoading = false, error = "No results for \"$query\"")
        return@launch
      }
      _uiState.value =
          _uiState.value.copy(
              center = feature.toLocation(),
              selected = feature.toLocation(),
              isLoading = false,
              showConfirmDialog = true)
    }
  }

  fun onConfirmDialogNo() {
    val current = _uiState.value
    _uiState.value =
        current.copy(
            showConfirmDialog = false,
            selected = null,
        )
  }

  fun onConfirmDialogYes() {
    val current = _uiState.value
    viewModelScope.launch {
      _events.emit(
          LocationPickerEvent.Confirmed(
              PickedLocation(
                  name = current.selected!!.name,
                  latitude = current.selected.latitude,
                  longitude = current.selected.longitude,
              )))
    }
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(error = null)
  }
}

private fun GeocodingFeature.toLocation(): Location {
  return Location(
      latitude = this.lat,
      longitude = this.lon,
      name = this.placeName!!,
  )
}
