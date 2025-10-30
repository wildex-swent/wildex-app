package com.android.wildex.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.map.MapPin
import com.android.wildex.model.map.PinDetails
import com.android.wildex.model.social.Like
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
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
    private val currentUserId: Id = Firebase.auth.uid!!,
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val postsRepository: PostsRepository = RepositoryProvider.postRepository,
    private val likeRepository: LikeRepository = RepositoryProvider.likeRepository,
    // private val reportsRepository: ReportsRepository = RepositoryProvider.reportsRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(MapUIState())
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()
  private val _renderState = MutableStateFlow(MapRenderState())
  val renderState: StateFlow<MapRenderState> = _renderState.asStateFlow()

  fun loadUIState() {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null, selected = null)
    viewModelScope.launch { updateUIState() }
  }

  fun refreshUIState() {
    _uiState.value = _uiState.value.copy(isRefreshing = true, errorMsg = null)
    viewModelScope.launch { updateUIState() }
  }

  private suspend fun updateUIState() {
    try {
      val user = userRepository.getUser(currentUserId)
      val isProfessional = user.userType == UserType.PROFESSIONAL

      val tabs =
          if (isProfessional) listOf(MapTab.Posts, MapTab.MyPosts, MapTab.Reports)
          else listOf(MapTab.Posts, MapTab.MyPosts)

      val active = _uiState.value.activeTab.let { cur -> if (cur in tabs) cur else MapTab.Posts }

      val pins =
          when (active) {
            MapTab.Posts -> loadPostPins(all = true, currentUserId = currentUserId)
            MapTab.MyPosts -> loadPostPins(all = false, currentUserId = currentUserId)
            MapTab.Reports -> loadReportPins()
          }

      _uiState.value =
          _uiState.value.copy(
              availableTabs = tabs,
              activeTab = active,
              pins = pins,
              isLoading = false,
              isRefreshing = false,
              isError = false,
              errorMsg = null,
          )
    } catch (e: Exception) {
      _uiState.value =
          _uiState.value.copy(
              isError = true,
              isLoading = false,
              isRefreshing = false,
              errorMsg = "Unexpected error: ${e.message ?: "unknown"}",
          )
    }
  }

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
            val post = postsRepository.getPost(pin.id)
            val author = runCatching { userRepository.getSimpleUser(post.authorId) }.getOrNull()
            val liked =
                runCatching { likeRepository.getLikeForPost(post.postId) != null }
                    .getOrDefault(false)
            _uiState.value =
                _uiState.value.copy(selected = PinDetails.PostDetails(post, author, liked))
          }
          is MapPin.ReportPin -> {
            // TODO reports when repo exists
          }
        }
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMsg = "Failed to load pin details: ${e.message}")
      }
    }
  }

  fun clearSelection() {
    _uiState.value = _uiState.value.copy(selected = null)
  }

  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  private suspend fun loadPostPins(all: Boolean, currentUserId: Id): List<MapPin> {
    val posts: List<Post> =
        if (all) postsRepository.getAllPosts() else postsRepository.getAllPostsByAuthor()
    return posts
        .filter { it.location != null }
        .map { p ->
          val markerUrl: URL =
              if (p.authorId == currentUserId) p.pictureURL
              else
                  runCatching { userRepository.getSimpleUser(p.authorId).profilePictureURL }
                      .getOrNull() ?: p.pictureURL
          MapPin.PostPin(
              id = p.postId,
              authorId = p.authorId,
              location = p.location!!,
              imageURL = markerUrl,
          )
        }
  }

  private suspend fun loadReportPins(): List<MapPin> {
    // TODO when ReportsRepository exists
    return emptyList()
  }

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

  fun toggleLike(postId: Id) {
    // 1) optimistic UI update first
    val before = _uiState.value
    val optimisticSelected =
        when (val sel = before.selected) {
          is PinDetails.PostDetails ->
              if (sel.post.postId == postId) {
                val newLiked = !sel.likedByMe
                sel.copy(
                    likedByMe = newLiked,
                    post =
                        sel.post.copy(
                            likesCount =
                                if (newLiked) sel.post.likesCount + 1
                                else (sel.post.likesCount - 1).coerceAtLeast(0)))
              } else sel
          else -> sel
        }

    _uiState.value = before.copy(selected = optimisticSelected)

    // 2) do the real work in background
    viewModelScope.launch {
      try {
        val existing = likeRepository.getLikeForPost(postId)
        if (existing != null) {
          likeRepository.deleteLike(existing.likeId)
        } else {
          val newLike =
              Like(
                  likeId = likeRepository.getNewLikeId(),
                  postId = postId,
                  userId = currentUserId,
              )
          likeRepository.addLike(newLike)
        }
      } catch (e: Exception) {
        // 3) revert if backend failed
        val cur = _uiState.value
        val reverted =
            when (val sel = cur.selected) {
              is PinDetails.PostDetails ->
                  if (sel.post.postId == postId) {
                    // just flip back
                    val newLiked = !sel.likedByMe
                    sel.copy(
                        likedByMe = newLiked,
                        post =
                            sel.post.copy(
                                likesCount =
                                    if (newLiked) sel.post.likesCount + 1
                                    else (sel.post.likesCount - 1).coerceAtLeast(0)))
                  } else sel
              else -> sel
            }
        _uiState.value =
            cur.copy(selected = reverted, errorMsg = "Could not update like: ${e.message}")
      }
    }
  }
}
