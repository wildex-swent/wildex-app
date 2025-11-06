package com.android.wildex.ui.map

import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.map.PinDetails
import com.android.wildex.model.report.Report
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.report.ReportStatus
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Location
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapScreenViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var userRepository: UserRepository
  private lateinit var postsRepository: PostsRepository
  private lateinit var reportRepository: ReportRepository
  private lateinit var likeRepository: LikeRepository
  private lateinit var animalRepository: AnimalRepository
  private lateinit var viewModel: MapScreenViewModel

  private val loggedInUserId = "uid-1"
  private val now = Timestamp(Date())
  private val lausanne = Location(46.5197, 6.6323, "Lausanne")

  private val post1 =
      Post("p1", "user-a", "https://example.com/p1.jpg", lausanne, "first", now, "a1", 0, 0)
  private val post2 =
      Post("p2", loggedInUserId, "https://example.com/p2.jpg", lausanne, "second", now, "a2", 1, 0)
  private val report1 =
      Report("r1", "url", lausanne, now, "injured", "author", "assignee", ReportStatus.PENDING)

  private val regularUser =
      User(
          loggedInUserId,
          "regular-user",
          "John",
          "Doe",
          "bio",
          "https://example.com/me.jpg",
          UserType.REGULAR,
          now,
          "CH",
          1,
      )
  private val proUser = regularUser.copy(userType = UserType.PROFESSIONAL)

  @Before
  fun setUp() {
    userRepository = mockk()
    postsRepository = mockk()
    reportRepository = mockk()
    likeRepository = mockk(relaxed = true)
    animalRepository = mockk(relaxed = true)
    viewModel =
        MapScreenViewModel(
            userRepository = userRepository,
            postRepository = postsRepository,
            reportRepository = reportRepository,
            likeRepository = likeRepository,
            animalRepository = animalRepository,
            currentUserId = loggedInUserId,
        )
  }

  @Test
  fun loadUIState_regularAndProUsers_showExpectedTabs() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { postsRepository.getAllPosts() } returns listOf(post1, post2)
        coEvery { postsRepository.getAllPostsByGivenAuthor(any()) } returns listOf(post2)
        coEvery { reportRepository.getAllReports() } returns emptyList()
        coEvery { userRepository.getSimpleUser(any()) } returns SimpleUser("a", "b", "c")

        viewModel.loadUIState()
        advanceUntilIdle()
        val regState = viewModel.uiState.value
        Assert.assertEquals(listOf(MapTab.Posts, MapTab.MyPosts), regState.availableTabs)
        Assert.assertEquals(2, regState.pins.size)

        coEvery { userRepository.getUser(loggedInUserId) } returns proUser
        coEvery { reportRepository.getAllReports() } returns listOf(report1)
        viewModel.loadUIState()
        advanceUntilIdle()
        val proState = viewModel.uiState.value
        Assert.assertTrue(MapTab.Reports in proState.availableTabs)
      }

  @Test
  fun tabSwitching_andRefreshing_updatesCorrectData() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { postsRepository.getAllPosts() } returns listOf(post1, post2)
        coEvery { postsRepository.getAllPostsByGivenAuthor(any()) } returns listOf(post2)
        coEvery { userRepository.getSimpleUser(any()) } returns SimpleUser("a", "b", "c")

        viewModel.loadUIState()
        advanceUntilIdle()
        viewModel.onTabSelected(MapTab.MyPosts)
        advanceUntilIdle()
        val myPosts = viewModel.uiState.value
        Assert.assertEquals(MapTab.MyPosts, myPosts.activeTab)
        Assert.assertEquals(1, myPosts.pins.size)

        viewModel.refreshUIState()
        advanceUntilIdle()
        Assert.assertFalse(viewModel.uiState.value.isRefreshing)
      }

  @Test
  fun pinSelection_loadsCorrectDetails_forPostAndReport() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns proUser
        coEvery { userRepository.getSimpleUser(any()) } returns SimpleUser("x", "y", "url")
        coEvery { postsRepository.getAllPosts() } returns listOf(post1)
        coEvery { postsRepository.getPost("p1") } returns post1
        coEvery { likeRepository.getLikeForPost(any()) } returns null
        coEvery { reportRepository.getAllReports() } returns listOf(report1)
        coEvery { reportRepository.getReport("r1") } returns report1

        viewModel.loadUIState()
        advanceUntilIdle()
        viewModel.onPinSelected("p1")
        advanceUntilIdle()
        Assert.assertTrue(viewModel.uiState.value.selected is PinDetails.PostDetails)

        viewModel.onTabSelected(MapTab.Reports)
        advanceUntilIdle()
        viewModel.onPinSelected("r1")
        advanceUntilIdle()
        Assert.assertTrue(viewModel.uiState.value.selected is PinDetails.ReportDetails)
      }

  @Test
  fun toggleLike_performsOptimisticUpdate_andCallsRepository() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { postsRepository.getAllPosts() } returns listOf(post1)
        coEvery { postsRepository.getPost("p1") } returns post1
        coEvery { userRepository.getSimpleUser(any()) } returns SimpleUser("x", "y", "url")
        coEvery { likeRepository.getLikeForPost("p1") } returns null
        coEvery { likeRepository.getNewLikeId() } returns "like-1"

        viewModel.loadUIState()
        advanceUntilIdle()
        viewModel.onPinSelected("p1")
        advanceUntilIdle()
        val before = (viewModel.uiState.value.selected as PinDetails.PostDetails).post.likesCount
        viewModel.toggleLike("p1")
        advanceUntilIdle()
        val after = (viewModel.uiState.value.selected as PinDetails.PostDetails).post.likesCount
        Assert.assertEquals(before + 1, after)
        coVerify { likeRepository.addLike(any()) }
      }

  @Test
  fun clearFunctions_andErrorHandling_workCorrectly() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } throws RuntimeException("boom")
        viewModel.loadUIState()
        advanceUntilIdle()
        val s = viewModel.uiState.value
        Assert.assertTrue(s.isError)
        Assert.assertFalse(s.isLoading)
        Assert.assertNotNull(s.errorMsg)

        viewModel.clearSelection()
        Assert.assertNull(viewModel.uiState.value.selected)
        viewModel.clearErrorMsg()
        Assert.assertNull(viewModel.uiState.value.errorMsg)
        viewModel.clearRenderError()
        Assert.assertNull(viewModel.renderState.value.renderError)
      }

  @Test
  fun renderState_updatesCorrectly_onPermissionAndRecenter() {
    viewModel.onLocationPermissionResult(true)
    Assert.assertTrue(viewModel.renderState.value.showUserLocation)
    viewModel.requestRecenter()
    Assert.assertNotNull(viewModel.renderState.value.recenterNonce)
    viewModel.consumeRecenter()
    Assert.assertNull(viewModel.renderState.value.recenterNonce)
  }
}
