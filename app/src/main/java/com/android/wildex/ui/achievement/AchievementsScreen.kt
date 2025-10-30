package com.android.wildex.ui.achievement

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.wildex.model.achievement.Achievement

/**
 * Screen that displays the user's achievements in two sections: unlocked and locked.
 *
 * @param viewModel The [AchievementsScreenViewModel] that provides the UI state and handles data
 *   loading.
 * @param onGoBack Callback invoked when the user presses the back button in the top app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(viewModel: AchievementsScreenViewModel, onGoBack: () -> Unit) {}
/**
 * Represents a single achievement item displayed in the achievements grid.
 *
 * The visual appearance depends on whether the achievement has been unlocked:
 * - Unlocked achievements are shown in full opacity.
 * - Locked achievements appear faded to indicate inaccessibility.
 *
 * @param achievement The [Achievement] to display, including its name and image URL.
 * @param unlocked Whether the achievement has been unlocked by the user.
 */
@Composable fun AchievementItem(achievement: Achievement, unlocked: Boolean) {}

/**
 * A horizontal divider with a centered label.
 *
 * @param text The label text to display in the center of the divider.
 * @param color The color of the divider and text.
 * @param thickness The thickness of the divider lines.
 * @param padding The horizontal padding around the label text.
 */
@Composable
fun LabeledDivider(text: String, color: Color, thickness: Dp = 2.dp, padding: Dp = 8.dp) {}
