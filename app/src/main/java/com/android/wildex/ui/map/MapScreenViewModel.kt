package com.android.wildex.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.map.MapPin
import com.android.wildex.model.map.PinDetails
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.social.CommentRepository
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the different tabs available on the map screen.
 * - Posts: Shows all posts on the map.
 * - MyPosts: Shows posts created by the user.
 * - Reports: Shows reports made by or assigned to the user.
 */
enum class MapTab {
  Posts,
  MyPosts,
  Reports,
}

/**
 * Represents the UI state of the Map Screen.
 *
 * @property availableTabs List of tabs available to the user based on their role and context.
 * @property activeTab The currently selected tab.
 * @property pins List of map pins to be displayed on the map.
 * @property selected Details of the currently selected pin, if any.
 * @property centerCoordinates The coordinates to center the map on.
 * @property isLoading Indicates whether the map data is currently loading.
 * @property isRefreshing Indicates whether the map data is currently refreshing.
 * @property isError Indicates whether there was an error loading the map data.
 * @property errorMsg Optional error message to display if loading fails.
 */
data class MapUIState(
    val availableTabs: List<MapTab> = emptyList(),
    val activeTab: MapTab = MapTab.Posts,
    val pins: List<MapPin> = emptyList(),
    val selected: List<PinDetails> = emptyList(),
    val selectedIndex: Int = 0,
    val centerCoordinates: Location = Location(46.5197, 6.6323, "Lausanne"),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isError: Boolean = false,
    val errorMsg: String? = null,
)

/**
 * Represents the rendering state of the Map Screen.
 *
 * @property showUserLocation Indicates whether to show the user's current location on the map.
 * @property recenterNonce A nonce value used to trigger recentering of the map.
 * @property renderError Optional error message related to map rendering issues.
 */
data class MapRenderState(
    val showUserLocation: Boolean = false,
    val recenterNonce: Long? = null,
    val renderError: String? = null,
)

/** ViewModel for managing the state and logic of the Map Screen.* */
class MapScreenViewModel(
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val postRepository: PostsRepository = RepositoryProvider.postRepository,
    private val likeRepository: LikeRepository = RepositoryProvider.likeRepository,
    private val commentRepository: CommentRepository = RepositoryProvider.commentRepository,
    private val reportRepository: ReportRepository = RepositoryProvider.reportRepository,
    private val animalRepository: AnimalRepository = RepositoryProvider.animalRepository,
    private val currentUserId: Id = Firebase.auth.uid ?: "",
) : ViewModel() {

  /** Backing property for the map screen UI state. */
  private val _uiState = MutableStateFlow(MapUIState())
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()
  /** Backing property for the map screen render state. */
  private val _renderState = MutableStateFlow(MapRenderState())
  val renderState: StateFlow<MapRenderState> = _renderState.asStateFlow()

  /**
   * Loads the data for the currently visible map (self or other).
   *
   * @param userUid The user ID for whom to load the map data. Defaults to the current user.
   */
  fun loadUIState(userUid: Id) {
    _uiState.value = _uiState.value.copy(isLoading = true, isError = false, errorMsg = null)
    viewModelScope.launch { updateUIState(userUid) }
  }

  /**
   * Refreshes the data for the currently visible map (self or other).
   *
   * @param userUid The user ID for whom to refresh the map data. Defaults to the current user.
   */
  fun refreshUIState(userUid: Id) {
    _uiState.value =
        _uiState.value.copy(
            isRefreshing = true,
            isError = false,
            errorMsg = null,
            isLoading = false,
            selected = emptyList(),
            selectedIndex = 0)
    viewModelScope.launch { updateUIState(userUid) }
  }

  /**
   * Updates the UI state by fetching map pins and user information.
   *
   * @param userUid The user ID for whom to load the map data.
   */
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

      val previousState = _uiState.value
      val previousCenter = previousState.centerCoordinates
      val previousPinIds = previousState.pins.map { it.id }
      val currentPinIds = pins.map { it.id }
      val tabChanged = previousPinIds != currentPinIds
      val centerCoordinates =
          when {
            tabChanged && pins.isNotEmpty() -> pins[0].location
            else -> previousCenter
          }

      _uiState.value =
          _uiState.value.copy(
              availableTabs = tabs,
              activeTab = active,
              pins = pins,
              isLoading = false,
              isRefreshing = false,
              centerCoordinates = centerCoordinates,
              isError = false,
              errorMsg = null,
              selected = emptyList(),
              selectedIndex = 0)
    } catch (e: Exception) {
      setErrorMsg(e.localizedMessage ?: "Failed to load map components.")
      _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false, isError = true)
    }
  }

  /**
   * Handles tab selection by the user.
   *
   * @param tab The selected [MapTab].
   * @param userUid The user ID for whom to load the map data. Defaults to the current user.
   */
  fun onTabSelected(tab: MapTab, userUid: Id) {
    if (tab !in _uiState.value.availableTabs) return
    _uiState.value = _uiState.value.copy(activeTab = tab, selected = emptyList(), selectedIndex = 0)
    refreshUIState(userUid)
  }

  /**
   * Handles pin selection by the user.
   *
   * @param pinId The ID of the selected pin.
   */
  fun onPinSelected(pinId: Id) {
    viewModelScope.launch {
      val pin = _uiState.value.pins.firstOrNull { it.id == pinId } ?: return@launch
      _uiState.value = _uiState.value.copy(centerCoordinates = pin.location)

      try {
        when (pin) {
          is MapPin.PostPin -> {
            val post = postRepository.getPost(pin.id)
            val author = userRepository.getSimpleUser(post.authorId)
            val liked = likeRepository.getLikeForPost(post.postId) != null
            val animalName = animalRepository.getAnimal(post.animalId).name
            val likeCount = likeRepository.getLikesForPost(post.postId).size
            val commentCount = commentRepository.getAllCommentsByPost(post.postId).size
            _uiState.value =
                _uiState.value.copy(
                    selected =
                        listOf(
                            PinDetails.PostDetails(
                                post,
                                author,
                                liked,
                                likeCount,
                                commentCount,
                                animalName,
                            )),
                    selectedIndex = 0,
                )
          }
          is MapPin.ReportPin -> {
            val report = reportRepository.getReport(pin.id)
            val author = userRepository.getSimpleUser(report.authorId)
            val assignee = report.assigneeId?.let { userRepository.getSimpleUser(it) }
            _uiState.value =
                _uiState.value.copy(
                    selected = listOf(PinDetails.ReportDetails(report, author, assignee)),
                    selectedIndex = 0,
                )
          }
          is MapPin.ClusterPin -> null
        }
      } catch (e: Exception) {
        setErrorMsg("Failed to load pin: ${e.message}")
      }
    }
  }

  /**
   * Handles cluster pin selection by the user.
   *
   * @param cluster The selected [MapPin.ClusterPin].
   */
  fun onClusterPinClicked(cluster: MapPin.ClusterPin) {
    viewModelScope.launch {
      try {
        val pins = _uiState.value.pins

        val children = cluster.childIds.mapNotNull { id -> pins.firstOrNull { it.id == id } }

        // Load all details in parallel
        val details = loadClusterDetails(children)

        if (details.isNotEmpty()) {
          _uiState.value =
              _uiState.value.copy(
                  selected = details, // enable group selection
                  selectedIndex = 0, // start at first item
                  centerCoordinates = cluster.location,
              )
        }
      } catch (e: Exception) {
        setErrorMsg("Failed to load group: ${e.message}")
      }
    }
  }

  /**
   * Loads details for all pins within a cluster.
   *
   * @param children List of [MapPin] representing the pins within the cluster.
   * @return A list of [PinDetails] containing the details of each pin.
   */
  private suspend fun loadClusterDetails(children: List<MapPin>): List<PinDetails> =
      coroutineScope {
        val jobs = children.map { pin -> async { loadDetailForClusterPin(pin) } }
        jobs.mapNotNull { it.await() }
      }

  /**
   * Loads details for a single pin within a cluster.
   *
   * @param pin The [MapPin] for which to load details.
   * @return The [PinDetails] for the pin, or null if loading fails.
   */
  private suspend fun loadDetailForClusterPin(pin: MapPin): PinDetails? =
      when (pin) {
        is MapPin.PostPin ->
            try {
              val post = postRepository.getPost(pin.id)
              val author = userRepository.getSimpleUser(post.authorId)
              val liked = likeRepository.getLikeForPost(post.postId) != null
              val likeCount = likeRepository.getLikesForPost(post.postId).size
              val commentCount = commentRepository.getAllCommentsByPost(post.postId).size
              val animalName = animalRepository.getAnimal(post.animalId).name

              PinDetails.PostDetails(
                  post = post,
                  author = author,
                  likedByMe = liked,
                  likeCount = likeCount,
                  commentCount = commentCount,
                  animalName = animalName,
              )
            } catch (_: Exception) {
              null
            }
        is MapPin.ReportPin ->
            try {
              val report = reportRepository.getReport(pin.id)
              val author = userRepository.getSimpleUser(report.authorId)
              val assignee = report.assigneeId?.let { userRepository.getSimpleUser(it) }
              PinDetails.ReportDetails(report, author, assignee)
            } catch (_: Exception) {
              null
            }
        else -> null
      }

  /** Clears the currently selected pin details. */
  fun clearSelection() {
    _uiState.value = _uiState.value.copy(selected = emptyList(), selectedIndex = 0)
  }

  /** Clears any existing error message from the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Handles the result of the location permission request.
   *
   * @param granted Indicates whether the location permission was granted.
   */
  fun onLocationPermissionResult(granted: Boolean) {
    _renderState.value = _renderState.value.copy(showUserLocation = granted)
  }

  /** Requests the map to recenter on the user's location. */
  fun requestRecenter() {
    _renderState.value = _renderState.value.copy(recenterNonce = System.currentTimeMillis())
  }

  /** Consumes the recenter request by clearing the nonce. */
  fun consumeRecenter() {
    if (_renderState.value.recenterNonce != null) {
      _renderState.value = _renderState.value.copy(recenterNonce = null)
    }
  }

  /** Clears any existing render error from the render state. */
  fun clearRenderError() {
    _renderState.value = _renderState.value.copy(renderError = null)
  }

  fun selectNext() {
    val sel = _uiState.value.selected
    if (sel.isEmpty()) return
    val next = (_uiState.value.selectedIndex + 1) % sel.size
    _uiState.value = _uiState.value.copy(selectedIndex = next)
  }

  fun selectPrev() {
    val sel = _uiState.value.selected
    if (sel.isEmpty()) return
    val prev = (_uiState.value.selectedIndex - 1 + sel.size) % sel.size
    _uiState.value = _uiState.value.copy(selectedIndex = prev)
  }

  /** Sets a new error message in the UI state. */
  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  /**
   * Toggles the like status of a post. If the post is already liked by the current user, it removes
   * the like. Otherwise, it creates and adds a new like entry.
   *
   * @param postId The unique identifier of the post to toggle like status.
   */
  fun toggleLike(postId: Id) {
    val uid = currentUserId
    if (uid.isBlank()) {
      setErrorMsg("You must be logged in to like posts.")
      return
    }

    val before = _uiState.value
    val beforeSelection = before.selected
    val optimistic = buildOptimisticSelection(beforeSelection, postId)

    val didOptimisticallyChange = optimistic != beforeSelection
    if (didOptimisticallyChange) {
      _uiState.value = before.copy(selected = optimistic)
    }

    viewModelScope.launch {
      try {
        val existing = likeRepository.getLikeForPost(postId)
        if (existing != null) likeRepository.deleteLike(existing.likeId)
        else
            likeRepository.addLike(
                Like(likeId = likeRepository.getNewLikeId(), postId = postId, userId = uid))
      } catch (e: Exception) {
        val cur = _uiState.value
        val curSel = cur.selected
        val hadTarget = curSel.any { it is PinDetails.PostDetails && it.post.postId == postId }
        if (hadTarget) {
          _uiState.value =
              cur.copy(selected = beforeSelection, errorMsg = "Could not update like: ${e.message}")
        } else setErrorMsg("Could not update like: ${e.message}")
      }
    }
  }

  /**
   * Builds an optimistic selection state for a post after toggling its like status.
   *
   * @param beforeSelection The current list of [PinDetails] before the like toggle.
   * @param postId The ID of the post whose like status is being toggled.
   * @return A new list of [PinDetails] reflecting the optimistic like status change.
   */
  private fun buildOptimisticSelection(
      beforeSelection: List<PinDetails>,
      postId: Id,
  ): List<PinDetails> =
      beforeSelection.map { sel ->
        if (sel is PinDetails.PostDetails && sel.post.postId == postId) {
          val likedNow = !sel.likedByMe
          sel.copy(
              likedByMe = likedNow,
              likeCount = if (likedNow) sel.likeCount + 1 else (sel.likeCount - 1).coerceAtLeast(0),
          )
        } else sel
      }

  /**
   * Loads all posts with their authors' avatar URLs.
   *
   * @return A list of [MapPin.PostPin] representing the posts with author avatars.
   */
  private suspend fun loadAllPostsWithAuthorAvatar(): List<MapPin> = coroutineScope {
    val posts = postRepository.getAllPosts().filter { it.location != null }
    val authorIds = posts.map { it.authorId }.distinct()
    val avatarDeferreds =
        authorIds.associateWith { id ->
          async {
            runCatching { userRepository.getSimpleUser(id).profilePictureURL }.getOrDefault("")
          }
        }
    val avatars = avatarDeferreds.mapValues { (_, deferred) -> deferred.await() }
    posts.map { p ->
      MapPin.PostPin(
          id = p.postId,
          authorId = p.authorId,
          location = p.location!!,
          imageURL = avatars[p.authorId] ?: "",
      )
    }
  }

  /**
   * Loads posts created by a specific user with their post image URLs.
   *
   * @param userId The ID of the user whose posts are to be loaded.
   * @return A list of [MapPin.PostPin] representing the user's posts with post images.
   */
  private suspend fun loadPostsOfUserWithPostImage(userId: Id): List<MapPin> {
    val posts = postRepository.getAllPostsByGivenAuthor(userId)
    return posts
        .filter { it.location != null }
        .map { p ->
          MapPin.PostPin(
              id = p.postId,
              authorId = p.authorId,
              location = p.location!!,
              imageURL = p.pictureURL,
          )
        }
  }

  /**
   * Loads all reports as map pins.
   *
   * @return A list of [MapPin.ReportPin] representing all reports.
   */
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
          assigneeId = r.assigneeId,
      )
    }
  }

  /**
   * Loads reports involving a specific user, either as author or assignee.
   *
   * @param userId The ID of the user involved in the reports.
   * @return A list of [MapPin.ReportPin] representing the reports involving the user
   */
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
          assigneeId = r.assigneeId,
      )
    }
  }
}
