package com.android.wildex.model.user

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.android.wildex.datastore.UserCacheStorage
import com.android.wildex.datastore.UserProto
import com.android.wildex.model.cache.user.UserCache
import com.android.wildex.model.cache.user.UserCacheSerializer
import com.android.wildex.model.cache.user.toProto
import com.android.wildex.utils.FirebaseEmulator
import com.android.wildex.utils.FirestoreTest
import com.android.wildex.utils.offline.FakeConnectivityObserver
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserCacheTest : FirestoreTest(USERS_COLLECTION_PATH) {
  private lateinit var dataStore: DataStore<UserCacheStorage>
  private lateinit var connectivityObserver: FakeConnectivityObserver
  private lateinit var cache: UserCache
  private lateinit var db: FirebaseFirestore
  private lateinit var userRepository: UserRepositoryFirestore
  private val testScope = TestScope(UnconfinedTestDispatcher())

  private val user =
      User(
          userId = "abc",
          username = "zeze",
          name = "zeyneb",
          surname = "something",
          bio = "empty",
          profilePictureURL = "",
          userType = UserType.REGULAR,
          creationDate = Timestamp(Date(0)),
          country = "CH",
          onBoardingStage = OnBoardingStage.COMPLETE)

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      dataStore =
          DataStoreFactory.create(serializer = UserCacheSerializer, scope = testScope) {
            File.createTempFile("usercache", ".pb")
          }
      connectivityObserver = FakeConnectivityObserver(initial = true)
      cache = UserCache(dataStore, connectivityObserver)
      db = FirebaseEmulator.firestore
      userRepository = UserRepositoryFirestore(db, cache)
    }
  }

  @Test
  fun offlineReadsFromCache() {
    runTest {
      connectivityObserver.setOnline(false)
      userRepository.addUser(user)
      db.collection(USERS_COLLECTION_PATH)
          .document(user.userId)
          .update("username", "tampered")
          .await()
      val result = userRepository.getUser(user.userId)
      assertEquals(user.userId, result.userId)
      assertEquals(user, result)
    }
  }

  @Test
  fun onlineAndStaleFetchesFromFirestore() {
    runTest {
      connectivityObserver.setOnline(true)

      userRepository.addUser(user)

      val staleTime = System.currentTimeMillis() - (20 * 60 * 1000L)
      val staleProto =
          UserProto.newBuilder()
              .setUserId("abc")
              .setUserName("cached_user")
              .setName("old")
              .setSurname("Cache")
              .setLastUpdated(staleTime)
              .build()

      dataStore.updateData { it.toBuilder().putUsers(user.userId, staleProto).build() }
      assertNull(cache.getUser(user.userId))

      val result = userRepository.getUser(user.userId)
      assertEquals(user, result)
    }
  }

  @Test
  fun onlineButFreshReadsFromCache() {
    runTest {
      connectivityObserver.setOnline(true)

      val freshTime = System.currentTimeMillis() - (5 * 60 * 1000L)
      val freshProto = user.toProto().toBuilder().setLastUpdated(freshTime).build()

      dataStore.updateData { it.toBuilder().putUsers(user.userId, freshProto).build() }

      assertEquals(user, cache.getUser(user.userId))
      assertEquals(user, userRepository.getUser(user.userId))
    }
  }
}
