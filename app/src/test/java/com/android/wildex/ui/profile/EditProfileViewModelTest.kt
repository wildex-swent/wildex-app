package com.android.wildex.ui.profile

import android.net.Uri
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var userRepository: UserRepository
  private lateinit var storageRepository: StorageRepository
  private lateinit var viewModel: EditProfileViewModel

  private val u1 =
      User(
          userId = "uid-1",
          username = "user_one",
          name = "First",
          surname = "User",
          bio = "bio",
          profilePictureURL = "oldPic",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "X",
          friendsCount = 1,
      )

  @Before
  fun setUp() {
    userRepository = mockk()
    storageRepository = mockk()
    viewModel =
        EditProfileViewModel(
            userRepository = userRepository,
            storageRepository = storageRepository,
            currentUserId = "uid-1",
        )
  }

  @Test
  fun viewModel_initializes_default_UI_state_isEmptyNotLoading() {
    val s = viewModel.uiState.value
    Assert.assertEquals("", s.name)
    Assert.assertEquals("", s.surname)
    Assert.assertEquals("", s.username)
    Assert.assertEquals("I am ...", s.description)
    Assert.assertEquals("Switzerland", s.country)
    Assert.assertEquals("", s.profileImageUrl)
    Assert.assertFalse(s.isLoading)
    Assert.assertFalse(s.isError)
    Assert.assertNull(s.errorMsg)
  }

  @Test
  fun loadUIState_success_populatesFields_andStopsLoading() {
    mainDispatcherRule.runTest {
      coEvery { userRepository.getUser("uid-1") } returns u1

      viewModel.loadUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals(u1.name, s.name)
      Assert.assertEquals(u1.surname, s.surname)
      Assert.assertEquals(u1.username, s.username)
      Assert.assertEquals(u1.bio, s.description)
      Assert.assertEquals(u1.country, s.country)
      Assert.assertEquals(u1.profilePictureURL, s.profileImageUrl)
      Assert.assertFalse(s.isLoading)
      Assert.assertFalse(s.isError)
      Assert.assertNull(s.errorMsg)
    }
  }

  @Test
  fun loadUIState_error_setsError_andStopsLoading() {
    mainDispatcherRule.runTest {
      coEvery { userRepository.getUser("uid-1") } throws RuntimeException("boom")

      viewModel.loadUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertTrue(s.isError)
      Assert.assertEquals("Unexpected error: boom", s.errorMsg)
      Assert.assertFalse(s.isLoading)
    }
  }

  @Test
  fun loadUIState_withBlankCurrentUserId_setsError_andStopsLoading() {
    mainDispatcherRule.runTest {
      viewModel =
          EditProfileViewModel(
              userRepository = userRepository,
              storageRepository = storageRepository,
              currentUserId = "",
          )

      viewModel.loadUIState()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals("Empty user id", s.errorMsg)
      Assert.assertTrue(s.isError)
      Assert.assertFalse(s.isLoading)
    }
  }

  @Test
  fun saveProfileChanges_whenFormInvalid_setsError_andDoesNotCallRepos() {
    viewModel.saveProfileChanges()

    val s = viewModel.uiState.value
    Assert.assertEquals("At least one field is not valid", s.errorMsg)

    coVerify(exactly = 0) { userRepository.getUser(any()) }
    coVerify(exactly = 0) { storageRepository.deleteUserProfilePicture(any()) }
    coVerify(exactly = 0) { storageRepository.uploadUserProfilePicture(any(), any()) }
    coVerify(exactly = 0) { userRepository.editUser(any(), any()) }
    confirmVerified(userRepository, storageRepository)
  }

  @Test
  fun saveProfileChanges_success_deletesUploadsAndEditsUser_andClearsError() {
    mainDispatcherRule.runTest {
      viewModel.setName("A")
      viewModel.setSurname("B")
      viewModel.setUsername("C")
      viewModel.setDescription("D")
      viewModel.setNewProfileImageUrl("ignored")

      val anyUri = mockk<Uri>(relaxed = true)

      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { storageRepository.deleteUserProfilePicture("uid-1") } returns Unit
      coEvery { storageRepository.uploadUserProfilePicture("uid-1", any()) } returns "newPic"
      coEvery { userRepository.editUser(any(), any()) } returns Unit

      viewModel.saveProfileChanges(anyUri)
      advanceUntilIdle()

      val captured = slot<User>()
      coVerify(exactly = 1) { storageRepository.deleteUserProfilePicture("uid-1") }
      coVerify(exactly = 1) { storageRepository.uploadUserProfilePicture("uid-1", anyUri) }
      coVerify(exactly = 1) { userRepository.getUser("uid-1") }
      coVerify(exactly = 1) { userRepository.editUser("uid-1", capture(captured)) }
      confirmVerified(userRepository, storageRepository)

      val edited = captured.captured
      Assert.assertEquals("A", edited.name)
      Assert.assertEquals("B", edited.surname)
      Assert.assertEquals("C", edited.username)
      Assert.assertEquals("D", edited.bio)
      Assert.assertEquals("newPic", edited.profilePictureURL)
      Assert.assertNull(viewModel.uiState.value.errorMsg)
    }
  }

  @Test
  fun saveProfileChanges_whenUploadFails_setsError_andDoesNotEditUser() {
    mainDispatcherRule.runTest {
      viewModel.setName("A")
      viewModel.setSurname("B")
      viewModel.setUsername("C")
      viewModel.setDescription("D")

      val anyUri = mockk<Uri>(relaxed = true)

      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { storageRepository.deleteUserProfilePicture("uid-1") } returns Unit
      coEvery { storageRepository.uploadUserProfilePicture("uid-1", any()) } throws
          RuntimeException("x")

      viewModel.saveProfileChanges(anyUri)
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals("Failed to save profile changes: x", s.errorMsg)

      coVerify(exactly = 1) { userRepository.getUser("uid-1") }
      coVerify(exactly = 1) { storageRepository.deleteUserProfilePicture("uid-1") }
      coVerify(exactly = 1) { storageRepository.uploadUserProfilePicture("uid-1", anyUri) }
      coVerify(exactly = 0) { userRepository.editUser(any(), any()) }
      confirmVerified(userRepository, storageRepository)
    }
  }

  @Test
  fun saveProfileChanges_withNoImage_keepsExistingUrl_andEditsUserWithoutStorageCalls() {
    mainDispatcherRule.runTest {
      viewModel.setName("A")
      viewModel.setSurname("B")
      viewModel.setUsername("C")
      viewModel.setDescription("D")

      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { userRepository.editUser(any(), any()) } returns Unit

      viewModel.saveProfileChanges()
      advanceUntilIdle()

      val captured = slot<User>()
      coVerify(exactly = 0) { storageRepository.deleteUserProfilePicture(any()) }
      coVerify(exactly = 0) { storageRepository.uploadUserProfilePicture(any(), any()) }
      coVerify(exactly = 1) { userRepository.getUser("uid-1") }
      coVerify(exactly = 1) { userRepository.editUser("uid-1", capture(captured)) }
      confirmVerified(userRepository, storageRepository)

      val edited = captured.captured
      Assert.assertEquals("oldPic", edited.profilePictureURL)
      Assert.assertNull(viewModel.uiState.value.errorMsg)
    }
  }

  @Test
  fun setters_validation_toggle_and_isValid_true_when_all_fields_non_blank() {
    viewModel.setName("")
    Assert.assertEquals("Name cannot be empty", viewModel.uiState.value.invalidNameMsg)
    viewModel.setName("John")
    Assert.assertNull(viewModel.uiState.value.invalidNameMsg)

    viewModel.setSurname("")
    Assert.assertEquals("Surname cannot be empty", viewModel.uiState.value.invalidSurnameMsg)
    viewModel.setSurname("Doe")
    Assert.assertNull(viewModel.uiState.value.invalidSurnameMsg)

    viewModel.setUsername("")
    Assert.assertEquals("Username cannot be empty", viewModel.uiState.value.invalidUsernameMsg)
    viewModel.setUsername("jdoe")
    Assert.assertNull(viewModel.uiState.value.invalidUsernameMsg)

    viewModel.setDescription("")
    Assert.assertEquals("Bio cannot be empty", viewModel.uiState.value.invalidDescriptionMsg)
    viewModel.setDescription("desc")
    Assert.assertNull(viewModel.uiState.value.invalidDescriptionMsg)

    Assert.assertTrue(viewModel.uiState.value.isValid)
  }

  @Test
  fun saveProfileChanges_whenGetUserFails_setsError_andNoStorageOrEdit() {
    mainDispatcherRule.runTest {
      viewModel.setName("A")
      viewModel.setSurname("B")
      viewModel.setUsername("C")
      viewModel.setDescription("D")

      coEvery { userRepository.getUser("uid-1") } throws RuntimeException("boom")

      viewModel.saveProfileChanges()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals("Failed to save profile changes: boom", s.errorMsg)

      coVerify(exactly = 1) { userRepository.getUser("uid-1") }
      coVerify(exactly = 0) { storageRepository.deleteUserProfilePicture(any()) }
      coVerify(exactly = 0) { storageRepository.uploadUserProfilePicture(any(), any()) }
      coVerify(exactly = 0) { userRepository.editUser(any(), any()) }
      confirmVerified(userRepository, storageRepository)
    }
  }

  @Test
  fun saveProfileChanges_whenEditUserFails_setsError() {
    mainDispatcherRule.runTest {
      viewModel.setName("A")
      viewModel.setSurname("B")
      viewModel.setUsername("C")
      viewModel.setDescription("D")

      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { userRepository.editUser(any(), any()) } throws RuntimeException("edit-failed")

      viewModel.saveProfileChanges()
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals("Failed to save profile changes: edit-failed", s.errorMsg)

      coVerify(exactly = 1) { userRepository.getUser("uid-1") }
      coVerify(exactly = 1) { userRepository.editUser("uid-1", any()) }
      coVerify(exactly = 0) { storageRepository.deleteUserProfilePicture(any()) }
      coVerify(exactly = 0) { storageRepository.uploadUserProfilePicture(any(), any()) }
      confirmVerified(userRepository, storageRepository)
    }
  }

  @Test
  fun saveProfileChanges_uploadReturnsNull_keepsExistingUrl() {
    mainDispatcherRule.runTest {
      viewModel.setName("A")
      viewModel.setSurname("B")
      viewModel.setUsername("C")
      viewModel.setDescription("D")

      val anyUri = mockk<Uri>(relaxed = true)

      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { storageRepository.deleteUserProfilePicture("uid-1") } returns Unit
      coEvery { storageRepository.uploadUserProfilePicture("uid-1", any()) } returns null
      coEvery { userRepository.editUser(any(), any()) } returns Unit

      viewModel.saveProfileChanges(anyUri)
      advanceUntilIdle()

      val captured = slot<User>()
      coVerify(exactly = 1) { storageRepository.deleteUserProfilePicture("uid-1") }
      coVerify(exactly = 1) { storageRepository.uploadUserProfilePicture("uid-1", anyUri) }
      coVerify(exactly = 1) { userRepository.getUser("uid-1") }
      coVerify(exactly = 1) { userRepository.editUser("uid-1", capture(captured)) }
      confirmVerified(userRepository, storageRepository)

      Assert.assertEquals("oldPic", captured.captured.profilePictureURL)
    }
  }

  @Test
  fun clearErrorMsg_clearsPreviousError() {
    viewModel.saveProfileChanges()
    Assert.assertNotNull(viewModel.uiState.value.errorMsg)

    viewModel.clearErrorMsg()
    Assert.assertNull(viewModel.uiState.value.errorMsg)
  }
}
