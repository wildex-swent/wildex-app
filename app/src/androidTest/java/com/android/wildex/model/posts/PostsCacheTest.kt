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
}
