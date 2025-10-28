package com.android.wildex.model.storage

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.android.wildex.utils.FirebaseEmulator
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StorageRepositoryFirebaseTest {

  private lateinit var storageRepository: StorageRepositoryFirebase
  private lateinit var testImageUri: Uri

  @Before
  fun setUp() {
    if (!FirebaseEmulator.isRunning) {
      throw IllegalStateException(
          "Firebase Emulator must be running to execute these tests. Run 'firebase emulators:start' first.")
    }

    storageRepository = StorageRepositoryFirebase(FirebaseEmulator.storage)

    testImageUri = createTestImageFile()
  }

  @After fun tearDown() {}

  private fun createTestImageFile(): Uri {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val testFile = File(context.cacheDir, "test_image.jpg")
    FileOutputStream(testFile).use { output ->
      val testData = ByteArray(100) { it.toByte() }
      output.write(testData)
    }
    return Uri.fromFile(testFile)
  }

  @Test
  fun storageRepository_canBeInstantiatedWithDefaultConstructor() = runTest {
    // This test covers the default constructor (with default parameter)
    // which uses Firebase.storage by default
    val defaultRepository = StorageRepositoryFirebase()

    assertNotNull("Repository should be created with default constructor", defaultRepository)
  }

  @Test
  fun uploadUserProfilePicture_returnsDownloadUrl_whenUploadSucceeds() = runTest {
    val userId = "user123"

    try {
      val result = storageRepository.uploadUserProfilePicture(userId, testImageUri)

      assertNotNull("Upload should return a URL", result)
      assertTrue("URL should contain userId", result!!.contains(userId))
      assertTrue("URL should point to Firebase Storage", result.contains("firebasestorage"))
    } finally {
      runCatching { FirebaseEmulator.storage.reference.child("users/$userId.jpg").delete().await() }
    }
  }

  @Test
  fun uploadUserProfilePicture_createsFileAtCorrectPath() = runTest {
    val userId = "testUser456"

    try {
      val url = storageRepository.uploadUserProfilePicture(userId, testImageUri)

      assertNotNull("Upload should succeed", url)
      val expectedPathSegment = "users%2F$userId.jpg"
      assertTrue(
          "URL should contain correct path: $expectedPathSegment",
          url!!.contains(expectedPathSegment),
      )
    } finally {
      runCatching { FirebaseEmulator.storage.reference.child("users/$userId.jpg").delete().await() }
    }
  }

  @Test
  fun uploadPostImage_returnsDownloadUrl_whenUploadSucceeds() = runTest {
    val postId = "post789"

    try {
      val result = storageRepository.uploadPostImage(postId, testImageUri)

      assertNotNull("Upload should return a URL", result)
      assertTrue("URL should contain postId", result!!.contains(postId))
      assertTrue("URL should point to Firebase Storage", result.contains("firebasestorage"))
    } finally {
      runCatching { FirebaseEmulator.storage.reference.child("posts/$postId.jpg").delete().await() }
    }
  }

  @Test
  fun uploadPostImage_createsFileAtCorrectPath() = runTest {
    val postId = "testPost123"

    try {
      val url = storageRepository.uploadPostImage(postId, testImageUri)

      assertNotNull("Upload should succeed", url)
      val expectedPathSegment = "posts%2F$postId.jpg"
      assertTrue(
          "URL should contain correct path: $expectedPathSegment",
          url!!.contains(expectedPathSegment),
      )
    } finally {
      runCatching { FirebaseEmulator.storage.reference.child("posts/$postId.jpg").delete().await() }
    }
  }

  @Test
  fun uploadAnimalPicture_returnsDownloadUrl_whenUploadSucceeds() = runTest {
    val animalId = "animal123"

    try {
      val result = storageRepository.uploadAnimalPicture(animalId, testImageUri)

      assertNotNull("Upload should return a URL", result)
      assertTrue("URL should contain animalId", result!!.contains(animalId))
      assertTrue("URL should point to Firebase Storage", result.contains("firebasestorage"))
    } finally {
      runCatching {
        FirebaseEmulator.storage.reference.child("animals/$animalId.jpg").delete().await()
      }
    }
  }

  @Test
  fun uploadAnimalPicture_createsFileAtCorrectPath() = runTest {
    val animalId = "testAnimal789"

    try {
      val url = storageRepository.uploadAnimalPicture(animalId, testImageUri)

      assertNotNull("Upload should succeed", url)
      val expectedPathSegment = "animals%2F$animalId.jpg"
      assertTrue(
          "URL should contain correct path: $expectedPathSegment",
          url!!.contains(expectedPathSegment),
      )
    } finally {
      runCatching {
        FirebaseEmulator.storage.reference.child("animals/$animalId.jpg").delete().await()
      }
    }
  }

  @Test
  fun uploadMultipleUserProfilePictures_eachReturnsUniqueUrl() = runTest {
    val userId1 = "user001"
    val userId2 = "user002"
    val userId3 = "user003"

    try {
      val url1 = storageRepository.uploadUserProfilePicture(userId1, testImageUri)
      val url2 = storageRepository.uploadUserProfilePicture(userId2, testImageUri)
      val url3 = storageRepository.uploadUserProfilePicture(userId3, testImageUri)

      assertNotNull(url1)
      assertNotNull(url2)
      assertNotNull(url3)
      assertTrue("Each URL should be unique", url1 != url2 && url2 != url3 && url1 != url3)
    } finally {
      runCatching {
        FirebaseEmulator.storage.reference.child("users/$userId1.jpg").delete().await()
      }
      runCatching {
        FirebaseEmulator.storage.reference.child("users/$userId2.jpg").delete().await()
      }
      runCatching {
        FirebaseEmulator.storage.reference.child("users/$userId3.jpg").delete().await()
      }
    }
  }

  @Test
  fun uploadUserProfilePicture_overwritesExistingImage() = runTest {
    val userId = "userOverwrite"

    try {
      val firstUrl = storageRepository.uploadUserProfilePicture(userId, testImageUri)
      val secondUrl = storageRepository.uploadUserProfilePicture(userId, testImageUri)

      assertNotNull(firstUrl)
      assertNotNull(secondUrl)
      assertEquals(
          "Uploading to same path should overwrite URL",
          FirebaseEmulator.storage.reference
              .child("users/$userId.jpg")
              .downloadUrl
              .await()
              .toString(),
          secondUrl,
      )
    } finally {
      runCatching { FirebaseEmulator.storage.reference.child("users/$userId.jpg").delete().await() }
    }
  }

  @Test
  fun deleteUserProfilePicture_succeeds_afterUpload() = runTest {
    val userId = "userToDelete"

    try {
      storageRepository.uploadUserProfilePicture(userId, testImageUri)

      storageRepository.deleteUserProfilePicture(userId)

      assertFalse(FirebaseEmulator.storage.reference.child("users/$userId.jpg").exists())
    } finally {
      runCatching { FirebaseEmulator.storage.reference.child("users/$userId.jpg").delete().await() }
    }
  }

  @Test
  fun deletePostImage_succeeds_afterUpload() = runTest {
    val postId = "postToDelete"

    try {
      storageRepository.uploadPostImage(postId, testImageUri)

      storageRepository.deletePostImage(postId)

      assertFalse(FirebaseEmulator.storage.reference.child("posts/$postId.jpg").exists())
    } finally {
      runCatching { FirebaseEmulator.storage.reference.child("posts/$postId.jpg").delete().await() }
    }
  }

  @Test
  fun deleteAnimalPicture_succeeds_afterUpload() = runTest {
    val animalId = "animalToDelete"

    try {
      storageRepository.uploadAnimalPicture(animalId, testImageUri)

      storageRepository.deleteAnimalPicture(animalId)

      assertFalse(FirebaseEmulator.storage.reference.child("animals/$animalId.jpg").exists())
    } finally {
      runCatching {
        FirebaseEmulator.storage.reference.child("animals/$animalId.jpg").delete().await()
      }
    }
  }

  @Test
  fun uploadUserProfilePicture_returnsNull_whenInvalidUriProvided() = runTest {
    val userId = "invalidUriTest"
    val invalidUri = Uri.parse("invalid://path/to/nowhere")

    try {
      val result = storageRepository.uploadUserProfilePicture(userId, invalidUri)

      assertNull("Upload should return null for invalid URI", result)
    } finally {
      runCatching { FirebaseEmulator.storage.reference.child("users/$userId.jpg").delete().await() }
    }
  }

  @Test
  fun uploadPostImage_returnsNull_whenInvalidUriProvided() = runTest {
    val postId = "invalidUriTest"
    val invalidUri = Uri.parse("invalid://path/to/nowhere")

    try {
      val result = storageRepository.uploadPostImage(postId, invalidUri)

      assertNull("Upload should return null for invalid URI", result)
    } finally {
      runCatching { FirebaseEmulator.storage.reference.child("posts/$postId.jpg").delete().await() }
    }
  }

  @Test
  fun uploadAnimalPicture_returnsNull_whenInvalidUriProvided() = runTest {
    val animalId = "invalidUriTest"
    val invalidUri = Uri.parse("invalid://path/to/nowhere")

    try {
      val result = storageRepository.uploadAnimalPicture(animalId, invalidUri)

      assertNull("Upload should return null for invalid URI", result)
    } finally {
      runCatching {
        FirebaseEmulator.storage.reference.child("animals/$animalId.jpg").delete().await()
      }
    }
  }

  @Test
  fun deleteUserProfilePicture_catchesException_whenFileDoesNotExist() = runTest {
    val userId = "nonExistentUserDelete"

    try {
      // Attempt to delete non-existent file - should catch exception and log it
      storageRepository.deleteUserProfilePicture(userId)

      // If we reach here, exception was caught (not propagated)
      assertFalse(
          "File should not exist",
          FirebaseEmulator.storage.reference.child("users/$userId.jpg").exists())
    } finally {
      runCatching { FirebaseEmulator.storage.reference.child("users/$userId.jpg").delete().await() }
    }
  }

  @Test
  fun deletePostImage_catchesException_whenFileDoesNotExist() = runTest {
    val postId = "nonExistentPostDelete"

    try {
      // Attempt to delete non-existent file - should catch exception and log it
      storageRepository.deletePostImage(postId)

      // If we reach here, exception was caught (not propagated)
      assertFalse(
          "File should not exist",
          FirebaseEmulator.storage.reference.child("posts/$postId.jpg").exists())
    } finally {
      runCatching { FirebaseEmulator.storage.reference.child("posts/$postId.jpg").delete().await() }
    }
  }

  @Test
  fun deleteAnimalPicture_catchesException_whenFileDoesNotExist() = runTest {
    val animalId = "nonExistentAnimalDelete"

    try {
      storageRepository.deleteAnimalPicture(animalId)

      // If we reach here, exception was caught (not propagated)
      assertFalse(
          "File should not exist",
          FirebaseEmulator.storage.reference.child("animals/$animalId.jpg").exists())
    } finally {
      runCatching {
        FirebaseEmulator.storage.reference.child("animals/$animalId.jpg").delete().await()
      }
    }
  }

  private fun StorageReference.exists(): Boolean = runBlocking {
    return@runBlocking try {
      metadata.await()
      true
    } catch (_: Exception) {
      false
    }
  }
}
