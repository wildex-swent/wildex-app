package com.android.wildex.ui.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.android.wildex.WildexApp
import com.android.wildex.ui.theme.WildexTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NavigationTestM1 {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
        runBlocking { FirebaseAuth.getInstance().signInAnonymously() }
    }

    @After
    fun teardown() {
        FirebaseAuth.getInstance().signOut()
    }

    @Test
    fun wildexApp_startsAtHomeScreen_whenUserIsAuthenticated() {
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            WildexTheme { WildexApp(context = LocalContext.current, navController = navController) }
        }
        composeRule.waitForIdle()
        // Should start at home screen when user is authenticated
        assertEquals(Screen.Home.route, navController.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun wildexApp_startsAtAuthScreen_whenUserIsNotAuthenticated() {
        // Sign out first
        FirebaseAuth.getInstance().signOut()
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            WildexTheme { WildexApp(context = LocalContext.current, navController = navController) }
        }
        composeRule.waitForIdle()
        // Should start at auth screen when user is not authenticated
        assertEquals(Screen.Auth.route, navController.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun bottomNavigationMenu_isDisplayedOnHomeScreen() {
        composeRule.setContent { WildexTheme { WildexApp(context = LocalContext.current) } }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()

        // Verify all tabs are displayed
        composeRule.onNodeWithTag(NavigationTestTags.HOME_TAB).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.MAP_TAB).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.NEW_POST_TAB).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.COLLECTION_TAB).assertIsDisplayed()
        composeRule.onNodeWithTag(NavigationTestTags.REPORT_TAB).assertIsDisplayed()
    }

    @Test
    fun bottomNavigationMenu_navigatesToMapScreen() {
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            WildexTheme { WildexApp(context = LocalContext.current, navController = navController) }
        }
        composeRule.waitForIdle()

        // Click on Map tab
        composeRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()
        composeRule.waitForIdle()

        // Verify we're on the Map screen
        assertEquals(Screen.Map.route, navController.currentBackStackEntry?.destination?.route)
        composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    }

    @Test
    fun bottomNavigationMenu_navigatesToCameraScreen() {
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            WildexTheme { WildexApp(context = LocalContext.current, navController = navController) }
        }
        composeRule.waitForIdle()

        // Click on New Post tab
        composeRule.onNodeWithTag(NavigationTestTags.NEW_POST_TAB).performClick()
        composeRule.waitForIdle()

        // Verify we're on the New Post screen
        assertEquals(Screen.NewPost.route, navController.currentBackStackEntry?.destination?.route)
        composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    }

    @Test
    fun bottomNavigationMenu_navigatesToCollectionScreen() {
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            WildexTheme { WildexApp(context = LocalContext.current, navController = navController) }
        }
        composeRule.waitForIdle()

        // Click on Collection tab
        composeRule.onNodeWithTag(NavigationTestTags.COLLECTION_TAB).performClick()
        composeRule.waitForIdle()

        // Verify we're on the Collection screen
        assertEquals(
            Screen.Collection.route,
            navController.currentBackStackEntry?.destination?.route
        )
        composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    }

    @Test
    fun bottomNavigationMenu_navigatesToReportScreen() {
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            WildexTheme { WildexApp(context = LocalContext.current, navController = navController) }
        }
        composeRule.waitForIdle()

        // Click on Report tab
        composeRule.onNodeWithTag(NavigationTestTags.REPORT_TAB).performClick()
        composeRule.waitForIdle()

        // Verify we're on the Report screen
        assertEquals(Screen.Report.route, navController.currentBackStackEntry?.destination?.route)
        composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    }

    @Test
    fun bottomNavigationMenu_navigatesBetweenScreens() {
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            WildexTheme { WildexApp(context = LocalContext.current, navController = navController) }
        }
        composeRule.waitForIdle()

        // Start at Home
        assertEquals(Screen.Home.route, navController.currentBackStackEntry?.destination?.route)

        // Navigate to Map
        composeRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()
        composeRule.waitForIdle()
        assertEquals(Screen.Map.route, navController.currentBackStackEntry?.destination?.route)

        // Navigate to Collection
        composeRule.onNodeWithTag(NavigationTestTags.COLLECTION_TAB).performClick()
        composeRule.waitForIdle()
        assertEquals(
            Screen.Collection.route,
            navController.currentBackStackEntry?.destination?.route
        )

        // Navigate back to Home
        composeRule.onNodeWithTag(NavigationTestTags.HOME_TAB).performClick()
        composeRule.waitForIdle()
        assertEquals(Screen.Home.route, navController.currentBackStackEntry?.destination?.route)
    }
}
