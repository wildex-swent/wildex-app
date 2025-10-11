package com.android.wildex.ui.navigation

import androidx.navigation.NavHostController

sealed class Screen(
    val route: String,
    val name: String,
    val isTopLevelDestination: Boolean = false
) {
  object Auth : Screen(route = "auth", name = "Authentication")

  object Home : Screen(route = "home", name = "Home", isTopLevelDestination = true)

  data class PostDetails(val postUid: String) :
      Screen(route = "post_details/${postUid}", name = "Post Details") {
    companion object {
      const val route = "post_details/{postUid}"
    }
  }

  data class Profile(val userUid: String) : Screen(route = "profile/${userUid}", name = "Profile") {
    companion object {
      const val route = "profile/{userUid}"
    }
  }

  data class Achievements(val userUid: String) :
      Screen(route = "achievements/${userUid}", name = "Achievements") {
    companion object {
      const val route = "achievements/{userUid}"
    }
  }

  object Settings : Screen(route = "settings", name = "Settings")

  object EditProfile : Screen(route = "edit_profile", name = "Edit profile")

  object Map : Screen(route = "map", name = "Map", isTopLevelDestination = true)

  object NewPost : Screen(route = "new_post", name = "New Post", isTopLevelDestination = true)

  object Collection :
      Screen(route = "collection", name = "Collection", isTopLevelDestination = true)

  data class AnimalDetails(val animalUid: String) :
      Screen(route = "animal_detail/${animalUid}", name = "Animal Details") {
    companion object {
      const val route = "animal_detail/{animalUid}"
    }
  }

  object Report : Screen(route = "report", name = "Report", isTopLevelDestination = true)

  data class ReportDetails(val reportUid: String) :
      Screen(route = "report_detail/${reportUid}", name = "Report Details") {
    companion object {
      const val route = "report_detail/{reportUid}"
    }
  }

  object SubmitReport : Screen(route = "submit_report", name = "Submit Report")
}

open class NavigationActions(
    private val navController: NavHostController,
) {
  /**
   * Navigate to the specified screen.
   *
   * @param screen The screen to navigate to
   */
  open fun navigateTo(screen: Screen) {
    if (screen.isTopLevelDestination && currentRoute() == screen.route) {
      // If the user is already on the top-level destination, do nothing
      return
    }
    navController.navigate(screen.route) {
      if (screen.isTopLevelDestination) {
        launchSingleTop = true
        popUpTo(screen.route) { inclusive = true }
      }
      if (screen !is Screen.Auth) {
        // Restore state when reselecting a previously selected item
        restoreState = true
      }
    }
  }

  /** Navigate back to the previous screen. */
  open fun goBack() {
    navController.popBackStack()
  }

  /**
   * Get the current route of the navigation controller.
   *
   * @return The current route
   */
  open fun currentRoute(): String {
    return navController.currentDestination?.route ?: ""
  }
}
