package com.android.wildex.ui.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.wildex.R
import kotlinx.coroutines.delay

@Composable
fun ActionsRow(onMarkAllRead: () -> Unit, onDeleteAll: () -> Unit) {

  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Mark all read
    FilledTonalButton(
        onClick = onMarkAllRead,
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = colorScheme.primary.copy(alpha = 0.1f),
                contentColor = colorScheme.primary,
            ),
        modifier = Modifier.weight(1f).testTag(NotificationScreenTestTags.MARK_ALL_AS_READ_BUTTON),
        shape = RoundedCornerShape(8.dp),
    ) {
      Icon(Icons.Default.Done, contentDescription = null)
      Spacer(Modifier.width(6.dp))
      Text(stringResource(R.string.notifications_mark_all_read))
    }

    // Delete all
    FilledTonalButton(
        onClick = onDeleteAll,
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = colorScheme.error.copy(alpha = 0.1f),
                contentColor = colorScheme.error,
            ),
        modifier = Modifier.weight(1f).testTag(NotificationScreenTestTags.CLEAR_ALL_BUTTON),
        shape = RoundedCornerShape(8.dp),
    ) {
      Icon(Icons.Default.Delete, contentDescription = null)
      Spacer(Modifier.width(6.dp))
      Text(stringResource(R.string.notifications_delete_all))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteNotification(
    itemId: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    animationDuration: Int = 500,
    content: @Composable RowScope.() -> Unit = {},
) {
  var visible by remember(itemId) { mutableStateOf(true) }

  val dismissState = rememberSwipeToDismissBoxState(initialValue = SwipeToDismissBoxValue.Settled)

  LaunchedEffect(dismissState.currentValue) {
    if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
      visible = false
      delay(animationDuration.toLong())
      onDelete()
    }
  }

  AnimatedVisibility(
      visible = visible,
      exit =
          slideOutHorizontally(
              targetOffsetX = { fullWidth -> fullWidth },
              animationSpec = tween(durationMillis = animationDuration),
          ) + fadeOut(animationSpec = tween(durationMillis = animationDuration)),
      modifier = modifier,
  ) {
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
          val backgroundColor = colorScheme.background
          val errorColor = colorScheme.error

          val progress = dismissState.progress
          val animatedColor = lerp(errorColor, backgroundColor, progress)

          Box(
              modifier =
                  Modifier.fillMaxSize().background(animatedColor).padding(horizontal = 20.dp),
              contentAlignment = Alignment.CenterStart,
          ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = colorScheme.onError,
                modifier = Modifier.size(24.dp).alpha(progress),
            )
          }
        },
        content = content,
    )
  }
}
