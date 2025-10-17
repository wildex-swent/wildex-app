package com.android.wildex.ui.home /*

                                   import androidx.compose.ui.test.assertIsDisplayed
                                   import androidx.compose.ui.test.assertIsNotDisplayed
                                   import androidx.compose.ui.test.junit4.createComposeRule
                                   import androidx.compose.ui.test.onNodeWithTag
                                   import androidx.compose.ui.test.performClick
                                   import androidx.test.ext.junit.runners.AndroidJUnit4
                                   import org.junit.Rule
                                   import org.junit.Test
                                   import org.junit.runner.RunWith

                                   @RunWith(AndroidJUnit4::class)
                                   class HomeScreenTest {

                                     @get:Rule
                                     val composeTestRule = createComposeRule()

                                     @Test
                                     fun testTagsAreCorrectlySetWhenNoPost() {
                                       composeTestRule.setContent { HomeScreen() }

                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.NOTIFICATION_BELL).assertIsDisplayed()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.PROFILE_PICTURE).assertIsDisplayed()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.NO_POST_ICON).assertIsDisplayed()

                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_AUTHOR_PICTURE).assertIsNotDisplayed()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_IMAGE).assertIsNotDisplayed()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_LIKE).assertIsNotDisplayed()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_COMMENT).assertIsNotDisplayed()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_LOCATION).assertIsNotDisplayed()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_LIKE_BUTTON).assertIsNotDisplayed()
                                     }

                                     @Test
                                     fun testTagsAreCorrectlySetWhenPosts() {
                                       composeTestRule.setContent { HomeScreen() }

                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.NOTIFICATION_BELL).assertIsDisplayed()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.PROFILE_PICTURE).assertIsDisplayed()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_AUTHOR_PICTURE).assertIsDisplayed()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_IMAGE).assertIsDisplayed()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_LIKE).assertIsDisplayed()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_COMMENT).assertIsDisplayed()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_LOCATION).assertIsDisplayed()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_LIKE_BUTTON).assertIsDisplayed()

                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.NO_POST_ICON).assertIsNotDisplayed()
                                     }

                                     @Test
                                     fun clickInteractionsWork() {
                                       composeTestRule.setContent { HomeScreen() }

                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.NOTIFICATION_BELL).performClick()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.PROFILE_PICTURE).performClick()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_AUTHOR_PICTURE).performClick()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_IMAGE).performClick()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_LIKE).performClick()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_COMMENT).performClick()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_LOCATION).performClick()
                                       composeTestRule.onNodeWithTag(HomeScreenTestTags.POST_LIKE_BUTTON).performClick()
                                     }
                                   }*/
