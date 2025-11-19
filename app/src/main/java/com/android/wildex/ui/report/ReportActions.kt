package com.android.wildex.ui.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.wildex.model.user.UserType

@Composable
fun ExpandableTextCore(
    text: String,
    collapsedLines: Int,
    style: TextStyle,
    color: Color,
    bodyModifier: Modifier = Modifier,
    toggleModifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  var hasOverflow by remember { mutableStateOf(false) }

  Column {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { result -> hasOverflow = result.hasVisualOverflow },
        modifier = bodyModifier,
    )

    if (hasOverflow || expanded) {
      Spacer(Modifier.height(2.dp))
      Text(
          text = if (expanded) "Show less" else "Read more",
          style = typography.labelSmall,
          color = colorScheme.tertiary,
          modifier = toggleModifier.align(Alignment.End).clickable { expanded = !expanded },
      )
    }
  }
}

@Composable
fun ReportActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors,
    border: BorderStroke? = null,
) {
  OutlinedButton(
      onClick = onClick,
      modifier = modifier.height(44.dp),
      shape = RoundedCornerShape(999.dp),
      colors = colors,
      border = border,
  ) {
    Text(text = text, style = typography.labelLarge)
  }
}

/** Adaptive action row using pill buttons. */
@Composable
fun ReportDetailsActionRow(
    uiState: ReportDetailsUIState,
    onCancel: () -> Unit = {},
    onSelfAssign: () -> Unit = {},
    onResolve: () -> Unit = {},
    onUnSelfAssign: () -> Unit = {},
) {
  val hasAssignee = uiState.assignee != null

  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp)
              .testTag(ReportDetailsScreenTestTags.ACTION_ROW),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    when (uiState.currentUser.userType) {
      UserType.REGULAR -> {
        if (uiState.isCreatedByCurrentUser) {
          ReportActionButton(
              text = "Delete report",
              onClick = onCancel,
              modifier = Modifier.fillMaxWidth().testTag(ReportDetailsScreenTestTags.ACTION_CANCEL),
              colors =
                  ButtonDefaults.outlinedButtonColors(
                      contentColor = colorScheme.tertiary,
                  ),
              border = BorderStroke(1.dp, colorScheme.tertiary),
          )
        }
      }
      UserType.PROFESSIONAL -> {
        when {
          !hasAssignee -> {
            ReportActionButton(
                text = "Assign to me",
                onClick = onSelfAssign,
                modifier =
                    Modifier.weight(1f).testTag(ReportDetailsScreenTestTags.ACTION_SELF_ASSIGN),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                    ),
            )
            if (uiState.isCreatedByCurrentUser) {
              ReportActionButton(
                  text = "Delete",
                  onClick = onCancel,
                  modifier = Modifier.weight(1f).testTag(ReportDetailsScreenTestTags.ACTION_CANCEL),
                  colors =
                      ButtonDefaults.outlinedButtonColors(
                          contentColor = colorScheme.tertiary,
                      ),
                  border = BorderStroke(1.dp, colorScheme.tertiary),
              )
            }
          }
          uiState.isAssignedToCurrentUser -> {
            ReportActionButton(
                text = "Resolve",
                onClick = onResolve,
                modifier = Modifier.weight(1f).testTag(ReportDetailsScreenTestTags.ACTION_RESOLVE),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                    ),
            )
            ReportActionButton(
                text = "Cancel",
                onClick = onUnSelfAssign,
                modifier =
                    Modifier.weight(1f).testTag(ReportDetailsScreenTestTags.ACTION_UNSELFASSIGN),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = colorScheme.tertiary,
                    ),
                border = BorderStroke(1.dp, colorScheme.tertiary),
            )
          }
          else -> {
            if (uiState.isCreatedByCurrentUser) {
              ReportActionButton(
                  text = "Delete",
                  onClick = onCancel,
                  modifier =
                      Modifier.fillMaxWidth().testTag(ReportDetailsScreenTestTags.ACTION_CANCEL),
                  colors =
                      ButtonDefaults.outlinedButtonColors(
                          contentColor = colorScheme.tertiary,
                      ),
                  border = BorderStroke(1.dp, colorScheme.tertiary),
              )
            }
          }
        }
      }
    }
  }
}
