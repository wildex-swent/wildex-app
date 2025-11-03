package com.android.wildex.ui.map

import com.android.wildex.model.animal.Animal
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.map.PinDetails
import com.android.wildex.model.report.Report
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.report.ReportStatus
import com.android.wildex.model.social.Like
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

  private fun stubSimpleUsers() {
    coEvery { userRepository.getSimpleUser("user-a") } returns
        SimpleUser("user-a", "author-a", "https://example.com/author-a.jpg")
    coEvery { userRepository.getSimpleUser(loggedInUserId) } returns
        SimpleUser(loggedInUserId, "regular-user", "https://example.com/me.jpg")
  }

  private fun stubAnimal() {
    coEvery { animalRepository.getAnimal("a1") } returns
        Animal("a1", "canidae", "fox", "", "https://example.com/fox.jpg")
  }

  private fun stubReportsEmpty() {
    coEvery { reportRepository.getAllReports() } returns emptyList()
    coEvery { reportRepository.getAllReportsByAuthor(any()) } returns emptyList()
    coEvery { reportRepository.getAllReportsByAssignee(any()) } returns emptyList()
  }

  @Test
  fun load_and_tabs_behave_correctly() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { postsRepository.getAllPosts() } returns listOf(post1, post2)
        coEvery { postsRepository.getAllPostsByGivenAuthor(loggedInUserId) } returns listOf(post2)
        stubReportsEmpty()
        stubSimpleUsers()

        viewModel.loadUIState()
        advanceUntilIdle()

        val s1 = viewModel.uiState.value
        Assert.assertEquals(listOf(MapTab.Posts, MapTab.MyPosts), s1.availableTabs)
        Assert.assertEquals(MapTab.Posts, s1.activeTab)
        Assert.assertEquals(2, s1.pins.size)

        viewModel.onTabSelected(MapTab.MyPosts)
        advanceUntilIdle()
        val s2 = viewModel.uiState.value
        Assert.assertEquals(MapTab.MyPosts, s2.activeTab)
        Assert.assertEquals(1, s2.pins.size)
        Assert.assertEquals("p2", s2.pins.first().id)

        viewModel.refreshUIState()
        advanceUntilIdle()
        val s3 = viewModel.uiState.value
        Assert.assertEquals(MapTab.MyPosts, s3.activeTab)

        viewModel.onTabSelected(MapTab.Reports)
        advanceUntilIdle()
        val s4 = viewModel.uiState.value
        Assert.assertEquals(MapTab.MyPosts, s4.activeTab)

        coEvery { reportRepository.getAllReports() } returns listOf(report1)
        viewModel.loadUIState()
        advanceUntilIdle()
        val s5 = viewModel.uiState.value
        Assert.assertEquals(listOf(MapTab.Posts, MapTab.MyPosts), s5.availableTabs)

        coEvery { userRepository.getUser(loggedInUserId) } returns professionalUser
        coEvery { reportRepository.getAllReports() } returns listOf(report1)
        viewModel.loadUIState()
        advanceUntilIdle()
        val s6 = viewModel.uiState.value
        Assert.assertEquals(
            listOf(MapTab.Posts, MapTab.MyPosts, MapTab.Reports),
            s6.availableTabs,
        )
      }

  @Test
  fun pin_selection_covers_post_report_and_animal_fallback() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { postsRepository.getAllPosts() } returns listOf(post1)
        coEvery { postsRepository.getAllPostsByGivenAuthor(loggedInUserId) } returns emptyList()
        coEvery { reportRepository.getAllReports() } returns emptyList()
        stubSimpleUsers()
        stubAnimal()
        coEvery { postsRepository.getPost("p1") } returns post1
        coEvery { likeRepository.getLikeForPost("p1") } returns null

        viewModel.loadUIState()
        advanceUntilIdle()

        viewModel.onPinSelected("p1")
        advanceUntilIdle()
        val sel1 = viewModel.uiState.value.selected as PinDetails.PostDetails
        Assert.assertEquals("fox", sel1.animalName)

        coEvery { animalRepository.getAnimal("a1") } throws RuntimeException("nope")
        viewModel.onPinSelected("p1")
        advanceUntilIdle()
        val sel2 = viewModel.uiState.value.selected as PinDetails.PostDetails
        Assert.assertEquals("animal", sel2.animalName)

        coEvery { userRepository.getUser(loggedInUserId) } returns professionalUser
        coEvery { postsRepository.getAllPosts() } returns emptyList()
        coEvery { reportRepository.getAllReports() } returns listOf(report1)
        coEvery { reportRepository.getReport("r1") } returns report1
        coEvery { userRepository.getSimpleUser("author-r") } returns
            SimpleUser("author-r", "reporter", "https://example.com/x.jpg")
        coEvery { userRepository.getSimpleUser("pro-1") } returns
            SimpleUser("pro-1", "assignee", "https://example.com/y.jpg")

        viewModel.loadUIState()
        advanceUntilIdle()
        viewModel.onTabSelected(MapTab.Reports)
        advanceUntilIdle()
        viewModel.onPinSelected("r1")
        advanceUntilIdle()

        val sel3 = viewModel.uiState.value.selected
        Assert.assertTrue(sel3 is PinDetails.ReportDetails)
      }

  @Test
  fun error_paths_are_handled() =
      mainDispatcherRule.runTest {
        // ---- onPinSelected throws
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { postsRepository.getAllPosts() } returns listOf(post1)
        coEvery { postsRepository.getAllPostsByGivenAuthor(loggedInUserId) } returns emptyList()
        coEvery { reportRepository.getAllReports() } returns emptyList()
        coEvery { userRepository.getSimpleUser("user-a") } returns
            SimpleUser("user-a", "author-a", "https://example.com/a.jpg")
        coEvery { postsRepository.getPost("p1") } throws RuntimeException("db down")

        viewModel.loadUIState()
        advanceUntilIdle()
        viewModel.onPinSelected("p1")
        advanceUntilIdle()
        val msg = viewModel.uiState.value.errorMsg
        Assert.assertNotNull(msg)
        Assert.assertTrue(msg!!.startsWith("Failed to load pin:"))

        val vm2 =
            MapScreenViewModel(
                loggedInUserId = "",
                userRepository = userRepository,
                postRepository = postsRepository,
                reportRepository = reportRepository,
                likeRepository = likeRepository,
                animalRepository = animalRepository,
            )
        vm2.loadUIState()
        advanceUntilIdle()
        val s2 = vm2.uiState.value
        Assert.assertTrue(s2.isError)
        Assert.assertEquals("No logged in user", s2.errorMsg)
        Assert.assertFalse(s2.isLoading)
      }

  @Test
  fun toggleLike_covers_all_branches() =
      mainDispatcherRule.runTest {
        // happy path
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
        val before = (viewModel.uiState.value.selected as PinDetails.PostDetails).post.likesCount

        viewModel.toggleLike("p1")
        advanceUntilIdle()

        val after = (viewModel.uiState.value.selected as PinDetails.PostDetails).post.likesCount
        Assert.assertEquals(before + 1, after)
        coVerify(exactly = 1) { likeRepository.addLike(any()) }

        // blank uid branch
        val vmBlank =
            MapScreenViewModel(
                loggedInUserId = "",
                userRepository = userRepository,
                postRepository = postsRepository,
                reportRepository = reportRepository,
                likeRepository = likeRepository,
                animalRepository = animalRepository,
            )
        vmBlank.toggleLike("whatever")
        Assert.assertEquals(
            "You must be logged in to like posts.",
            vmBlank.uiState.value.errorMsg,
        )

        // delete fails branch (already liked -> delete -> exception -> restore)
        coEvery { likeRepository.getLikeForPost("p1") } returns Like("like-1", "p1", loggedInUserId)
        coEvery { likeRepository.deleteLike("like-1") } throws RuntimeException("network")

        viewModel.toggleLike("p1")
        advanceUntilIdle()

        val err = viewModel.uiState.value.errorMsg
        Assert.assertNotNull(err)
        Assert.assertTrue(err!!.contains("Could not update like"))
      }

  @Test
  fun utility_methods_work() =
      mainDispatcherRule.runTest {
        // selection
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

        // set/clear errors
        viewModel.setErrorMsg("err")
        Assert.assertEquals("err", viewModel.uiState.value.errorMsg)
        viewModel.clearErrorMsg()
        Assert.assertNull(viewModel.uiState.value.errorMsg)

        viewModel.setRenderMsg("render")
        Assert.assertEquals("render", viewModel.renderState.value.renderError)
        viewModel.clearRenderError()
        Assert.assertNull(viewModel.renderState.value.renderError)
      }

  @Test
  fun render_state_changes_are_applied() {
    viewModel.onLocationPermissionResult(true)
    Assert.assertTrue(viewModel.renderState.value.showUserLocation)

    viewModel.requestRecenter()
    val withNonce = viewModel.renderState.value.recenterNonce
    Assert.assertNotNull(withNonce)

    viewModel.consumeRecenter()
    Assert.assertNull(viewModel.renderState.value.recenterNonce)
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
}
