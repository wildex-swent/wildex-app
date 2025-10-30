package com.android.wildex.ui.achievement

import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.InputKey
import com.android.wildex.model.achievement.UserAchievementsRepository
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

  private lateinit var fakeRepo: FakeUserAchievementsRepository
  private lateinit var viewModel: AchievementsScreenViewModel

  private val postMaster =
      Achievement(
          achievementId = "achievement_1",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/2583/2583343.png",
          description = "Reach 10 posts",
          name = "Post Master",
          expects = setOf(InputKey.POST_IDS),
          condition = { inputs ->
            val postIds = inputs[InputKey.POST_IDS].orEmpty()
            postIds.size >= 10
          },
      )

  private val communityBuilder =
      Achievement(
          achievementId = "achievement_3",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/1077/1077012.png",
          description = "Write 20 comments",
          name = "Community Builder",
          expects = setOf(InputKey.COMMENT_IDS),
          condition = { inputs ->
            val commentIds = inputs[InputKey.COMMENT_IDS].orEmpty()
            commentIds.size >= 20
          },
      )

  private val firstPost =
      Achievement(
          achievementId = "achievement_5",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/1828/1828961.png",
          description = "Create your first post",
          name = "First Post",
          expects = setOf(InputKey.POST_IDS),
          condition = { inputs ->
            val postIds = inputs[InputKey.POST_IDS].orEmpty()
            postIds.isNotEmpty()
          },
      )

  private val conversationalist =
      Achievement(
          achievementId = "achievement_7",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/2462/2462719.png",
          description = "Write 50 comments overall",
          name = "Conversationalist",
          expects = setOf(InputKey.COMMENT_IDS),
          condition = { inputs ->
            val commentIds = inputs[InputKey.COMMENT_IDS].orEmpty()
            commentIds.size >= 50
          },
      )

  private val mockAchievement1 =
      Achievement(
          achievementId = "mockPostId",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/616/616408.png",
          description = "This is a mock achievement for testing purposes",
          name = "Mock Achievement",
          expects = setOf(InputKey.TEST_IDS),
          condition = { inputs ->
            val testIds = inputs[InputKey.TEST_IDS].orEmpty()
            testIds.size == 1 && testIds[0] == "mockPostId"
          },
      )

  private val mockAchievement2 =
      Achievement(
          achievementId = "mockPostId2",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/4339/4339544.png",
          description = "This is another mock achievement for testing purposes",
          name = "Mock Achievement 2",
          expects = setOf(InputKey.TEST_IDS),
          condition = { inputs ->
            val testIds = inputs[InputKey.TEST_IDS].orEmpty()
            testIds.size == 2
          },
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

    override suspend fun updateUserAchievements(
        userId: String,
        inputs: Map<InputKey, List<String>>
    ) {}

    override suspend fun getAchievementsCountOfUser(userId: String): Int = unlocked.size
  }

  @Before
  fun setup() {
    fakeRepo = FakeUserAchievementsRepository()
    viewModel = AchievementsScreenViewModel(fakeRepo)
  }

  @After fun tearDown() {}

  @Test
  fun loadingScreen_shownWhileFetchingAchievements() {
    runBlocking {
      val fetchSignal = CompletableDeferred<Unit>()
      fakeRepo.fetchSignal = fetchSignal
      fakeRepo.unlocked = unlockedAchievement
      fakeRepo.all = achievements

      viewModel.loadAchievements()

      composeTestRule.setContent { AchievementsScreen(viewModel = viewModel, onGoBack = {}) }

      composeTestRule
          .onNodeWithTag(AchievementsScreenTestTags.LOADING, useUnmergedTree = true)
          .assertIsDisplayed()

      fetchSignal.complete(Unit)
      composeTestRule.waitForIdle()

      composeTestRule
          .onNodeWithTag(AchievementsScreenTestTags.UNLOCKED_SECTION, useUnmergedTree = true)
          .assertExists()
      composeTestRule
          .onNodeWithTag(AchievementsScreenTestTags.LOCKED_SECTION, useUnmergedTree = true)
          .assertExists()

      // Scroll to each unlocked achievement and assert it's present
      unlockedAchievement.forEach { achievement ->
        composeTestRule
            .onNodeWithTag(AchievementsScreenTestTags.UNLOCKED_SECTION, useUnmergedTree = true)
            .performScrollToNode(hasText(achievement.name))
        composeTestRule.onNodeWithText(achievement.name, useUnmergedTree = true).assertIsDisplayed()
      }

      // Scroll to each locked achievement and assert it's present
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
      fakeRepo.fetchSignal = CompletableDeferred<Unit>()
      fakeRepo.shouldThrowOnFetch = true

      viewModel.loadAchievements()

      composeTestRule.setContent { AchievementsScreen(viewModel = viewModel, onGoBack = {}) }

      fakeRepo.fetchSignal?.complete(Unit)
      composeTestRule.waitForIdle()

      composeTestRule
          .onNodeWithText("Failed to load achievements: Network error", useUnmergedTree = true)
          .assertExists()
    }
  }

  @Test
  fun unlockedAndLocked_sectionsDisplayAchievements() {
    runBlocking {
      fakeRepo.unlocked = unlockedAchievement
      fakeRepo.all = achievements

      viewModel.loadAchievements()

      composeTestRule.setContent { AchievementsScreen(viewModel = viewModel, onGoBack = {}) }
      composeTestRule.waitForIdle()

      composeTestRule
          .onNodeWithTag(AchievementsScreenTestTags.UNLOCKED_SECTION, useUnmergedTree = true)
          .assertExists()
      composeTestRule
          .onNodeWithTag(AchievementsScreenTestTags.LOCKED_SECTION, useUnmergedTree = true)
          .assertExists()

      // Scroll to unlocked achievements and assert presence
      unlockedAchievement.forEach { achievement ->
        composeTestRule
            .onNodeWithTag(AchievementsScreenTestTags.UNLOCKED_SECTION, useUnmergedTree = true)
            .performScrollToNode(hasText(achievement.name))
        composeTestRule.onNodeWithText(achievement.name, useUnmergedTree = true).assertIsDisplayed()
      }

      // Scroll to locked achievements and assert presence
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
      fakeRepo.unlocked = emptyList()
      fakeRepo.all = emptyList()

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
      // Arrange: make first three unlocked and rest locked
      fakeRepo.unlocked = unlockedAchievement
      fakeRepo.all = achievements

      viewModel.loadAchievements()
      composeTestRule.setContent { AchievementsScreen(viewModel = viewModel, onGoBack = {}) }
      composeTestRule.waitForIdle()

      // Fetch semantics nodes for all achievement images
      val nodes =
          composeTestRule
              .onAllNodesWithTag(
                  AchievementsScreenTestTags.ACHIEVEMENT_IMAGE, useUnmergedTree = true)
              .fetchSemanticsNodes()

      // Find unlocked node by achievementId and assert alpha
      val unlockedNode =
          nodes.firstOrNull {
            it.config.getOrNull(AchievementIdKey) == unlockedAchievement[0].achievementId
          }
      check(unlockedNode != null) { "Unlocked achievement node not found in semantics" }
      val unlockedAlpha = unlockedNode!!.config.getOrNull(AchievementAlphaKey)
      check(unlockedAlpha == 1f) { "Expected unlocked alpha 1f but was $unlockedAlpha" }

      // Find locked node by achievementId and assert alpha
      val lockedNode =
          nodes.firstOrNull {
            it.config.getOrNull(AchievementIdKey) == lockedAchievement[0].achievementId
          }
      check(lockedNode != null) { "Locked achievement node not found in semantics" }
      val lockedAlpha = lockedNode!!.config.getOrNull(AchievementAlphaKey)
      check(lockedAlpha == 0.3f) { "Expected locked alpha 0.3f but was $lockedAlpha" }
    }
  }
}
