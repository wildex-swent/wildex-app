package com.android.wildex.ui.notification

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserType
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class NotificationScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private val sampleNotifications =
      listOf(
          NotificationUIState(
              notificationId = "1",
              simpleUser =
                  SimpleUser(
                      userId = "user1",
                      username = "Jean",
                      profilePictureURL = "",
                      userType = UserType.REGULAR),
              notificationRoute = "route/to/post/1",
              notificationTitle = "Jean has liked your post",
              notificationDescription = "3min ago",
          ),
          NotificationUIState(
              notificationId = "2",
              simpleUser =
                  SimpleUser(
                      userId = "user2",
                      username = "Bob",
                      profilePictureURL = "",
                      userType = UserType.REGULAR),
              notificationRoute = "route/to/post/2",
              notificationTitle = "Bob spotted a tiger",
              notificationDescription = "15min ago",
          ),
          NotificationUIState(
              notificationId = "3",
              simpleUser =
                  SimpleUser(
                      userId = "user3",
                      username = "Alice",
                      profilePictureURL = "",
                      userType = UserType.REGULAR),
              notificationRoute = "route/to/post/3",
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
    val testUser =
        SimpleUser(
            userId = authorId,
            username = "Author",
            profilePictureURL = "",
            userType = UserType.REGULAR)

    composeRule.setContent {
      MaterialTheme(colorScheme = lightColorScheme()) {
        NotificationItem(
            notificationContentId = contentId,
            notificationRoute = "route/to/$contentId",
            simpleUser = testUser,
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
    composeRule.setContent {
      MaterialTheme(colorScheme = lightColorScheme()) { NoNotificationView() }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(NotificationScreenTestTags.NO_NOTIFICATION_TEXT).assertIsDisplayed()
  }

  @Test
  fun notificationScreen_showsSampleNotificationsByDefault() {
    composeRule.setContent {
      MaterialTheme(colorScheme = lightColorScheme()) { NotificationScreen() }
    }
    composeRule.waitForIdle()

    composeRule
        .onAllNodesWithTag(NotificationScreenTestTags.NO_NOTIFICATION_TEXT)
        .assertCountEquals(0)
    composeRule.onNodeWithText("Jean has liked your post").assertIsDisplayed()
  }
}
