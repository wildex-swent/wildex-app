package com.android.wildex.ui.navigation

import androidx.navigation.NavHostController

sealed class Screen(
    val route: String,
    val name: String,
    val isTopLevelDestination: Boolean = false
) {
  object Auth : Screen(route = "auth", name = "Authentication")

  object Home : Screen(route = "home", name = "Home", isTopLevelDestination = true)

  object Settings : Screen(route = "settings", name = "Settings", isTopLevelDestination = false)

  data class PostDetails(val postUid: String) :
      Screen(route = "post_details/${postUid}", name = "Post Details") {
    companion object {
      const val PATH = "post_details"
    }
  }

  data class Profile(val userUid: String) : Screen(route = "profile/${userUid}", name = "Profile") {
    companion object {
      const val PATH = "profile"
    }
  }

  data class Map(val userUid: String) :
      Screen(route = "map/${userUid}", name = "Map", isTopLevelDestination = true) {
    companion object {
      const val PATH = "map"
    }
  }

  object Camera : Screen(route = "camera", name = "Camera", isTopLevelDestination = true)

  data class Collection(val userUid: String) :
      Screen(route = "collection/${userUid}", name = "Collection") {
    companion object {
      const val PATH = "collection"
    }
  }

  object Report : Screen(route = "report", name = "Report", isTopLevelDestination = true)
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
        // Restore state when re-selecting a previously selected item
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
