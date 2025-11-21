package com.android.wildex.ui.social

import com.android.wildex.model.friendRequest.FriendRequest
import com.android.wildex.model.friendRequest.FriendRequestRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserFriends
import com.android.wildex.model.user.UserFriendsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.utils.MainDispatcherRule
import com.google.firebase.Timestamp
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FriendScreenViewModelTest {
  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var userRepository: UserRepository
  private lateinit var userFriendsRepository: UserFriendsRepository
  private lateinit var friendRequestRepository: FriendRequestRepository
  private lateinit var postsRepository: PostsRepository
  private lateinit var userRecommender: UserRecommender
  private lateinit var viewModel: FriendScreenViewModel

  private val u1 =
      User(
          userId = "currentUserId",
          username = "currentUsername",
          name = "John",
          surname = "Doe",
          bio = "This is a bio",
          profilePictureURL =
              "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "France",
          friendsCount = 3,
      )

  private val su1 = SimpleUser(u1.userId, u1.username, u1.profilePictureURL, u1.userType)

  private val u2 =
      User(
          userId = "user2",
          username = "user2",
          name = "Bob",
          surname = "Smith",
          bio = "This is my bob bio",
          profilePictureURL =
              "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "France",
          friendsCount = 3,
      )

  private val su2 = SimpleUser(u2.userId, u2.username, u2.profilePictureURL, u2.userType)

  private val u3 =
      User(
          userId = "user3",
          username = "user3",
          name = "Harissa",
          surname = "Lakaka",
          bio = "This is my bob bio",
          profilePictureURL =
              "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "France",
          friendsCount = 3,
      )

  private val su3 = SimpleUser(u3.userId, u3.username, u3.profilePictureURL, u3.userType)

  private val u4 =
      User(
          userId = "user4",
          username = "user4",
          name = "Paul",
          surname = "Atreides",
          bio = "This is my bob bio",
          profilePictureURL =
              "https://www.shareicon.net/data/512x512/2016/05/24/770137_man_512x512.png",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "France",
          friendsCount = 3,
      )

  private val su4 = SimpleUser(u4.userId, u4.username, u4.profilePictureURL, u4.userType)

  private val userFriends1 =
      UserFriends(userId = u1.userId, friendsId = listOf(u2.userId), friendsCount = 1)

  private val userFriends2 =
      UserFriends(userId = u2.userId, friendsId = listOf(u1.userId, u3.userId), friendsCount = 2)

  private val request1 = FriendRequest(senderId = u1.userId, receiverId = u3.userId)

  private val request2 = FriendRequest(senderId = u4.userId, receiverId = u1.userId)

  private val suggestions =
      listOf(
          RecommendationResult(su3, "is the chosen one"),
          RecommendationResult(
              su4, "elle me dit écris une chanson contente, pas une chanson déprimante"))

  @Before
  fun setUp() {
    userRepository = mockk()
    userFriendsRepository = mockk()
    friendRequestRepository = mockk()
    postsRepository = mockk()
    userRecommender = mockk()

    viewModel =
        FriendScreenViewModel(
            currentUserId = "currentUserId",
            userRepository = userRepository,
            userFriendsRepository = userFriendsRepository,
            friendRequestRepository = friendRequestRepository,
            userRecommender = userRecommender,
        )

    coEvery { userFriendsRepository.getAllFriendsOfUser("currentUserId") } returns
        userFriends1.friendsId
    coEvery { friendRequestRepository.getAllFriendRequestsBySender("currentUserId") } returns
        listOf(request1)
    coEvery { userRepository.getSimpleUser(u2.userId) } returns su2
    coEvery { userRecommender.getRecommendedUsers() } returns suggestions
    coEvery { friendRequestRepository.getAllFriendRequestsByReceiver("currentUserId") } returns
        listOf(request2)
    coEvery { userFriendsRepository.getAllFriendsOfUser("user2") } returns userFriends2.friendsId
    coEvery { userRepository.getSimpleUser(u1.userId) } returns su1
    coEvery { userRepository.getSimpleUser(u3.userId) } returns su3
    coEvery { userRepository.getSimpleUser(u4.userId) } returns su4
  }

  @Test
  fun viewModelInitializesStateCorrectly() {
    val state = viewModel.uiState.value

    Assert.assertEquals(emptyList<FriendState>(), state.friends)
    Assert.assertEquals(emptyList<RecommendationResult>(), state.suggestions)
    Assert.assertEquals(emptyList<FriendRequest>(), state.receivedRequests)
    Assert.assertEquals(emptyList<FriendRequest>(), state.sentRequests)
    Assert.assertFalse(state.isCurrentUser)
    Assert.assertFalse(state.isRefreshing)
    Assert.assertFalse(state.isLoading)
    Assert.assertFalse(state.isError)
    Assert.assertNull(state.errorMsg)
  }

  @Test
  fun loadingStateOfCurrentUserWorksCorrectly() {
    mainDispatcherRule.runTest {
      val deferred = CompletableDeferred<List<Id>>()
      coEvery { userFriendsRepository.getAllFriendsOfUser("currentUserId") } coAnswers
          {
            deferred.await()
          }
      viewModel.loadUIState("currentUserId")
      Assert.assertTrue(viewModel.uiState.value.isLoading)
      Assert.assertFalse(viewModel.uiState.value.isRefreshing)
      deferred.complete(userFriends1.friendsId)
      advanceUntilIdle()
      val state = viewModel.uiState.value
      val expectedFriendStates =
          listOf(FriendState(su2, isFriend = true, isPending = false, isCurrentUser = false))
      Assert.assertEquals(expectedFriendStates, state.friends)
      Assert.assertEquals(suggestions, state.suggestions)
      Assert.assertEquals(listOf(request2), state.receivedRequests)
      Assert.assertEquals(listOf(request1), state.sentRequests)
      Assert.assertTrue(state.isCurrentUser)
      Assert.assertFalse(state.isRefreshing)
      Assert.assertFalse(state.isLoading)
      Assert.assertFalse(state.isError)
      Assert.assertNull(state.errorMsg)
    }
  }

  @Test
  fun loadingStateOfCurrentUser_WhenRepoThrows_WorksCorrectly() {
    mainDispatcherRule.runTest {
      coEvery { userFriendsRepository.getAllFriendsOfUser("currentUserId") } throws
          RuntimeException("cheh")
      viewModel.loadUIState("currentUserId")
      advanceUntilIdle()
      val state = viewModel.uiState.value
      Assert.assertEquals(emptyList<FriendState>(), state.friends)
      Assert.assertEquals(emptyList<RecommendationResult>(), state.suggestions)
      Assert.assertEquals(emptyList<FriendRequest>(), state.receivedRequests)
      Assert.assertEquals(emptyList<FriendRequest>(), state.sentRequests)
      Assert.assertFalse(state.isCurrentUser)
      Assert.assertFalse(state.isRefreshing)
      Assert.assertFalse(state.isLoading)
      Assert.assertTrue(state.isError)
      Assert.assertEquals("cheh", state.errorMsg)
    }
  }

  @Test
  fun loadingStateOfOtherUserWorksCorrectly() {
    mainDispatcherRule.runTest {
      val deferred = CompletableDeferred<List<Id>>()
      coEvery { userFriendsRepository.getAllFriendsOfUser("user2") } coAnswers { deferred.await() }
      viewModel.loadUIState("user2")
      Assert.assertTrue(viewModel.uiState.value.isLoading)
      Assert.assertFalse(viewModel.uiState.value.isRefreshing)
      deferred.complete(userFriends2.friendsId)
      advanceUntilIdle()
      val state = viewModel.uiState.value
      val expectedFriendStates =
          listOf(
              FriendState(su1, isFriend = false, isPending = false, isCurrentUser = true),
              FriendState(su3, isFriend = false, isPending = true, isCurrentUser = false))
      Assert.assertEquals(expectedFriendStates, state.friends)
      Assert.assertEquals(emptyList<RecommendationResult>(), state.suggestions)
      Assert.assertEquals(emptyList<FriendRequest>(), state.receivedRequests)
      Assert.assertEquals(emptyList<FriendRequest>(), state.sentRequests)
      Assert.assertFalse(state.isCurrentUser)
      Assert.assertFalse(state.isRefreshing)
      Assert.assertFalse(state.isLoading)
      Assert.assertFalse(state.isError)
      Assert.assertNull(state.errorMsg)
    }
  }

  @Test
  fun loadingStateOfOtherUser_WhenRepoThrows_WorksCorrectly() {
    mainDispatcherRule.runTest {
      coEvery { userFriendsRepository.getAllFriendsOfUser("user2") } throws RuntimeException("cheh")
      viewModel.loadUIState("user2")
      advanceUntilIdle()
      val state = viewModel.uiState.value
      Assert.assertEquals(emptyList<FriendState>(), state.friends)
      Assert.assertEquals(emptyList<RecommendationResult>(), state.suggestions)
      Assert.assertEquals(emptyList<FriendRequest>(), state.receivedRequests)
      Assert.assertEquals(emptyList<FriendRequest>(), state.sentRequests)
      Assert.assertFalse(state.isCurrentUser)
      Assert.assertFalse(state.isRefreshing)
      Assert.assertFalse(state.isLoading)
      Assert.assertTrue(state.isError)
      Assert.assertEquals("cheh", state.errorMsg)
    }
  }

  @Test
  fun refreshingStateWorksCorrectly() {
    mainDispatcherRule.runTest {
      val deferred = CompletableDeferred<List<Id>>()
      coEvery { userFriendsRepository.getAllFriendsOfUser("currentUserId") } coAnswers
          {
            deferred.await()
          }
      viewModel.refreshUIState("currentUserId")
      Assert.assertFalse(viewModel.uiState.value.isLoading)
      Assert.assertTrue(viewModel.uiState.value.isRefreshing)
      deferred.complete(userFriends1.friendsId)
      advanceUntilIdle()
      val state = viewModel.uiState.value
      val expectedFriendStates =
          listOf(FriendState(su2, isFriend = true, isPending = false, isCurrentUser = false))
      Assert.assertEquals(expectedFriendStates, state.friends)
      Assert.assertEquals(suggestions, state.suggestions)
      Assert.assertEquals(listOf(request2), state.receivedRequests)
      Assert.assertEquals(listOf(request1), state.sentRequests)
      Assert.assertTrue(state.isCurrentUser)
      Assert.assertFalse(state.isRefreshing)
      Assert.assertFalse(state.isLoading)
      Assert.assertFalse(state.isError)
      Assert.assertNull(state.errorMsg)
    }
  }

  @Test
  fun sendFriendRequestWhenCurrentUserWorksCorrectly() {
    mainDispatcherRule.runTest {
      coEvery { friendRequestRepository.initializeFriendRequest("currentUserId", u4.userId) } just
          Runs
      viewModel.loadUIState("currentUserId")
      advanceUntilIdle()
      coEvery { userRecommender.getRecommendedUsers() } returns
          listOf(RecommendationResult(su3, "is the chosen one"))
      viewModel.sendRequestToUser(u4.userId)
      advanceUntilIdle()
      val state = viewModel.uiState.value
      val expectedSentRequests = listOf(request1) + listOf(FriendRequest(u1.userId, u4.userId))
      val expectedFriendStates =
          listOf(FriendState(su2, isFriend = true, isPending = false, isCurrentUser = false))
      val expectedSuggestions =
          listOf(
              RecommendationResult(su3, "is the chosen one"),
          )
      Assert.assertEquals(expectedFriendStates, state.friends)
      Assert.assertEquals(expectedSuggestions, state.suggestions)
      Assert.assertEquals(listOf(request2), state.receivedRequests)
      Assert.assertEquals(expectedSentRequests, state.sentRequests)
      Assert.assertTrue(state.isCurrentUser)
      Assert.assertFalse(state.isRefreshing)
      Assert.assertFalse(state.isLoading)
      Assert.assertFalse(state.isError)
      Assert.assertNull(state.errorMsg)
    }
  }

  @Test
  fun sendFriendRequestWhenCurrentUser_WhenRepoThrows_WorksCorrectly() {
    mainDispatcherRule.runTest {
      coEvery { friendRequestRepository.initializeFriendRequest("currentUserId", u4.userId) } throws
          RuntimeException("cheh")
      viewModel.loadUIState("currentUserId")
      advanceUntilIdle()
      val stateBeforeFailure = viewModel.uiState.value
      viewModel.sendRequestToUser(u4.userId)
      advanceUntilIdle()
      val stateAfterFailure = viewModel.uiState.value
      Assert.assertEquals(stateBeforeFailure.friends, stateAfterFailure.friends)
      Assert.assertEquals(stateBeforeFailure.suggestions, stateAfterFailure.suggestions)
      Assert.assertEquals(stateBeforeFailure.receivedRequests, stateAfterFailure.receivedRequests)
      Assert.assertEquals(stateBeforeFailure.sentRequests, stateAfterFailure.sentRequests)
      Assert.assertEquals(stateBeforeFailure.isCurrentUser, stateAfterFailure.isCurrentUser)
      Assert.assertEquals(stateBeforeFailure.isRefreshing, stateAfterFailure.isRefreshing)
      Assert.assertEquals(stateBeforeFailure.isLoading, stateAfterFailure.isLoading)
      Assert.assertEquals(stateBeforeFailure.isError, stateAfterFailure.isError)
      Assert.assertEquals("Failed to send request to user user4 : cheh", stateAfterFailure.errorMsg)
    }
  }

  @Test
  fun sendFriendRequestWhenOtherUserWorksCorrectly() {
    mainDispatcherRule.runTest {
      coEvery { friendRequestRepository.getAllFriendRequestsBySender("currentUserId") } returns
          emptyList()
      coEvery { friendRequestRepository.initializeFriendRequest("currentUserId", u3.userId) } just
          Runs
      viewModel.loadUIState("user2")
      advanceUntilIdle()
      val oldState = viewModel.uiState.value
      val expectedFriendOldStates =
          listOf(
              FriendState(su1, isFriend = false, isPending = false, isCurrentUser = true),
              FriendState(su3, isFriend = false, isPending = false, isCurrentUser = false))
      Assert.assertEquals(expectedFriendOldStates, oldState.friends)
      viewModel.sendRequestToUser(u3.userId)
      advanceUntilIdle()
      val newState = viewModel.uiState.value
      val expectedFriendStates =
          listOf(
              FriendState(su1, isFriend = false, isPending = false, isCurrentUser = true),
              FriendState(su3, isFriend = false, isPending = true, isCurrentUser = false))
      Assert.assertEquals(expectedFriendStates, newState.friends)
      Assert.assertEquals(emptyList<RecommendationResult>(), newState.suggestions)
      Assert.assertEquals(emptyList<FriendRequest>(), newState.receivedRequests)
      Assert.assertEquals(emptyList<FriendRequest>(), newState.sentRequests)
      Assert.assertFalse(newState.isCurrentUser)
      Assert.assertFalse(newState.isRefreshing)
      Assert.assertFalse(newState.isLoading)
      Assert.assertFalse(newState.isError)
      Assert.assertNull(newState.errorMsg)
    }
  }

  @Test
  fun unfollowUserWorksCorrectly() {
    mainDispatcherRule.runTest {
      coEvery { userFriendsRepository.deleteFriendToUserFriendsOfUser(u2.userId, u1.userId) } just
          Runs
      coEvery { userFriendsRepository.deleteFriendToUserFriendsOfUser(u1.userId, u2.userId) } just
          Runs
      viewModel.loadUIState(u1.userId)
      advanceUntilIdle()
      viewModel.unfollowUser(u2.userId)
      advanceUntilIdle()
      val state = viewModel.uiState.value
      val expectedFriendStates =
          listOf(FriendState(su2, isFriend = false, isPending = false, isCurrentUser = false))
      Assert.assertEquals(expectedFriendStates, state.friends)
      Assert.assertEquals(suggestions, state.suggestions)
      Assert.assertEquals(listOf(request2), state.receivedRequests)
      Assert.assertEquals(listOf(request1), state.sentRequests)
      Assert.assertTrue(state.isCurrentUser)
      Assert.assertFalse(state.isRefreshing)
      Assert.assertFalse(state.isLoading)
      Assert.assertFalse(state.isError)
      Assert.assertNull(state.errorMsg)
    }
  }

  @Test
  fun unfollowUser_WhenRepoThrows_WorksCorrectly() {
    mainDispatcherRule.runTest {
      coEvery { userFriendsRepository.deleteFriendToUserFriendsOfUser(u2.userId, u1.userId) } throws
          RuntimeException("cheh")
      coEvery { userFriendsRepository.deleteFriendToUserFriendsOfUser(u1.userId, u2.userId) } just
          Runs
      viewModel.loadUIState(u1.userId)
      advanceUntilIdle()
      val stateBeforeFailure = viewModel.uiState.value
      viewModel.unfollowUser(u2.userId)
      advanceUntilIdle()
      val stateAfterFailure = viewModel.uiState.value
      Assert.assertEquals(stateBeforeFailure.friends, stateAfterFailure.friends)
      Assert.assertEquals(stateBeforeFailure.suggestions, stateAfterFailure.suggestions)
      Assert.assertEquals(stateBeforeFailure.receivedRequests, stateAfterFailure.receivedRequests)
      Assert.assertEquals(stateBeforeFailure.sentRequests, stateAfterFailure.sentRequests)
      Assert.assertEquals(stateBeforeFailure.isCurrentUser, stateAfterFailure.isCurrentUser)
      Assert.assertEquals(stateBeforeFailure.isRefreshing, stateAfterFailure.isRefreshing)
      Assert.assertEquals(stateBeforeFailure.isLoading, stateAfterFailure.isLoading)
      Assert.assertEquals(stateBeforeFailure.isError, stateAfterFailure.isError)
      Assert.assertEquals("Failed to unfollow user user2 : cheh", stateAfterFailure.errorMsg)
    }
  }

  @Test
  fun acceptReceivedRequestWorksCorrectly() {
    mainDispatcherRule.runTest {
      coEvery {
        friendRequestRepository.acceptFriendRequest(FriendRequest(u4.userId, u1.userId))
      } just Runs
      coEvery { userFriendsRepository.addFriendToUserFriendsOfUser(u4.userId, u1.userId) } just Runs
      coEvery { userFriendsRepository.addFriendToUserFriendsOfUser(u1.userId, u4.userId) } just Runs
      viewModel.loadUIState(u1.userId)
      advanceUntilIdle()
      val friendStates = viewModel.uiState.value.friends
      viewModel.acceptReceivedRequest(u4.userId)
      advanceUntilIdle()
      val state = viewModel.uiState.value
      val expectedReceivedRequests = emptyList<FriendRequest>()
      val expectedFriendStates =
          friendStates +
              listOf(FriendState(su4, isFriend = true, isPending = false, isCurrentUser = false))
      Assert.assertEquals(expectedFriendStates, state.friends)
      Assert.assertEquals(suggestions, state.suggestions)
      Assert.assertEquals(expectedReceivedRequests, state.receivedRequests)
      Assert.assertEquals(listOf(request1), state.sentRequests)
      Assert.assertTrue(state.isCurrentUser)
      Assert.assertFalse(state.isRefreshing)
      Assert.assertFalse(state.isLoading)
      Assert.assertFalse(state.isError)
      Assert.assertNull(state.errorMsg)
    }
  }

  @Test
  fun acceptReceivedRequest_WhenRepoThrows_WorksCorrectly() {
    mainDispatcherRule.runTest {
      coEvery {
        friendRequestRepository.acceptFriendRequest(FriendRequest(u4.userId, u1.userId))
      } throws RuntimeException("cheh")
      viewModel.loadUIState(u1.userId)
      advanceUntilIdle()
      val stateBeforeFailure = viewModel.uiState.value
      viewModel.acceptReceivedRequest(u4.userId)
      advanceUntilIdle()
      val stateAfterFailure = viewModel.uiState.value
      Assert.assertEquals(stateBeforeFailure.friends, stateAfterFailure.friends)
      Assert.assertEquals(stateBeforeFailure.suggestions, stateAfterFailure.suggestions)
      Assert.assertEquals(stateBeforeFailure.receivedRequests, stateAfterFailure.receivedRequests)
      Assert.assertEquals(stateBeforeFailure.sentRequests, stateAfterFailure.sentRequests)
      Assert.assertEquals(stateBeforeFailure.isCurrentUser, stateAfterFailure.isCurrentUser)
      Assert.assertEquals(stateBeforeFailure.isRefreshing, stateAfterFailure.isRefreshing)
      Assert.assertEquals(stateBeforeFailure.isLoading, stateAfterFailure.isLoading)
      Assert.assertEquals(stateBeforeFailure.isError, stateAfterFailure.isError)
      Assert.assertEquals(
          "Failed to accept request from user user4 : cheh", stateAfterFailure.errorMsg)
    }
  }

  @Test
  fun declineReceivedRequestWorksCorrectly() {
    mainDispatcherRule.runTest {
      coEvery {
        friendRequestRepository.refuseFriendRequest(FriendRequest(u4.userId, u1.userId))
      } just Runs
      viewModel.loadUIState(u1.userId)
      advanceUntilIdle()
      viewModel.declineReceivedRequest(u4.userId)
      advanceUntilIdle()
      val state = viewModel.uiState.value
      val expectedReceivedRequests = emptyList<FriendRequest>()
      val expectedFriendStates =
          listOf(FriendState(su2, isFriend = true, isPending = false, isCurrentUser = false))
      Assert.assertEquals(expectedFriendStates, state.friends)
      Assert.assertEquals(suggestions, state.suggestions)
      Assert.assertEquals(expectedReceivedRequests, state.receivedRequests)
      Assert.assertEquals(listOf(request1), state.sentRequests)
      Assert.assertTrue(state.isCurrentUser)
      Assert.assertFalse(state.isRefreshing)
      Assert.assertFalse(state.isLoading)
      Assert.assertFalse(state.isError)
      Assert.assertNull(state.errorMsg)
    }
  }

  @Test
  fun declineReceivedRequest_WhenRepoThrows_WorksCorrectly() {
    mainDispatcherRule.runTest {
      coEvery {
        friendRequestRepository.refuseFriendRequest(FriendRequest(u4.userId, u1.userId))
      } throws RuntimeException("cheh")
      viewModel.loadUIState(u1.userId)
      advanceUntilIdle()
      val stateBeforeFailure = viewModel.uiState.value
      viewModel.declineReceivedRequest(u4.userId)
      advanceUntilIdle()
      val stateAfterFailure = viewModel.uiState.value
      Assert.assertEquals(stateBeforeFailure.friends, stateAfterFailure.friends)
      Assert.assertEquals(stateBeforeFailure.suggestions, stateAfterFailure.suggestions)
      Assert.assertEquals(stateBeforeFailure.receivedRequests, stateAfterFailure.receivedRequests)
      Assert.assertEquals(stateBeforeFailure.sentRequests, stateAfterFailure.sentRequests)
      Assert.assertEquals(stateBeforeFailure.isCurrentUser, stateAfterFailure.isCurrentUser)
      Assert.assertEquals(stateBeforeFailure.isRefreshing, stateAfterFailure.isRefreshing)
      Assert.assertEquals(stateBeforeFailure.isLoading, stateAfterFailure.isLoading)
      Assert.assertEquals(stateBeforeFailure.isError, stateAfterFailure.isError)
      Assert.assertEquals(
          "Failed to decline request from user user4 : cheh", stateAfterFailure.errorMsg)
    }
  }

  @Test
  fun cancelSentRequestWhenCurrentUserWorksCorrectly() {
    mainDispatcherRule.runTest {
      coEvery {
        friendRequestRepository.refuseFriendRequest(
            FriendRequest(senderId = u1.userId, receiverId = u3.userId))
      } just Runs
      viewModel.loadUIState(u1.userId)
      advanceUntilIdle()
      viewModel.cancelSentRequest(u3.userId)
      advanceUntilIdle()
      val state = viewModel.uiState.value
      val expectedSentRequest = emptyList<FriendRequest>()
      val expectedFriendStates =
          listOf(FriendState(su2, isFriend = true, isPending = false, isCurrentUser = false))
      Assert.assertEquals(expectedFriendStates, state.friends)
      Assert.assertEquals(suggestions, state.suggestions)
      Assert.assertEquals(listOf(request2), state.receivedRequests)
      Assert.assertEquals(expectedSentRequest, state.sentRequests)
      Assert.assertTrue(state.isCurrentUser)
      Assert.assertFalse(state.isRefreshing)
      Assert.assertFalse(state.isLoading)
      Assert.assertFalse(state.isError)
      Assert.assertNull(state.errorMsg)
    }
  }

  @Test
  fun cancelSentRequestWhenOtherUserWorksCorrectly() {
    mainDispatcherRule.runTest {
      coEvery {
        friendRequestRepository.refuseFriendRequest(
            FriendRequest(senderId = u1.userId, receiverId = u3.userId))
      } just Runs
      viewModel.loadUIState(u2.userId)
      advanceUntilIdle()
      viewModel.cancelSentRequest(u3.userId)
      advanceUntilIdle()
      val state = viewModel.uiState.value
      val expectedSentRequests = emptyList<FriendRequest>()
      val expectedFriendStates =
          listOf(
              FriendState(su1, isFriend = false, isPending = false, isCurrentUser = true),
              FriendState(su3, isFriend = false, isPending = false, isCurrentUser = false))
      Assert.assertEquals(expectedFriendStates, state.friends)
      Assert.assertEquals(emptyList<RecommendationResult>(), state.suggestions)
      Assert.assertEquals(emptyList<FriendRequest>(), state.receivedRequests)
      Assert.assertEquals(expectedSentRequests, state.sentRequests)
      Assert.assertFalse(state.isCurrentUser)
      Assert.assertFalse(state.isRefreshing)
      Assert.assertFalse(state.isLoading)
      Assert.assertFalse(state.isError)
      Assert.assertNull(state.errorMsg)
    }
  }

  @Test
  fun cancelSentRequest_WhenRepoThrows_WorksCorrectly() {
    mainDispatcherRule.runTest {
      coEvery {
        friendRequestRepository.refuseFriendRequest(
            FriendRequest(senderId = u1.userId, receiverId = u3.userId))
      } throws RuntimeException("cheh")
      viewModel.loadUIState(u1.userId)
      advanceUntilIdle()
      val stateBeforeFailure = viewModel.uiState.value
      viewModel.cancelSentRequest(u3.userId)
      advanceUntilIdle()
      val stateAfterFailure = viewModel.uiState.value
      Assert.assertEquals(stateBeforeFailure.friends, stateAfterFailure.friends)
      Assert.assertEquals(stateBeforeFailure.suggestions, stateAfterFailure.suggestions)
      Assert.assertEquals(stateBeforeFailure.receivedRequests, stateAfterFailure.receivedRequests)
      Assert.assertEquals(stateBeforeFailure.sentRequests, stateAfterFailure.sentRequests)
      Assert.assertEquals(stateBeforeFailure.isCurrentUser, stateAfterFailure.isCurrentUser)
      Assert.assertEquals(stateBeforeFailure.isRefreshing, stateAfterFailure.isRefreshing)
      Assert.assertEquals(stateBeforeFailure.isLoading, stateAfterFailure.isLoading)
      Assert.assertEquals(stateBeforeFailure.isError, stateAfterFailure.isError)
      Assert.assertEquals(
          "Failed to cancel request to user user3 : cheh", stateAfterFailure.errorMsg)
    }
  }
}
