package com.android.wildex.model.user

import com.android.wildex.utils.FirebaseEmulator
import com.android.wildex.utils.FirestoreTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val USER_SETTINGS_COLLECTION_PATH = "userSettings"

class UserSettingsRepositoryFirestoreTest : FirestoreTest(USER_SETTINGS_COLLECTION_PATH) {
  private var repository = UserSettingsRepositoryFirestore(FirebaseEmulator.firestore)

  private suspend fun getUsersCount(): Int = super.getCount()

  @Test
  fun initializeUserSettingsWhenNoUserExists() = runTest {
    var exceptionThrown = false

    try {
      repository.initializeUserSettings(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
    }

    assertFalse(exceptionThrown)
    assertEquals(1, getUsersCount())

    val enableNotification = repository.getEnableNotification(user1.userId)
    val appearanceMode = repository.getAppearanceMode(user1.userId)

    assertTrue(enableNotification)
    assertEquals(AppearanceMode.AUTOMATIC, appearanceMode)
  }

  @Test
  fun initializeUsersettingsWhenUserAlreadyExists() = runTest {
    repository.initializeUserSettings(user1.userId)

    var exceptionThrown = false

    try {
      repository.initializeUserSettings(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userSettings with userId '${user1.userId}' already exists.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun getEnableNotificationWhenUserDoesNotExist() = runTest {
    var exceptionThrown = false

    try {
      repository.getEnableNotification(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userSettings with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun setEnableNotificationWhenUserDoesNotExist() = runTest {
    var exceptionThrown = false

    try {
      repository.setEnableNotification(user1.userId, true)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userSettings with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun getAppearanceModeWhenUserDoesNotExist() = runTest {
    var exceptionThrown = false

    try {
      repository.getAppearanceMode(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userSettings with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun setAppearanceModeWhenUserDoesNotExist() = runTest {
    var exceptionThrown = false

    try {
      repository.setAppearanceMode(user1.userId, AppearanceMode.AUTOMATIC)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userSettings with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun deleteUserSettingsWhenUserDoesNotExist() = runTest {
    var exceptionThrown = false

    try {
      repository.deleteUserSettings(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true

      assertEquals("A userSettings with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }

  @Test
  fun setEnableNotificationSuccess() = runTest {
    repository.initializeUserSettings(user1.userId)

    repository.setEnableNotification(user1.userId, false)
    val enableNotification = repository.getEnableNotification(user1.userId)

    assertFalse(enableNotification)
  }

  @Test
  fun setAppearanceModeSuccess() = runTest {
    repository.initializeUserSettings(user1.userId)

    repository.setAppearanceMode(user1.userId, AppearanceMode.DARK)
    val appearanceMode = repository.getAppearanceMode(user1.userId)

    assertEquals(AppearanceMode.DARK, appearanceMode)
  }

  @Test
  fun deleteAccountSuccess() = runTest{
    var exceptionThrown = false
    repository.initializeUserSettings(user1.userId)

    repository.deleteUserSettings(user1.userId)
    try {
      repository.getEnableNotification(user1.userId)
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals("A userSettings with userId '${user1.userId}' does not exist.", e.message)
    }

    assertTrue(exceptionThrown)
  }
}
