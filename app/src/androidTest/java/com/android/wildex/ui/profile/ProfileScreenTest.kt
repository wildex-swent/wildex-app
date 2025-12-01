package com.android.wildex.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.android.wildex.BuildConfig
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepositoryFirestore
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingScreenTestTags
import com.android.wildex.usecase.achievement.UpdateUserAchievementsUseCase
import com.android.wildex.utils.LocalRepositories
import com.google.firebase.Timestamp
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private fun ComposeContentTestRule.scrollToTagWithinScroll(tag: String) {
    onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag(tag))
  }

  private val sampleUser =
      User(
          userId = "u-1",
          username = "jane_doe",
          name = "Jane",
          surname = "Doe",
          bio = "Bio of Jane",
          profilePictureURL = "https://example.com/pic.jpg",
          userType = UserType.REGULAR,
          creationDate = Timestamp(0, 0),
          country = "Switzerland",
      )

  /** Shared test achievements repo + use case + VM for tests that don't need custom repos. */
  private lateinit var defaultAchievementsRepo: FakeAchievementsRepo
  private lateinit var defaultUpdateUseCase: UpdateUserAchievementsUseCase
  private lateinit var defaultViewModel: ProfileScreenViewModel
  private val userRepository = LocalRepositories.userRepository
  private val userAnimalsRepository = LocalRepositories.userAnimalsRepository
  private val userFriendsRepository = LocalRepositories.userFriendsRepository
  private val animalRepository = LocalRepositories.animalRepository

  @Before
  fun setup() {
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    defaultAchievementsRepo = FakeAchievementsRepo()
    defaultUpdateUseCase = createTestUpdateAchievementsUseCase(defaultAchievementsRepo)
    defaultViewModel =
        ProfileScreenViewModel(
            userRepository = userRepository,
            achievementRepository = defaultAchievementsRepo,
            postRepository = LocalRepositories.postsRepository,
            updateUserAchievements = defaultUpdateUseCase,
            userAnimalsRepository = userAnimalsRepository,
            userFriendsRepository = userFriendsRepository,
            currentUserId = "currentUserId-1",
        )
    runBlocking {
      userRepository.addUser(
          User(
              userId = "u-1",
              username = "user1",
              name = "User",
              surname = "One",
              bio = "",
              profilePictureURL = "",
              userType = UserType.REGULAR,
              creationDate = Timestamp.now(),
              country = ""))
      animalRepository.addAnimal(
          Animal(animalId = "animal0", pictureURL = "", name = "", species = "", description = ""))
      animalRepository.addAnimal(
          Animal(animalId = "animal1", pictureURL = "", name = "", species = "", description = ""))
      userAnimalsRepository.initializeUserAnimals("u-1")
      userAnimalsRepository.addAnimalToUserAnimals("u-1", "animal0")
      userAnimalsRepository.addAnimalToUserAnimals("u-1", "animal1")
      userFriendsRepository.initializeUserFriends("u-1")
      userFriendsRepository.addFriendToUserFriendsOfUser("friend0", "u-1")
      userFriendsRepository.addFriendToUserFriendsOfUser("friend1", "u-1")
      userFriendsRepository.addFriendToUserFriendsOfUser("friend2", "u-1")
      userAnimalsRepository.initializeUserAnimals("currentUserId-1")
      userAnimalsRepository.addAnimalToUserAnimals("currentUserId-1", "animal0")
      userFriendsRepository.initializeUserFriends("currentUserId-1")
      userFriendsRepository.addFriendToUserFriendsOfUser("u-1", "currentUserId-1")
      userFriendsRepository.addFriendToUserFriendsOfUser("friend0", "currentUserId-1")
    }
  }

  @After
  fun tearDown() {
    LocalRepositories.clearAll()
  }

  /** Test helper: create a test-friendly update use case that stays on the test thread. */
  private fun createTestUpdateAchievementsUseCase(
      achRepo: UserAchievementsRepository,
  ): UpdateUserAchievementsUseCase =
      UpdateUserAchievementsUseCase(userAchievementsRepository = achRepo)

  /** Helper to create a list of fake achievements. */
  private fun fakeAchievements(count: Int = 5): List<Achievement> =
      (1..count).map { i ->
        Achievement(
            achievementId = "a$i",
            name = "A$i",
            pictureURL = "url$i",
            description = "",
            condition = { true },
        )
      }

  /** A fake achievements repo that returns a fixed list of achievements. */
  class FakeAchievementsRepo(private val achievements: List<Achievement> = emptyList()) :
      UserAchievementsRepository {
    override suspend fun getAllAchievementsByUser(userId: String): List<Achievement> = achievements

    override suspend fun getAllAchievementsByCurrentUser(): List<Achievement> = achievements

    override suspend fun getAllAchievements(): List<Achievement> = achievements

    override suspend fun updateUserAchievements(userId: String) {}

    override suspend fun initializeUserAchievements(userId: String) {}

    override suspend fun getAchievementsCountOfUser(userId: String): Int = achievements.size

    override suspend fun deleteUserAchievements(userId: Id) {}
  }

  @Test
  fun topBar_combined_owner_and_not_owner() {
    val owner = mutableStateOf(false)
    var back = 0
    var settings = 0
    composeRule.setContent {
      ProfileTopBar(ownerProfile = owner.value, onGoBack = { back++ }, onSettings = { settings++ })
    }
    composeRule.onNodeWithTag(ProfileScreenTestTags.GO_BACK).performClick()
    composeRule.onAllNodesWithTag(ProfileScreenTestTags.SETTINGS).assertCountEquals(0)
    Assert.assertEquals(1, back)
    composeRule.runOnUiThread { owner.value = true }
    composeRule.onNodeWithText("My Profile").assertIsDisplayed()
    composeRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS).assertIsDisplayed().performClick()
    Assert.assertEquals(1, settings)
  }

  @Test
  fun profileContent_core() {
    var collection = 0
    var friends = 0
    composeRule.setContent {
      ProfileContent(
          user = sampleUser,
          ownerProfile = false,
          onAchievements = {},
          onCollection = { collection++ },
          onMap = {},
          onFriends = { friends++ },
          onFriendRequest = {},
      )
    }
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_NAME).assertTextContains("Jane Doe")
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_USERNAME).assertTextContains("jane_doe")
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_COUNTRY)
        .assertTextContains("Switzerland")
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_DESCRIPTION).assertIsDisplayed()
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS).assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.COLLECTION).assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.MAP).assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.FRIENDS).assertExists()
    composeRule.onNodeWithText("Animals").assertExists()
    composeRule.onNodeWithText("Friends").assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.COLLECTION).performClick()
    composeRule.onNodeWithTag(ProfileScreenTestTags.FRIENDS).performClick()
    repeat(3) { composeRule.onNodeWithTag(ProfileScreenTestTags.COLLECTION).performClick() }
    repeat(2) { composeRule.onNodeWithTag(ProfileScreenTestTags.FRIENDS).performClick() }
    Assert.assertEquals(4, collection)
    Assert.assertEquals(3, friends)
  }

  @Test
  fun profileScreen_shows_description_node_even_when_bio_empty() = runTest {
    val user = sampleUser.copy(bio = "")
    val userRepo = mockk<UserRepositoryFirestore>()
    coEvery { userRepo.getUser("u-1") } returns user
    val achRepo = FakeAchievementsRepo(emptyList())
    val updateUseCase = createTestUpdateAchievementsUseCase(achRepo)
    val vm =
        ProfileScreenViewModel(
            userRepository = userRepo,
            achievementRepository = achRepo,
            postRepository = LocalRepositories.postsRepository,
            updateUserAchievements = updateUseCase,
            userAnimalsRepository = userAnimalsRepository,
            userFriendsRepository = userFriendsRepository,
            currentUserId = "someone-else",
        )
    composeRule.setContent {
      ProfileScreen(
          profileScreenViewModel = vm,
          userUid = "u-1",
          onFriendRequest = {},
      )
    }
    advanceUntilIdle()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_DESCRIPTION).assertIsDisplayed()
  }

  @Test
  fun profileScreen_owner_shows_my_profile_and_callbacks() = runTest {
    val user = sampleUser.copy(userId = "u-1")
    val userRepo = mockk<UserRepositoryFirestore>()
    coEvery { userRepo.getUser("u-1") } returns user
    val achRepo = FakeAchievementsRepo(emptyList())
    val updateUseCase = createTestUpdateAchievementsUseCase(achRepo)
    val vm =
        ProfileScreenViewModel(
            userRepository = userRepo,
            achievementRepository = achRepo,
            postRepository = LocalRepositories.postsRepository,
            updateUserAchievements = updateUseCase,
            userAnimalsRepository = userAnimalsRepository,
            userFriendsRepository = userFriendsRepository,
            currentUserId = "u-1",
        )
    var achievements = 0
    var map = 0
    composeRule.setContent {
      ProfileScreen(
          profileScreenViewModel = vm,
          userUid = "u-1",
          onAchievements = { if (it == "u-1") achievements++ },
          onMap = { if (it == "u-1") map++ },
      )
    }
    advanceUntilIdle()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("My Profile").assertIsDisplayed()
    composeRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS).assertIsDisplayed()
    composeRule.onAllNodesWithTag(ProfileScreenTestTags.FRIEND_REQUEST).assertCountEquals(0)
    composeRule.scrollToTagWithinScroll(ProfileScreenTestTags.ACHIEVEMENTS_CTA)
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_CTA).performClick()
    composeRule.scrollToTagWithinScroll(ProfileScreenTestTags.MAP_CTA)
    composeRule.onNodeWithTag(ProfileScreenTestTags.MAP_CTA).performClick()
    Assert.assertEquals(1, achievements)
    Assert.assertEquals(1, map)
  }

  @Test
  fun profileScreen_not_owner_shows_friend_request_and_passes_id() = runTest {
    val user = sampleUser
    val userRepo = mockk<UserRepositoryFirestore>()
    coEvery { userRepo.getUser("u-1") } returns user
    val achRepo = FakeAchievementsRepo(emptyList())
    val updateUseCase = createTestUpdateAchievementsUseCase(achRepo)
    val vm =
        ProfileScreenViewModel(
            userRepository = userRepo,
            achievementRepository = achRepo,
            postRepository = LocalRepositories.postsRepository,
            updateUserAchievements = updateUseCase,
            userAnimalsRepository = userAnimalsRepository,
            userFriendsRepository = userFriendsRepository,
            currentUserId = "someone-else",
        )
    var requests = 0
    var lastId = ""
    composeRule.setContent {
      ProfileScreen(
          profileScreenViewModel = vm,
          userUid = "u-1",
          onFriendRequest = {
            requests++
            lastId = it
          },
      )
    }
    advanceUntilIdle()
    composeRule.waitForIdle()
    composeRule.onAllNodesWithTag(ProfileScreenTestTags.SETTINGS).assertCountEquals(0)
    composeRule.scrollToTagWithinScroll(ProfileScreenTestTags.FRIEND_REQUEST)
    val req = composeRule.onNodeWithTag(ProfileScreenTestTags.FRIEND_REQUEST)
    req.performClick()
    req.performClick()
    Assert.assertEquals(2, requests)
    Assert.assertEquals("u-1", lastId)
  }

  @Test
  fun profile_defaults_and_map_achievements_defaults() {
    composeRule.setContent {
      Column {
        ProfileImageAndName()
        ProfileDescription()
        ProfileAchievements(ownerProfile = true)
        ProfileMap()
      }
    }
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_NAME).assertTextContains("Name Surname")
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_USERNAME).assertTextContains("Username")
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_COUNTRY).assertTextContains("Country")
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_DESCRIPTION).assertIsDisplayed()
    composeRule.onNodeWithText("Bio:...").assertIsDisplayed()
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS).assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_CTA).assertExists().performClick()
    composeRule.onNodeWithTag(ProfileScreenTestTags.MAP).assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.MAP_CTA).assertExists().performClick()
  }

  @Test
  fun profileFriendRequest_default_button_enabled_and_text() {
    composeRule.setContent { ProfileFriendRequest() }
    composeRule.onNodeWithTag(ProfileScreenTestTags.FRIEND_REQUEST).assertExists().assertIsEnabled()
    composeRule.onNodeWithText("Send Friend Request").assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.FRIEND_REQUEST).performClick()
    composeRule.onNodeWithText("Cancel Friend Request").assertExists()
  }

  @Test
  fun achievements_carousel_navigation_and_cta_visibility() {
    val items = fakeAchievements(5)
    val owner = mutableStateOf(true)
    var ownerClicks = 0

    composeRule.setContent {
      ProfileAchievements(
          id = "user-x",
          onAchievements = { if (it == "user-x") ownerClicks++ },
          ownerProfile = owner.value,
          listAchievement = items,
      )
    }
    composeRule.onNodeWithText("A1").assertExists()
    composeRule.onNodeWithText("A2").assertExists()
    composeRule.onNodeWithText("A3").assertExists()
    composeRule.onNodeWithText("A4").assertDoesNotExist()
    composeRule.onNodeWithText("A5").assertDoesNotExist()

    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_NEXT).performClick()
    composeRule.onNodeWithText("A2").assertExists()
    composeRule.onNodeWithText("A3").assertExists()
    composeRule.onNodeWithText("A4").assertExists()

    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_NEXT).performClick()
    composeRule.onNodeWithText("A3").assertExists()
    composeRule.onNodeWithText("A4").assertExists()
    composeRule.onNodeWithText("A5").assertExists()

    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_NEXT).performClick()
    composeRule.onNodeWithText("A4").assertExists()
    composeRule.onNodeWithText("A5").assertExists()
    composeRule.onNodeWithText("A1").assertExists()

    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_PREV).performClick()
    composeRule.onNodeWithText("A3").assertExists()
    composeRule.onNodeWithText("A4").assertExists()
    composeRule.onNodeWithText("A5").assertExists()

    composeRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS_PREV).performClick()
    composeRule.onNodeWithText("A2").assertExists()
    composeRule.onNodeWithText("A3").assertExists()
    composeRule.onNodeWithText("A4").assertExists()

    composeRule
        .onNodeWithText("View all achievements", substring = true)
        .assertExists()
        .performClick()
    Assert.assertEquals(1, ownerClicks)

    composeRule.runOnUiThread { owner.value = false }

    composeRule.onAllNodesWithText("View all achievements", substring = true).assertCountEquals(0)
  }

  @Test
  fun friendsAndAnimalsStatsShowTheCorrectStats() {
    val updateUseCase =
        createTestUpdateAchievementsUseCase(LocalRepositories.userAchievementsRepository)
    val vm =
        ProfileScreenViewModel(
            userRepository = userRepository,
            achievementRepository = LocalRepositories.userAchievementsRepository,
            postRepository = LocalRepositories.postsRepository,
            updateUserAchievements = updateUseCase,
            userAnimalsRepository = userAnimalsRepository,
            userFriendsRepository = userFriendsRepository,
            currentUserId = "someone-else",
        )
    composeRule.setContent {
      ProfileScreen(
          profileScreenViewModel = vm,
          userUid = "u-1",
      )
    }
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.ANIMAL_COUNT, useUnmergedTree = true)
        .assertTextEquals("2")
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.FRIENDS_COUNT, useUnmergedTree = true)
        .assertTextEquals("3")
  }

  @Test
  fun achievements_cta_hidden_for_non_owner_still_when_used_in_content() {
    val items = fakeAchievements(2)
    composeRule.setContent {
      ProfileContent(
          user = sampleUser,
          ownerProfile = false,
          achievements = items,
          onAchievements = {},
          onCollection = {},
          onMap = {},
          onFriends = {},
          onFriendRequest = {},
      )
    }
    composeRule.onAllNodesWithText("View all achievements", substring = true).assertCountEquals(0)
  }

  @Test
  fun loadingScreen_showsWhileFetchingPosts() {
    val fetchSignal = CompletableDeferred<Unit>()
    val delayedUsersRepo =
        object : LocalRepositories.UserRepositoryImpl() {
          override suspend fun getUser(userId: Id): User {
            fetchSignal.await()
            return super.getUser(userId)
          }
        }
    runBlocking {
      delayedUsersRepo.addUser(user = sampleUser)
      val achRepo = FakeAchievementsRepo()
      val updateUseCase = createTestUpdateAchievementsUseCase(achRepo)
      val vm =
          ProfileScreenViewModel(
              userRepository = delayedUsersRepo,
              achievementRepository = achRepo,
              postRepository = LocalRepositories.postsRepository,
              updateUserAchievements = updateUseCase,
              userAnimalsRepository = userAnimalsRepository,
              userFriendsRepository = userFriendsRepository,
              currentUserId = "currentUserId-1",
          )

      composeRule.setContent { ProfileScreen(vm, "u-1") }
      composeRule.waitForIdle()
      composeRule
          .onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN, useUnmergedTree = true)
          .assertIsDisplayed()
      fetchSignal.complete(Unit)
      composeRule.waitForIdle()
      composeRule
          .onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN, useUnmergedTree = true)
          .assertIsNotDisplayed()
    }
  }

  @Test
  fun failScreenShown_whenUserLookupFails() {
    composeRule.setContent { ProfileScreen(defaultViewModel, "") }
    composeRule.waitForIdle()
    composeRule
        .onNodeWithTag(LoadingScreenTestTags.LOADING_FAIL, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun profileImageAndName_shows_professional_badge_when_professional() {
    composeRule.setContent {
      ProfileContent(
          user = sampleUser.copy(userType = UserType.PROFESSIONAL),
          ownerProfile = false,
          onAchievements = {},
          onCollection = {},
          onMap = {},
          onFriends = {},
          onFriendRequest = {},
      )
    }
    composeRule.onNodeWithContentDescription("Professional badge").assertIsDisplayed()
  }

  @Test
  fun staticMiniMap_withPins_executesLocalSubsetAndCameraLogic() {
    val pins =
        listOf(
            Point.fromLngLat(6.632, 46.519),
            Point.fromLngLat(6.64, 46.525),
            Point.fromLngLat(6.65, 46.53),
        )
    composeRule.setContent {
      ProfileMap(
          id = "u-1",
          onMap = {},
          pins = pins,
      )
    }
    composeRule.onNodeWithTag(ProfileScreenTestTags.MAP).assertIsDisplayed()
  }
}
