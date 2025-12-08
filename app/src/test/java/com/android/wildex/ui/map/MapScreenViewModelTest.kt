package com.android.wildex.ui.map

import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.map.MapPin
import com.android.wildex.model.map.PinDetails
import com.android.wildex.model.report.Report
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.social.CommentRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
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
  private lateinit var commentRepository: CommentRepository
  private lateinit var animalRepository: AnimalRepository
  private lateinit var viewModel: MapScreenViewModel

  private val loggedInUserId = "uid-1"
  private val now = Timestamp.now()
  private val lausanne = Location(46.5197, 6.6323, "Lausanne")

  private val post1 =
      Post("p1", "user-a", "https://example.com/p1.jpg", lausanne, "first", now, "a1")
  private val post2 =
      Post("p2", loggedInUserId, "https://example.com/p2.jpg", lausanne, "second", now, "a2")
  private val report1 = Report("r1", "url", lausanne, now, "injured", "author", "assignee")
  private val report2 =
      Report(
          "r2",
          "url2",
          lausanne,
          now,
          "help",
          "x",
          "uid-other",
      )

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
      )
  private val proUser = regularUser.copy(userType = UserType.PROFESSIONAL)

  @Before
  fun setUp() {
    userRepository = mockk()
    postsRepository = mockk()
    reportRepository = mockk()
    likeRepository = mockk(relaxed = true)
    commentRepository = mockk(relaxed = true)
    animalRepository = mockk(relaxed = true)
    viewModel =
        MapScreenViewModel(
            userRepository = userRepository,
            postRepository = postsRepository,
            reportRepository = reportRepository,
            likeRepository = likeRepository,
            commentRepository = commentRepository,
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
        coEvery { userRepository.getSimpleUser(any()) } returns
            SimpleUser("a", "b", "c", userType = UserType.REGULAR)

        viewModel.loadUIState(loggedInUserId)
        advanceUntilIdle()
        val regState = viewModel.uiState.value
        Assert.assertEquals(listOf(MapTab.Posts, MapTab.MyPosts), regState.availableTabs)
        Assert.assertEquals(2, regState.pins.size)

        coEvery { userRepository.getUser(loggedInUserId) } returns proUser
        coEvery { reportRepository.getAllReports() } returns listOf(report1)
        viewModel.loadUIState(loggedInUserId)
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
        coEvery { userRepository.getSimpleUser(any()) } returns
            SimpleUser("a", "b", "c", userType = UserType.REGULAR)

        viewModel.loadUIState(loggedInUserId)
        advanceUntilIdle()
        viewModel.onTabSelected(MapTab.MyPosts, loggedInUserId)
        advanceUntilIdle()
        val myPosts = viewModel.uiState.value
        Assert.assertEquals(MapTab.MyPosts, myPosts.activeTab)
        Assert.assertEquals(1, myPosts.pins.size)

        viewModel.refreshUIState(loggedInUserId)
        advanceUntilIdle()
        Assert.assertFalse(viewModel.uiState.value.isRefreshing)
      }

  @Test
  fun pinSelection_loadsCorrectDetails_forPostAndReport() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns proUser
        coEvery { userRepository.getSimpleUser(any()) } returns
            SimpleUser("x", "y", "url", userType = UserType.REGULAR)
        coEvery { postsRepository.getAllPosts() } returns listOf(post1)
        coEvery { postsRepository.getPost("p1") } returns post1
        coEvery { likeRepository.getLikeForPost(any()) } returns null
        coEvery { likeRepository.getLikesForPost("p1") } returns emptyList()
        coEvery { commentRepository.getAllCommentsByPost("p1") } returns emptyList()
        coEvery { reportRepository.getAllReports() } returns listOf(report1)
        coEvery { reportRepository.getReport("r1") } returns report1

        viewModel.loadUIState(loggedInUserId)
        advanceUntilIdle()
        viewModel.onPinSelected("p1")
        advanceUntilIdle()
        Assert.assertTrue(viewModel.uiState.value.selected[0] is PinDetails.PostDetails)

        viewModel.onTabSelected(MapTab.Reports, loggedInUserId)
        advanceUntilIdle()
        viewModel.onPinSelected("r1")
        advanceUntilIdle()
        Assert.assertTrue(viewModel.uiState.value.selected[0] is PinDetails.ReportDetails)
      }

  @Test
  fun toggleLike_performsOptimisticUpdate_andCallsRepository() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { postsRepository.getAllPosts() } returns listOf(post1)
        coEvery { postsRepository.getPost("p1") } returns post1
        coEvery { userRepository.getSimpleUser(any()) } returns
            SimpleUser("x", "y", "url", userType = UserType.REGULAR)
        coEvery { likeRepository.getLikeForPost("p1") } returns null
        coEvery { likeRepository.getLikesForPost("p1") } returns emptyList()
        coEvery { commentRepository.getAllCommentsByPost("p1") } returns emptyList()
        coEvery { likeRepository.getNewLikeId() } returns "like-1"

        viewModel.loadUIState(loggedInUserId)
        advanceUntilIdle()
        viewModel.onPinSelected("p1")
        advanceUntilIdle()
        val before = (viewModel.uiState.value.selected[0] as PinDetails.PostDetails).likeCount
        viewModel.toggleLike("p1")
        advanceUntilIdle()
        val after = (viewModel.uiState.value.selected[0] as PinDetails.PostDetails).likeCount
        Assert.assertEquals(before + 1, after)
        coVerify { likeRepository.addLike(any()) }
      }

  @Test
  fun clearFunctions_andErrorHandling_workCorrectly() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } throws RuntimeException("boom")
        viewModel.loadUIState(loggedInUserId)
        advanceUntilIdle()
        val s = viewModel.uiState.value
        Assert.assertTrue(s.isError)
        Assert.assertFalse(s.isLoading)
        Assert.assertNotNull(s.errorMsg)

        viewModel.clearSelection()
        Assert.assertTrue(viewModel.uiState.value.selected.isEmpty())
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

  @Test
  fun otherUser_reportsTab_usesLoadReportsInvolvingUser() =
      mainDispatcherRule.runTest {
        val otherUserId = "uid-other"
        val r1 = report1
        val r1Dup = r1.copy()
        val r2Assigned = report2
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { userRepository.getSimpleUser(any()) } returns
            SimpleUser("u", "n", "pic", userType = UserType.REGULAR)
        coEvery { postsRepository.getAllPostsByGivenAuthor(otherUserId) } returns emptyList()
        coEvery { reportRepository.getAllReportsByAuthor(otherUserId) } returns listOf(r1, r1Dup)
        coEvery { reportRepository.getAllReportsByAssignee(otherUserId) } returns listOf(r2Assigned)

        viewModel.loadUIState(otherUserId)
        advanceUntilIdle()

        val initial = viewModel.uiState.value
        Assert.assertEquals(listOf(MapTab.MyPosts, MapTab.Reports), initial.availableTabs)
        Assert.assertEquals(MapTab.MyPosts, initial.activeTab)

        viewModel.onTabSelected(MapTab.Reports, otherUserId)
        advanceUntilIdle()

        val after = viewModel.uiState.value
        Assert.assertEquals(MapTab.Reports, after.activeTab)
        Assert.assertEquals(2, after.pins.size)
      }

  @Test
  fun errorBranches_cover_blankUid_AnimalFallback_pinFail_and_toggleFailureRollback() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { userRepository.getSimpleUser(any()) } returns
            SimpleUser("s", "u", "p", userType = UserType.REGULAR)
        coEvery { postsRepository.getAllPosts() } returns listOf(post1)
        coEvery { reportRepository.getAllReports() } returns emptyList()

        viewModel.loadUIState(loggedInUserId)
        advanceUntilIdle()

        coEvery { postsRepository.getPost("p1") } throws RuntimeException("boom")
        viewModel.onPinSelected("p1")
        advanceUntilIdle()
        Assert.assertTrue(viewModel.uiState.value.errorMsg?.contains("Failed to load pin") == true)

        coEvery { postsRepository.getPost("p1") } returns post1
        coEvery { likeRepository.getLikeForPost("p1") } returns null
        coEvery { likeRepository.getLikesForPost("p1") } returns emptyList()
        coEvery { commentRepository.getAllCommentsByPost("p1") } returns emptyList()
        coEvery { animalRepository.getAnimal(any()) } throws RuntimeException("nope")
        viewModel.onPinSelected("p1")
        advanceUntilIdle()
        val afterAnimalFail = viewModel.uiState.value
        Assert.assertTrue(afterAnimalFail.errorMsg?.contains("Failed to load pin") == true)
        Assert.assertTrue(afterAnimalFail.selected.isEmpty())

        coEvery { animalRepository.getAnimal(any()) } returns mockk(relaxed = true)
        viewModel.onPinSelected("p1")
        advanceUntilIdle()
        Assert.assertTrue(viewModel.uiState.value.selected.first() is PinDetails.PostDetails)

        coEvery { likeRepository.getNewLikeId() } returns "like-99"
        coEvery { likeRepository.addLike(any()) } throws RuntimeException("net")
        val before = (viewModel.uiState.value.selected[0] as PinDetails.PostDetails).likeCount
        viewModel.toggleLike("p1")
        advanceUntilIdle()
        val after = (viewModel.uiState.value.selected[0] as PinDetails.PostDetails).likeCount
        Assert.assertEquals(before, after)
        Assert.assertTrue(
            viewModel.uiState.value.errorMsg?.contains("Could not update like") == true)
        val vmBlank =
            MapScreenViewModel(
                userRepository = userRepository,
                postRepository = postsRepository,
                reportRepository = reportRepository,
                likeRepository = likeRepository,
                commentRepository = commentRepository,
                animalRepository = animalRepository,
                currentUserId = "",
            )
        vmBlank.loadUIState(loggedInUserId)
        advanceUntilIdle()
        Assert.assertTrue(vmBlank.uiState.value.isError)
        Assert.assertTrue(vmBlank.uiState.value.errorMsg?.contains("No logged in user.") == true)
        vmBlank.toggleLike("p1")
        Assert.assertTrue(
            vmBlank.uiState.value.errorMsg?.contains("You must be logged in to like posts.") ==
                true)
      }

  @Test
  fun cover_missing_branches() =
      mainDispatcherRule.runTest {
        coEvery { userRepository.getUser(loggedInUserId) } returns regularUser
        coEvery { postsRepository.getAllPosts() } returns listOf(post1, post2)
        coEvery { postsRepository.getPost("p1") } returns post1
        coEvery { userRepository.getSimpleUser(any()) } returns
            SimpleUser("x", "y", "url", userType = UserType.REGULAR)
        val like = Like("lk1", "p1", loggedInUserId)
        coEvery { likeRepository.getLikeForPost("p1") } returns like
        coEvery { likeRepository.getLikesForPost("p1") } returns listOf(like)
        coEvery { commentRepository.getAllCommentsByPost("p1") } returns emptyList()
        coEvery { likeRepository.deleteLike("lk1") } returns Unit

        viewModel.loadUIState(loggedInUserId)
        advanceUntilIdle()
        viewModel.onPinSelected("p1")
        advanceUntilIdle()
        viewModel.toggleLike("p1")
        advanceUntilIdle()
        Assert.assertEquals(
            0,
            (viewModel.uiState.value.selected[0] as PinDetails.PostDetails).likeCount,
        )
        coEvery { likeRepository.getLikeForPost("p2") } returns null
        coEvery { likeRepository.getNewLikeId() } returns "like-X"
        coEvery { likeRepository.addLike(any()) } throws RuntimeException("boom")
        viewModel.toggleLike("p2")
        advanceUntilIdle()
        Assert.assertTrue(
            viewModel.uiState.value.errorMsg?.contains("Could not update like: boom") == true)
      }

  @Test
  fun selectNext_and_selectPrev_cycleThroughSelection() =
      mainDispatcherRule.runTest {
        val fld = MapScreenViewModel::class.java.getDeclaredField("_uiState")
        fld.isAccessible = true
        @Suppress("UNCHECKED_CAST") val flow = fld.get(viewModel) as MutableStateFlow<MapUIState>

        val user =
            SimpleUser(
                userId = "u",
                username = "user",
                profilePictureURL = "",
                userType = UserType.REGULAR,
            )

        val d1 =
            PinDetails.PostDetails(
                post = post1,
                author = user,
                likedByMe = false,
                likeCount = 0,
                commentCount = 0,
                animalName = "fox",
            )
        val d2 =
            PinDetails.PostDetails(
                post = post2,
                author = user,
                likedByMe = true,
                likeCount = 5,
                commentCount = 1,
                animalName = "owl",
            )
        val d3 = d1.copy(post = post1.copy(postId = "p3"))

        flow.value =
            flow.value.copy(
                selected = listOf(d1, d2, d3),
                selectedIndex = 0,
            )

        // index: 0 -> 1 -> 2 -> 0
        viewModel.selectNext()
        Assert.assertEquals(1, viewModel.uiState.value.selectedIndex)
        viewModel.selectNext()
        Assert.assertEquals(2, viewModel.uiState.value.selectedIndex)
        viewModel.selectNext()
        Assert.assertEquals(0, viewModel.uiState.value.selectedIndex)

        // prev from 0 goes to 2
        viewModel.selectPrev()
        Assert.assertEquals(2, viewModel.uiState.value.selectedIndex)
      }

  @Test
  fun onClusterPinClicked_populatesGroupSelection_andSkipsFailingChildren() =
      mainDispatcherRule.runTest {
        // Prepare pins already in uiState
        val postPin1 =
            MapPin.PostPin(
                id = "p1",
                authorId = post1.authorId,
                location = lausanne,
                imageURL = "",
            )
        val postPin2 =
            MapPin.PostPin(
                id = "p2",
                authorId = post2.authorId,
                location = lausanne,
                imageURL = "",
            )
        val reportPin1 =
            MapPin.ReportPin(
                id = "r1",
                authorId = report1.authorId,
                location = report1.location,
                imageURL = "",
                assigneeId = report1.assigneeId,
            )

        // Inject pins into uiState
        val fld = MapScreenViewModel::class.java.getDeclaredField("_uiState")
        fld.isAccessible = true
        @Suppress("UNCHECKED_CAST") val flow = fld.get(viewModel) as MutableStateFlow<MapUIState>
        flow.value = flow.value.copy(pins = listOf(postPin1, postPin2, reportPin1))

        // Mocks for children jobs:
        coEvery { postsRepository.getPost("p1") } returns post1
        coEvery { postsRepository.getPost("p2") } throws RuntimeException("fail-child")
        coEvery { userRepository.getSimpleUser(any()) } returns
            SimpleUser("u", "name", "", userType = UserType.REGULAR)
        coEvery { likeRepository.getLikeForPost(any()) } returns null
        coEvery { likeRepository.getLikesForPost(any()) } returns emptyList()
        coEvery { commentRepository.getAllCommentsByPost(any()) } returns emptyList()
        coEvery { animalRepository.getAnimal(any()) } returns mockk(relaxed = true)
        coEvery { reportRepository.getReport("r1") } returns report1

        val cluster =
            MapPin.ClusterPin(
                id = "cluster-1",
                location = lausanne,
                childIds = listOf("p1", "p2", "r1"),
                count = 3,
            )

        viewModel.onClusterPinClicked(cluster)
        advanceUntilIdle()

        val sel = viewModel.uiState.value.selected
        // We expect p1 (post) and r1 (report); p2 failed and is skipped
        Assert.assertEquals(2, sel.size)
        Assert.assertTrue(sel.any { it is PinDetails.PostDetails && it.post.postId == "p1" })
        Assert.assertTrue(sel.any { it is PinDetails.ReportDetails && it.report.reportId == "r1" })
        Assert.assertEquals(lausanne, viewModel.uiState.value.centerCoordinates)
      }
}
