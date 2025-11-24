package com.android.wildex.ui.locationpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.location.GeocodingRepository
import com.android.wildex.model.location.PickedLocation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LocationPickerUiState(
    val centerLat: Double = 46.5197,
    val centerLon: Double = 6.6323,
    val selectedLat: Double? = null,
    val selectedLon: Double? = null,
    val selectedName: String? = null,
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

  fun onLocationPermissionResult(granted: Boolean) {
    _uiState.value = _uiState.value.copy(isLocationPermissionGranted = granted)
  }

  fun onUserLocationAvailable(latitude: Double, longitude: Double) {
    val current = _uiState.value
    if (current.selectedLat == null && current.selectedLon == null) {
      _uiState.value = current.copy(centerLat = latitude, centerLon = longitude)
    }
  }

  fun onMapClicked(lat: Double, lon: Double) {
    viewModelScope.launch {
      _uiState.value =
          _uiState.value.copy(
              selectedLat = lat,
              selectedLon = lon,
              isLoading = true,
              error = null,
          )

      val name =
          runCatching { geocodingRepository.reverseGeocode(lat = lat, lon = lon) }
              .getOrElse { null }

      _uiState.value =
          _uiState.value.copy(
              selectedName = name ?: "($lat, $lon)",
              isLoading = false,
              showConfirmDialog = true,
          )
    }
  }

  fun onSearchSubmitted(query: String) {
    if (query.isBlank()) return

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, error = null)

      val feature = runCatching { geocodingRepository.forwardGeocode(query) }.getOrElse { null }

      if (feature == null) {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                error = "No results for \"$query\"",
            )
        return@launch
      }

      val lat = feature.lat
      val lon = feature.lon

      _uiState.value =
          _uiState.value.copy(
              centerLat = lat,
              centerLon = lon,
              selectedLat = lat,
              selectedLon = lon,
              selectedName = feature.placeName ?: query,
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
            selectedLat = null,
            selectedLon = null,
            selectedName = null,
        )
  }

  fun onConfirmDialogYes() {
    val current = _uiState.value
    val lat = current.selectedLat ?: return
    val lon = current.selectedLon ?: return
    val name = current.selectedName ?: "($lat, $lon)"

    viewModelScope.launch {
      _events.emit(
          LocationPickerEvent.Confirmed(
              PickedLocation(
                  name = name,
                  latitude = lat,
                  longitude = lon,
              )))
    }
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(error = null)
  }
}
