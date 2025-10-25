package com.android.wildex.ui.map

import androidx.lifecycle.ViewModel
import com.android.wildex.model.map.MapPin
import com.android.wildex.model.map.PinDetails
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MapTab {
  Posts,
  MyPosts,
  Reports,
}

data class MapUiState(
    val isLoading: Boolean = true,
    val errorMsg: String? = null,
    val availableTabs: List<MapTab>,
    val activeTab: MapTab,
    val pins: List<MapPin> = emptyList(),
    val selected: PinDetails? = null,
)

class MapViewModel(
    private val userType: UserType,
    private val currentUserId: Id?,
    private val postsRepo: PostsRepository,
    private val likeRepo: LikeRepository,
    private val userRepo: UserRepository,
    // private val reportsRepo: ReportsRepository?
) : ViewModel() {

  val isUserProfessional: Boolean = userType == UserType.PROFESSIONAL
  private val _uiState =
      MutableStateFlow(
          MapUiState(
              availableTabs =
                  if (isUserProfessional) listOf(MapTab.Posts, MapTab.MyPosts, MapTab.Reports)
                  else listOf(MapTab.Posts, MapTab.MyPosts),
              activeTab = if (isUserProfessional) MapTab.Posts else MapTab.Reports,
          ))
  val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
}
