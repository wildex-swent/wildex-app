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
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
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
  Reports
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
    val renderError: String? = null
)

class MapScreenViewModel(
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val postRepository: PostsRepository = RepositoryProvider.postRepository,
    private val likeRepository: LikeRepository = RepositoryProvider.likeRepository,
    private val reportRepository: ReportRepository = RepositoryProvider.reportRepository,
    private val animalRepository: AnimalRepository = RepositoryProvider.animalRepository,
    private val currentUserId: Id = Firebase.auth.uid ?: "",
) : ViewModel() {

  private val _uiState = MutableStateFlow(MapUIState())
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()

  private val _renderState = MutableStateFlow(MapRenderState())
  val renderState: StateFlow<MapRenderState> = _renderState.asStateFlow()

  /** Loads or reloads the full UI state for a given user. */
  fun loadUIState(userUid: Id = currentUserId) {
    _uiState.value = _uiState.value.copy(isLoading = true, isError = false, errorMsg = null)
    viewModelScope.launch { updateUIState(userUid) }
  }

  /** Refreshes the data for the currently visible map (self or other). */
  fun refreshUIState(userUid: Id = currentUserId) {
    _uiState.value =
        _uiState.value.copy(
            isRefreshing = true, isError = false, errorMsg = null, isLoading = false)
    viewModelScope.launch { updateUIState(userUid) }
  }

  private suspend fun updateUIState(userUid: Id) {
    if (currentUserId.isBlank()) {
      setErrorMsg("No logged in user.")
      _uiState.value = _uiState.value.copy(isLoading = false, isError = true)
      return
    }

    try {
      val me = userRepository.getUser(currentUserId)
      val isPro = me.userType == UserType.PROFESSIONAL
      val isSelf = userUid == currentUserId

      val tabs =
          if (isSelf) {
            if (isPro) listOf(MapTab.Posts, MapTab.MyPosts, MapTab.Reports)
            else listOf(MapTab.Posts, MapTab.MyPosts)
          } else {
            listOf(MapTab.MyPosts, MapTab.Reports)
          }

      val active = _uiState.value.activeTab.let { if (it in tabs) it else tabs.first() }

      val pins =
          when (active) {
            MapTab.Posts -> loadAllPostsWithAuthorAvatar()
            MapTab.MyPosts -> loadPostsOfUserWithPostImage(userUid)
            MapTab.Reports ->
                if (isSelf) loadAllReportsAsPins() else loadReportsInvolvingUser(userUid)
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
      setErrorMsg(e.localizedMessage ?: "Failed to load map.")
      _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false, isError = true)
    }
  }

  fun onTabSelected(tab: MapTab, userUid: Id = currentUserId) {
    if (tab !in _uiState.value.availableTabs) return
    _uiState.value = _uiState.value.copy(activeTab = tab, selected = null)
    refreshUIState(userUid)
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

  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  fun toggleLike(postId: Id) {
    val uid = currentUserId
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
    if (optimistic != null) _uiState.value = before.copy(selected = optimistic)

    viewModelScope.launch {
      try {
        val existing = likeRepository.getLikeForPost(postId)
        if (existing != null) likeRepository.deleteLike(existing.likeId)
        else
            likeRepository.addLike(
                Like(
                    likeId = likeRepository.getNewLikeId(),
                    postId = postId,
                    userId = uid,
                ))
      } catch (e: Exception) {
        val cur = _uiState.value
        val curSel = cur.selected
        if (curSel is PinDetails.PostDetails && curSel.post.postId == postId) {
          _uiState.value =
              cur.copy(selected = beforeSelection, errorMsg = "Could not update like: ${e.message}")
        } else setErrorMsg("Could not update like: ${e.message}")
      }
    }
  }

  private suspend fun loadAllPostsWithAuthorAvatar(): List<MapPin> {
    val posts = postRepository.getAllPosts()
    val authorCache = mutableMapOf<Id, URL>()
    return posts
        .filter { it.location != null }
        .map { p ->
          val avatar =
              authorCache.getOrPut(p.authorId) {
                userRepository.getSimpleUser(p.authorId).profilePictureURL
              }
          MapPin.PostPin(
              id = p.postId, authorId = p.authorId, location = p.location!!, imageURL = avatar)
        }
  }

  private suspend fun loadPostsOfUserWithPostImage(userId: Id): List<MapPin> {
    val posts = postRepository.getAllPostsByGivenAuthor(userId)
    return posts
        .filter { it.location != null }
        .map { p ->
          MapPin.PostPin(
              id = p.postId,
              authorId = p.authorId,
              location = p.location!!,
              imageURL = p.pictureURL)
        }
  }

  private suspend fun loadAllReportsAsPins(): List<MapPin> {
    val reports = reportRepository.getAllReports()
    val authorCache = mutableMapOf<Id, URL>()
    return reports.map { r ->
      val avatar =
          authorCache.getOrPut(r.authorId) {
            userRepository.getSimpleUser(r.authorId).profilePictureURL
          }
      MapPin.ReportPin(
          id = r.reportId,
          authorId = r.authorId,
          location = r.location,
          imageURL = avatar,
          status = r.status,
          assigneeId = r.assigneeId,
      )
    }
  }

  private suspend fun loadReportsInvolvingUser(userId: Id): List<MapPin> {
    val authored = reportRepository.getAllReportsByAuthor(userId)
    val assigned = reportRepository.getAllReportsByAssignee(userId)
    val all = (authored + assigned).associateBy { it.reportId }.values
    return all.map { r ->
      MapPin.ReportPin(
          id = r.reportId,
          authorId = r.authorId,
          location = r.location,
          imageURL = r.imageURL,
          status = r.status,
          assigneeId = r.assigneeId,
      )
    }
  }
}
