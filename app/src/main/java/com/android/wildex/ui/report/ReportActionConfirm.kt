package com.android.wildex.ui.report

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

enum class ReportActionToConfirm {
  CANCEL,
  SELF_ASSIGN,
  RESOLVE,
  UNSELFASSIGN,
}

@Composable
fun ReportActionConfirmDialog(
    action: ReportActionToConfirm,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
  val (title, message, confirmLabel) =
      when (action) {
        ReportActionToConfirm.CANCEL ->
            Triple(
                "Delete this report?",
                "This action is irreversible and will permanently delete the report.",
                "Delete report",
            )
        ReportActionToConfirm.SELF_ASSIGN ->
            Triple(
                "Assign this report to you?",
                "You will be marked as the person handling this situation.",
                "Assign to me",
            )
        ReportActionToConfirm.RESOLVE ->
            Triple(
                "Mark this report as resolved?",
                "This action is irreversible and will permanently delete the report.",
                "Resolve",
            )
        ReportActionToConfirm.UNSELFASSIGN ->
            Triple(
                "Stop handling this report?",
                "You will no longer be assigned to this report.",
                "Unassign",
            )
      }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(title, fontWeight = FontWeight.Bold) },
      text = { Text(message) },
      confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
      dismissButton = { TextButton(onClick = onDismiss) { Text("No, keep it") } },
  )
}
