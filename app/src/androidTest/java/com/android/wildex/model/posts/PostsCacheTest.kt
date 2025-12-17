package com.android.wildex.model.posts

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.android.wildex.datastore.PostCacheStorage
import com.android.wildex.datastore.PostProto
import com.android.wildex.model.cache.posts.PostCacheSerializer
import com.android.wildex.model.cache.posts.PostsCache
import com.android.wildex.model.cache.posts.toProto
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepositoryFirestore
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
class PostCacheTest : FirestoreTest(POSTS_COLLECTION_PATH) {
  private lateinit var dataStore: DataStore<PostCacheStorage>
  private lateinit var connectivityObserver: FakeConnectivityObserver
  private lateinit var cache: PostsCache
  private lateinit var db: FirebaseFirestore
  private lateinit var postRepository: PostsRepositoryFirestore
  private val testScope = TestScope(UnconfinedTestDispatcher())
  private val staleTime = System.currentTimeMillis() - (20 * 60 * 1000L)

  private val post =
      Post(
          postId = "abc",
          authorId = "author_123",
          pictureURL = "http://example.com/pic.jpg",
          location = null,
          description = "A lovely post",
          date = Timestamp(Date(0)),
          animalId = "animal_456",
      )
  private val postA1 =
      Post(
          postId = "p1",
          authorId = "authorA",
          pictureURL = "url1",
          location = null,
          description = "d1",
          date = Timestamp(Date(0)),
          animalId = "animal1",
      )

  private val postA2 = postA1.copy(postId = "p2", description = "d2")
  private val postB1 = postA1.copy(postId = "p3", authorId = "authorB")

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      dataStore =
          DataStoreFactory.create(serializer = PostCacheSerializer, scope = testScope) {
            File.createTempFile("postscache", ".pb")
          }
      connectivityObserver = FakeConnectivityObserver(initial = true)
      cache = PostsCache(dataStore, connectivityObserver)
      db = FirebaseEmulator.firestore
      postRepository = PostsRepositoryFirestore(db, cache)
    }
  }

  @Test
  fun offlineReadsFromCache() {
    runTest {
      connectivityObserver.setOnline(false)
      postRepository.addPost(post)
      db.collection(POSTS_COLLECTION_PATH)
          .document(post.postId)
          .update("description", "tampered")
          .await()
      val result = postRepository.getPost(post.postId)
      assertEquals(post.postId, result.postId)
      assertEquals(post, result)
    }
  }

  @Test
  fun onlineAndStaleFetchesFromFirestore() {
    runTest {
      connectivityObserver.setOnline(true)
      postRepository.addPost(post)

      val staleTime = System.currentTimeMillis() - (20 * 60 * 1000L)
      val staleProto =
          PostProto.newBuilder()
              .setPostId("abc")
              .setAuthorId("cached_user")
              .setPictureUrl("old")
              .setDescription("Cache")
              .setLastUpdated(staleTime)
              .setAnimalId("animal")
              .setDate(123)
              .build()

      dataStore.updateData { it.toBuilder().putPosts(post.postId, staleProto).build() }
      assertNull(cache.getPost(post.postId))

      val result = postRepository.getPost(post.postId)
      assertEquals(post, result)
    }
  }

  @Test
  fun onlineButFreshReadsFromCache() {
    runTest {
      connectivityObserver.setOnline(true)

      val freshTime = System.currentTimeMillis() - (5 * 60 * 1000L)
      val freshProto = post.toProto().toBuilder().setLastUpdated(freshTime).build()

      dataStore.updateData { it.toBuilder().putPosts(post.postId, freshProto).build() }

      assertEquals(post, cache.getPost(post.postId))
      assertEquals(post, postRepository.getPost(post.postId))
    }
  }

  @Test
  fun offlineAndEmptyCache_getAllPostsByAuthorReturnsEmptyList() = runTest {
    connectivityObserver.setOnline(false)
    cache.clearAll()

    val result = cache.getAllPostsByAuthor("authorA")
    assertEquals(emptyList<Post>(), result)
  }

  @Test
  fun onlineAndNoMatchingPosts_getAllPostsByAuthorReturnsNull() = runTest {
    connectivityObserver.setOnline(true)
    cache.savePosts(listOf(postB1))

    val result = cache.getAllPostsByAuthor("authorA")
    assertNull(result)
  }

  @Test
  fun getAllPostsByAuthorReturnsOnlyMatchingPosts_whenFreshAndOnline() = runTest {
    connectivityObserver.setOnline(true)
    cache.savePosts(listOf(postA1, postA2, postB1))

    val result = cache.getAllPostsByAuthor("authorA")
    assertEquals(2, result?.size)
    assertEquals(setOf("p1", "p2"), result?.map { it.postId }?.toSet())
  }

  @Test
  fun onlineAndStaleMatchingPosts_getAllPostsByAuthorReturnsNull() = runTest {
    connectivityObserver.setOnline(true)

    val staleProtoA1 = postA1.toProto().toBuilder().setLastUpdated(staleTime).build()
    dataStore.updateData { it.toBuilder().putPosts(postA1.postId, staleProtoA1).build() }

    val result = cache.getAllPostsByAuthor("authorA")
    assertNull(result)
  }

  @Test
  fun deletePostsByUserRemovesAllMatchingPosts() = runTest {
    connectivityObserver.setOnline(true)
    cache.savePosts(listOf(postA1, postA2, postB1))

    cache.deletePostsByUser("authorA")

    val remaining = cache.getAllPosts()
    assertEquals(1, remaining?.size)
    assertEquals("authorB", remaining?.first()?.authorId)
  }

  @Test
  fun savePostThenDeletePostRemovesIt() = runTest {
    connectivityObserver.setOnline(true)

    cache.savePost(postA1)
    assertEquals(postA1, cache.getPost(postA1.postId))

    cache.deletePost(postA1.postId)
    assertNull(cache.getPost(postA1.postId))
  }

  @Test
  fun clearAllRemovesEverything() = runTest {
    connectivityObserver.setOnline(true)
    cache.savePosts(listOf(postA1, postB1))

    cache.clearAll()

    assertNull(cache.getAllPosts())
  }

  @Test
  fun getAllPosts_offlineAndEmptyCacheReturnsEmptyList() = runTest {
    connectivityObserver.setOnline(false)
    cache.clearAll()

    assertEquals(emptyList<Post>(), cache.getAllPosts())
  }

  @Test
  fun getAllPosts_onlineAndStaleCacheReturnsNull() = runTest {
    connectivityObserver.setOnline(true)

    val staleProto = postA1.toProto().toBuilder().setLastUpdated(staleTime).build()
    dataStore.updateData { it.toBuilder().putPosts(postA1.postId, staleProto).build() }

    assertNull(cache.getAllPosts())
  }
}
