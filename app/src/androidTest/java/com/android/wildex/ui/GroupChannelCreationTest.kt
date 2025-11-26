package com.android.wildex.ui

import android.Manifest
import android.app.NotificationManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.rule.GrantPermissionRule
import com.android.wildex.MainActivity
import com.android.wildex.model.notification.NotificationChannelType
import com.android.wildex.model.notification.NotificationGroupType
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class GroupChannelCreationTest {
  @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

  @Test
  fun testNotificationChannelsCreated() {
    // Use spyk to override getSystemService
    val activity = spyk(composeRule.activity)
    val notificationManager = spyk(activity.getSystemService(NotificationManager::class.java))
    composeRule.cancelAndRecreateRecomposer()

    // Verify the NotificationManager was used correctly
    verify(exactly = NotificationGroupType.entries.size) {
      notificationManager.createNotificationChannelGroup(any())
    }
    verify(exactly = NotificationChannelType.entries.size) {
      notificationManager.createNotificationChannel(any())
    }
  }

  @Test
  fun testNotificationChannelsExist() {
    val notificationManager =
        composeRule.activity.getSystemService(NotificationManager::class.java)!!

    NotificationGroupType.entries.forEach {
      val group = notificationManager.getNotificationChannelGroup(it.groupId)
      assertNotNull(group)
    }

    NotificationChannelType.entries.forEach {
      val channel = notificationManager.getNotificationChannel(it.channelId)
      assertNotNull(channel)
    }
  }
}
