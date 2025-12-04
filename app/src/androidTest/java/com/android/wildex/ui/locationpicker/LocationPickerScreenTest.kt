package com.android.wildex.ui.locationpicker

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.android.wildex.BuildConfig
import com.android.wildex.model.utils.Location
import com.android.wildex.utils.LocalRepositories
import com.mapbox.common.MapboxOptions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationPickerScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.ACCESS_FINE_LOCATION,
      )

  private val fakeGeocodingRepository = LocalRepositories.geocodingRepository

  private lateinit var vm: LocationPickerViewModel

  @Before
  fun setup() {
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    vm = LocationPickerViewModel(geocodingRepository = fakeGeocodingRepository)
  }

  @Test
  fun backButton_invokesCallback() {
    var backClicked = false
    composeTestRule.setContent {
      LocationPickerScreen(
          onBack = { backClicked = true },
          onLocationPicked = {},
          viewModel = vm,
      )
    }
    composeTestRule
        .onNodeWithTag(LocationPickerTestTags.BACK_BUTTON)
        .assertIsDisplayed()
        .performClick()
    assert(backClicked)
  }

  @Test
  fun typingQuery_showsSuggestions_andClickingSuggestion_opensConfirmDialog() {
    composeTestRule.setContent {
      LocationPickerScreen(
          onBack = {},
          onLocationPicked = {},
          viewModel = vm,
      )
    }
    composeTestRule
        .onNodeWithText("Search city, address or place", substring = false)
        .performTextInput("Paris")
    composeTestRule.waitUntil(timeoutMillis = 2_000) {
      composeTestRule.onAllNodesWithText("Paris Suggestion 0").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Paris Suggestion 0").performClick()
    composeTestRule.onNodeWithTag(LocationPickerTestTags.CONFIRM_DIALOG).assertIsDisplayed()
  }

  @Test
  fun gpsIcon_writesCurrentLocationNameIntoSearchBar() {
    composeTestRule.setContent {
      LocationPickerScreen(
          onBack = {},
          onLocationPicked = {},
          viewModel = vm,
      )
    }
    composeTestRule.runOnIdle {
      vm.onLocationPermissionResult(true)
      vm.onUserLocationAvailable(10.0, 20.0)
    }
    composeTestRule.onNodeWithTag(LocationPickerTestTags.GPS_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Location(10.0, 20.0)").assertIsDisplayed()
  }

  @Test
  fun clickingSearchIcon_opensConfirmDialog_withForwardGeocodeResult() {
    composeTestRule.setContent {
      LocationPickerScreen(
          onBack = {},
          onLocationPicked = {},
          viewModel = vm,
      )
    }
    composeTestRule.onNodeWithTag(LocationPickerTestTags.SEARCH_FIELD).performTextInput("Lausanne")

    composeTestRule.onNodeWithTag(LocationPickerTestTags.SEARCH_BUTTON).performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(LocationPickerTestTags.CONFIRM_DIALOG).assertIsDisplayed()

    composeTestRule.onAllNodesWithText("Lausanne", substring = true).onFirst().assertIsDisplayed()
  }

  @Test
  fun clearIcon_clearsQuery() {
    composeTestRule.setContent {
      LocationPickerScreen(
          onBack = {},
          onLocationPicked = {},
          viewModel = vm,
      )
    }
    composeTestRule.onNodeWithTag(LocationPickerTestTags.SEARCH_FIELD).performTextInput("Abc")

    composeTestRule
        .onNodeWithTag(LocationPickerTestTags.CLEAR_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.onNodeWithText("Abc", substring = false).assertDoesNotExist()
  }

  @Test
  fun clickingSuggestion_thenPressingNo_closesDialog_andDoesNotCallOnLocationPicked() {
    var pickedLocation: Location? = null
    composeTestRule.setContent {
      LocationPickerScreen(
          onBack = {},
          onLocationPicked = { pickedLocation = it },
          viewModel = vm,
      )
    }
    composeTestRule
        .onNodeWithText("Search city, address or place", substring = false)
        .performTextInput("Paris")
    composeTestRule.waitUntil(timeoutMillis = 2_000) {
      composeTestRule.onAllNodesWithText("Paris Suggestion 0").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Paris Suggestion 0").performClick()
    composeTestRule.onNodeWithTag(LocationPickerTestTags.CONFIRM_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LocationPickerTestTags.CONFIRM_NO).performClick()
    composeTestRule.onNodeWithTag(LocationPickerTestTags.CONFIRM_DIALOG).assertDoesNotExist()
    assert(pickedLocation == null)
  }

  @Test
  fun clickingSuggestion_thenPressingYes_callsOnLocationPicked() {
    var pickedLocation: Location? = null
    composeTestRule.setContent {
      LocationPickerScreen(
          onBack = {},
          onLocationPicked = { pickedLocation = it },
          viewModel = vm,
      )
    }
    composeTestRule
        .onNodeWithText("Search city, address or place", substring = false)
        .performTextInput("Paris")
    composeTestRule.waitUntil(timeoutMillis = 2_000) {
      composeTestRule.onAllNodesWithText("Paris Suggestion 0").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Paris Suggestion 0").performClick()
    composeTestRule.onNodeWithTag(LocationPickerTestTags.CONFIRM_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LocationPickerTestTags.CONFIRM_YES).performClick()
    composeTestRule.waitForIdle()
    assert(pickedLocation != null)
    assert(pickedLocation?.name == "Paris Suggestion 0")
  }

  @Test
  fun clickingSearchIcon_clearsFocusOnSearchField() {
    composeTestRule.setContent {
      LocationPickerScreen(
          onBack = {},
          onLocationPicked = {},
          viewModel = vm,
      )
    }
    val searchField =
        composeTestRule.onNodeWithTag(LocationPickerTestTags.SEARCH_FIELD, useUnmergedTree = true)
    val searchButton = composeTestRule.onNodeWithTag(LocationPickerTestTags.SEARCH_BUTTON)
    searchField.performClick()
    searchField.assertIsFocused()
    searchField.performTextInput("Lausanne")
    searchButton.performClick()
    searchField.assertIsNotFocused()
  }

  @Test
  fun imeSearchAction_clearsFocusOnSearchField() {
    composeTestRule.setContent {
      LocationPickerScreen(
          onBack = {},
          onLocationPicked = {},
          viewModel = vm,
      )
    }
    val searchField =
        composeTestRule.onNodeWithTag(LocationPickerTestTags.SEARCH_FIELD, useUnmergedTree = true)
    searchField.performClick()
    searchField.assertIsFocused()
    searchField.performTextInput("Lausanne")
    searchField.performImeAction()
    searchField.assertIsNotFocused()
  }
}
