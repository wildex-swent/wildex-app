package com.android.wildex.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.map.MapPin
import com.android.wildex.model.map.PinDetails
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.social.Like
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MapTab {
  Posts,
  MyPosts,
  Reports,
}

data class MapUIState(
    val availableTabs: List<MapTab> = emptyList(),
    val activeTab: MapTab = MapTab.Posts,
    val pins: List<MapPin> = emptyList(),
    val selected: PinDetails? = null,
    val centerCoordinates: Location = Location(46.5197, 6.6323, "Lausanne"),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isError: Boolean = false,
    val errorMsg: String? = null
)

data class MapRenderState(
    val showUserLocation: Boolean = false,
    val recenterNonce: Long? = null,
    val renderError: String? = null,
)

/** Base VM shared by all map screens. */
abstract class BaseMapViewModel(
    protected val loggedInUserId: Id = Firebase.auth.uid ?: "",
    protected val userRepository: UserRepository = RepositoryProvider.userRepository,
    protected val postRepository: PostsRepository = RepositoryProvider.postRepository,
    protected val likeRepository: LikeRepository = RepositoryProvider.likeRepository,
    protected val reportRepository: ReportRepository = RepositoryProvider.reportRepository,
    protected val animalRepository: AnimalRepository = RepositoryProvider.animalRepository,
) : ViewModel() {

  protected val _uiState = MutableStateFlow(MapUIState())
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()

  protected val _renderState = MutableStateFlow(MapRenderState())
  val renderState: StateFlow<MapRenderState> = _renderState.asStateFlow()

  fun loadUIState() {
    _uiState.value =
        _uiState.value.copy(isLoading = true, isError = false, errorMsg = null, selected = null)
    viewModelScope.launch { reload() }
  }

  fun refreshUIState() {
    _uiState.value =
        _uiState.value.copy(
            isRefreshing = true, isError = false, errorMsg = null, isLoading = false)
    viewModelScope.launch { reload() }
  }

  protected abstract suspend fun reload()

  fun onTabSelected(tab: MapTab) {
    if (tab !in _uiState.value.availableTabs) return
    _uiState.value = _uiState.value.copy(activeTab = tab, selected = null)
    refreshUIState()
  }

  fun onPinSelected(pinId: Id) {
    viewModelScope.launch {
      val pin = _uiState.value.pins.firstOrNull { it.id == pinId } ?: return@launch
      try {
        when (pin) {
          is MapPin.PostPin -> {
            val post = postRepository.getPost(pin.id)
            val author = runCatching { userRepository.getSimpleUser(post.authorId) }.getOrNull()
            val liked =
                runCatching { likeRepository.getLikeForPost(post.postId) != null }
                    .getOrDefault(false)
            val animalName =
                try {
                  animalRepository.getAnimal(post.animalId).name
                } catch (_: Exception) {
                  "animal"
                }
            _uiState.value =
                _uiState.value.copy(
                    selected = PinDetails.PostDetails(post, author, liked, animalName))
          }
          is MapPin.ReportPin -> {
            val report = reportRepository.getReport(pin.id)
            val author = runCatching { userRepository.getSimpleUser(report.authorId) }.getOrNull()
            val assignee =
                report.assigneeId?.let {
                  runCatching { userRepository.getSimpleUser(it) }.getOrNull()
                }
            _uiState.value =
                _uiState.value.copy(selected = PinDetails.ReportDetails(report, author, assignee))
          }
        }
      } catch (e: Exception) {
        setErrorMsg("Failed to load pin: ${e.message}")
      }
    }
  }

  fun clearSelection() {
    _uiState.value = _uiState.value.copy(selected = null)
  }

  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  fun onLocationPermissionResult(granted: Boolean) {
    _renderState.value = _renderState.value.copy(showUserLocation = granted)
  }

  fun requestRecenter() {
    _renderState.value = _renderState.value.copy(recenterNonce = System.currentTimeMillis())
  }

  fun consumeRecenter() {
    if (_renderState.value.recenterNonce != null) {
      _renderState.value = _renderState.value.copy(recenterNonce = null)
    }
  }

  fun clearRenderError() {
    _renderState.value = _renderState.value.copy(renderError = null)
  }

  fun setErrorMsg(message: String?) {
    _uiState.value = _uiState.value.copy(errorMsg = message)
  }

  fun setRenderMsg(message: String?) {
    _renderState.value = _renderState.value.copy(renderError = message)
  }

  fun toggleLike(postId: Id) {
    val uid = loggedInUserId
    if (uid.isBlank()) {
      setErrorMsg("You must be logged in to like posts.")
      return
    }
    val before = _uiState.value
    val beforeSelection = before.selected
    val optimistic =
        (beforeSelection as? PinDetails.PostDetails)
            ?.takeIf { it.post.postId == postId }
            ?.let { sel ->
              val likedNow = !sel.likedByMe
              sel.copy(
                  likedByMe = likedNow,
                  post =
                      sel.post.copy(
                          likesCount =
                              if (likedNow) sel.post.likesCount + 1
                              else (sel.post.likesCount - 1).coerceAtLeast(0)),
              )
            }

    if (optimistic != null) {
      _uiState.value = before.copy(selected = optimistic)
    }

    viewModelScope.launch {
      try {
        val existing = likeRepository.getLikeForPost(postId)
        if (existing != null) {
          likeRepository.deleteLike(existing.likeId)
        } else {
          likeRepository.addLike(
              Like(
                  likeId = likeRepository.getNewLikeId(),
                  postId = postId,
                  userId = uid,
              ))
        }
      } catch (e: Exception) {
        val cur = _uiState.value
        val curSel = cur.selected
        if (curSel is PinDetails.PostDetails && curSel.post.postId == postId) {
          _uiState.value =
              cur.copy(
                  selected = beforeSelection,
                  errorMsg = "Could not update like: ${e.message}",
              )
        } else {
          setErrorMsg("Could not update like: ${e.message}")
        }
      }
    }
  }
}
