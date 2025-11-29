package com.android.wildex.model.social

import androidx.test.platform.app.InstrumentationRegistry
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.android.wildex.utils.LocalRepositories
import com.google.firebase.Timestamp
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SearchDataUpdaterTest {

  private val mockUser1 =
      User(
          userId = "jonaUserId",
          username = "jona82",
          name = "jonathan",
          surname = "pilemand",
          bio = "",
          profilePictureURL = "",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "")

  private val mockUser2 =
      User(
          userId = "paulUserId",
          username = "muaddib",
          name = "paul",
          surname = "atreides",
          bio = "",
          profilePictureURL = "",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "")

  private val mockUser3 =
      User(
          userId = "raniaUserId",
          username = "ainar",
          name = "rania",
          surname = "hida",
          bio = "",
          profilePictureURL = "",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "")

  private val mockUser4 =
      User(
          userId = "youssefUserId",
          username = "youssef-9511",
          name = "youssef",
          surname = "benhayoun",
          bio = "",
          profilePictureURL = "",
          userType = UserType.REGULAR,
          creationDate = Timestamp.now(),
          country = "")

  private val userRepository = LocalRepositories.userRepository

  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  private val fileSearchDataStorage = FileSearchDataStorage(context)

  private val searchDataUpdater = SearchDataUpdater(userRepository, fileSearchDataStorage)

  private val file = File(context.cacheDir, "search_data.json")

  @Before
  fun setup() {
    runBlocking {
      userRepository.addUser(mockUser1)
      userRepository.addUser(mockUser2)
      userRepository.addUser(mockUser3)
      userRepository.addUser(mockUser4)
    }
  }

  @After
  fun tearDown() {
    LocalRepositories.clearAll()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun updatingWritesCorrectData() {
    runBlocking { searchDataUpdater.updateSearchData() }
    val mockMapJson =
        JSONObject()
            .put("jonathan pilemand jona82", "jonaUserId")
            .put("paul atreides muaddib", "paulUserId")
            .put("rania hida ainar", "raniaUserId")
            .put("youssef benhayoun youssef-9511", "youssefUserId")
    Assert.assertEquals(mockMapJson.toString(), file.readText())
    Assert.assertTrue(fileSearchDataStorage.updated.value)
  }
}
