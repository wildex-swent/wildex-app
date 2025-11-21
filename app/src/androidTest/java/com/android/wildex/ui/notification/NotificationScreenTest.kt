package com.android.wildex.ui.notification

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class NotificationScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private val sampleNotifications =
      listOf(
          NotificationUIState(
              notificationId = "1",
              notificationType = NotificationType.LIKE,
              notificationTitle = "Jean has liked your post",
              notificationDescription = "3min ago",
          ),
          NotificationUIState(
              notificationId = "2",
              notificationType = NotificationType.POST,
              notificationTitle = "Bob spotted a tiger",
              notificationDescription = "15min ago",
          ),
          NotificationUIState(
              notificationId = "3",
              notificationType = NotificationType.COMMENT,
              notificationTitle = "Alice commented on your post",
              notificationDescription = "Alice said: Wow, amazing!",
          ),
      )

  @Test
  fun sampleNotifications_areDisplayed() {
    composeRule.setContent {
      MaterialTheme(colorScheme = lightColorScheme()) {
        NotificationView(
            notifications = sampleNotifications,
            pd = PaddingValues(0.dp),
            onNotificationClick = { _, _ -> },
            onProfileClick = {},
        )
      }
    }
    composeRule.waitForIdle()

    sampleNotifications.forEach {
      composeRule.onNodeWithText(it.notificationTitle).assertIsDisplayed()
      composeRule.onNodeWithText(it.notificationDescription).assertIsDisplayed()
    }
  }

  @Test
  fun goBack_triggersCallback() {
    var back = 0
    composeRule.setContent { NotificationScreen(onGoBack = { back++ }) }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(NotificationScreenTestTags.GO_BACK).assertIsDisplayed().performClick()
    Assert.assertEquals(1, back)
  }

  @Test
  fun notificationItem_fixedSizes_and_textsDisplayed() {
    val longTitle = "Really really long title that should be truncated to stay on one line"
    val longDesc =
        "Really really long description that should also be truncated to stay on one line"
    val authorId = "authorX"
    val contentId = "content42"

    composeRule.setContent {
      MaterialTheme(colorScheme = lightColorScheme()) {
        NotificationItem(
            authorId = authorId,
            notificationContentId = contentId,
            notificationType = NotificationType.POST,
            notificationTitle = longTitle,
            notificationDescription = longDesc,
            onProfileClick = {},
            onNotificationClick = { _, _ -> },
            cs = MaterialTheme.colorScheme,
        )
      }
    }
    composeRule.waitForIdle()

    composeRule
        .onNodeWithTag(NotificationScreenTestTags.testTagForProfilePicture(authorId))
        .assertIsDisplayed()
        .assertWidthIsEqualTo(48.dp)

    composeRule
        .onNodeWithTag(NotificationScreenTestTags.testTagForNotification(contentId))
        .assertIsDisplayed()
        .assertWidthIsEqualTo(48.dp)

    composeRule.onNodeWithText(longTitle).assertIsDisplayed()
    composeRule.onNodeWithText(longDesc).assertIsDisplayed()
  }

  @Composable
  private fun TestScreenWithSamples(
      vm: NotificationScreenViewModel,
      samples: List<NotificationUIState>
  ) {
    NotificationView(notifications = samples, pd = PaddingValues(0.dp))
  }

  @Test
  fun refresh_keepsSampleNotificationsDisplayed() {
    val vm = NotificationScreenViewModel()
    composeRule.setContent {
      MaterialTheme(colorScheme = lightColorScheme()) {
        TestScreenWithSamples(vm, sampleNotifications)
      }
    }
    composeRule.waitForIdle()

    composeRule.runOnIdle { vm.refreshUIState() }
    composeRule.waitForIdle()

    sampleNotifications.forEach {
      composeRule.onNodeWithText(it.notificationTitle).assertIsDisplayed()
    }
  }

  @Test
  fun noNotificationView_showsEmptyMessage() {
    composeRule.setContent { NoNotificationView() }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(NotificationScreenTestTags.NO_NOTIFICATION_TEXT).assertIsDisplayed()
  }
}
