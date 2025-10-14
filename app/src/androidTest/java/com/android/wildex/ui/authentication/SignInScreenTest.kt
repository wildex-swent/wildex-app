package com.android.wildex.ui.authentication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.credentials.CredentialManager
import com.android.wildex.utils.FakeAuthRepository
import com.android.wildex.utils.FakeCredentialManager
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SignInScreenTest {
  private lateinit var fakeCredentialManager: CredentialManager
  private lateinit var fakeRepository: FakeAuthRepository
  private lateinit var viewModel: SignInViewModel

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    fakeCredentialManager = FakeCredentialManager.create("fakeToken")
    fakeRepository = FakeAuthRepository()
    viewModel = SignInViewModel(fakeRepository)
  }

  @Test
  fun signInScreenShowsLoginButtonWhenNoUserSignedIn() {
    composeTestRule.setContent {
      SignInScreen(authViewModel = viewModel, credentialManager = fakeCredentialManager)
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOADING_INDICATOR).assertIsNotDisplayed()
  }

  @Test
  fun appLogoIsAlwaysDisplayed() {
    composeTestRule.setContent {
      SignInScreen(authViewModel = viewModel, credentialManager = fakeCredentialManager)
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOADING_INDICATOR).assertIsNotDisplayed()
  }

  @Test
  fun loginButtonVisibleWhenUserIsNull() {
    composeTestRule.setContent {
      SignInScreen(authViewModel = viewModel, credentialManager = fakeCredentialManager)
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.WELCOME).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOADING_INDICATOR).assertIsNotDisplayed()
  }
}
