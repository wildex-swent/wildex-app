package com.android.wildex.model.user

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.android.wildex.datastore.UserCacheStorage
import com.android.wildex.datastore.UserProto
import com.android.wildex.model.cache.user.UserCache
import com.android.wildex.model.cache.user.UserCacheSerializer
import com.android.wildex.model.cache.user.toProto
import com.android.wildex.model.cache.user.userDataStore
import com.android.wildex.utils.FirebaseEmulator
import com.android.wildex.utils.FirestoreTest
import com.android.wildex.utils.offline.FakeConnectivityObserver
import com.android.wildex.utils.offline.TestContext
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
class PleaseBeWorkingTest : FirestoreTest(USERS_COLLECTION_PATH) {
  private lateinit var dataStore: DataStore<UserCacheStorage>
  private lateinit var context: Context
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
      )

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      val appContext = ApplicationProvider.getApplicationContext<Context>()
      dataStore =
          DataStoreFactory.create(serializer = UserCacheSerializer, scope = testScope) {
            File.createTempFile("usercache", ".pb")
          }
      context = TestContext(dataStore, appContext)
      connectivityObserver = FakeConnectivityObserver(initial = true)
      cache = UserCache(context, connectivityObserver)
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

      context.userDataStore.updateData { it.toBuilder().putUsers(user.userId, staleProto).build() }
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

      context.userDataStore.updateData { it.toBuilder().putUsers(user.userId, freshProto).build() }

      assertEquals(user, cache.getUser(user.userId))
      assertEquals(user, userRepository.getUser(user.userId))
    }
  }
}
