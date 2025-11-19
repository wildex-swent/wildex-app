package com.android.wildex.ui.camera

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.animaldetector.AnimalDetectResponse
import com.android.wildex.model.animaldetector.AnimalInfoRepository
import com.android.wildex.model.animaldetector.Taxonomy
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.user.UserAnimalsRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraScreenViewModelTest {

  private lateinit var viewModel: CameraScreenViewModel
  private lateinit var postsRepository: PostsRepository
  private lateinit var storageRepository: StorageRepository
  private lateinit var userAnimalsRepository: UserAnimalsRepository
  private lateinit var animalRepository: AnimalRepository
  private lateinit var animalInfoRepository: AnimalInfoRepository
  private lateinit var context: Context

  private val testUserId = "test-user-123"
  private val testPostId = "test-post-123"
  private val testAnimalId = "test-animal-123"
  private val testImageUrl = "https://test.com/image.jpg"

  @Before
  fun setup() {

    postsRepository = mockk(relaxed = true)
    storageRepository = mockk(relaxed = true)
    userAnimalsRepository = mockk(relaxed = true)
    animalRepository = mockk(relaxed = true)
    animalInfoRepository = mockk(relaxed = true)
    context = mockk(relaxed = true)

    viewModel =
        CameraScreenViewModel(
            postsRepository = postsRepository,
            storageRepository = storageRepository,
            userAnimalsRepository = userAnimalsRepository,
            animalRepository = animalRepository,
            animalInfoRepository = animalInfoRepository,
            currentUserId = testUserId,
        )
    Dispatchers.setMain(StandardTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is correct`() {
    val state = viewModel.uiState.value
    assertNull(state.animalDetectResponse)
    assertNull(state.currentImageUri)
    assertEquals("", state.description)
    assertFalse(state.addLocation)
    assertNull(state.errorMsg)
    assertFalse(state.isLoading)
    assertFalse(state.isDetecting)
  }

  @Test
  fun `resetState clears all state`() {
    viewModel.updateDescription("Test description")
    viewModel.toggleAddLocation()
    viewModel.resetState()
    val state = viewModel.uiState.value
    assertNull(state.animalDetectResponse)
    assertNull(state.currentImageUri)
    assertEquals("", state.description)
    assertFalse(state.addLocation)
    assertNull(state.errorMsg)
    assertFalse(state.isLoading)
    assertFalse(state.isDetecting)
  }

  @Test
  fun `updateImageUri updates the image uri in state`() {
    val uri = mockk<Uri>()
    viewModel.updateImageUri(uri)
    assertEquals(uri, viewModel.uiState.value.currentImageUri)
  }

  @Test
  fun `updateDescription updates the description in state`() {
    val description = "Beautiful eagle spotted"
    viewModel.updateDescription(description)
    assertEquals(description, viewModel.uiState.value.description)
  }

  @Test
  fun `toggleAddLocation toggles the boolean value`() {
    assertFalse(viewModel.uiState.value.addLocation)
    viewModel.toggleAddLocation()
    assertTrue(viewModel.uiState.value.addLocation)
    viewModel.toggleAddLocation()
    assertFalse(viewModel.uiState.value.addLocation)
  }

  @Test
  fun `detectAnimalImage failure sets error and clearErrorMsg clears the error message`() =
      runTest {
        coEvery { animalInfoRepository.detectAnimal(any(), any()) } throws Exception("Boom!")
        viewModel.detectAnimalImage(mockk(), context)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMsg)
        viewModel.clearErrorMsg()
        assertNull(viewModel.uiState.value.errorMsg)
      }

  @Test
  fun `detectAnimalImage success updates state correctly`() = runTest {
    val uri = mockk<Uri>()
    val taxonomy = Taxonomy(id = testAnimalId, species = "Aquila chrysaetos")
    val response =
        AnimalDetectResponse(
            animalType = "Golden Eagle",
            taxonomy = taxonomy,
            confidence = 0.95f,
        )
    coEvery { animalInfoRepository.detectAnimal(context, uri) } returns listOf(response)
    viewModel.detectAnimalImage(uri, context)
    advanceUntilIdle()
    val state = viewModel.uiState.value
    assertEquals(response, state.animalDetectResponse)
    assertEquals(uri, state.currentImageUri)
    assertFalse(state.isDetecting)
  }

  @Test
  fun `detectAnimalImage sets isDetecting to true during detection`() = runTest {
    val uri = mockk<Uri>()
    val taxonomy = Taxonomy(id = testAnimalId, species = "Test species")
    val response =
        AnimalDetectResponse(
            animalType = "Test Animal",
            taxonomy = taxonomy,
            confidence = 0.8f,
        )
    coEvery { animalInfoRepository.detectAnimal(context, uri) } coAnswers
        {
          assertTrue(viewModel.uiState.value.isDetecting)
          listOf(response)
        }
    viewModel.detectAnimalImage(uri, context)
    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.isDetecting)
  }

  @Test
  fun `createPost success creates post and registers animal and calls onPost`() = runTest {
    val uri = mockk<Uri>()
    val taxonomy = Taxonomy(id = testAnimalId, species = "Aquila chrysaetos")
    val response =
        AnimalDetectResponse(
            animalType = "Golden Eagle",
            taxonomy = taxonomy,
            confidence = 0.95f,
        )
    val description = "Amazing sighting!"
    val location =
        android.location.Location("test").apply {
          latitude = 46.5
          longitude = 6.5
        }
    val fusedLocationClient = mockk<FusedLocationProviderClient>()
    mockkStatic("com.google.android.gms.location.LocationServices")
    every { LocationServices.getFusedLocationProviderClient(context) } returns fusedLocationClient
    every { fusedLocationClient.lastLocation } returns Tasks.forResult(location)
    viewModel.updateImageUri(uri)
    viewModel.updateDescription(description)
    coEvery { animalInfoRepository.detectAnimal(context, uri) } returns listOf(response)
    viewModel.detectAnimalImage(uri, context)
    advanceUntilIdle()
    coEvery { postsRepository.getNewPostId() } returns testPostId
    coEvery { storageRepository.uploadPostImage(testPostId, uri) } returns testImageUrl
    coEvery { animalInfoRepository.getAnimalDescription("Golden Eagle") } returns "A majestic bird"
    coEvery { storageRepository.uploadAnimalPicture(testAnimalId, any()) } returns ""
    coEvery { postsRepository.addPost(any()) } just Runs
    coEvery { animalRepository.addAnimal(any()) } just Runs
    coEvery { userAnimalsRepository.addAnimalToUserAnimals(testUserId, testAnimalId) } just Runs
    var onPostCalled = false
    viewModel.createPost(context) { onPostCalled = true }
    advanceUntilIdle()
    assertTrue(onPostCalled)
    coVerify { postsRepository.addPost(any()) }
    coVerify { animalRepository.addAnimal(any()) }
    coVerify { userAnimalsRepository.addAnimalToUserAnimals(testUserId, testAnimalId) }
    assertNull(viewModel.uiState.value.currentImageUri)
    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun `createPost returns early if currentImageUri is null`() = runTest {
    viewModel.resetState()
    var onPostCalled = false
    viewModel.createPost(context) { onPostCalled = true }
    advanceUntilIdle()
    assertFalse(onPostCalled)
    coVerify(exactly = 0) { postsRepository.addPost(any()) }
  }

  @Test
  fun `createPost returns early if animalDetectResponse is null`() = runTest {
    viewModel.resetState()
    val uri = mockk<Uri>()
    viewModel.updateImageUri(uri)
    var onPostCalled = false
    viewModel.createPost(context) { onPostCalled = true }
    advanceUntilIdle()
    assertFalse(onPostCalled)
    coVerify(exactly = 0) { postsRepository.addPost(any()) }
  }

  @Test
  fun `createPost failure sets error message and keeps loading false`() = runTest {
    val uri = mockk<Uri>()
    val taxonomy = Taxonomy(id = testAnimalId, species = "Test species")
    val response =
        AnimalDetectResponse(
            animalType = "Test Animal",
            taxonomy = taxonomy,
            confidence = 0.8f,
        )
    viewModel.updateImageUri(uri)
    coEvery { animalInfoRepository.detectAnimal(context, uri) } returns listOf(response)
    viewModel.detectAnimalImage(uri, context)
    advanceUntilIdle()
    coEvery { postsRepository.getNewPostId() } throws Exception("Database error")
    var onPostCalled = false
    viewModel.createPost(context) { onPostCalled = true }
    advanceUntilIdle()
    assertFalse(onPostCalled)
    assertNotNull(viewModel.uiState.value.errorMsg)
    assertTrue(viewModel.uiState.value.errorMsg!!.contains("Failed to create post"))
    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun `createPost uses correct post data`() = runTest {
    viewModel.resetState()
    val uri = mockk<Uri>()
    val taxonomy = Taxonomy(id = testAnimalId, species = "Aquila chrysaetos")
    val response =
        AnimalDetectResponse(
            animalType = "Golden Eagle",
            taxonomy = taxonomy,
            confidence = 0.95f,
        )
    val description = "Test description"
    val location =
        android.location.Location("test").apply {
          latitude = 46.5
          longitude = 6.5
        }
    mockkStatic("com.google.android.gms.location.LocationServices")
    val fusedLocationClient = mockk<FusedLocationProviderClient>()
    every { LocationServices.getFusedLocationProviderClient(context) } returns fusedLocationClient
    every { fusedLocationClient.lastLocation } returns Tasks.forResult(location)
    viewModel.updateImageUri(uri)
    viewModel.toggleAddLocation()
    viewModel.updateDescription(description)
    coEvery { animalInfoRepository.detectAnimal(context, uri) } returns listOf(response)
    viewModel.detectAnimalImage(uri, context)
    advanceUntilIdle()

    coEvery { postsRepository.getNewPostId() } returns testPostId
    coEvery { storageRepository.uploadPostImage(testPostId, uri) } returns testImageUrl
    coEvery { animalInfoRepository.getAnimalDescription(any()) } returns "Description"
    coEvery { storageRepository.uploadAnimalPicture(any(), any()) } returns ""
    coEvery { postsRepository.addPost(any()) } just Runs
    coEvery { animalRepository.addAnimal(any()) } just Runs
    coEvery { userAnimalsRepository.addAnimalToUserAnimals(any(), any()) } just Runs
    viewModel.createPost(context) {}
    advanceUntilIdle()

    coVerify {
      postsRepository.addPost(
          match { post ->
            val loc = post.location
            post.postId == testPostId &&
                post.authorId == testUserId &&
                post.description == description &&
                post.pictureURL == testImageUrl &&
                post.animalId == testAnimalId &&
                loc != null &&
                loc.latitude == location.latitude &&
                loc.longitude == location.longitude
          })
    }
  }
}
