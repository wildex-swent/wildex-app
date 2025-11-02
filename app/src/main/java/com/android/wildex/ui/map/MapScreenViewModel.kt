package com.android.wildex.ui.map

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MapScreenViewModel(
    loggedInUserId: Id = Firebase.auth.uid ?: "",
    userRepository: UserRepository = RepositoryProvider.userRepository,
    postRepository: PostsRepository = RepositoryProvider.postRepository,
    reportRepository: ReportRepository = RepositoryProvider.reportRepository,
    likeRepository: LikeRepository = RepositoryProvider.likeRepository,
    animalRepository: AnimalRepository = RepositoryProvider.animalRepository,
) :
    BaseMapViewModel(
        loggedInUserId = loggedInUserId,
        userRepository = userRepository,
        postRepository = postRepository,
        likeRepository = likeRepository,
        reportRepository = reportRepository,
        animalRepository = animalRepository,
    ) {

  override suspend fun reload() {
    val currentUid = loggedInUserId
    if (currentUid.isBlank()) {
      _uiState.value =
          _uiState.value.copy(
              isError = true,
              isLoading = false,
              isRefreshing = false,
              errorMsg = "No logged in user",
          )
      return
    }
    try {
      val me = userRepository.getUser(currentUid)
      val isPro = me.userType == UserType.PROFESSIONAL

      val tabs =
          if (isPro) listOf(MapTab.Posts, MapTab.MyPosts, MapTab.Reports)
          else listOf(MapTab.Posts, MapTab.MyPosts)

      val active = uiState.value.activeTab.let { if (it in tabs) it else MapTab.Posts }

      val pins =
          when (active) {
            MapTab.Posts -> loadAllPostsWithAuthorAvatar()
            MapTab.MyPosts -> loadPostsOfUserWithPostImage(currentUid)
            MapTab.Reports -> loadAllReportsAsPins()
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
              errorMsg = e.message ?: "Failed to load self map",
          )
    }
  }
}
