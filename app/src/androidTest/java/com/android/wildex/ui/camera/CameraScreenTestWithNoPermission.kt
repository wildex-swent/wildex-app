package com.android.wildex.ui.camera

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.test.rule.GrantPermissionRule
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.animaldetector.AnimalDetectResponse
import com.android.wildex.model.animaldetector.AnimalInfoRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.ui.LoadingScreenTestTags
import com.android.wildex.utils.LocalRepositories
import io.mockk.*
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CameraScreenTestWithNoPermission {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.ACCESS_FINE_LOCATION,
      )

  private val postsRepository: PostsRepository = LocalRepositories.postsRepository
  private val storageRepository: StorageRepository = LocalRepositories.storageRepository
  private val animalInfoRepository: AnimalInfoRepository = LocalRepositories.animalInfoRepository
  private val animalRepository: AnimalRepository = LocalRepositories.animalRepository
  private val userAnimalsRepository: UserAnimalsRepository = LocalRepositories.userAnimalsRepository
  private val currentUserId = "currentUserId"

  private lateinit var viewModel: CameraScreenViewModel

  @Before
  fun setup() {
    viewModel =
        CameraScreenViewModel(
            postsRepository,
            storageRepository,
            userAnimalsRepository,
            animalRepository,
            animalInfoRepository,
            currentUserId,
        )
  }

  @After
  fun tearDown() {
    LocalRepositories.clearAll()
  }

  // ========== INITIAL SCREEN DISPLAY TESTS ==========

  @Test
  fun cameraScreen_initialDisplay_onCameraPermission() {
    composeTestRule.setContent { CameraScreen(cameraScreenViewModel = viewModel) }
    composeTestRule.waitForIdle()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CAMERA_PERMISSION_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CAMERA_PREVIEW_SCREEN).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.DETECTING_SCREEN).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.POST_CREATION_SCREEN).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN).assertIsNotDisplayed()
  }

  // ========== PERMISSION SCREEN TESTS =========
  @Test
  fun permissionScreen_canBeShown() {
    composeTestRule.setContent { CameraScreen(cameraScreenViewModel = viewModel) }
    composeTestRule.waitForIdle()
    assertPermissionScreenIsDisplayed()
  }

  @Test
  fun permissionScreen_canUploadImage() {
    val vm = spyk(viewModel)
    val fakeUri = Uri.parse("content://fake/image.jpg")
    val testRegistry =
        object : ActivityResultRegistry() {
          override fun <I, O> onLaunch(
              requestCode: Int,
              contract: ActivityResultContract<I, O>,
              input: I,
              options: ActivityOptionsCompat?,
          ) {
            dispatchResult(requestCode, fakeUri)
          }
        }
    composeTestRule.setContent {
      WithActivityResultRegistry(testRegistry) { CameraScreen(cameraScreenViewModel = vm) }
    }
    composeTestRule.waitForIdle()
    assertPermissionScreenIsDisplayed()

    composeTestRule
        .onNodeWithTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_UPLOAD_BUTTON)
        .performClick()
    composeTestRule.waitForIdle()

    verify { vm.updateImageUri(fakeUri) }
    verify { vm.detectAnimalImage(fakeUri, composeTestRule.activity) }
  }

  // ========== DETECTING SCREEN TESTS ==========

  @Test
  fun detectingScreen_canBeShown() {
    val mockUri = mockk<Uri>()
    val fetchSignal = CompletableDeferred<Unit>()
    val delayedInfoRepo =
        object : LocalRepositories.AnimalInfoRepositoryImpl() {
          override suspend fun detectAnimal(
              context: Context,
              imageUri: Uri,
              coroutineContext: CoroutineContext,
          ): List<AnimalDetectResponse> {
            fetchSignal.await()
            return super.detectAnimal(context, imageUri, coroutineContext)
          }
        }
    val slowDetectVm =
        CameraScreenViewModel(
            postsRepository,
            storageRepository,
            userAnimalsRepository,
            animalRepository,
            delayedInfoRepo,
            currentUserId,
        )
    // Make detection slow so we can see the detecting screen
    composeTestRule.setContent { CameraScreen(cameraScreenViewModel = slowDetectVm) }
    composeTestRule.waitForIdle()
    slowDetectVm.detectAnimalImage(mockUri, composeTestRule.activity)
    composeTestRule.waitForIdle()
    assertDetectingScreenIsDisplayed()
    fetchSignal.complete(Unit)
  }

  // ========== POST CREATION SCREEN TESTS ==========

  @Test
  fun postCreationScreen_canBeShown() {
    val mockUri = mockk<Uri>()
    composeTestRule.setContent { CameraScreen(cameraScreenViewModel = viewModel) }
    composeTestRule.waitForIdle()
    runBlocking { viewModel.detectAnimalImage(mockUri, composeTestRule.activity) }
    composeTestRule.waitForIdle()
    assertPostCreationScreenIsDisplayed()
  }

  @Test
  fun postCreationScreen_inputsDescription() {
    val mockUri = mockk<Uri>()
    composeTestRule.setContent { CameraScreen(cameraScreenViewModel = viewModel) }
    composeTestRule.waitForIdle()
    runBlocking { viewModel.detectAnimalImage(mockUri, composeTestRule.activity) }
    composeTestRule.waitForIdle()
    assertPostCreationScreenIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_DESCRIPTION_FIELD)
        .performScrollTo()
        .performTextInput("Test description")
    assert(viewModel.uiState.value.description == "Test description")
  }

  @Test
  fun postCreationScreen_inputsLocation() {
    val mockUri = mockk<Uri>()
    composeTestRule.setContent { CameraScreen(cameraScreenViewModel = viewModel) }
    composeTestRule.waitForIdle()
    runBlocking { viewModel.detectAnimalImage(mockUri, composeTestRule.activity) }
    composeTestRule.waitForIdle()
    assertPostCreationScreenIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_LOCATION_TOGGLE)
        .performScrollTo()
        .performClick()
    assert(viewModel.uiState.value.addLocation)
    composeTestRule
        .onNodeWithTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_LOCATION_TOGGLE)
        .performScrollTo()
        .performClick()
    assert(!viewModel.uiState.value.addLocation)
  }

  // ========== LOADING SCREEN TESTS ===============
  @Test
  fun loadingScreen_canBeShown() {
    val fetchSignal = CompletableDeferred<Unit>()
    val delayedPostRepo =
        object : LocalRepositories.PostsRepositoryImpl() {
          override suspend fun addPost(post: Post) {
            fetchSignal.await()
            return super.addPost(post)
          }
        }
    val slowPostVm =
        CameraScreenViewModel(
            delayedPostRepo,
            storageRepository,
            userAnimalsRepository,
            animalRepository,
            animalInfoRepository,
            currentUserId,
        )
    val mockUri = Uri.EMPTY
    composeTestRule.setContent { CameraScreen(cameraScreenViewModel = slowPostVm) }
    slowPostVm.detectAnimalImage(mockUri, composeTestRule.activity)
    assertPostCreationScreenIsDisplayed()
    slowPostVm.createPost(composeTestRule.activity) {}
    composeTestRule.onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN).assertIsDisplayed()
    fetchSignal.complete(Unit)
  }

  // ========== STATE TRANSITION TESTS ==========

  @Test
  fun cameraScreen_transition_fromCameraPreviewToDetecting() {
    val mockUri = Uri.EMPTY
    val fetchSignal = CompletableDeferred<Unit>()
    val delayedInfoRepo =
        object : LocalRepositories.AnimalInfoRepositoryImpl() {
          override suspend fun detectAnimal(
              context: Context,
              imageUri: Uri,
              coroutineContext: CoroutineContext,
          ): List<AnimalDetectResponse> {
            fetchSignal.await()
            return super.detectAnimal(context, imageUri, coroutineContext)
          }
        }
    val slowDetectVm =
        CameraScreenViewModel(
            postsRepository,
            storageRepository,
            userAnimalsRepository,
            animalRepository,
            delayedInfoRepo,
            currentUserId,
        )
    // Make detection slow so we can see the detecting screen
    composeTestRule.setContent { CameraScreen(cameraScreenViewModel = slowDetectVm) }
    composeTestRule.waitForIdle()
    assertPermissionScreenIsDisplayed()
    slowDetectVm.detectAnimalImage(mockUri, composeTestRule.activity)
    composeTestRule.waitForIdle()
    assertDetectingScreenIsDisplayed()
    fetchSignal.complete(Unit)
    composeTestRule.waitForIdle()
    assertPostCreationScreenIsDisplayed()
  }

  @Test
  fun cameraScreen_transition_fromPostCreationBackToCameraPermission() {
    val mockUri = mockk<Uri>()
    composeTestRule.setContent { CameraScreen(cameraScreenViewModel = viewModel) }
    runBlocking { viewModel.detectAnimalImage(mockUri, composeTestRule.activity) }
    composeTestRule.waitForIdle()
    composeTestRule.waitForIdle()
    assertPostCreationScreenIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_CANCEL_BUTTON)
        .performScrollTo()
        .performClick()
    composeTestRule.waitForIdle()
    assertPermissionScreenIsDisplayed()
  }

  // ========== ON POST CALLBACK TESTS ==========

  @Test
  fun cameraScreen_onPost_calledAfterSuccessfulPostCreation() {
    val mockUri = Uri.EMPTY
    var onPostCalled = false
    composeTestRule.setContent {
      CameraScreen(cameraScreenViewModel = viewModel) { onPostCalled = true }
    }
    runBlocking { viewModel.detectAnimalImage(mockUri, composeTestRule.activity) }
    composeTestRule.waitForIdle()
    assertPostCreationScreenIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_CONFIRM_BUTTON)
        .performScrollTo()
        .performClick()
    composeTestRule.waitForIdle()
    assert(onPostCalled)
  }

  @Test
  fun cameraScreen_onPost_notCalledOnCancel() {
    val mockUri = Uri.EMPTY
    var onPostCalled = false
    val delayedPostRepo =
        object : LocalRepositories.PostsRepositoryImpl() {
          override suspend fun addPost(post: Post) {
            throw Exception("Post creation failed")
          }
        }
    val slowPostVm =
        CameraScreenViewModel(
            delayedPostRepo,
            storageRepository,
            userAnimalsRepository,
            animalRepository,
            animalInfoRepository,
            currentUserId,
        )
    composeTestRule.setContent {
      CameraScreen(cameraScreenViewModel = slowPostVm, onPost = { onPostCalled = true })
    }
    composeTestRule.waitForIdle()
    runBlocking { slowPostVm.detectAnimalImage(mockUri, composeTestRule.activity) }
    composeTestRule.waitForIdle()
    assertPostCreationScreenIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_CANCEL_BUTTON)
        .performScrollTo()
        .performClick()
    composeTestRule.waitForIdle()
    assert(!onPostCalled)
  }

  private fun assertDetectingScreenIsDisplayed() {
    composeTestRule.onNodeWithTag(CameraScreenTestTags.DETECTING_SCREEN).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(DetectingScreenTestTags.DETECTING_SCREEN_ANIMATION)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(DetectingScreenTestTags.DETECTING_SCREEN_IMAGE)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(DetectingScreenTestTags.DETECTING_SCREEN_LOADING_BAR)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(DetectingScreenTestTags.DETECTING_SCREEN_PHRASE_1)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(DetectingScreenTestTags.DETECTING_SCREEN_PHRASE_2)
        .assertIsDisplayed()
  }

  private fun assertPostCreationScreenIsDisplayed() {
    composeTestRule.onNodeWithTag(CameraScreenTestTags.POST_CREATION_SCREEN).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_ANIMAL_NAME)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_CANCEL_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_CONFIDENCE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_CONFIRM_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_DESCRIPTION_FIELD)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_FAMILY_BADGE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_HEADER_IMAGE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_LOCATION_TOGGLE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_SPECIES_NAME)
        .performScrollTo()
        .assertIsDisplayed()
  }

  private fun assertPermissionScreenIsDisplayed() {
    composeTestRule
        .onNodeWithTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_CAMERA_ICON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_MESSAGE_1)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_MESSAGE_2)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraPermissionScreenTestTags.CAMERA_PERMISSION_UPLOAD_BUTTON)
        .assertIsDisplayed()
  }

  @Composable
  private fun WithActivityResultRegistry(
      activityResultRegistry: ActivityResultRegistry,
      content: @Composable () -> Unit,
  ) {
    val activityResultRegistryOwner =
        object : ActivityResultRegistryOwner {
          override val activityResultRegistry = activityResultRegistry
        }
    CompositionLocalProvider(
        LocalActivityResultRegistryOwner provides activityResultRegistryOwner) {
          content()
        }
  }
}
