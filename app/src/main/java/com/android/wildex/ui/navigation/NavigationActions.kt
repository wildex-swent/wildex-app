package com.android.wildex.ui.navigation

import androidx.navigation.NavHostController
import com.android.wildex.model.utils.Id

sealed class Screen(
    val route: String,
    val name: String,
    val isTopLevelDestination: Boolean = false,
) {
  object Auth : Screen(route = "auth", name = "Authentication")

  object Home : Screen(route = "home", name = "Home", isTopLevelDestination = true)

  data class PostDetails(val postUid: Id) :
      Screen(route = "post_details/${postUid}", name = "Post Details") {
    companion object {
      const val PATH = "post_details/{postUid}"
    }
  }

  data class Profile(val userUid: Id) : Screen(route = "profile/${userUid}", name = "Profile") {
    companion object {
      const val PATH = "profile/{userUid}"
    }
  }

  data class Social(val userUid: Id) : Screen(route = "social/${userUid}", name = "Social") {
    companion object {
      const val PATH = "social/{userUid}"
    }
  }

  object EditProfile : Screen(route = "edit_profile", name = "Edit Profile")

  data class Map(val userUid: Id) :
      Screen(route = "map/${userUid}", name = "Map", isTopLevelDestination = true) {
    companion object {
      const val PATH = "map/{userUid}"
    }
  }

  object Camera : Screen(route = "camera", name = "Camera", isTopLevelDestination = true)

  data class Collection(val userUid: Id) :
      Screen(route = "collection/${userUid}", name = "Collection") {
    companion object {
      const val PATH = "collection/{userUid}"
    }
  }

  object Report : Screen(route = "report", name = "Report", isTopLevelDestination = true)

  data class ReportDetails(val reportUid: Id) :
      Screen(route = "report_details/${reportUid}", name = "Report Details") {
    companion object {
      const val PATH = "report_details/{reportUid}"
    }
  }

  object SubmitReport : Screen(route = "submit_report", name = "Submit Report")

  object Settings : Screen(route = "settings", name = "Settings")

  data class Achievements(val userUid: Id) :
      Screen(route = "achievement_screen/${userUid}", name = "Achievement Screen") {
    companion object {
      const val PATH = "achievement_screen/{userUid}"
    }
  }

  object LocationPicker : Screen(route = "location_picker", name = "Location Picker")

  data class AnimalInformation(val animalUid: Id) :
      Screen(route = "animal_information_screen/${animalUid}", name = "Animal Information Screen") {
    companion object {
      const val PATH = "animal_information_screen/{animalUid}"
    }
  }

  object Notifications : Screen(route = "notifications", name = "Notifications")

  companion object {
    fun fromString(path: String): Screen {
      val pathParts = path.split("/")
      return when (pathParts[0]) {
        "auth" -> Auth
        "home" -> Home
        "post_details" -> PostDetails(pathParts[1])
        "profile" -> Profile(pathParts[1])
        "edit_profile" -> EditProfile
        "map" -> Map(pathParts[1])
        "camera" -> Camera
        "collection" -> Collection(pathParts[1])
        "report" -> Report
        "report_details" -> ReportDetails(pathParts[1])
        "submit_report" -> SubmitReport
        "settings" -> Settings
        "friend_screen" -> Social(pathParts[1])
        "achievement_screen" -> Achievements(pathParts[1])
        "animal_information_screen" -> AnimalInformation(pathParts[1])
        "notifications" -> Notifications
        else -> Auth
      }
    }
  }
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
