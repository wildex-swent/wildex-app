package com.android.wildex.ui.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.wildex.R
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.UserType

object ReportActionsTestTags {
  const val ACTION_ROW = "report_actions_action_row"
  const val ACTION_CANCEL = "report_actions_action_cancel"
  const val ACTION_SELF_ASSIGN = "report_actions_action_self_assign"
  const val ACTION_RESOLVE = "report_actions_action_resolve"
  const val ACTION_UNSELFASSIGN = "report_actions_action_unselfassign"
}

/**
 * A pill-shaped action button used in report details.
 *
 * @param text The button text.
 * @param onClick Callback when the button is clicked.
 * @param modifier Modifier to be applied to the button.
 * @param colors The colors to be used for the button.
 * @param border Optional border for the button.
 * @param isActionInProgress Whether an action is currently in progress, disabling the button if
 *   true.
 */
@Composable
fun ReportActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors,
    border: BorderStroke? = null,
    isActionInProgress: Boolean = false,
) {
  OutlinedButton(
      onClick = onClick,
      modifier = modifier.height(44.dp),
      shape = RoundedCornerShape(999.dp),
      colors = colors,
      border = border,
      enabled = !isActionInProgress) {
        Text(text = text, style = typography.labelLarge)
      }
}

/**
 * Adaptive action row using pill buttons.
 *
 * @param hasAssignee Whether the report has an assignee.
 * @param currentUser The current user.
 * @param isCreatedByCurrentUser Whether the report was created by the current user.
 * @param isAssignedToCurrentUser Whether the report is assigned to the current user.
 * @param isActionInProgress Whether an action is currently in progress, disabling buttons if true
 * @param onCancel Callback when the cancel action is triggered.
 * @param onSelfAssign Callback when the self-assign action is triggered.
 * @param onResolve Callback when the resolve action is triggered.
 * @param onUnSelfAssign Callback when the un-self-assign action is triggered.
 */
@Composable
fun ReportDetailsActionRow(
    hasAssignee: Boolean,
    currentUser: SimpleUser,
    isCreatedByCurrentUser: Boolean,
    isAssignedToCurrentUser: Boolean,
    isActionInProgress: Boolean = false,
    onCancel: () -> Unit = {},
    onSelfAssign: () -> Unit = {},
    onResolve: () -> Unit = {},
    onUnSelfAssign: () -> Unit = {},
) {
  val context = LocalContext.current
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp)
              .testTag(ReportActionsTestTags.ACTION_ROW),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    when (currentUser.userType) {
      UserType.REGULAR -> {
        if (isCreatedByCurrentUser) {
          ReportActionButton(
              text = context.getString(R.string.report_details_cancel_third),
              onClick = onCancel,
              modifier = Modifier.fillMaxWidth().testTag(ReportActionsTestTags.ACTION_CANCEL),
              isActionInProgress = isActionInProgress,
              colors =
                  ButtonDefaults.outlinedButtonColors(
                      contentColor = colorScheme.onBackground,
                  ),
              border = BorderStroke(1.dp, colorScheme.onBackground),
          )
        }
      }
      UserType.PROFESSIONAL -> {
        when {
          !hasAssignee && isCreatedByCurrentUser -> {
            ReportActionButton(
                text = context.getString(R.string.report_details_self_assign_third),
                onClick = onSelfAssign,
                isActionInProgress = isActionInProgress,
                modifier = Modifier.weight(1f).testTag(ReportActionsTestTags.ACTION_SELF_ASSIGN),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                    ),
            )
            ReportActionButton(
                text = context.getString(R.string.report_details_button_delete),
                onClick = onCancel,
                isActionInProgress = isActionInProgress,
                modifier = Modifier.weight(1f).testTag(ReportActionsTestTags.ACTION_CANCEL),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = colorScheme.onBackground,
                    ),
                border = BorderStroke(1.dp, colorScheme.onBackground),
            )
          }
          !hasAssignee -> {
            ReportActionButton(
                text = context.getString(R.string.report_details_self_assign_third),
                onClick = onSelfAssign,
                isActionInProgress = isActionInProgress,
                modifier = Modifier.weight(1f).testTag(ReportActionsTestTags.ACTION_SELF_ASSIGN),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                    ),
            )
          }
          isAssignedToCurrentUser -> {
            ReportActionButton(
                text = context.getString(R.string.report_details_resolve_third),
                onClick = onResolve,
                isActionInProgress = isActionInProgress,
                modifier = Modifier.weight(1f).testTag(ReportActionsTestTags.ACTION_RESOLVE),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                    ),
            )
            ReportActionButton(
                text = context.getString(R.string.report_details_unself_assign_third),
                onClick = onUnSelfAssign,
                isActionInProgress = isActionInProgress,
                modifier = Modifier.weight(1f).testTag(ReportActionsTestTags.ACTION_UNSELFASSIGN),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = colorScheme.onBackground,
                    ),
                border = BorderStroke(1.dp, colorScheme.onBackground),
            )
          }
          isCreatedByCurrentUser -> {
            ReportActionButton(
                text = context.getString(R.string.report_details_button_delete),
                onClick = onCancel,
                isActionInProgress = isActionInProgress,
                modifier = Modifier.fillMaxWidth().testTag(ReportActionsTestTags.ACTION_CANCEL),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = colorScheme.onBackground,
                    ),
                border = BorderStroke(1.dp, colorScheme.onBackground),
            )
          }
        }
      }
    }
  }
}
