package com.android.wildex.ui.utils.refresh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Wildex pull to refresh indicator composable.
 *
 * @param state The pull to refresh state.
 * @param isRefreshing Whether the pull to refresh is currently refreshing.
 * @param modifier Modifier to apply to the indicator.
 */
@Composable
fun WildexPullToRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
    PullToRefreshDefaults.Indicator(
        state = state,
        isRefreshing = isRefreshing,
        containerColor = MaterialTheme.colorScheme.background,
        color = MaterialTheme.colorScheme.primary,
    )
  }
}
