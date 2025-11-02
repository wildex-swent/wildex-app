package com.android.wildex.ui.map

import com.android.wildex.model.animal.Animal
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
  private val now = Timestamp.now()
  private val lausanne = Location(46.5197, 6.6323, "Lausanne")

  private val post1 =
      Post(
          postId = "p1",
          authorId = "user-a",
          pictureURL = "https://example.com/p1.jpg",
          location = lausanne,
          description = "first",
          date = now,
          animalId = "a1",
          likesCount = 0,
          commentsCount = 0,
      )

  private val post2 =
      Post(
          postId = "p2",
          authorId = loggedInUserId,
          pictureURL = "https://example.com/p2.jpg",
          location = lausanne,
          description = "second",
          date = now,
          animalId = "a2",
          likesCount = 1,
          commentsCount = 0,
      )

  private val regularUser =
      User(
          userId = loggedInUserId,
          username = "regular-user",
          name = "John",
          surname = "Doe",
          bio = "Regular account",
          profilePictureURL = "https://example.com/me.jpg",
          userType = UserType.REGULAR,
          creationDate = now,
          country = "Switzerland",
          friendsCount = 3,
      )

  private val professionalUser =
      User(
          userId = loggedInUserId,
          username = "pro-user",
          name = "Jane",
          surname = "Smith",
          bio = "Professional account",
          profilePictureURL = "https://example.com/pro.jpg",
          userType = UserType.PROFESSIONAL,
          creationDate = now,
          country = "Switzerland",
          friendsCount = 10,
      )

  private val report1 =
      Report(
          reportId = "r1",
          imageURL = "https://example.com/report.png",
          location = lausanne,
          date = now,
          description = "injured bird",
          authorId = "author-r",
          assigneeId = "pro-1",
          status = ReportStatus.PENDING,
      )

  @Before
  fun setUp() {
    userRepository = mockk()
    postsRepository = mockk()
    reportRepository = mockk()
    likeRepository = mockk(relaxed = true)
    animalRepository = mockk()

    viewModel =
        MapScreenViewModel(
            loggedInUserId = loggedInUserId,
            userRepository = userRepository,
            postRepository = postsRepository,
            reportRepository = reportRepository,
            likeRepository = likeRepository,
            animalRepository = animalRepository,
        )
  }

  // ---------- helpers ----------

  private fun stubSimpleUsers() {
    coEvery { userRepository.getSimpleUser("user-a") } returns
        SimpleUser("user-a", "author-a", "https://example.com/author-a.jpg")
    coEvery { userRepository.getSimpleUser(loggedInUserId) } returns
        SimpleUser(loggedInUserId, "regular-user", "https://example.com/me.jpg")
  }

  private fun stubCommonReportsEmpty() {
    coEvery { reportRepository.getAllReports() } returns emptyList()
    coEvery { reportRepository.getAllReportsByAuthor(any()) } returns emptyList()
    coEvery { reportRepository.getAllReportsByAssignee(any()) } returns emptyList()
  }

  private fun stubAnimal() {
    coEvery { animalRepository.getAnimal("a1") } returns
        Animal(
            animalId = "a1",
            species = "canidae",
            name = "fox",
            description = "",
            pictureURL = "https://example.com/fox.jpg",
        )
  }

  @Test
  fun loadUIState_regularUser_showsPostsAndMyPostsTabs() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { postsRepository.getAllPosts() } returns listOf(post1, post2)
        coEvery { postsRepository.getAllPostsByGivenAuthor(loggedInUserId) } returns listOf(post2)
        stubCommonReportsEmpty()
        stubSimpleUsers()

        viewModel.loadUIState()
        advanceUntilIdle()

        val s = viewModel.uiState.value
        Assert.assertEquals(listOf(MapTab.Posts, MapTab.MyPosts), s.availableTabs)
        Assert.assertEquals(MapTab.Posts, s.activeTab)
        Assert.assertEquals(2, s.pins.size)
      }

  @Test
  fun loadUIState_professionalUser_includesReportsTab_andLoadsReports() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns professionalUser
        coEvery { postsRepository.getAllPosts() } returns listOf(post1)
        coEvery { postsRepository.getAllPostsByGivenAuthor(loggedInUserId) } returns listOf(post2)
        coEvery { reportRepository.getAllReports() } returns listOf(report1)
        coEvery { reportRepository.getAllReportsByAuthor(any()) } returns emptyList()
        coEvery { reportRepository.getAllReportsByAssignee(any()) } returns emptyList()
        coEvery { userRepository.getSimpleUser("user-a") } returns
            SimpleUser("user-a", "author-a", "https://example.com/author-a.jpg")
        coEvery { userRepository.getSimpleUser(loggedInUserId) } returns
            SimpleUser(loggedInUserId, "pro-user", "https://example.com/pro.jpg")

        viewModel.loadUIState()
        advanceUntilIdle()

        val s = viewModel.uiState.value
        Assert.assertEquals(
            listOf(MapTab.Posts, MapTab.MyPosts, MapTab.Reports),
            s.availableTabs,
        )
        Assert.assertEquals(MapTab.Posts, s.activeTab)
        Assert.assertEquals(1, s.pins.size)
      }

  @Test
  fun onTabSelected_invalidTab_doesNothing() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { postsRepository.getAllPosts() } returns listOf(post1)
        coEvery { postsRepository.getAllPostsByGivenAuthor(loggedInUserId) } returns listOf(post2)
        stubCommonReportsEmpty()
        stubSimpleUsers()

        viewModel.loadUIState()
        advanceUntilIdle()

        val before = viewModel.uiState.value

        viewModel.onTabSelected(MapTab.Reports)
        advanceUntilIdle()

        val after = viewModel.uiState.value
        Assert.assertEquals(before.activeTab, after.activeTab)
        Assert.assertEquals(before.availableTabs, after.availableTabs)
      }

  @Test
  fun onTabSelected_MyPosts_loadsPostsOfCurrentUser() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { postsRepository.getAllPosts() } returns listOf(post1, post2)
        coEvery { postsRepository.getAllPostsByGivenAuthor(loggedInUserId) } returns listOf(post2)
        stubCommonReportsEmpty()
        stubSimpleUsers()

        viewModel.loadUIState()
        advanceUntilIdle()

        viewModel.onTabSelected(MapTab.MyPosts)
        advanceUntilIdle()

        val s = viewModel.uiState.value
        Assert.assertEquals(MapTab.MyPosts, s.activeTab)
        Assert.assertEquals(1, s.pins.size)
        Assert.assertEquals("p2", s.pins.first().id)
        coVerify(exactly = 1) { postsRepository.getAllPostsByGivenAuthor(loggedInUserId) }
      }

  @Test
  fun refreshUIState_keepsActiveTab_andReloads() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { postsRepository.getAllPosts() } returns listOf(post1, post2)
        coEvery { postsRepository.getAllPostsByGivenAuthor(loggedInUserId) } returns listOf(post2)
        stubCommonReportsEmpty()
        stubSimpleUsers()

        viewModel.loadUIState()
        advanceUntilIdle()
        viewModel.onTabSelected(MapTab.MyPosts)
        advanceUntilIdle()

        viewModel.refreshUIState()
        advanceUntilIdle()

        val s = viewModel.uiState.value
        Assert.assertEquals(MapTab.MyPosts, s.activeTab)
        Assert.assertEquals(1, s.pins.size)
      }

  @Test
  fun reload_regularUser_withActiveReports_returnsEmptyPinsForReports() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { postsRepository.getAllPosts() } returns listOf(post1)
        coEvery { postsRepository.getAllPostsByGivenAuthor(loggedInUserId) } returns listOf(post2)
        coEvery { reportRepository.getAllReports() } returns listOf(report1)
        coEvery { reportRepository.getAllReportsByAuthor(any()) } returns emptyList()
        coEvery { reportRepository.getAllReportsByAssignee(any()) } returns emptyList()
        stubSimpleUsers()

        viewModel.loadUIState()
        advanceUntilIdle()

        val s = viewModel.uiState.value
        Assert.assertEquals(MapTab.Posts, s.activeTab)
        Assert.assertEquals(listOf(MapTab.Posts, MapTab.MyPosts), s.availableTabs)
      }

  @Test
  fun onPinSelected_postPin_loadsPostAndAuthorAndLike() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { postsRepository.getAllPosts() } returns listOf(post1)
        coEvery { postsRepository.getAllPostsByGivenAuthor(loggedInUserId) } returns emptyList()
        coEvery { reportRepository.getAllReports() } returns emptyList()
        coEvery { postsRepository.getPost("p1") } returns post1
        coEvery { userRepository.getSimpleUser("user-a") } returns
            SimpleUser("user-a", "author-a", "https://example.com/a.jpg")
        coEvery { userRepository.getSimpleUser(loggedInUserId) } returns
            SimpleUser(loggedInUserId, "regular-user", "https://example.com/me.jpg")
        coEvery { likeRepository.getLikeForPost("p1") } returns null
        stubAnimal()

        viewModel.loadUIState()
        advanceUntilIdle()

        viewModel.onPinSelected("p1")
        advanceUntilIdle()

        val s = viewModel.uiState.value
        Assert.assertNotNull(s.selected)
        Assert.assertTrue(s.selected is PinDetails.PostDetails)
        val d = s.selected as PinDetails.PostDetails
        Assert.assertEquals("fox", d.animalName)
      }

  @Test
  fun onPinSelected_reportPin_loadsReportAuthorAndAssignee() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns professionalUser
        coEvery { postsRepository.getAllPosts() } returns emptyList()
        coEvery { postsRepository.getAllPostsByGivenAuthor(loggedInUserId) } returns emptyList()
        coEvery { reportRepository.getAllReports() } returns listOf(report1)
        coEvery { reportRepository.getAllReportsByAuthor(any()) } returns emptyList()
        coEvery { reportRepository.getAllReportsByAssignee(any()) } returns emptyList()
        coEvery { reportRepository.getReport("r1") } returns report1
        coEvery { userRepository.getSimpleUser("author-r") } returns
            SimpleUser("author-r", "reporter", "https://example.com/x.jpg")
        coEvery { userRepository.getSimpleUser("pro-1") } returns
            SimpleUser("pro-1", "assignee", "https://example.com/y.jpg")
        coEvery { userRepository.getSimpleUser(loggedInUserId) } returns
            SimpleUser(loggedInUserId, "pro-user", "https://example.com/pro.jpg")

        viewModel.loadUIState()
        advanceUntilIdle()

        viewModel.onTabSelected(MapTab.Reports)
        advanceUntilIdle()

        val pins = viewModel.uiState.value.pins
        Assert.assertEquals(1, pins.size)
        Assert.assertEquals("r1", pins.first().id)

        viewModel.onPinSelected("r1")
        advanceUntilIdle()

        val s = viewModel.uiState.value
        Assert.assertNotNull(s.selected)
        Assert.assertTrue(s.selected is PinDetails.ReportDetails)
      }

  @Test
  fun toggleLike_onSelectedPost_addsLike_andOptimisticallyUpdates() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { postsRepository.getAllPosts() } returns listOf(post1)
        coEvery { postsRepository.getPost("p1") } returns post1
        coEvery { userRepository.getSimpleUser("user-a") } returns
            SimpleUser("user-a", "author-a", "https://example.com/a.jpg")
        coEvery { userRepository.getSimpleUser(loggedInUserId) } returns
            SimpleUser(loggedInUserId, "regular-user", "https://example.com/me.jpg")
        coEvery { likeRepository.getLikeForPost("p1") } returns null
        coEvery { likeRepository.getNewLikeId() } returns "like-42"
        coEvery { likeRepository.addLike(any()) } returns Unit
        stubAnimal()

        viewModel.loadUIState()
        advanceUntilIdle()
        viewModel.onPinSelected("p1")
        advanceUntilIdle()

        val beforeLikes =
            (viewModel.uiState.value.selected as PinDetails.PostDetails).post.likesCount

        viewModel.toggleLike("p1")
        advanceUntilIdle()

        val afterLikes =
            (viewModel.uiState.value.selected as PinDetails.PostDetails).post.likesCount

        Assert.assertEquals(beforeLikes + 1, afterLikes)
        coVerify(exactly = 1) { likeRepository.addLike(any()) }
      }

  @Test
  fun loadUIState_userFetchFails_setsErrorAndStopsLoading() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } throws RuntimeException("boom")

        viewModel.loadUIState()
        advanceUntilIdle()

        val s = viewModel.uiState.value
        Assert.assertTrue(s.isError)
        Assert.assertFalse(s.isLoading)
        Assert.assertNotNull(s.errorMsg)
      }

  @Test
  fun onLocationPermissionResult_updatesRenderState() {
    viewModel.onLocationPermissionResult(true)
    Assert.assertTrue(viewModel.renderState.value.showUserLocation)
  }

  @Test
  fun requestRecenter_setsNonce_and_consumeRecenter_clearsIt() {
    viewModel.requestRecenter()
    val withNonce = viewModel.renderState.value
    Assert.assertNotNull(withNonce.recenterNonce)

    viewModel.consumeRecenter()
    val cleared = viewModel.renderState.value
    Assert.assertNull(cleared.recenterNonce)
  }

  @Test
  fun clearSelection_setsSelectedToNull() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { postsRepository.getAllPosts() } returns listOf(post1)
        coEvery { postsRepository.getPost("p1") } returns post1
        coEvery { userRepository.getSimpleUser("user-a") } returns
            SimpleUser("user-a", "author-a", "https://example.com/a.jpg")
        coEvery { userRepository.getSimpleUser(loggedInUserId) } returns
            SimpleUser(loggedInUserId, "regular-user", "https://example.com/me.jpg")
        coEvery { likeRepository.getLikeForPost("p1") } returns null
        stubAnimal()

        viewModel.loadUIState()
        advanceUntilIdle()
        viewModel.onPinSelected("p1")
        advanceUntilIdle()
        Assert.assertNotNull(viewModel.uiState.value.selected)

        viewModel.clearSelection()
        Assert.assertNull(viewModel.uiState.value.selected)
      }

  @Test
  fun testClearAndSetMethods() {
    viewModel.setErrorMsg("Error occurred")
    Assert.assertEquals("Error occurred", viewModel.uiState.value.errorMsg)
    viewModel.setRenderMsg("Render error")
    Assert.assertEquals("Render error", viewModel.renderState.value.renderError)
    viewModel.clearErrorMsg()
    Assert.assertNull(viewModel.uiState.value.errorMsg)
    viewModel.clearRenderError()
    Assert.assertNull(viewModel.renderState.value.renderError)
  }
}
