package com.android.wildex.ui.notification

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
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

  @Test
  fun initialState_displaysAllNotifications() {
    val vm = NotificationScreenViewModel()
    composeRule.setContent { NotificationScreen(onGoBack = {}, notificationScreenViewModel = vm) }
    composeRule.waitForIdle()

    val notifications = vm.uiState.value.notifications
    Assert.assertTrue(notifications.isNotEmpty())
    notifications.forEach {
      composeRule.onNodeWithText(it.notificationTitle).assertIsDisplayed()
      composeRule.onNodeWithText(it.notificationDescription).assertIsDisplayed()
    }
  }

  @Test
  fun goBack_makes_callback() {
    var back = 0
    composeRule.setContent { NotificationScreen(onGoBack = { back++ }) }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(NotificationScreenTestTags.GO_BACK).assertIsDisplayed().performClick()
    Assert.assertEquals(1, back)
  }

  @Test
  fun notificationItem_fixedSize_image_and_arrow_and_texts_displayed() {
    val longTitle = "Really really long title that should be truncated to stay on one line"
    val longDesc =
        "Really really long description that should also be truncated to stay on one line"
    val authorId = "author1"
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

  @Test
  fun refresh_keep_display_notifications() {
    val vm = NotificationScreenViewModel()
    composeRule.setContent { NotificationScreen(onGoBack = {}, notificationScreenViewModel = vm) }
    composeRule.waitForIdle()

    composeRule.runOnIdle { vm.refreshUIState() }
    composeRule.waitForIdle()

    vm.uiState.value.notifications.forEach {
      composeRule.onNodeWithText(it.notificationTitle).assertIsDisplayed()
    }
  }

  @Test
  fun noNotificationView_display_message_empty() {
    composeRule.setContent { NoNotificationView() }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(NotificationScreenTestTags.NO_NOTIFICATION_TEXT).assertIsDisplayed()
  }
}
