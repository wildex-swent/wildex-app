package com.android.wildex.ui.profile

import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.user.UserFriendsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Location
import com.android.wildex.usecase.achievement.UpdateUserAchievementsUseCase
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileScreenViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var userRepository: UserRepository
  private lateinit var achievementsRepository: UserAchievementsRepository
  private lateinit var postsRepository: PostsRepository
  private lateinit var updateUserAchievements: UpdateUserAchievementsUseCase
  private lateinit var userAnimalsRepository: UserAnimalsRepository
  private lateinit var userFriendsRepository: UserFriendsRepository
  private lateinit var viewModel: ProfileScreenViewModel

  private val u1 =
      User(
          userId = "uid-1",
          username = "user_one",
          name = "First",
          surname = "User",
          bio = "bio",
          profilePictureURL = "pic",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "X",
      )

  private val u2 = u1.copy(username = "user_one_2")

  private val a1: Achievement = mockk()
  private val a2: Achievement = mockk()

  @Before
  fun setUp() {
    userRepository = mockk()
    achievementsRepository = mockk()
    postsRepository = mockk()
    updateUserAchievements = mockk()
    userAnimalsRepository = mockk()
    userFriendsRepository = mockk()

    coEvery { updateUserAchievements(any()) } returns Unit
    coEvery { postsRepository.getAllPostsByGivenAuthor(any()) } returns emptyList()
    coEvery { userAnimalsRepository.getAnimalsCountOfUser("uid-1") } returns 3
    coEvery { userFriendsRepository.getFriendsCountOfUser("uid-1") } returns 4

    viewModel =
        ProfileScreenViewModel(
            userRepository = userRepository,
            achievementRepository = achievementsRepository,
            postRepository = postsRepository,
            updateUserAchievements = updateUserAchievements,
            currentUserId = "uid-1",
        )
  }

  @Test
  fun viewModel_initializes_default_UI_state_isEmptyLoading() {
    val s = viewModel.uiState.value
    Assert.assertFalse(s.isError)
    Assert.assertFalse(s.isUserOwner)
    Assert.assertFalse(s.isLoading)
    Assert.assertFalse(s.isRefreshing)
    Assert.assertEquals(0, s.friendsCount)
    Assert.assertEquals(0, s.animalCount)
    Assert.assertTrue(s.achievements.isEmpty())
    Assert.assertTrue(s.recentPins.isEmpty())
    Assert.assertNull(s.errorMsg)
  }

  @Test
  fun refreshUIState_owner_true_and_false_paths_includesRecentPins() {
    mainDispatcherRule.runTest {
      val pWithLoc = mockk<com.android.wildex.model.social.Post>(relaxed = true)
      every { pWithLoc.location } returns Location(latitude = 46.5, longitude = 6.6, name = "")
      every { pWithLoc.date } returns Timestamp(1000, 0)

      val pNoLoc = mockk<com.android.wildex.model.social.Post>(relaxed = true)
      every { pNoLoc.location } returns null
      every { pNoLoc.date } returns Timestamp(500, 0)

      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns listOf(a1, a2)
      coEvery { postsRepository.getAllPostsByGivenAuthor("uid-1") } returns listOf(pNoLoc, pWithLoc)

      viewModel.refreshUIState("uid-1")
      advanceUntilIdle()
      val s1 = viewModel.uiState.value
      Assert.assertEquals(u1, s1.user)
      Assert.assertTrue(s1.isUserOwner)
      Assert.assertEquals(listOf(a1, a2), s1.achievements)
      Assert.assertEquals(3, s1.animalCount)
      Assert.assertEquals(4, s1.friendsCount)
      Assert.assertEquals(1, s1.recentPins.size)
      Assert.assertEquals(6.6, s1.recentPins[0].longitude(), 0.0)
      Assert.assertEquals(46.5, s1.recentPins[0].latitude(), 0.0)

      viewModel =
          ProfileScreenViewModel(
              userRepository = userRepository,
              achievementRepository = achievementsRepository,
              postRepository = postsRepository,
              updateUserAchievements = updateUserAchievements,
              currentUserId = "someone-else",
          )
      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns emptyList()
      coEvery { postsRepository.getAllPostsByGivenAuthor("uid-1") } returns emptyList()

      viewModel.refreshUIState("uid-1")
      advanceUntilIdle()
      val s2 = viewModel.uiState.value
      Assert.assertEquals(u1, s2.user)
      Assert.assertFalse(s2.isUserOwner)
      Assert.assertEquals(3, s2.animalCount)
      Assert.assertEquals(4, s2.friendsCount)
      Assert.assertTrue(s2.achievements.isEmpty())
      Assert.assertTrue(s2.recentPins.isEmpty())
    }
  }

  @Test
  fun refreshUIState_error_paths_userRepo_then_achievementsRepo() {
    mainDispatcherRule.runTest {
      coEvery { userRepository.getUser("uid-1") } throws RuntimeException("boom")
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns listOf(a1)
      coEvery { postsRepository.getAllPostsByGivenAuthor("uid-1") } returns emptyList()

      viewModel.refreshUIState("uid-1")
      advanceUntilIdle()
      val e1 = viewModel.uiState.value
      Assert.assertTrue(e1.isError)
      Assert.assertEquals(false, e1.isUserOwner)
      Assert.assertEquals("Unexpected error: boom", e1.errorMsg)

      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } throws
          RuntimeException("x")
      coEvery { postsRepository.getAllPostsByGivenAuthor("uid-1") } returns emptyList()

      viewModel.refreshUIState("uid-1")
      advanceUntilIdle()
      val e2 = viewModel.uiState.value
      Assert.assertEquals(u1, e2.user)
      Assert.assertTrue(e2.achievements.isEmpty())
      Assert.assertEquals("x", e2.errorMsg)
      Assert.assertTrue(e2.recentPins.isEmpty())
      Assert.assertFalse(e2.isError)
    }
  }

  @Test
  fun refreshUIState_multipleCalls_updatesWithLatestData_includingPins() {
    mainDispatcherRule.runTest {
      val p1 = mockk<com.android.wildex.model.social.Post>(relaxed = true)
      every { p1.location } returns Location(46.5, 6.6, "")
      every { p1.date } returns Timestamp(1000, 0)

      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns listOf(a1)
      coEvery { postsRepository.getAllPostsByGivenAuthor("uid-1") } returns listOf(p1)
      viewModel.refreshUIState("uid-1")
      advanceUntilIdle()

      val p2 = mockk<com.android.wildex.model.social.Post>(relaxed = true)
      every { p2.location } returns Location(48.8, 8.8, "")
      every { p2.date } returns Timestamp(2000, 0)

      val pOldNoLoc = mockk<com.android.wildex.model.social.Post>(relaxed = true)
      every { pOldNoLoc.location } returns null
      every { pOldNoLoc.date } returns Timestamp(1500, 0)

      coEvery { userRepository.getUser("uid-1") } returns u2
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns listOf(a2)
      coEvery { postsRepository.getAllPostsByGivenAuthor("uid-1") } returns listOf(pOldNoLoc, p2)
      viewModel.refreshUIState("uid-1")
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals(u2, s.user)
      Assert.assertEquals(listOf(a2), s.achievements)
      Assert.assertTrue(s.isUserOwner)
      Assert.assertEquals(1, s.recentPins.size)
      Assert.assertEquals(8.8, s.recentPins[0].longitude(), 0.0)
      Assert.assertEquals(48.8, s.recentPins[0].latitude(), 0.0)
    }
  }

  @Test
  fun refreshUIState_withBlankUserId_setsErrorAndStopsLoading_and_clearErrorMsg() {
    mainDispatcherRule.runTest {
      viewModel.refreshUIState("")
      advanceUntilIdle()
      val s = viewModel.uiState.value
      Assert.assertEquals("Empty user id", s.errorMsg)
      Assert.assertFalse(s.isLoading)
      Assert.assertTrue(s.isError)
      Assert.assertFalse(s.isUserOwner)
      Assert.assertTrue(s.recentPins.isEmpty())
      Assert.assertEquals(0, s.animalCount)
      Assert.assertEquals(0, s.friendsCount)

      viewModel.clearErrorMsg()
      Assert.assertNull(viewModel.uiState.value.errorMsg)
    }
  }

  @Test
  fun refreshUIState_whenUnexpectedErrorAfterFetch_hitsOuterCatch() {
    mainDispatcherRule.runTest {
      val badUser = mockk<User>(relaxed = true)
      every { badUser.userId } throws RuntimeException("kaboom")
      coEvery { userRepository.getUser("uid-1") } returns badUser
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns emptyList()
      coEvery { postsRepository.getAllPostsByGivenAuthor("uid-1") } returns emptyList()

      viewModel.refreshUIState("uid-1")
      advanceUntilIdle()
      val s = viewModel.uiState.value
      Assert.assertTrue(s.errorMsg?.startsWith("Unexpected error: kaboom") == true)
      Assert.assertFalse(s.isLoading)
      Assert.assertTrue(s.isError)
    }
  }

  @Test
  fun refreshUIState_whenPostsRepositoryFails_setsErrorMsg_andKeepsUserAndAchievements() {
    mainDispatcherRule.runTest {
      coEvery { userRepository.getUser("uid-1") } returns u1
      coEvery { achievementsRepository.getAllAchievementsByUser("uid-1") } returns listOf(a1, a2)
      coEvery { postsRepository.getAllPostsByGivenAuthor("uid-1") } throws
          RuntimeException("posts boom")
      viewModel.refreshUIState("uid-1")
      advanceUntilIdle()

      val s = viewModel.uiState.value
      Assert.assertEquals(u1, s.user)
      Assert.assertEquals(listOf(a1, a2), s.achievements)
      Assert.assertTrue(s.recentPins.isEmpty())
      Assert.assertEquals("posts boom", s.errorMsg)
      Assert.assertFalse(s.isError)
    }
  }
}
