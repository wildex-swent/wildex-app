package com.android.wildex.ui.achievement

import android.content.Context
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingScreenTestTags
import com.android.wildex.utils.FirebaseEmulator
import com.android.wildex.utils.LocalRepositories
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AchievementsScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var userAchievementsRepository: FakeUserAchievementsRepository
  private lateinit var userRepository: UserRepository
  private lateinit var currentUserId: String
  private lateinit var viewModel: AchievementsScreenViewModel

  private val postMaster =
      Achievement(
          achievementId = "achievement_1",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/2583/2583343.png",
          description = "Reach 10 posts",
          name = "Post Master",
      )

  private val communityBuilder =
      Achievement(
          achievementId = "achievement_3",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/1077/1077012.png",
          description = "Write 20 comments",
          name = "Community Builder",
      )

  private val firstPost =
      Achievement(
          achievementId = "achievement_5",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/1828/1828961.png",
          description = "Create your first post",
          name = "First Post",
      )

  private val conversationalist =
      Achievement(
          achievementId = "achievement_7",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/2462/2462719.png",
          description = "Write 50 comments overall",
          name = "Conversationalist",
      )

  private val mockAchievement1 =
      Achievement(
          achievementId = "mockPostId",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/616/616408.png",
          description = "This is a mock achievement for testing purposes",
          name = "Mock Achievement",
      )

  private val mockAchievement2 =
      Achievement(
          achievementId = "mockPostId2",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/4339/4339544.png",
          description = "This is another mock achievement for testing purposes",
          name = "Mock Achievement 2",
      )

  private val unlockedAchievement = listOf(postMaster, communityBuilder, firstPost)
  private val lockedAchievement = listOf(conversationalist, mockAchievement1, mockAchievement2)
  private val achievements = unlockedAchievement + lockedAchievement

  class FakeUserAchievementsRepository : UserAchievementsRepository {
    var unlocked: List<Achievement> = emptyList()
    var all: List<Achievement> = emptyList()
    var shouldThrowOnFetch = false
    var fetchSignal: CompletableDeferred<Unit>? = null

    override suspend fun initializeUserAchievements(userId: String) {}

    override suspend fun getAllAchievementsByUser(userId: String): List<Achievement> =
        getAllAchievementsByCurrentUser()

    override suspend fun getAllAchievementsByCurrentUser(): List<Achievement> {
      fetchSignal?.await()
      if (shouldThrowOnFetch) throw Exception("Network error")
      return unlocked
    }

    override suspend fun getAllAchievements(): List<Achievement> = all

    override suspend fun updateUserAchievements(userId: String) {}

    override suspend fun getAchievementsCountOfUser(userId: String): Int = unlocked.size

    override suspend fun deleteUserAchievements(userId: Id) {}
  }

  @Before
  fun setup() {
    userAchievementsRepository = FakeUserAchievementsRepository()
    userRepository = LocalRepositories.userRepository
    currentUserId = "currentUserId"
    viewModel = AchievementsScreenViewModel(
        userAchievementsRepository = userAchievementsRepository,
        userRepository = userRepository,
        currentUserId = currentUserId
    )
  }

  @After
  fun tearDown() {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.auth.signOut()
      FirebaseEmulator.clearAuthEmulator()
    }
  }

  @Test
  fun loadingScreen_shownWhileFetchingAchievements() {
    runBlocking {
      val fetchSignal = CompletableDeferred<Unit>()
      userAchievementsRepository.fetchSignal = fetchSignal
      userAchievementsRepository.unlocked = unlockedAchievement
      userAchievementsRepository.all = achievements

      viewModel.loadUIState(currentUserId)

      composeTestRule.setContent { AchievementsScreen(viewModel = viewModel, onGoBack = {}) }

      composeTestRule
          .onNodeWithTag(AchievementsScreenTestTags.LOADING, useUnmergedTree = true)
          .assertIsDisplayed()

      composeTestRule
          .onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN, useUnmergedTree = true)
          .assertIsDisplayed()

      fetchSignal.complete(Unit)
      composeTestRule.waitForIdle()

      composeTestRule
          .onNodeWithTag(AchievementsScreenTestTags.UNLOCKED_SECTION, useUnmergedTree = true)
          .assertExists()
      composeTestRule
          .onNodeWithTag(AchievementsScreenTestTags.LOCKED_SECTION, useUnmergedTree = true)
          .assertExists()

      unlockedAchievement.forEach { achievement ->
        composeTestRule
            .onNodeWithTag(AchievementsScreenTestTags.UNLOCKED_SECTION, useUnmergedTree = true)
            .performScrollToNode(hasText(achievement.name))
        composeTestRule.onNodeWithText(achievement.name, useUnmergedTree = true).assertIsDisplayed()
      }

      lockedAchievement.forEach { achievement ->
        composeTestRule
            .onNodeWithTag(AchievementsScreenTestTags.LOCKED_SECTION, useUnmergedTree = true)
            .performScrollToNode(hasText(achievement.name))
        composeTestRule.onNodeWithText(achievement.name, useUnmergedTree = true).assertIsDisplayed()
      }
    }
  }

  @Test
  fun errorScreen_shownWhenRepositoryThrows() {
    runBlocking {
      userAchievementsRepository.fetchSignal = CompletableDeferred<Unit>()
      userAchievementsRepository.shouldThrowOnFetch = true

      viewModel.loadUIState(currentUserId)

      composeTestRule.setContent { AchievementsScreen(viewModel = viewModel, onGoBack = {}) }

      userAchievementsRepository.fetchSignal?.complete(Unit)
      composeTestRule.waitForIdle()

      composeTestRule
          .onNodeWithText("Failed to load achievements: Network error", useUnmergedTree = true)
          .assertExists()
    }
  }

  @Test
  fun unlockedAndLocked_sectionsDisplayAchievements() {
    runBlocking {
      userAchievementsRepository.unlocked = unlockedAchievement
      userAchievementsRepository.all = achievements

      viewModel.loadUIState(currentUserId)

      composeTestRule.setContent { AchievementsScreen(viewModel = viewModel, onGoBack = {}) }
      composeTestRule.waitForIdle()

      composeTestRule
          .onNodeWithTag(AchievementsScreenTestTags.UNLOCKED_SECTION, useUnmergedTree = true)
          .assertExists()
      composeTestRule
          .onNodeWithTag(AchievementsScreenTestTags.LOCKED_SECTION, useUnmergedTree = true)
          .assertExists()

      unlockedAchievement.forEach { achievement ->
        composeTestRule
            .onNodeWithTag(AchievementsScreenTestTags.UNLOCKED_SECTION, useUnmergedTree = true)
            .performScrollToNode(hasText(achievement.name))
        composeTestRule.onNodeWithText(achievement.name, useUnmergedTree = true).assertIsDisplayed()
      }

      lockedAchievement.forEach { achievement ->
        composeTestRule
            .onNodeWithTag(AchievementsScreenTestTags.LOCKED_SECTION, useUnmergedTree = true)
            .performScrollToNode(hasText(achievement.name))
        composeTestRule.onNodeWithText(achievement.name, useUnmergedTree = true).assertIsDisplayed()
      }
    }
  }

  @Test
  fun backButton_invokesCallback() {
    runBlocking {
      var backClicked = false
      userAchievementsRepository.unlocked = emptyList()
      userAchievementsRepository.all = emptyList()

      composeTestRule.setContent {
        AchievementsScreen(viewModel = viewModel, onGoBack = { backClicked = true })
      }
      composeTestRule.waitForIdle()

      composeTestRule
          .onNodeWithTag(AchievementsScreenTestTags.BACK_BUTTON, useUnmergedTree = true)
          .performClick()
      assert(backClicked)
    }
  }

  @Test
  fun achievementOpacity_matchesLockedState() {
    runBlocking {
      userAchievementsRepository.unlocked = unlockedAchievement
      userAchievementsRepository.all = achievements

      viewModel.loadUIState(currentUserId)
      composeTestRule.setContent { AchievementsScreen(viewModel = viewModel, onGoBack = {}) }
      composeTestRule.waitForIdle()

      unlockedAchievement.forEach { achievement ->
        composeTestRule
            .onNodeWithTag(AchievementsScreenTestTags.UNLOCKED_SECTION, useUnmergedTree = true)
            .performScrollToNode(hasText(achievement.name))
      }
      lockedAchievement.forEach { achievement ->
        composeTestRule
            .onNodeWithTag(AchievementsScreenTestTags.LOCKED_SECTION, useUnmergedTree = true)
            .performScrollToNode(hasText(achievement.name))
      }

      val nodes =
          composeTestRule
              .onAllNodesWithTag(
                  AchievementsScreenTestTags.ACHIEVEMENT_IMAGE,
                  useUnmergedTree = true,
              )
              .fetchSemanticsNodes()

      val unlockedNode =
          nodes.firstOrNull {
            it.config.getOrNull(AchievementIdKey) == unlockedAchievement[0].achievementId
          } ?: error("Unlocked achievement node not found in semantics")
      val unlockedAlpha = unlockedNode.config.getOrNull(AchievementAlphaKey)
      check(unlockedAlpha == 1f) { "Expected unlocked alpha 1f but was $unlockedAlpha" }

      val lockedNode =
          nodes.firstOrNull {
            it.config.getOrNull(AchievementIdKey) == lockedAchievement[0].achievementId
          } ?: error("Locked achievement node not found in semantics")
      val lockedAlpha = lockedNode.config.getOrNull(AchievementAlphaKey)
      check(lockedAlpha == 0.3f) { "Expected locked alpha 0.3f but was $lockedAlpha" }
    }
  }

  @Test
  fun topAppBar_and_labeledDivider_showCorrectText() {
    runBlocking {
      userAchievementsRepository.unlocked = emptyList()
      userAchievementsRepository.all = emptyList()

      composeTestRule.setContent { AchievementsScreen(viewModel = viewModel, onGoBack = {}) }
      composeTestRule.waitForIdle()

      composeTestRule
          .onNodeWithTag(AchievementsScreenTestTags.TOP_APP_BAR, useUnmergedTree = true)
          .assertIsDisplayed()

      val ctx = ApplicationProvider.getApplicationContext<Context>()
      val unlockedLabel = ctx.getString(com.android.wildex.R.string.unlocked_achievements)
      val toDiscover = ctx.getString(com.android.wildex.R.string.to_discover)
      composeTestRule.onNodeWithText(unlockedLabel, useUnmergedTree = true).assertIsDisplayed()
      composeTestRule.onNodeWithText(toDiscover, useUnmergedTree = true).assertIsDisplayed()
    }
  }
}
