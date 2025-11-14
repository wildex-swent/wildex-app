package com.android.wildex.ui.profile

import android.net.Uri
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
import androidx.compose.ui.test.performScrollTo
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.utils.LocalRepositories
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class EditProfileScreenTest {

  @get:Rule val composeRule = createComposeRule()

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
          friendsCount = 10,
      )

  @Test
  fun initialState_showsFields_andProfilePreview() {
    val userRepo = LocalRepositories.UserRepositoryImpl()
    runBlocking { userRepo.addUser(sampleUser()) }
    val vm = EditProfileViewModel(userRepository = userRepo, currentUserId = "uid-1")

    composeRule.setContent { EditProfileScreen(editScreenViewModel = vm, isNewUser = true) }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(EditProfileScreenTestTags.INPUT_NAME).assertTextContains("Jane")
    composeRule.onNodeWithTag(EditProfileScreenTestTags.INPUT_SURNAME).assertTextContains("Doe")
    composeRule
        .onNodeWithTag(EditProfileScreenTestTags.INPUT_USERNAME)
        .assertTextContains("jane_doe")
    composeRule
        .onNodeWithTag(EditProfileScreenTestTags.INPUT_DESCRIPTION)
        .assertTextContains("Bio of Jane")
    composeRule.onNodeWithTag(EditProfileScreenTestTags.DROPDOWN_COUNTRY).assertIsDisplayed()
    composeRule.onNodeWithTag(EditProfileScreenTestTags.PROFILE_PICTURE_PREVIEW).assertIsDisplayed()
  }

  @Test
  fun countryDropdown_opens_and_selects_country() {
    val userRepo = LocalRepositories.UserRepositoryImpl()
    runBlocking { userRepo.addUser(sampleUser()) }
    val vm = EditProfileViewModel(userRepository = userRepo, currentUserId = "uid-1")

    composeRule.setContent { EditProfileScreen(editScreenViewModel = vm, isNewUser = true) }
    composeRule.waitForIdle()

    val initialCountry = vm.uiState.value.country
    composeRule.onNodeWithTag(EditProfileScreenTestTags.DROPDOWN_COUNTRY).assertIsDisplayed()

    composeRule
        .onNode(
            hasClickAction()
                .and(hasAnyAncestor(hasTestTag(EditProfileScreenTestTags.DROPDOWN_COUNTRY))),
            useUnmergedTree = true,
        )
        .performClick()

    val countryItemMatcher =
        SemanticsMatcher("country item") { node ->
          val tag = node.config.getOrNull(SemanticsProperties.TestTag)
          tag is String && tag.startsWith(EditProfileScreenTestTags.COUNTRY_ELEMENT)
        }

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodes(countryItemMatcher).fetchSemanticsNodes().isNotEmpty()
    }

    val items = composeRule.onAllNodes(countryItemMatcher)
    items[0].performClick()
    composeRule.waitForIdle()

    val newCountry = vm.uiState.value.country
    Assert.assertNotEquals(initialCountry, newCountry)
    composeRule.onNodeWithTag(EditProfileScreenTestTags.DROPDOWN_COUNTRY).assertIsDisplayed()
  }

  @Test
  fun changeProfileImage_updatesPreview() {
    val userRepo = LocalRepositories.UserRepositoryImpl()
    runBlocking { userRepo.addUser(sampleUser()) }
    val vm = EditProfileViewModel(userRepository = userRepo, currentUserId = "uid-1")

    composeRule.setContent { EditProfileScreen(editScreenViewModel = vm) }
    composeRule.waitForIdle()

    vm.setNewProfileImageUri(Uri.parse("content://local/new"))
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(EditProfileScreenTestTags.PROFILE_PICTURE_PREVIEW).assertIsDisplayed()
  }

  @Test
  fun goBack_invokes_callback() {
    val userRepo = LocalRepositories.UserRepositoryImpl()
    runBlocking { userRepo.addUser(sampleUser()) }
    val vm = EditProfileViewModel(userRepository = userRepo, currentUserId = "uid-1")

    var back = 0
    composeRule.setContent {
      EditProfileScreen(editScreenViewModel = vm, onGoBack = { back++ }, isNewUser = true)
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(EditProfileScreenTestTags.GO_BACK).performClick()
    Assert.assertEquals(1, back)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun save_click_invokes_onSave_when_isNewUser_true() = runTest {
    val userRepo = LocalRepositories.UserRepositoryImpl()
    runBlocking { userRepo.addUser(sampleUser()) }
    val vm = EditProfileViewModel(userRepository = userRepo, currentUserId = "uid-1")

    vm.setName("Jane")
    vm.setSurname("Doe")
    vm.setUsername("jane_doe")
    vm.setNewProfileImageUri(Uri.parse("content://picked/img"))

    var saved = 0
    composeRule.setContent {
      EditProfileScreen(editScreenViewModel = vm, isNewUser = true, onSave = { saved++ })
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(EditProfileScreenTestTags.SAVE).performScrollTo().performClick()
    advanceUntilIdle()
    Assert.assertEquals(1, saved)
  }
}
