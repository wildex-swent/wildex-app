package com.android.wildex.ui.social

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.model.friendRequest.FriendRequestRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserFriendsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.android.wildex.utils.LocalRepositories
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FriendScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val userRepository: UserRepository = LocalRepositories.userRepository
  private val userFriendsRepository: UserFriendsRepository = LocalRepositories.userFriendsRepository
  private val friendRequestRepository: FriendRequestRepository =
      LocalRepositories.friendRequestRepository
  private val postsRepository: PostsRepository = LocalRepositories.postsRepository
  private lateinit var friendScreenViewModel: FriendScreenViewModel

  private val currentUser =
      User(
          userId = "currentUserId",
          username = "currentUser",
          name = "Current",
          surname = "User",
          bio = "I might be him",
          profilePictureURL = "",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "Le pays des bisounours")

  private val user1 =
      User(
          userId = "user1",
          username = "user1",
          name = "Kratos",
          surname = "Labubu",
          bio = "Don't be sorry, be better",
          profilePictureURL = "",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "War")

  private val user2 =
      User(
          userId = "user2",
          username = "user2",
          name = "David",
          surname = "Gilmour",
          bio = "Shine on you crazy diamond",
          profilePictureURL = "",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "RockNRoll")

  private val user3 =
      User(
          userId = "user3",
          username = "user3",
          name = "Jesus",
          surname = "Christ",
          bio = "Merry Christmas",
          profilePictureURL = "",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "Heaven")

  private val user4 =
      User(
          userId = "user4",
          username = "user4",
          name = "Emmanuel",
          surname = "Macron",
          bio = "C'est de la poudre de perlimpinpin",
          profilePictureURL = "",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "France")

  @Before
  fun setup() = runBlocking {
    userRepository.addUser(currentUser)
    userRepository.addUser(user1)
    userRepository.addUser(user2)
    userRepository.addUser(user3)
    userRepository.addUser(user4)

    userFriendsRepository.initializeUserFriends("currentUserId")
    userFriendsRepository.addFriendToUserFriendsOfUser("user1", "currentUserId")
    userFriendsRepository.addFriendToUserFriendsOfUser("user2", "currentUserId")
    userFriendsRepository.initializeUserFriends("user1")
    userFriendsRepository.addFriendToUserFriendsOfUser("currentUserId", "user1")
    userFriendsRepository.addFriendToUserFriendsOfUser("user3", "user1")
    userFriendsRepository.initializeUserFriends("user2")
    userFriendsRepository.addFriendToUserFriendsOfUser("currentUserId", "user2")
    userFriendsRepository.addFriendToUserFriendsOfUser("user3", "user2")
    userFriendsRepository.initializeUserFriends("user3")
    userFriendsRepository.addFriendToUserFriendsOfUser("user1", "user3")
    userFriendsRepository.addFriendToUserFriendsOfUser("user2", "user3")
    userFriendsRepository.initializeUserFriends("user4")

    friendRequestRepository.initializeFriendRequest("user4", "currentUserId")
    friendRequestRepository.initializeFriendRequest("user4", "user1")
    friendRequestRepository.initializeFriendRequest("user1", "user2")

    friendScreenViewModel =
        FriendScreenViewModel(
            currentUserId = "currentUserId",
            userRepository = userRepository,
            userFriendsRepository = userFriendsRepository,
            friendRequestRepository = friendRequestRepository,
            userRecommender =
                UserRecommender(
                    currentUserId = "currentUserId",
                    userRepository = userRepository,
                    postRepository = postsRepository,
                    userFriendsRepository = userFriendsRepository,
                    friendRequestRepository = friendRequestRepository))
  }

  @After
  fun tearDown() {
    LocalRepositories.clearAll()
  }

  @Test
  fun initialStateCurrentUserDisplaysCorrectly() {
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "currentUserId",
          onProfileClick = {},
          onGoBack = {})
    }

    composeTestRule.onNodeWithTag(FriendScreenTestTags.GO_BACK_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.FRIENDS_TAB_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.REQUESTS_TAB_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SearchBarTestTags.SEARCH_BAR).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.SCREEN_TITLE)
        .assertIsDisplayed()
        .assertTextEquals("Social")
    composeTestRule.onNodeWithTag(FriendScreenTestTags.NO_FRIENDS).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.NO_SUGGESTIONS).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.NO_SENT_REQUESTS).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.NO_RECEIVED_REQUESTS).assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user1.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForUnfollowButton(user1.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user2.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForUnfollowButton(user2.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user3.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForFollowButton(user3.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user4.userId))
        .assertIsNotDisplayed()
  }

  @Test
  fun initialStateOtherUserDisplaysCorrectly() {
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "user1",
          onProfileClick = {},
          onGoBack = {})
    }
    composeTestRule.onNodeWithTag(FriendScreenTestTags.GO_BACK_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.FRIENDS_TAB_BUTTON).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.REQUESTS_TAB_BUTTON).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(SearchBarTestTags.SEARCH_BAR).assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.SCREEN_TITLE)
        .assertIsDisplayed()
        .assertTextEquals("Social")
    composeTestRule.onNodeWithTag(FriendScreenTestTags.NO_FRIENDS).assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(currentUser.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForUnfollowButton(currentUser.userId))
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForFollowButton(currentUser.userId))
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(
            FriendScreenTestTags.testTagForAcceptReceivedRequestButton(currentUser.userId))
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(
            FriendScreenTestTags.testTagForDeclineReceivedRequestButton(currentUser.userId))
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForCancelSentRequestButton(currentUser.userId))
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user3.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForFollowButton(user3.userId))
        .assertIsDisplayed()
  }

  @Test
  fun currentUserCanNavigateBetweenTabs() {
    runBlocking { friendRequestRepository.initializeFriendRequest("currentUserId", "user3") }
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "currentUserId",
          onProfileClick = {},
          onGoBack = {})
    }

    composeTestRule.onNodeWithTag(SearchBarTestTags.SEARCH_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.FRIENDS_TAB_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.REQUESTS_TAB_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user1.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForUnfollowButton(user1.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user2.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForUnfollowButton(user2.userId))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.NO_SUGGESTIONS).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user3.userId))
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user4.userId))
        .assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.REQUESTS_TAB_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user1.userId))
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user2.userId))
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user3.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForCancelSentRequestButton(user3.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user4.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForAcceptReceivedRequestButton(user4.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForDeclineReceivedRequestButton(user4.userId))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.FRIENDS_TAB_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user1.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForUnfollowButton(user1.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user2.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForUnfollowButton(user2.userId))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.NO_SUGGESTIONS).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user3.userId))
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user4.userId))
        .assertIsNotDisplayed()
  }

  @Test
  fun onGoBackButtonInvokesCallback() {
    var goBackPushed = false
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "currentUserId",
          onProfileClick = {},
          onGoBack = { goBackPushed = true })
    }
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.GO_BACK_BUTTON)
        .assertIsDisplayed()
        .performClick()
    Assert.assertTrue(goBackPushed)
  }

  @Test
  fun onProfileClickInvokesCallback() {
    var profileClicked = ""
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "currentUserId",
          onProfileClick = { profileClicked = it },
          onGoBack = {})
    }
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForProfilePicture(user1.userId))
        .performClick()
    Assert.assertEquals("user1", profileClicked)
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForProfilePicture(user3.userId))
        .performClick()
    Assert.assertEquals("user3", profileClicked)
    composeTestRule.onNodeWithTag(FriendScreenTestTags.REQUESTS_TAB_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForProfilePicture(user4.userId))
        .performClick()
    Assert.assertEquals("user4", profileClicked)
    composeTestRule.onNodeWithTag(FriendScreenTestTags.FRIENDS_TAB_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForProfilePicture(user2.userId))
        .performClick()
    Assert.assertEquals("user2", profileClicked)
  }

  @Test
  fun unfollowingAllFriendsShowsNoFriendsText() {
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "currentUserId",
          onProfileClick = {},
          onGoBack = {})
    }
    composeTestRule.onNodeWithTag(FriendScreenTestTags.NO_FRIENDS).assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForUnfollowButton(user1.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForFollowButton(user1.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user1.userId))
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForUnfollowButton(user2.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForFollowButton(user2.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user2.userId))
        .assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.NO_FRIENDS).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.NO_FRIENDS_TEXT)
        .assertIsDisplayed()
        .assertTextEquals(
            "You don't have any friends yet. Look at our suggestions and discover new people!")
  }

  @Test
  fun followingAllSuggestionsShowsNoSuggestions() {
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "currentUserId",
          onProfileClick = {},
          onGoBack = {})
    }
    composeTestRule.onNodeWithTag(FriendScreenTestTags.NO_SUGGESTIONS).assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForFollowButton(user3.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.NO_SUGGESTIONS).assertIsDisplayed()
  }

  @Test
  fun clearingAllReceivedRequestsShowsNoReceivedRequests() {
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "currentUserId",
          onProfileClick = {},
          onGoBack = {})
    }
    composeTestRule.onNodeWithTag(FriendScreenTestTags.REQUESTS_TAB_BUTTON).performClick()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.NO_RECEIVED_REQUESTS).assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForDeclineReceivedRequestButton(user4.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.NO_RECEIVED_REQUESTS).assertIsDisplayed()
  }

  @Test
  fun cancellingAllSentRequestsShowsNoSentRequests() {
    runBlocking { friendRequestRepository.initializeFriendRequest("currentUserId", "user3") }
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "currentUserId",
          onProfileClick = {},
          onGoBack = {})
    }
    composeTestRule.onNodeWithTag(FriendScreenTestTags.REQUESTS_TAB_BUTTON).performClick()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.NO_SENT_REQUESTS).assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForCancelSentRequestButton(user3.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.NO_SENT_REQUESTS).assertIsDisplayed()
  }

  @Test
  fun otherUserWithNoFriendsShowsNoOtherUserFriends() {
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "user4",
          onProfileClick = {},
          onGoBack = {})
    }
    composeTestRule.onNodeWithTag(FriendScreenTestTags.NO_FRIENDS).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.NO_FRIENDS_TEXT)
        .assertIsDisplayed()
        .assertTextEquals("This user has no friends… Ask them to become your friend!")
  }

  @Test
  fun currentUserCanRemoveAndReAddFriend() {
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "currentUserId",
          onProfileClick = {},
          onGoBack = {})
    }
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForUnfollowButton(user1.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForFollowButton(user1.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
  }

  @Test
  fun currentUserCanRemoveAndReAddFriendWhenOtherUser() {
    runBlocking { userFriendsRepository.addFriendToUserFriendsOfUser("user2", "user1") }
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "user1",
          onProfileClick = {},
          onGoBack = {})
    }
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForUnfollowButton(user2.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForFollowButton(user2.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
  }

  @Test
  fun currentUserCanSendRequestToSuggestionThenCancelAndSeeSuggestionAgain() {
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "currentUserId",
          onProfileClick = {},
          onGoBack = {})
    }
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForFollowButton(user3.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.REQUESTS_TAB_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForCancelSentRequestButton(user3.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.FRIENDS_TAB_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForFollowButton(user3.userId))
        .assertIsDisplayed()
  }

  @Test
  fun currentUserCanSendRequestWhenOtherUser() {
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "user1",
          onProfileClick = {},
          onGoBack = {})
    }
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user3.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForFollowButton(user3.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForCancelSentRequestButton(user3.userId))
        .assertIsDisplayed()
  }

  @Test
  fun currentUserCanAcceptReceivedRequestAndSeeNewFriends() {
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "currentUserId",
          onProfileClick = {},
          onGoBack = {})
    }
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user4.userId))
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForUnfollowButton(user4.userId))
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user1.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user2.userId))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.REQUESTS_TAB_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForAcceptReceivedRequestButton(user4.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.FRIENDS_TAB_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user4.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForUnfollowButton(user4.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user1.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user2.userId))
        .assertIsDisplayed()
  }

  @Test
  fun currentUserCanAcceptReceivedRequestAndSeeNewFriendWhenOtherUser() {
    runBlocking { friendRequestRepository.initializeFriendRequest("user3", "currentUserId") }
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "user1",
          onProfileClick = {},
          onGoBack = {})
    }
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForUnfollowButton(user3.userId))
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForAcceptReceivedRequestButton(user3.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForUnfollowButton(user3.userId))
        .assertIsDisplayed()
  }

  @Test
  fun currentUserCanDeclineReceivedRequestAndSeeOldFriends() {
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "currentUserId",
          onProfileClick = {},
          onGoBack = {})
    }
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user4.userId))
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForUnfollowButton(user4.userId))
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user1.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user2.userId))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.REQUESTS_TAB_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForDeclineReceivedRequestButton(user4.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.FRIENDS_TAB_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user4.userId))
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForUnfollowButton(user4.userId))
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user1.userId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForTemplate(user2.userId))
        .assertIsDisplayed()
  }

  @Test
  fun currentUserCanDeclineReceivedRequestAndSeeResultWhenOtherUser() {
    runBlocking { friendRequestRepository.initializeFriendRequest("user3", "currentUserId") }
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "user1",
          onProfileClick = {},
          onGoBack = {})
    }
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForFollowButton(user3.userId))
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForDeclineReceivedRequestButton(user3.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForFollowButton(user3.userId))
        .assertIsDisplayed()
  }

  @Test
  fun currentUserCanCancelSentRequest() {
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "currentUserId",
          onProfileClick = {},
          onGoBack = {})
    }
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForFollowButton(user3.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(FriendScreenTestTags.REQUESTS_TAB_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForCancelSentRequestButton(user3.userId))
        .assertIsDisplayed()
        .performClick()
        .assertIsNotDisplayed()
  }

  @Test
  fun currentUserCanCancelSentRequestWhenOtherUser() {
    composeTestRule.setContent {
      FriendScreen(
          friendScreenViewModel = friendScreenViewModel,
          userId = "user1",
          onProfileClick = {},
          onGoBack = {})
    }
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForFollowButton(user3.userId))
        .performClick()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForCancelSentRequestButton(user3.userId))
        .performClick()
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(FriendScreenTestTags.testTagForFollowButton(user3.userId))
        .assertIsDisplayed()
  }

  @Test
  fun searchBarFocusTakesAllScreen(){}

  @Test
  fun searchBarUnFocusRestoresScreen(){}

  @Test
  fun searchBarInputDisplaysCorrectRecommendations(){}

  @Test
  fun searchBarResultsAreClickableAndWorkAsIntended(){}
}
