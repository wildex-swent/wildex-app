package com.android.wildex.ui.social

import com.android.wildex.model.social.Post
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Location
import com.android.wildex.utils.LocalRepositories
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

class UserRecommenderTest {

  private val user = User(
    userId = "",
    username = "",
    name = "john",
    surname = "cena",
    bio = "",
    profilePictureURL = "",
    userType = UserType.REGULAR,
    creationDate = Timestamp.now(),
    country = "",
    friendsCount = 0
  )

  private val post = Post(
    postId = "",
    authorId = "",
    pictureURL = "",
    location = Location(0.0, 0.0, ""),
    description = "",
    date = Timestamp.now(),
    animalId = "",
    likesCount = 0,
    commentsCount = 0
  )

  private val userRepository = LocalRepositories.userRepository
  private val userFriendsRepository = LocalRepositories.userFriendsRepository
  private val postsRepository = LocalRepositories.postsRepository
  private val friendRequestRepository = LocalRepositories.relationshipRepository
  private val userRecommender = UserRecommender(
    currentUserId = "user0",
    userRepository = userRepository,
    postRepository = postsRepository,
    userFriendsRepository = userFriendsRepository,
    friendRequestRepository = friendRequestRepository
  )

  @Before
  fun setup() {}

  @After
  fun tearDown() {
    LocalRepositories.clearAll()
  }

  @Test
  fun mutualFriendsScoreIsComputedCorrectly() {
    val user0 = user.copy(userId = "user0", username = "user0", country = "Listembourg")
    val user1 = user.copy(userId = "user1", username = "user1", country = "Listembourg")
    val user2 = user.copy(userId = "user2", username = "user2", country = "Listembourg")
    val user3 = user.copy(userId = "user3", username = "user3", country = "Listembourg")
    val user4 = user.copy(userId = "user4", username = "user4", country = "Listembourg")
    runBlocking {
      userRepository.addUser(user0)
      userRepository.addUser(user1)
      userRepository.addUser(user2)
      userRepository.addUser(user3)
      userRepository.addUser(user4)

      userFriendsRepository.initializeUserFriends("user0")
      userFriendsRepository.addFriendToUserFriendsOfUser("user1", "user0")
      userFriendsRepository.addFriendToUserFriendsOfUser("user2", "user0")
      userFriendsRepository.initializeUserFriends("user1")
      userFriendsRepository.addFriendToUserFriendsOfUser("user0", "user1")
      userFriendsRepository.addFriendToUserFriendsOfUser("user3", "user1")
      userFriendsRepository.initializeUserFriends("user2")
      userFriendsRepository.addFriendToUserFriendsOfUser("user0", "user2")
      userFriendsRepository.addFriendToUserFriendsOfUser("user3", "user2")
      userFriendsRepository.addFriendToUserFriendsOfUser("user4", "user2")
      userFriendsRepository.initializeUserFriends("user3")
      userFriendsRepository.addFriendToUserFriendsOfUser("user1", "user3")
      userFriendsRepository.addFriendToUserFriendsOfUser("user2", "user3")
      userFriendsRepository.initializeUserFriends("user4")
      userFriendsRepository.addFriendToUserFriendsOfUser("user2", "user4")

      val expectedResults = listOf(
        RecommendationResult(
          user = SimpleUser("user3", "user3", ""),
          reason = "shares 2 common friends with you"
        ),
        RecommendationResult(
          user = SimpleUser("user4", "user4", ""),
          reason = "shares 1 common friend with you"
        )
      )
      val recommendedUsers = userRecommender.getRecommendedUsers()
      Assert.assertEquals(expectedResults.size, recommendedUsers.size)
      Assert.assertEquals(expectedResults, recommendedUsers)
    }
  }

  @Test
  fun popularityScoreIsComputedCorrectly() {
    val user0 = user.copy(userId = "user0", username = "user0", country = "Listembourg")
    val user1 = user.copy(userId = "user1", username = "user1", country = "Listembourg")
    val user2 = user.copy(userId = "user2", username = "user2", country = "Listembourg")
    val user3 = user.copy(userId = "user3", username = "user3", country = "Listembourg")
    val user4 = user.copy(userId = "user4", username = "user4", country = "Listembourg")
    runBlocking {
      userRepository.addUser(user0)
      userRepository.addUser(user1)
      userRepository.addUser(user2)
      userRepository.addUser(user3)
      userRepository.addUser(user4)

      userFriendsRepository.initializeUserFriends("user0")
      userFriendsRepository.initializeUserFriends("user1")
      userFriendsRepository.addFriendToUserFriendsOfUser("user2", "user1")
      userFriendsRepository.addFriendToUserFriendsOfUser("user3", "user1")
      userFriendsRepository.addFriendToUserFriendsOfUser("user4", "user1")
      userFriendsRepository.initializeUserFriends("user2")
      userFriendsRepository.addFriendToUserFriendsOfUser("user1", "user2")
      userFriendsRepository.initializeUserFriends("user3")
      userFriendsRepository.addFriendToUserFriendsOfUser("user1", "user3")
      userFriendsRepository.initializeUserFriends("user4")
      userFriendsRepository.addFriendToUserFriendsOfUser("user1", "user4")

      val expectedResults = listOf(
        RecommendationResult(
          user = SimpleUser("user1", "user1", ""),
          reason = "is popular in Listembourg"
        ),
        RecommendationResult(
          user = SimpleUser("user2", "user2", ""),
          reason = "is popular in Listembourg"
        ),
        RecommendationResult(
          user = SimpleUser("user3", "user3", ""),
          reason = "is popular in Listembourg"
        ),
        RecommendationResult(
          user = SimpleUser("user4", "user4", ""),
          reason = "is popular in Listembourg"
        )
      )
      val recommendedUsers = userRecommender.getRecommendedUsers()
      Assert.assertEquals(expectedResults.size, recommendedUsers.size)
      Assert.assertEquals(expectedResults, recommendedUsers)
    }
  }

  @Test
  fun geoActivityScoreIsComputedCorrectly() {
    //Current user
    val user0 = user.copy(userId = "user0", username = "user0", country = "Listembourg")
    //Close by and active user
    val user1 = user.copy(userId = "user1", username = "user1", country = "Listembourg")
    //Less close by and less active user
    val user2 = user.copy(userId = "user2", username = "user2", country = "Listembourg")
    //Close by but not active user
    val user3 = user.copy(userId = "user3", username = "user3", country = "Farawayland")
    //Not close by and not active user
    val user4 = user.copy(userId = "user4", username = "user4", country = "Farawayland")
    //Very close by but not recently active user
    val user5 = user.copy(userId = "user5", username = "user5", country = "Listembourg")
    //Very active but not close by user
    val user6 = user.copy(userId = "user6", username = "user6", country = "Farawayland")
    runBlocking {
      userRepository.addUser(user0)
      userRepository.addUser(user1)
      userRepository.addUser(user2)
      userRepository.addUser(user3)
      userRepository.addUser(user4)
      userRepository.addUser(user5)
      userRepository.addUser(user6)
      userFriendsRepository.initializeUserFriends("user0")
      userFriendsRepository.initializeUserFriends("user1")
      userFriendsRepository.initializeUserFriends("user2")
      userFriendsRepository.initializeUserFriends("user3")
      userFriendsRepository.initializeUserFriends("user4")
      userFriendsRepository.initializeUserFriends("user5")
      userFriendsRepository.initializeUserFriends("user6")

      val post0 = post.copy(
        postId = "post0",
        authorId = "user0",
        location = Location(0.0, 0.0, "Listembourg"),
        date = Timestamp.now()
      )
      val post1 = post.copy(
        postId = "post1",
        authorId = "user1",
        location = Location(0.0, 0.0, "Listembourg"),
        date = Timestamp.now()
      )
      val post2 = post.copy(
        postId = "post2",
        authorId = "user1",
        location = Location(0.002, 0.0, "Listembourg"),
        date = Timestamp.now()
      )
      val post3 = post.copy(
        postId = "post3",
        authorId = "user1",
        location = Location(0.0, 0.002, "Farawayland"),
        date = Timestamp.now()
      )
      val post4 = post.copy(
        postId = "post4",
        authorId = "user1",
        location = Location(0.002, 0.002, "Farawayland"),
        date = Timestamp.now()
      )
      val post5 = post.copy(
        postId = "post5",
        authorId = "user1",
        location = Location(0.001, 0.001, "Farawayland"),
        date = Timestamp.now()
      )
      val post6 = post.copy(
        postId = "post6",
        authorId = "user2",
        location = Location(0.01, 0.01, "Farawayland"),
        date = Timestamp.now()
      )
      val post7 = post.copy(
        postId = "post7",
        authorId = "user2",
        location = Location(0.02, 0.02, "Farawayland"),
        date = Timestamp.now()
      )
      val post8 = post.copy(
        postId = "post8",
        authorId = "user2",
        location = Location(0.03, 0.03, "Farawayland"),
        date = Timestamp.now()
      )
      val post9 = post.copy(
        postId = "post9",
        authorId = "user3",
        location = Location(1.0, 1.0, "Farawayland"),
        date = Timestamp.now()
      )
      val post10 = post.copy(
        postId = "post10",
        authorId = "user4",
        location = Location(10.0, 10.0, "Farawayland"),
        date = Timestamp.now()
      )
      val oneYearAgo = ZonedDateTime.now(ZoneId.systemDefault()).minusYears(1)
      val timestampOneYearAgo = Timestamp(Date.from(oneYearAgo.toInstant()))
      val post11 = post.copy(
        postId = "post11",
        authorId = "user5",
        location = Location(0.0001, 0.0001, "Listembourg"),
        date = timestampOneYearAgo
      )
      val post12 = post.copy(
        postId = "post12",
        authorId = "user5",
        location = Location(0.0001, 0.0001, "Listembourg"),
        date = timestampOneYearAgo
      )
      val post13 = post.copy(
        postId = "post13",
        authorId = "user5",
        location = Location(0.0001, 0.0001, "Listembourg"),
        date = timestampOneYearAgo
      )
      val post14 = post.copy(
        postId = "post14",
        authorId = "user5",
        location = Location(0.0001, 0.0001, "Listembourg"),
        date = timestampOneYearAgo
      )
      val post15 = post.copy(
        postId = "post15",
        authorId = "user5",
        location = Location(0.0, 0.0, "Listembourg"),
        date = timestampOneYearAgo
      )
      postsRepository.addPost(post0)
      postsRepository.addPost(post1)
      postsRepository.addPost(post2)
      postsRepository.addPost(post3)
      postsRepository.addPost(post4)
      postsRepository.addPost(post5)
      postsRepository.addPost(post6)
      postsRepository.addPost(post7)
      postsRepository.addPost(post8)
      postsRepository.addPost(post9)
      postsRepository.addPost(post10)
      postsRepository.addPost(post11)
      postsRepository.addPost(post12)
      postsRepository.addPost(post13)
      postsRepository.addPost(post14)
      postsRepository.addPost(post15)
      for (i in 16..25){
        val postN = post.copy(
          postId = "post$i",
          authorId = "user6",
          location = Location(90.0, 90.0, "Farawayland"),
          date = Timestamp.now()
        )
        postsRepository.addPost(postN)
      }

      val expectedResults = listOf(
        RecommendationResult(
          user = SimpleUser("user1", "user1", ""),
          reason = "recently posted 5 times near you"
        ),
        RecommendationResult(
          user = SimpleUser("user2", "user2", ""),
          reason = "recently posted 3 times near you"
        ),
        RecommendationResult(
          user = SimpleUser("user3", "user3", ""),
          reason = "recently posted 1 time near you"
        ),
        RecommendationResult(
          user = SimpleUser("user4", "user4", ""),
          reason = "recently posted 1 time near you"
        )
      )
      val recommendedUsers = userRecommender.getRecommendedUsers()
      Assert.assertEquals(expectedResults.size, recommendedUsers.size)
      Assert.assertEquals(expectedResults, recommendedUsers)
    }
  }

  @Test
  fun alreadyFriendsAreExcluded() {
    val user0 = user.copy(userId = "user0", username = "user0", country = "Listembourg")
    val user1 = user.copy(userId = "user1", username = "user1", country = "Listembourg")
    val user2 = user.copy(userId = "user2", username = "user2", country = "Listembourg")
    val user3 = user.copy(userId = "user3", username = "user3", country = "Listembourg")
    val user4 = user.copy(userId = "user4", username = "user4", country = "Listembourg")
    runBlocking {
      userRepository.addUser(user0)
      userRepository.addUser(user1)
      userRepository.addUser(user2)
      userRepository.addUser(user3)
      userRepository.addUser(user4)

      userFriendsRepository.initializeUserFriends("user0")
      userFriendsRepository.addFriendToUserFriendsOfUser("user1", "user0")
      userFriendsRepository.addFriendToUserFriendsOfUser("user2", "user0")
      userFriendsRepository.initializeUserFriends("user1")
      userFriendsRepository.addFriendToUserFriendsOfUser("user0", "user1")
      userFriendsRepository.initializeUserFriends("user2")
      userFriendsRepository.addFriendToUserFriendsOfUser("user0", "user2")
      userFriendsRepository.initializeUserFriends("user3")
      userFriendsRepository.addFriendToUserFriendsOfUser("user4", "user3")
      userFriendsRepository.initializeUserFriends("user4")
      userFriendsRepository.addFriendToUserFriendsOfUser("user3", "user4")

      val post1 = post.copy(
        postId = "post1",
        authorId = "user1",
        location = Location(0.0, 0.0, "Listembourg"),
        date = Timestamp.now()
      )
      val post2 = post.copy(
        postId = "post2",
        authorId = "user1",
        location = Location(0.0, 0.0, "Listembourg"),
        date = Timestamp.now()
      )
      val post3 = post.copy(
        postId = "post3",
        authorId = "user2",
        location = Location(0.0, 0.0, "Listembourg"),
        date = Timestamp.now()
      )
      val post4 = post.copy(
        postId = "post4",
        authorId = "user2",
        location = Location(0.0, 0.0, "Listembourg"),
        date = Timestamp.now()
      )
      postsRepository.addPost(post1)
      postsRepository.addPost(post2)
      postsRepository.addPost(post3)
      postsRepository.addPost(post4)

      val expectedResults = listOf(
        RecommendationResult(
          user = SimpleUser("user3", "user3", ""),
          reason = "is popular in Listembourg"
        ),
        RecommendationResult(
          user = SimpleUser("user4", "user4", ""),
          reason = "is popular in Listembourg"
        )
      )
      val recommendedUsers = userRecommender.getRecommendedUsers()
      Assert.assertEquals(expectedResults.size, recommendedUsers.size)
      Assert.assertEquals(expectedResults, recommendedUsers)
    }
  }

  @Test
  fun sentPendingRequestsUsersAreExcluded(){
    val user0 = user.copy(userId = "user0", username = "user0", country = "Listembourg")
    val user1 = user.copy(userId = "user1", username = "user1", country = "Listembourg")
    val user2 = user.copy(userId = "user2", username = "user2", country = "Listembourg")
    val user3 = user.copy(userId = "user3", username = "user3", country = "Listembourg")

    runBlocking {
      userRepository.addUser(user0)
      userRepository.addUser(user1)
      userRepository.addUser(user2)
      userRepository.addUser(user3)

      userFriendsRepository.initializeUserFriends("user0")
      userFriendsRepository.addFriendToUserFriendsOfUser("user1", "user0")
      userFriendsRepository.addFriendToUserFriendsOfUser("user2", "user0")
      userFriendsRepository.initializeUserFriends("user1")
      userFriendsRepository.addFriendToUserFriendsOfUser("user0", "user1")
      userFriendsRepository.addFriendToUserFriendsOfUser("user3", "user1")
      userFriendsRepository.initializeUserFriends("user2")
      userFriendsRepository.addFriendToUserFriendsOfUser("user0", "user2")
      userFriendsRepository.addFriendToUserFriendsOfUser("user3", "user2")
      userFriendsRepository.initializeUserFriends("user3")
      userFriendsRepository.addFriendToUserFriendsOfUser("user1", "user3")
      userFriendsRepository.addFriendToUserFriendsOfUser("user2", "user3")

      friendRequestRepository.initializeRelationship(
        senderId = "user0",
        receiverId = "user3",
      )

      val expectedResults = emptyList<RecommendationResult>()
      val recommendedUsers = userRecommender.getRecommendedUsers()
      Assert.assertEquals(0, recommendedUsers.size)
      Assert.assertEquals(expectedResults, recommendedUsers)
    }
  }

  @Test
  fun receivedPendingRequestsUsersAreExcluded(){
    val user0 = user.copy(userId = "user0", username = "user0", country = "Listembourg")
    val user1 = user.copy(userId = "user1", username = "user1", country = "Listembourg")
    val user2 = user.copy(userId = "user2", username = "user2", country = "Listembourg")
    val user3 = user.copy(userId = "user3", username = "user3", country = "Listembourg")

    runBlocking {
      userRepository.addUser(user0)
      userRepository.addUser(user1)
      userRepository.addUser(user2)
      userRepository.addUser(user3)

      userFriendsRepository.initializeUserFriends("user0")
      userFriendsRepository.addFriendToUserFriendsOfUser("user1", "user0")
      userFriendsRepository.addFriendToUserFriendsOfUser("user2", "user0")
      userFriendsRepository.initializeUserFriends("user1")
      userFriendsRepository.addFriendToUserFriendsOfUser("user0", "user1")
      userFriendsRepository.addFriendToUserFriendsOfUser("user3", "user1")
      userFriendsRepository.initializeUserFriends("user2")
      userFriendsRepository.addFriendToUserFriendsOfUser("user0", "user2")
      userFriendsRepository.addFriendToUserFriendsOfUser("user3", "user2")
      userFriendsRepository.initializeUserFriends("user3")
      userFriendsRepository.addFriendToUserFriendsOfUser("user1", "user3")
      userFriendsRepository.addFriendToUserFriendsOfUser("user2", "user3")

      friendRequestRepository.initializeRelationship(
        senderId = "user3",
        receiverId = "user0",
      )

      val expectedResults = emptyList<RecommendationResult>()
      val recommendedUsers = userRecommender.getRecommendedUsers()
      Assert.assertEquals(0, recommendedUsers.size)
      Assert.assertEquals(expectedResults, recommendedUsers)
    }
  }

  @Test
  fun suggestionReasonIsAdapted(){
    val user0 = user.copy(userId = "user0", username = "user0", country = "Listembourg")
    val user1 = user.copy(userId = "user1", username = "user1", country = "Listembourg")
    val user2 = user.copy(userId = "user2", username = "user2", country = "Listembourg")
    val user3 = user.copy(userId = "user3", username = "user3", country = "Listembourg")

    runBlocking {
      userRepository.addUser(user0)
      userRepository.addUser(user1)
      userRepository.addUser(user2)
      userRepository.addUser(user3)
      for (i in 4..100){
        val userN = user.copy(userId = "user$i", username = "user$i", country = "Listembourg")
        userRepository.addUser(userN)
        userFriendsRepository.initializeUserFriends("user$i")
      }

      userFriendsRepository.initializeUserFriends("user0")
      userFriendsRepository.addFriendToUserFriendsOfUser("user4", "user0")
      userFriendsRepository.addFriendToUserFriendsOfUser("user0", "user4")
      userFriendsRepository.addFriendToUserFriendsOfUser("user5", "user0")
      userFriendsRepository.addFriendToUserFriendsOfUser("user0", "user5")
      userFriendsRepository.addFriendToUserFriendsOfUser("user6", "user0")
      userFriendsRepository.addFriendToUserFriendsOfUser("user0", "user6")
      userFriendsRepository.addFriendToUserFriendsOfUser("user7", "user0")
      userFriendsRepository.addFriendToUserFriendsOfUser("user0", "user7")
      userFriendsRepository.addFriendToUserFriendsOfUser("user8", "user0")
      userFriendsRepository.addFriendToUserFriendsOfUser("user0", "user8")
      userFriendsRepository.initializeUserFriends("user1")
      userFriendsRepository.addFriendToUserFriendsOfUser("user4", "user1")
      userFriendsRepository.addFriendToUserFriendsOfUser("user1", "user4")
      userFriendsRepository.addFriendToUserFriendsOfUser("user5", "user1")
      userFriendsRepository.addFriendToUserFriendsOfUser("user1", "user5")
      userFriendsRepository.addFriendToUserFriendsOfUser("user6", "user1")
      userFriendsRepository.addFriendToUserFriendsOfUser("user1", "user6")
      userFriendsRepository.addFriendToUserFriendsOfUser("user7", "user1")
      userFriendsRepository.addFriendToUserFriendsOfUser("user1", "user7")
      userFriendsRepository.addFriendToUserFriendsOfUser("user8", "user1")
      userFriendsRepository.addFriendToUserFriendsOfUser("user1", "user8")
      userFriendsRepository.initializeUserFriends("user2")
      for (i in 9..100){
        userFriendsRepository.addFriendToUserFriendsOfUser("user$i", "user2")
      }
      userFriendsRepository.initializeUserFriends("user3")

      val post1 = post.copy(
        postId = "post1",
        authorId = "user3",
        location = Location(0.0, 0.0, "Listembourg"),
        date = Timestamp.now()
      )
      val post2 = post.copy(
        postId = "post2",
        authorId = "user3",
        location = Location(0.0, 0.0, "Listembourg"),
        date = Timestamp.now()
      )
      val post3 = post.copy(
        postId = "post3",
        authorId = "user3",
        location = Location(0.0, 0.0, "Listembourg"),
        date = Timestamp.now()
      )
      val post4 = post.copy(
        postId = "post4",
        authorId = "user3",
        location = Location(0.0, 0.0, "Listembourg"),
        date = Timestamp.now()
      )
      val post5 = post.copy(
        postId = "post5",
        authorId = "user3",
        location = Location(0.0, 0.0, "Listembourg"),
        date = Timestamp.now()
      )
      val post6 = post.copy(
        postId = "post6",
        authorId = "user3",
        location = Location(0.0, 0.0, "Listembourg"),
        date = Timestamp.now()
      )
      postsRepository.addPost(post1)
      postsRepository.addPost(post2)
      postsRepository.addPost(post3)
      postsRepository.addPost(post4)
      postsRepository.addPost(post5)
      postsRepository.addPost(post6)

      val expectedResults = listOf(
        RecommendationResult(
          user = SimpleUser("user1", "user1", ""),
          reason = "shares 5 common friends with you"
        ),
        RecommendationResult(
          user = SimpleUser("user3", "user3", ""),
          reason = "recently posted 6 times near you"
        ),
        RecommendationResult(
          user = SimpleUser("user2", "user2", ""),
          reason = "is popular in Listembourg"
        )
      )
      val recommendedUsers = userRecommender.getRecommendedUsers()
      Assert.assertEquals(expectedResults.size, recommendedUsers.size)
      Assert.assertEquals(expectedResults, recommendedUsers)
    }
  }

  @Test
  fun noRecentPostsFromAnyUserDoesNotCauseDivisionErrors(){
    val user0 = user.copy(userId = "user0", username = "user0", country = "Listembourg")
    val user1 = user.copy(userId = "user1", username = "user1", country = "Listembourg")
    val user2 = user.copy(userId = "user2", username = "user2", country = "Listembourg")
    val user3 = user.copy(userId = "user3", username = "user3", country = "Listembourg")

    runBlocking {
      userRepository.addUser(user0)
      userRepository.addUser(user1)
      userRepository.addUser(user2)
      userRepository.addUser(user3)

      userFriendsRepository.initializeUserFriends("user0")
      userFriendsRepository.initializeUserFriends("user1")
      userFriendsRepository.initializeUserFriends("user2")
      userFriendsRepository.initializeUserFriends("user3")

      val oneYearAgo = ZonedDateTime.now(ZoneId.systemDefault()).minusYears(1)
      val timestampOneYearAgo = Timestamp(Date.from(oneYearAgo.toInstant()))
      val post1 = post.copy(
        postId = "post1",
        authorId = "user1",
        location = Location(0.0, 0.0, "Listembourg"),
        date = timestampOneYearAgo
      )
      val post2 = post.copy(
        postId = "post2",
        authorId = "user2",
        location = Location(0.0, 0.0, "Listembourg"),
        date = timestampOneYearAgo
      )
      val post3 = post.copy(
        postId = "post3",
        authorId = "user3",
        location = Location(0.0, 0.0, "Listembourg"),
        date = timestampOneYearAgo
      )
      val post4 = post.copy(
        postId = "post4",
        authorId = "user2",
        location = Location(0.0, 0.0, "Listembourg"),
        date = timestampOneYearAgo
      )
      postsRepository.addPost(post1)
      postsRepository.addPost(post2)
      postsRepository.addPost(post3)
      postsRepository.addPost(post4)

      val expectedResults = emptyList<RecommendationResult>()
      val recommendedUsers = userRecommender.getRecommendedUsers()
      Assert.assertEquals(0, recommendedUsers.size)
      Assert.assertEquals(expectedResults, recommendedUsers)
    }
  }

  @Test
  fun zeroMinimumDistanceDoesNotBreakAlgorithm() {
    val user0 = user.copy(userId = "user0", username = "user0", country = "Listembourg")
    val user1 = user.copy(userId = "user1", username = "user1", country = "Listembourg")
    val user2 = user.copy(userId = "user2", username = "user2", country = "Listembourg")
    val user3 = user.copy(userId = "user3", username = "user3", country = "Listembourg")

    runBlocking {
      userRepository.addUser(user0)
      userRepository.addUser(user1)
      userRepository.addUser(user2)
      userRepository.addUser(user3)

      userFriendsRepository.initializeUserFriends("user0")
      userFriendsRepository.initializeUserFriends("user1")
      userFriendsRepository.initializeUserFriends("user2")
      userFriendsRepository.initializeUserFriends("user3")


      val post1 = post.copy(
        postId = "post1",
        authorId = "user1",
        location = Location(0.0, 0.0, "Listembourg"),
        date = Timestamp.now()
      )
      val post2 = post.copy(
        postId = "post2",
        authorId = "user2",
        location = Location(0.1, 0.1, "Listembourg"),
        date = Timestamp.now()
      )
      val post3 = post.copy(
        postId = "post3",
        authorId = "user3",
        location = Location(0.2, 0.2, "Listembourg"),
        date = Timestamp.now()
      )
      postsRepository.addPost(post1)
      postsRepository.addPost(post2)
      postsRepository.addPost(post3)

      val expectedResults = listOf(
        RecommendationResult(
          user = SimpleUser(userId = "user1", username = "user1", profilePictureURL = ""),
          reason = "recently posted 1 time near you"
        ),
        RecommendationResult(
          user = SimpleUser(userId = "user2", username = "user2", profilePictureURL = ""),
          reason = "recently posted 1 time near you"
        )
      )
      val recommendedUsers = userRecommender.getRecommendedUsers()
      Assert.assertEquals(expectedResults.size, recommendedUsers.size)
      Assert.assertEquals(expectedResults, recommendedUsers)
    }
  }
}