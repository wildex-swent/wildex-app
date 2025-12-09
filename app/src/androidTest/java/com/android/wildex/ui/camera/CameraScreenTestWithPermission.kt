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
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.animaldetector.AnimalDetectResponse
import com.android.wildex.model.animaldetector.AnimalInfoRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.ui.LoadingScreenTestTags
import com.android.wildex.utils.LocalRepositories
import com.android.wildex.utils.offline.FakeConnectivityObserver
import io.mockk.*
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CameraScreenTestWithPermission {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.CAMERA,
      )

  private val postsRepository: PostsRepository = LocalRepositories.postsRepository
  private val storageRepository: StorageRepository = LocalRepositories.storageRepository
  private val animalInfoRepository: AnimalInfoRepository = LocalRepositories.animalInfoRepository
  private val animalRepository: AnimalRepository = LocalRepositories.animalRepository
  private val userAnimalsRepository: UserAnimalsRepository = LocalRepositories.userAnimalsRepository
  private val currentUserId = "currentUserId"
  private val fakeObserver = FakeConnectivityObserver(initial = true)

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
  fun cameraScreen_initialDisplay_onCameraPreview() {
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        CameraScreen(cameraScreenViewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(CameraScreenTestTags.CAMERA_PERMISSION_SCREEN)
        .assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CAMERA_PREVIEW_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.DETECTING_SCREEN).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.POST_CREATION_SCREEN).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN).assertIsNotDisplayed()
  }

  // ========== PREVIEW SCREEN TESTS =========
  @Test
  fun previewScreen_canBeShown() {
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        CameraScreen(cameraScreenViewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
    assertPreviewScreenIsDisplayed()
  }

  @Test
  fun previewScreen_canUploadImage() {
    fakeObserver.setOnline(true)
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
      WithActivityResultRegistry(testRegistry) {
        CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
          CameraScreen(cameraScreenViewModel = vm)
        }
      }
    }
    composeTestRule.waitForIdle()
    assertPreviewScreenIsDisplayed()

    composeTestRule
        .onNodeWithTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_UPLOAD_BUTTON)
        .performClick()
    composeTestRule.waitForIdle()

    verify { vm.updateImageUri(fakeUri) }
    verify { vm.detectAnimalImage(fakeUri, composeTestRule.activity) }
  }

  @Test
  fun previewScreen_canSwitchAndCapture() {
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        CameraScreen(cameraScreenViewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_CAPTURE_BUTTON)
        .performClick()

    viewModel.resetState()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_SWITCH_BUTTON)
        .performClick()
    // Can capture while switched
    composeTestRule
        .onNodeWithTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_CAPTURE_BUTTON)
        .performClick()
    viewModel.resetState()
    composeTestRule.waitForIdle()
    // Can switch back
    composeTestRule
        .onNodeWithTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_SWITCH_BUTTON)
        .performClick()
  }

  // ========== DETECTING SCREEN TESTS ==========

  @Test
  fun detectingScreen_canBeShown() {
    fakeObserver.setOnline(true)
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
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        CameraScreen(cameraScreenViewModel = slowDetectVm)
      }
    }
    composeTestRule.waitForIdle()
    slowDetectVm.detectAnimalImage(mockUri, composeTestRule.activity)
    composeTestRule.waitForIdle()
    assertDetectingScreenIsDisplayed()
    fetchSignal.complete(Unit)
  }

  // ========== POST CREATION SCREEN TESTS ==========

  @Test
  fun postCreationScreen_canBeShown() {
    fakeObserver.setOnline(true)
    val mockUri = mockk<Uri>()
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        CameraScreen(cameraScreenViewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
    runBlocking { viewModel.detectAnimalImage(mockUri, composeTestRule.activity) }
    composeTestRule.waitForIdle()
    assertPostCreationScreenIsDisplayed()
  }

  @Test
  fun postCreationScreen_inputsDescription() {
    fakeObserver.setOnline(true)
    val mockUri = mockk<Uri>()
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        CameraScreen(cameraScreenViewModel = viewModel)
      }
    }
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
    fakeObserver.setOnline(true)
    val mockUri = mockk<Uri>()
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        CameraScreen(cameraScreenViewModel = viewModel)
      }
    }
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
    fakeObserver.setOnline(true)
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
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        CameraScreen(cameraScreenViewModel = slowPostVm)
      }
    }
    composeTestRule.waitForIdle()
    slowPostVm.detectAnimalImage(mockUri, composeTestRule.activity)
    assertPostCreationScreenIsDisplayed()
    slowPostVm.createPost(composeTestRule.activity) {}
    composeTestRule.onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN).assertIsDisplayed()
    fetchSignal.complete(Unit)
  }

  // ========== STATE TRANSITION TESTS ==========

  @Test
  fun cameraScreen_transition_fromCameraPreviewToDetecting() {
    fakeObserver.setOnline(true)
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
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        CameraScreen(cameraScreenViewModel = slowDetectVm)
      }
    }
    composeTestRule.waitForIdle()
    assertPreviewScreenIsDisplayed()
    slowDetectVm.detectAnimalImage(mockUri, composeTestRule.activity)
    composeTestRule.waitForIdle()
    assertDetectingScreenIsDisplayed()
    fetchSignal.complete(Unit)
    composeTestRule.waitForIdle()
    assertPostCreationScreenIsDisplayed()
  }

  @Test
  fun cameraScreen_transition_fromPostCreationBackToCameraPreview() {
    fakeObserver.setOnline(true)
    val mockUri = mockk<Uri>()
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        CameraScreen(cameraScreenViewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
    runBlocking { viewModel.detectAnimalImage(mockUri, composeTestRule.activity) }
    composeTestRule.waitForIdle()
    assertPostCreationScreenIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostCreationScreenTestTags.POST_CREATION_SCREEN_CANCEL_BUTTON)
        .performScrollTo()
        .performClick()
    composeTestRule.waitForIdle()
    assertPreviewScreenIsDisplayed()
  }

  // ========== ON POST CALLBACK TESTS ==========

  @Test
  fun cameraScreen_onPost_calledAfterSuccessfulPostCreation() {
    fakeObserver.setOnline(true)
    val mockUri = Uri.EMPTY
    var onPostCalled = false
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        CameraScreen(cameraScreenViewModel = viewModel) { onPostCalled = true }
      }
    }
    composeTestRule.waitForIdle()
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
    fakeObserver.setOnline(true)
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
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        CameraScreen(cameraScreenViewModel = slowPostVm, onPost = { onPostCalled = true })
      }
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

  @Test
  fun offlineModeShowsSaveToGalleryScreen() {
    fakeObserver.setOnline(false)
    val uri = mockk<Uri>()

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        CameraScreen(cameraScreenViewModel = viewModel)
      }
    }

    viewModel.enterOfflinePreview(uri)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CameraScreenTestTags.SAVE_TO_GALLERY_SCREEN).assertIsDisplayed()
  }

  @Test
  fun uploadButtonIsNotDisplayedWhenOffline() {
    fakeObserver.setOnline(false)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        CameraScreen(cameraScreenViewModel = viewModel)
      }
    }

    composeTestRule
        .onNodeWithTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_UPLOAD_BUTTON)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_SWITCH_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_CAPTURE_BUTTON)
        .assertIsDisplayed()
  }

  @Test
  fun uploadButtonIsDisplayedWhenOnline() {
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        CameraScreen(cameraScreenViewModel = viewModel)
      }
    }

    composeTestRule
        .onNodeWithTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_UPLOAD_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_SWITCH_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_CAPTURE_BUTTON)
        .assertIsDisplayed()
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

  private fun assertPreviewScreenIsDisplayed() {
    composeTestRule.onNodeWithTag(CameraScreenTestTags.CAMERA_PREVIEW_SCREEN).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_CAMERA_VIEWFINDER)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_UPLOAD_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_SWITCH_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_CAPTURE_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraPreviewScreenTestTags.CAMERA_PREVIEW_ZOOM_VALUE)
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
