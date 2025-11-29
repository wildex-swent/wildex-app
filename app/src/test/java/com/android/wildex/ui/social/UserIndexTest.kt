package com.android.wildex.ui.social

import com.android.wildex.model.social.SearchDataProvider
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserType
import com.google.firebase.Timestamp
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserIndexTest {

  private lateinit var searchDataProvider: SearchDataProvider
  private lateinit var userRepository: UserRepository
  private lateinit var userIndex: UserIndex

  private val testDispatcher = StandardTestDispatcher()

  private val users =
      listOf(
          User(
              userId = "1",
              username = "jona82",
              name = "jonathan",
              surname = "pilemand",
              bio = "",
              profilePictureURL = "",
              userType = UserType.REGULAR,
              creationDate = Timestamp.now(),
              country = ""),
          User(
              userId = "2",
              username = "muaddib",
              name = "paul",
              surname = "atreides",
              bio = "",
              profilePictureURL = "",
              userType = UserType.REGULAR,
              creationDate = Timestamp.now(),
              country = ""),
          User(
              userId = "3",
              username = "ainar",
              name = "rania",
              surname = "hida",
              bio = "",
              profilePictureURL = "",
              userType = UserType.REGULAR,
              creationDate = Timestamp.now(),
              country = ""),
          User(
              userId = "4",
              username = "youssef-9511",
              name = "youssef",
              surname = "benhayoun",
              bio = "",
              profilePictureURL = "",
              userType = UserType.REGULAR,
              creationDate = Timestamp.now(),
              country = ""))

  private val searchData =
      mapOf(
          "jonathan pilemand jona82" to "1",
          "paul atreides muaddib" to "2",
          "rania hida ainar" to "3",
          "youssef benhayoun youssef-9511" to "4")

  private val updatedFlow = MutableStateFlow(false)

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    searchDataProvider = mockk()
    userRepository = mockk()

    every { searchDataProvider.getSearchData() } returns searchData
    every { searchDataProvider.dataNeedsUpdate } returns updatedFlow
    every { searchDataProvider.invalidateCache() } just Runs

    users.forEach { user -> coEvery { userRepository.getUser(user.userId) } returns user }

    userIndex = UserIndex(searchDataProvider, userRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
  }

  @Test
  fun exactUsernameMatchReturnsCorrectUser() = runTest {
    val result = userIndex.usersMatching("jona82", 10)

    Assert.assertEquals(1, result.size)
    Assert.assertEquals("jona82", result.first().username)
  }

  @Test
  fun exactNameReturnsCorrectUser() = runTest {
    val result = userIndex.usersMatching("jonathan", 10)

    Assert.assertEquals(1, result.size)
    Assert.assertEquals("jonathan", result.first().name)
  }

  @Test
  fun exactSurnameReturnsCorrectUser() = runTest {
    val result = userIndex.usersMatching("pilemand", 10)

    Assert.assertEquals(1, result.size)
    Assert.assertEquals("pilemand", result.first().surname)
  }

  @Test
  fun partialMatchReturnsCorrectUser() = runTest {
    val result = userIndex.usersMatching("jon", 10)

    Assert.assertEquals(1, result.size)
    Assert.assertEquals("jona82", result.first().username)
  }

  @Test
  fun multiWordQueryMatchesCorrectUser() = runTest {
    val result = userIndex.usersMatching("jonathan pile", 10)

    Assert.assertEquals(1, result.size)
    Assert.assertEquals("jonathan", result.first().name)
  }

  @Test
  fun nonMatchingQueryReturnsEmptyList() = runTest {
    val result = userIndex.usersMatching("nonexistent", 10)

    Assert.assertTrue(result.isEmpty())
  }

  @Test
  fun limitRestrictsNumberOfResults() = runTest {
    val result = userIndex.usersMatching("a", 2)

    Assert.assertEquals(2, result.size)
  }

  @Test
  fun resultsAreOrderedByRelevanceScore() = runTest {
    val result = userIndex.usersMatching("na", 10)

    Assert.assertEquals(2, result.size)
    Assert.assertEquals("rania", result.first().name)
    Assert.assertEquals("jonathan", result[1].name)
  }

  @Test
  fun cacheIsInvalidatedWhenDataNeedsUpdateIsTrue() = runTest {
    updatedFlow.value = true

    userIndex.usersMatching("jon", 10)

    verify(exactly = 1) { searchDataProvider.invalidateCache() }
  }

  @Test
  fun cacheIsNotInvalidatedWhenDataNeedsUpdateIsFalse() = runTest {
    updatedFlow.value = false

    userIndex.usersMatching("jon", 10)

    verify(exactly = 0) { searchDataProvider.invalidateCache() }
  }
}
