package com.android.wildex.model.animal

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.android.wildex.datastore.AnimalCacheStorage
import com.android.wildex.model.cache.animal.AnimalCache
import com.android.wildex.model.cache.animal.AnimalCacheSerializer
import com.android.wildex.model.cache.animal.toProto
import com.android.wildex.utils.FirestoreTest
import com.android.wildex.utils.offline.FakeConnectivityObserver
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnimalCacheTest : FirestoreTest(ANIMAL_COLLECTION_PATH) {
  private lateinit var dataStore: DataStore<AnimalCacheStorage>
  private lateinit var connectivityObserver: FakeConnectivityObserver
  private lateinit var cache: AnimalCache
  private val testScope = TestScope(UnconfinedTestDispatcher())
  private val staleTime = System.currentTimeMillis() - (20 * 60 * 1000L)

  @Before
  override fun setUp() {
    super.setUp()
    dataStore =
        DataStoreFactory.create(serializer = AnimalCacheSerializer, scope = testScope) {
          File.createTempFile("animalcache", ".pb")
        }
    connectivityObserver = FakeConnectivityObserver(initial = true)
    cache = AnimalCache(dataStore, connectivityObserver)
  }

  @Test
  fun offlineReadsAnimalFromCache() {
    runTest {
      connectivityObserver.setOnline(false)
      cache.saveAnimal(animal1)
      assertEquals(animal1, cache.getAnimal(animal1.animalId))
    }
  }

  @Test
  fun onlineAndStaleAnimalReturnsNull() {
    runTest {
      connectivityObserver.setOnline(true)
      val staleProto = animal1.toProto().toBuilder().setLastUpdated(staleTime).build()
      dataStore.updateData { it.toBuilder().putAnimals(animal1.animalId, staleProto).build() }

      assertNull(cache.getAnimal(animal1.animalId))
    }
  }

  @Test
  fun onlineAndFreshAnimalReadsFromCache() {
    runTest {
      connectivityObserver.setOnline(true)
      val freshTime = System.currentTimeMillis() - (5 * 10 * 1000L)
      val freshProto = animal1.toProto().toBuilder().setLastUpdated(freshTime).build()
      dataStore.updateData { it.toBuilder().putAnimals(animal1.animalId, freshProto).build() }

      assertEquals(animal1, cache.getAnimal(animal1.animalId))
    }
  }

  @Test
  fun offlineAndStaleAnimalsReturnEmptyList() {
    runTest {
      cache.clearAll()
      connectivityObserver.setOnline(false)
      assertEquals(emptyList<Animal>(), cache.getAllAnimals())
    }
  }

  @Test
  fun onlineAndStaleAnimalReturnNull() {
    runTest {
      connectivityObserver.setOnline(true)
      val staleProto = animal1.toProto().toBuilder().setLastUpdated(staleTime).build()
      dataStore.updateData { it.toBuilder().putAnimals(animal1.animalId, staleProto).build() }
      assertNull(cache.getAllAnimals())
    }
  }

  @Test
  fun onlineAndFreshAnimalsReturnsList() {
    runTest {
      connectivityObserver.setOnline(true)
      cache.saveAnimals(listOf(animal1, animal2))
      val result = cache.getAllAnimals()
      assertEquals(2, result?.size)
      assertTrue(result!!.containsAll(listOf(animal1, animal2)))
    }
  }

  @Test
  fun clearAllRemovesAllAnimals() {
    runTest {
      cache.saveAnimals(listOf(animal1, animal2))
      cache.clearAll()
      assertNull(cache.getAllAnimals())
    }
  }
}
