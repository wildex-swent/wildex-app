package com.android.wildex.model.user

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.android.wildex.datastore.UserAnimalsCacheStorage
import com.android.wildex.model.cache.user.UserAnimalsCache
import com.android.wildex.model.cache.user.UserAnimalsCacheSerializer
import com.android.wildex.model.cache.user.toProto
import com.android.wildex.utils.FirestoreTest
import com.android.wildex.utils.offline.FakeConnectivityObserver
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserAnimalsCacheTest : FirestoreTest(USER_ANIMALS_COLLECTION_PATH) {
  private lateinit var dataStore: DataStore<UserAnimalsCacheStorage>
  private lateinit var connectivityObserver: FakeConnectivityObserver
  private lateinit var cache: UserAnimalsCache
  private val testScope = TestScope(UnconfinedTestDispatcher())
  private val staleTime = System.currentTimeMillis() - (20 * 60 * 1000L)
  private val userAnimals =
      UserAnimals(userId = "user1", animalsId = listOf(animal1.animalId), animalsCount = 1)

  @Before
  override fun setUp() {
    super.setUp()
    dataStore =
        DataStoreFactory.create(serializer = UserAnimalsCacheSerializer, scope = testScope) {
          File.createTempFile("useranimalscache", ".pb")
        }
    connectivityObserver = FakeConnectivityObserver(initial = true)
    cache = UserAnimalsCache(dataStore, connectivityObserver)
  }

  @Test
  fun offlineReadsUserAnimalsFromCache() {
    runTest {
      connectivityObserver.setOnline(false)
      cache.saveUserAnimals(userAnimals)
      assertEquals(userAnimals, cache.getUserAnimals(userAnimals.userId))
    }
  }

  @Test
  fun onlineAndStaleUserAnimalsReturnsNull() {
    runTest {
      connectivityObserver.setOnline(true)
      val staleProto = userAnimals.toProto().toBuilder().setLastUpdated(staleTime).build()
      dataStore.updateData { it.toBuilder().putUserAnimals(userAnimals.userId, staleProto).build() }
      assertNull(cache.getUserAnimals(userAnimals.userId))
    }
  }

  @Test
  fun onlineAndFreshUserAnimalsReadsFromCache() {
    runTest {
      connectivityObserver.setOnline(true)
      val freshTime = System.currentTimeMillis() - (5 * 60 * 1000L)
      val freshProto = userAnimals.toProto().toBuilder().setLastUpdated(freshTime).build()
      dataStore.updateData { it.toBuilder().putUserAnimals(userAnimals.userId, freshProto).build() }
      assertEquals(userAnimals, cache.getUserAnimals(userAnimals.userId))
    }
  }

  @Test
  fun getAnimalsCountReturnsCachedValueWhenFresh() {
    runTest {
      connectivityObserver.setOnline(true)
      cache.saveUserAnimals(userAnimals)
      assertEquals(1, cache.getAnimalsCountOfUser(userAnimals.userId))
    }
  }

  @Test
  fun getAnimalsCountReturnsNullWhenStale() {
    runTest {
      connectivityObserver.setOnline(true)
      val staleProto = userAnimals.toProto().toBuilder().setLastUpdated(staleTime).build()
      dataStore.updateData { it.toBuilder().putUserAnimals(userAnimals.userId, staleProto).build() }
      assertNull(cache.getAnimalsCountOfUser(userAnimals.userId))
    }
  }

  @Test
  fun deleteUserAnimalsRemovesCachedData() {
    runTest {
      cache.saveUserAnimals(userAnimals)
      cache.deleteUserAnimals(userAnimals.userId)
      assertNull(cache.getUserAnimals(userAnimals.userId))
    }
  }

  @Test
  fun clearAllRemovesAllUserAnimals() {
    runTest {
      cache.saveUserAnimals(userAnimals)
      cache.clearAll()
      assertNull(cache.getUserAnimals(userAnimals.userId))
      assertNull(cache.getAnimalsCountOfUser(userAnimals.userId))
    }
  }
}
