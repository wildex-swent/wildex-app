package com.android.wildex.ui.achievement

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.AppTheme
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingScreenTestTags
import com.android.wildex.utils.LocalRepositories
import com.android.wildex.utils.offline.FakeConnectivityObserver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AchievementsScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var userAchievementsRepository: FakeUserAchievementsRepository
  private lateinit var viewModel: AchievementsScreenViewModel
  private val fakeObserver = FakeConnectivityObserver(initial = true)

  private val postMaster =
      Achievement(
          achievementId = "achievement_1",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/2583/2583343.png",
          description = "Reach 10 posts",
          name = "Post Master",
          progress = { listOf(Triple("Posts", 12, 10)) },
      )

  private val communityBuilder =
      Achievement(
          achievementId = "achievement_3",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/1077/1077012.png",
          description = "Write 20 comments",
          name = "Community Builder",
          progress = { listOf(Triple("Comments", 20, 20)) },
      )

  private val firstPost =
      Achievement(
          achievementId = "achievement_5",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/1828/1828961.png",
          description = "Create your first post",
          name = "First Post",
          progress = { listOf(Triple("Posts", 12, 1)) },
      )

  private val conversationalist =
      Achievement(
          achievementId = "achievement_7",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/2462/2462719.png",
          description = "Write 50 comments overall",
          name = "Conversationalist",
          progress = { listOf(Triple("Comments", 20, 50)) },
      )

  private val mockAchievement1 =
      Achievement(
          achievementId = "mockPostId",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/616/616408.png",
          description = "This is a mock achievement for testing purposes",
          name = "Mock Achievement",
          progress = { listOf(Triple("Mock", 0, 1)) },
      )

  private val mockAchievement2 =
      Achievement(
          achievementId = "mockPostId2",
          pictureURL = "https://cdn-icons-png.flaticon.com/512/4339/4339544.png",
          description = "This is another mock achievement for testing purposes",
          name = "Mock Achievement 2",
          progress = { listOf(Triple("Mock", 0, 1)) },
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
    runBlocking {
      userAchievementsRepository = FakeUserAchievementsRepository()
      viewModel =
          AchievementsScreenViewModel(userAchievementsRepository = userAchievementsRepository)
    }
  }

  @After
  fun tearDown() {
    LocalRepositories.clearAll()
  }

  @Test
  fun noArgument_callCheck() {
    fakeObserver.setOnline(true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        AchievementsScreen()
      }
    }
  }

  @Test
  fun loadingScreen_shownWhileFetchingAchievements() {
    fakeObserver.setOnline(true)
    val fetchSignal = CompletableDeferred<Unit>()
    userAchievementsRepository.fetchSignal = fetchSignal
    userAchievementsRepository.unlocked = unlockedAchievement
    userAchievementsRepository.all = achievements

    viewModel.loadUIState("")

    AppTheme.appearanceMode = AppearanceMode.AUTOMATIC
    composeTestRule.setContent {
      val config = LocalConfiguration.current
      config.uiMode =
          Configuration.UI_MODE_NIGHT_YES or
              (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        AchievementsScreen(viewModel = viewModel)
      }
    }

    composeTestRule
        .onNodeWithTag(LoadingScreenTestTags.LOADING_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()

    fetchSignal.complete(Unit)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(AchievementsScreenTestTags.ACHIEVEMENT_GRID, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(AchievementsScreenTestTags.ACHIEVEMENTS_PROGRESS_CARD)
        .performScrollTo()
        .assertIsDisplayed()

    unlockedAchievement.forEach { achievement ->
      composeTestRule
          .onNodeWithTag(
              AchievementsScreenTestTags.getTagForAchievement(achievement.achievementId, true),
              useUnmergedTree = true,
          )
          .performScrollTo()
          .assertIsDisplayed()
      composeTestRule.onNodeWithText(achievement.name, useUnmergedTree = true).assertIsDisplayed()
    }

    lockedAchievement.forEach { achievement ->
      composeTestRule
          .onNodeWithTag(
              AchievementsScreenTestTags.getTagForAchievement(achievement.achievementId, false),
              useUnmergedTree = true,
          )
          .performScrollTo()
          .assertIsDisplayed()
      composeTestRule.onNodeWithText(achievement.name, useUnmergedTree = true).assertIsDisplayed()
    }
  }

  @Test
  fun loadingFailed_shownWhenError() {
    fakeObserver.setOnline(true)
    userAchievementsRepository.shouldThrowOnFetch = true
    userAchievementsRepository.unlocked = unlockedAchievement
    userAchievementsRepository.all = achievements

    viewModel.loadUIState("")
    composeTestRule.setContent {
      val config = LocalConfiguration.current
      config.uiMode =
          Configuration.UI_MODE_NIGHT_YES or
              (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        AchievementsScreen(viewModel = viewModel)
      }
    }
    composeTestRule
        .onNodeWithTag(LoadingScreenTestTags.LOADING_FAIL, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun backButton_invokesCallback() {
    fakeObserver.setOnline(true)
    var backClicked = false
    userAchievementsRepository.unlocked = emptyList()
    userAchievementsRepository.all = emptyList()
    AppTheme.appearanceMode = AppearanceMode.AUTOMATIC
    composeTestRule.setContent {
      val config = LocalConfiguration.current
      config.uiMode =
          Configuration.UI_MODE_NIGHT_NO or
              (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        AchievementsScreen(viewModel = viewModel, onGoBack = { backClicked = true })
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(AchievementsScreenTestTags.BACK_BUTTON, useUnmergedTree = true)
        .performClick()
    assertTrue(backClicked)
  }

  @Test
  fun achievementOpacity_matchesLockedState() {
    fakeObserver.setOnline(true)
    userAchievementsRepository.unlocked = unlockedAchievement
    userAchievementsRepository.all = achievements
    AppTheme.appearanceMode = AppearanceMode.LIGHT
    viewModel.loadUIState("")
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        AchievementsScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()

    unlockedAchievement.forEach { achievement ->
      composeTestRule
      val alpha =
          composeTestRule
              .onNodeWithTag(
                  AchievementsScreenTestTags.getTagForAchievement(achievement.achievementId, true),
                  useUnmergedTree = true,
              )
              .performScrollTo()
              .assertIsDisplayed()
              .fetchSemanticsNode()
              .config
              .getOrNull(AchievementAlphaKey)
      assertEquals(1f, alpha)
    }
    lockedAchievement.forEach { achievement ->
      val alpha =
          composeTestRule
              .onNodeWithTag(
                  AchievementsScreenTestTags.getTagForAchievement(achievement.achievementId, false),
                  useUnmergedTree = true,
              )
              .performScrollTo()
              .assertIsDisplayed()
              .fetchSemanticsNode()
              .config
              .getOrNull(AchievementAlphaKey)
      assertEquals(0.5f, alpha)
    }
  }

  @Test
  fun topBar_displaysCorrectly_AndBackButtonWorks() {
    fakeObserver.setOnline(true)
    userAchievementsRepository.unlocked = emptyList()
    userAchievementsRepository.all = emptyList()
    var back = 0
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        AchievementsScreen(viewModel = viewModel, onGoBack = { ++back })
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(AchievementsScreenTestTags.TOP_APP_BAR, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AchievementsScreenTestTags.TITLE, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AchievementsScreenTestTags.BACK_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    assertEquals(1, back)
  }

  @Test
  fun clickingAchievementOpensDetailsDialog() {
    fakeObserver.setOnline(true)
    userAchievementsRepository.unlocked = unlockedAchievement
    userAchievementsRepository.all = achievements
    AppTheme.appearanceMode = AppearanceMode.DARK
    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        AchievementsScreen(viewModel = viewModel)
      }
    }

    unlockedAchievement.forEach {
      composeTestRule
          .onNodeWithTag(
              AchievementsScreenTestTags.getTagForAchievement(it.achievementId, true),
              useUnmergedTree = true,
          )
          .performScrollTo()
          .performClick()
      composeTestRule.waitForIdle()
      assertAchievementDetailIsDisplayed()
      composeTestRule
          .onNodeWithTag(AchievementsScreenTestTags.DETAILS_CLOSE_BUTTON, useUnmergedTree = true)
          .assertIsDisplayed()
          .performClick()
    }
    lockedAchievement.forEach {
      composeTestRule
          .onNodeWithTag(
              AchievementsScreenTestTags.getTagForAchievement(it.achievementId, false),
              useUnmergedTree = true,
          )
          .performScrollTo()
          .performClick()
      composeTestRule.waitForIdle()
      assertAchievementDetailIsDisplayed()
      composeTestRule
          .onNodeWithTag(AchievementsScreenTestTags.DETAILS_CLOSE_BUTTON, useUnmergedTree = true)
          .assertIsDisplayed()
          .performClick()
    }
  }

  @Test
  fun emptyAchievements_displayPlaceholder() {
    fakeObserver.setOnline(true)
    userAchievementsRepository.unlocked = emptyList()
    userAchievementsRepository.all = achievements

    composeTestRule.setContent {
      CompositionLocalProvider(LocalConnectivityObserver provides fakeObserver) {
        AchievementsScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(AchievementsScreenTestTags.ACHIEVEMENTS_PLACEHOLDER)
        .assertIsDisplayed()
  }

  private fun assertAchievementDetailIsDisplayed() {
    composeTestRule.onNodeWithTag(AchievementsScreenTestTags.DETAILS_IMAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AchievementsScreenTestTags.DETAILS_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AchievementsScreenTestTags.DETAILS_STATUS).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AchievementsScreenTestTags.DETAILS_DESCRIPTION)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(AchievementsScreenTestTags.DETAILS_PROGRESS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AchievementsScreenTestTags.DETAILS_DIALOG).assertIsDisplayed()
  }
}
