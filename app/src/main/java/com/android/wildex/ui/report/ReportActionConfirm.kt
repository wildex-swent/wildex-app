package com.android.wildex.ui.report

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.android.wildex.R

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
  val context = LocalContext.current
  val (title, message, confirmLabel) =
      when (action) {
        ReportActionToConfirm.CANCEL ->
            Triple(
                context.getString(R.string.report_details_cancel_first),
                context.getString(R.string.report_details_cancel_second),
                context.getString(R.string.report_details_cancel_third),
            )
        ReportActionToConfirm.SELF_ASSIGN ->
            Triple(
                context.getString(R.string.report_details_self_assign_first),
                context.getString(R.string.report_details_self_assign_second),
                context.getString(R.string.report_details_self_assign_third),
            )
        ReportActionToConfirm.RESOLVE ->
            Triple(
                context.getString(R.string.report_details_resolve_first),
                context.getString(R.string.report_details_resolve_second),
                context.getString(R.string.report_details_resolve_third),
            )
        ReportActionToConfirm.UNSELFASSIGN ->
            Triple(
                context.getString(R.string.report_details_unself_assign_first),
                context.getString(R.string.report_details_unself_assign_second),
                context.getString(R.string.report_details_unself_assign_third),
            )
      }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(title, fontWeight = FontWeight.Bold) },
      text = { Text(message) },
      confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
      dismissButton = {
        TextButton(onClick = onDismiss) {
          Text(context.getString(R.string.report_details_actions_dismiss))
        }
      },
  )
}
