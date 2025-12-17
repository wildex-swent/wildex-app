package com.android.wildex.ui.profile

import android.net.Uri
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.user.OnBoardingStage
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.ui.utils.CountryDropdownTestTags
import com.android.wildex.utils.LocalRepositories
import com.android.wildex.utils.offline.FakeConnectivityObserver
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditProfileScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var userRepository: UserRepository
  private lateinit var storageRepository: StorageRepository
  private val fakeObserver = FakeConnectivityObserver(initial = true)

  private fun sampleUser() =
      User(
          userId = "uid-1",
          username = "jane_doe",
          name = "Jane",
          surname = "Doe",
          bio = "Bio of Jane",
          profilePictureURL = "https://example.com/pic.jpg",
          userType = UserType.REGULAR,
          creationDate = Timestamp(0, 0),
          country = "Switzerland",
          onBoardingStage = OnBoardingStage.COMPLETE,
      )

  @Before
  fun setup() {
    userRepository = LocalRepositories.userRepository
    storageRepository = LocalRepositories.storageRepository
  }

  @After
  fun teardown() {
    LocalRepositories.clearAll()
  }

  @Test
  fun initialState_showsFields_andProfilePreview() {
    fakeObserver.setOnline(true)
    runBlocking { userRepository.addUser(sampleUser()) }
    val vm =
        EditProfileViewModel(
            userRepository = userRepository,
            storageRepository = storageRepository,
            currentUserId = "uid-1",
        )

    composeRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        EditProfileScreen(editScreenViewModel = vm)
      }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(EditProfileScreenTestTags.INPUT_NAME).assertTextContains("Jane")
    composeRule.onNodeWithTag(EditProfileScreenTestTags.INPUT_SURNAME).assertTextContains("Doe")
    composeRule
        .onNodeWithTag(EditProfileScreenTestTags.INPUT_USERNAME)
        .assertTextContains("jane_doe")
    composeRule
        .onNodeWithTag(EditProfileScreenTestTags.INPUT_DESCRIPTION)
        .assertTextContains("Bio of Jane")
    composeRule.onNodeWithTag(CountryDropdownTestTags.COUNTRY_DROPDOWN).assertIsDisplayed()
    composeRule.onNodeWithTag(EditProfileScreenTestTags.PROFILE_PICTURE_PREVIEW).assertIsDisplayed()
  }

  @Test
  fun countryDropdown_opens_and_selects_country() {
    fakeObserver.setOnline(true)
    runBlocking { userRepository.addUser(sampleUser()) }
    val vm =
        EditProfileViewModel(
            userRepository = userRepository,
            storageRepository = storageRepository,
            currentUserId = "uid-1",
        )

    composeRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        EditProfileScreen(editScreenViewModel = vm)
      }
    }
    composeRule.waitForIdle()

    val initialCountry = vm.uiState.value.country
    composeRule.onNodeWithTag(CountryDropdownTestTags.COUNTRY_DROPDOWN).assertIsDisplayed()

    composeRule
        .onNode(
            hasClickAction()
                .and(hasAnyAncestor(hasTestTag(CountryDropdownTestTags.COUNTRY_DROPDOWN))),
            useUnmergedTree = true,
        )
        .performClick()

    val countryItemMatcher =
        SemanticsMatcher("country item") { node ->
          val tag = node.config.getOrNull(SemanticsProperties.TestTag)
          tag is String && tag.startsWith(CountryDropdownTestTags.COUNTRY_ELEMENT)
        }

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodes(countryItemMatcher).fetchSemanticsNodes().isNotEmpty()
    }

    val items = composeRule.onAllNodes(countryItemMatcher)
    items[0].performClick()
    composeRule.waitForIdle()

    val newCountry = vm.uiState.value.country
    Assert.assertNotEquals(initialCountry, newCountry)
    composeRule.onNodeWithTag(CountryDropdownTestTags.COUNTRY_DROPDOWN).assertIsDisplayed()
  }

  @Test
  fun changeProfileImage_updatesPreview() {
    fakeObserver.setOnline(true)

    runBlocking { userRepository.addUser(sampleUser()) }
    val vm =
        EditProfileViewModel(
            userRepository = userRepository,
            storageRepository = storageRepository,
            currentUserId = "uid-1",
        )

    composeRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        EditProfileScreen(editScreenViewModel = vm)
      }
    }
    composeRule.waitForIdle()

    vm.setNewProfileImageUri(Uri.parse("content://local/new"))
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(EditProfileScreenTestTags.PROFILE_PICTURE_PREVIEW).assertIsDisplayed()
  }

  @Test
  fun goBack_invokes_callback() {
    fakeObserver.setOnline(true)
    runBlocking { userRepository.addUser(sampleUser()) }
    val vm =
        EditProfileViewModel(
            userRepository = userRepository,
            storageRepository = storageRepository,
            currentUserId = "uid-1",
        )

    var back = 0
    composeRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        EditProfileScreen(editScreenViewModel = vm, onGoBack = { back++ })
      }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(EditProfileScreenTestTags.GO_BACK).performClick()
    Assert.assertEquals(1, back)
  }
}
