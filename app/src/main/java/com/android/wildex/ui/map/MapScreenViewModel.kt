package com.android.wildex.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.map.MapPin
import com.android.wildex.model.map.PinDetails
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Id
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MapTab {
  Posts,
  MyPosts,
  Reports
}

data class MapUIState(
    val availableTabs: List<MapTab> = emptyList(),
    val activeTab: MapTab = MapTab.Posts,
    val pins: List<MapPin> = emptyList(),
    val selected: PinDetails? = null,
    val isLoading: Boolean = true,
    val errorMsg: String? = null,
    val isError: Boolean = false,
    val isRefreshing: Boolean = false,
)

data class MapRenderState(
    val showUserLocation: Boolean = false,
    val recenterNonce: Long? = null,
    val renderError: String? = null,
)

class MapViewModel(
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val postsRepository: PostsRepository = RepositoryProvider.postRepository,
    private val likeRepository: LikeRepository = RepositoryProvider.likeRepository,
    // private val reportsRepository: ReportsRepository = RepositoryProvider.reportsRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(MapUIState())
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()
  private val _renderState = MutableStateFlow(MapRenderState())
  val renderState: StateFlow<MapRenderState> = _renderState.asStateFlow()

  fun loadUIState(currentUserId: Id) {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null, selected = null)
    viewModelScope.launch { updateUIState(currentUserId) }
  }

  fun refreshUIState(currentUserId: Id) {
    _uiState.value = _uiState.value.copy(isRefreshing = true, errorMsg = null)
    viewModelScope.launch { updateUIState(currentUserId) }
  }

  private suspend fun updateUIState(currentUserId: Id) {
    // TODO: implement
  }

  fun onTabSelected(tab: MapTab, currentUserId: Id) {
    // TODO: implement
  }

  fun onPinSelected(pinId: Id) {
    // TODO: implement
  }

  fun clearSelection() {
    _uiState.value = _uiState.value.copy(selected = null)
  }

  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  private suspend fun loadPostPins(all: Boolean, currentUserId: Id): List<MapPin> {
    // TODO: implement
    return emptyList()
  }

  private suspend fun loadReportPins(): List<MapPin> {
    // TODO when ReportsRepository exists
    return emptyList()
  }

  // render intents the Composable will apply

  /** Call from UI when Android permission dialog resolves. */
  fun onLocationPermissionResult(granted: Boolean) {
    _renderState.value = _renderState.value.copy(showUserLocation = granted)
  }

  /** Ask UI to recenter once (UI will consume this nonce). */
  fun requestRecenter() {
    _renderState.value = _renderState.value.copy(recenterNonce = System.currentTimeMillis())
  }

  /** UI calls this after performing the recenter camera change. */
  fun consumeRecenter() {
    if (_renderState.value.recenterNonce != null) {
      _renderState.value = _renderState.value.copy(recenterNonce = null)
    }
  }

  fun clearRenderError() {
    _renderState.value = _renderState.value.copy(renderError = null)
  }
}
