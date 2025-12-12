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
  fun initial_UI_state_defaults() {
    val s = viewModel.uiState.value
    Assert.assertEquals("", s.name)
    Assert.assertEquals("", s.surname)
    Assert.assertEquals("", s.username)
    Assert.assertEquals("", s.description)
    Assert.assertEquals("", s.country)
    Assert.assertFalse(s.isLoading)
    Assert.assertFalse(s.isError)
    Assert.assertNull(s.errorMsg)
  }

  @Test
  fun loadUIState_success_populatesFields() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getAllUsers() } returns listOf(u1)
        coEvery { userRepository.getUser("uid-1") } returns u1
        viewModel.loadUIState()
        advanceUntilIdle()
        val s = viewModel.uiState.value
        Assert.assertEquals(u1.name, s.name)
        Assert.assertEquals(u1.surname, s.surname)
        Assert.assertEquals(u1.username, s.username)
        Assert.assertEquals(u1.bio, s.description)
        Assert.assertEquals(u1.country, s.country)
        Assert.assertFalse(s.isLoading)
        Assert.assertFalse(s.isError)
      }

  @Test
  fun loadUIState_error_setsError() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getAllUsers() } returns emptyList()

        var calls = 0
        coEvery { userRepository.getUser("uid-1") } answers
            {
              calls++
              if (calls == 1) u1 else throw RuntimeException("boom")
            }

        viewModel.loadUIState()
        advanceUntilIdle()
        val s = viewModel.uiState.value
        Assert.assertTrue(s.isError)
        Assert.assertEquals("Unexpected error: boom", s.errorMsg)
      }

  @Test
  fun saveProfileChanges_invalid_setsError_andNoRepoCalls() {
    viewModel.saveProfileChanges {}

    Assert.assertEquals("At least one field is not valid", viewModel.uiState.value.errorMsg)
    coVerify(exactly = 0) { userRepository.getUser(any()) }
    coVerify(exactly = 0) { storageRepository.uploadUserProfilePicture(any(), any()) }
    coVerify(exactly = 0) { userRepository.editUser(any(), any()) }
    confirmVerified(userRepository, storageRepository)
  }

  @Test
  fun saveProfileChanges_success_uploads_and_edits_user() =
      mainDispatcherRule.runTest {
        viewModel.setName("A")
        viewModel.setSurname("B")
        viewModel.setUsername("C")
        viewModel.setDescription("D")
        val anyUri = mockk<Uri>(relaxed = true)
        viewModel.setNewProfileImageUri(anyUri)

        coEvery { userRepository.getUser("uid-1") } returns u1
        coEvery { storageRepository.uploadUserProfilePicture("uid-1", any()) } returns "newPic"
        coEvery { userRepository.editUser(any(), any()) } returns Unit

        viewModel.saveProfileChanges {}

        advanceUntilIdle()

        val captured = slot<User>()
        coVerify(exactly = 1) { storageRepository.uploadUserProfilePicture("uid-1", anyUri) }
        coVerify(exactly = 1) { userRepository.getUser("uid-1") }
        coVerify(exactly = 1) { userRepository.editUser("uid-1", capture(captured)) }
        confirmVerified(userRepository, storageRepository)
        Assert.assertEquals("newPic", captured.captured.profilePictureURL)
        Assert.assertNull(viewModel.uiState.value.errorMsg)
      }

  @Test
  fun saveProfileChanges_uploadThrows_keepsOldUrl() =
      mainDispatcherRule.runTest {
        viewModel.setName("A")
        viewModel.setSurname("B")
        viewModel.setUsername("C")
        viewModel.setDescription("D")
        val pending = mockk<Uri>(relaxed = true)
        viewModel.setNewProfileImageUri(pending)
        coEvery { userRepository.getUser("uid-1") } returns u1
        coEvery { storageRepository.uploadUserProfilePicture("uid-1", any()) } throws
            Exception("Boom")
        coEvery { userRepository.editUser(any(), any()) } returns Unit

        viewModel.saveProfileChanges {}

        advanceUntilIdle()

        val captured = slot<User>()
        coVerify(exactly = 1) { storageRepository.uploadUserProfilePicture("uid-1", pending) }
        coVerify(exactly = 1) { userRepository.getUser("uid-1") }
        coVerify(exactly = 1) { userRepository.editUser("uid-1", capture(captured)) }
        confirmVerified(userRepository, storageRepository)
        Assert.assertEquals("oldPic", captured.captured.profilePictureURL)
      }

  @Test
  fun setters_validation_and_isValid() {
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

    Assert.assertTrue(viewModel.uiState.value.isValid)
  }

  @Test
  fun setCountry_updatesState() {
    viewModel.setCountry("France")
    Assert.assertEquals("France", viewModel.uiState.value.country)
  }

  @Test
  fun saveProfileChanges_usesCountry() =
      mainDispatcherRule.runTest {
        viewModel.setName("A")
        viewModel.setSurname("B")
        viewModel.setUsername("C")
        viewModel.setDescription("D")
        viewModel.setCountry("France")
        val img = mockk<Uri>(relaxed = true)
        viewModel.setNewProfileImageUri(img)

        coEvery { userRepository.getUser("uid-1") } returns u1
        coEvery { storageRepository.uploadUserProfilePicture("uid-1", any()) } returns "pic"
        coEvery { userRepository.editUser(any(), any()) } returns Unit

        viewModel.saveProfileChanges {}

        advanceUntilIdle()

        val captured = slot<User>()
        coVerify { userRepository.editUser("uid-1", capture(captured)) }
        Assert.assertEquals("France", captured.captured.country)
      }

  @Test
  fun setNewProfileImageUri_updatesUIState() {
    val uri = mockk<Uri>(relaxed = true)
    viewModel.setNewProfileImageUri(uri)
    Assert.assertEquals(uri, viewModel.uiState.value.pendingProfileImageUri)
  }

  @Test
  fun saveProfileChanges_setsProfileSaved_afterSuccess() =
      mainDispatcherRule.runTest {
        viewModel.setName("A")
        viewModel.setSurname("B")
        viewModel.setUsername("C")
        viewModel.setDescription("D")
        val pending = mockk<Uri>(relaxed = true)
        viewModel.setNewProfileImageUri(pending)

        coEvery { userRepository.getUser("uid-1") } returns u1
        coEvery { storageRepository.uploadUserProfilePicture("uid-1", any()) } returns "newPic"
        coEvery { userRepository.editUser(any(), any()) } returns Unit

        viewModel.saveProfileChanges {}

        advanceUntilIdle()

        Assert.assertTrue(viewModel.uiState.value.profileSaved)
        Assert.assertEquals(pending, viewModel.uiState.value.pendingProfileImageUri)
      }
}
