package com.android.wildex.ui.camera

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.rule.GrantPermissionRule
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.animaldetector.AnimalInfoRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.utils.LocalRepositories
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CameraScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

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
            animalInfoRepository,
            animalRepository,
            userAnimalsRepository,
            currentUserId,
        )
  }

  @After
  fun tearDown() {
    LocalRepositories.clearAll()
  }

}
