package com.android.wildex.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.android.wildex.R

object ReportCompletionDialogTestTags {
  const val TITLE = "completion_dialog_title"
  const val MESSAGE = "completion_dialog_message"
  const val CONFIRM = "completion_dialog_confirm"
  const val ANIMATION = "completion_dialog_animation"
}

@Composable
fun ReportCompletionDialog(
    type: ReportCompletionType,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {

  val title: String
  val message: String
  val animationRes: Int

  when (type) {
    ReportCompletionType.RESOLVED -> {
      title = "Resolved. Thanks for handling the situation."
      message =
          "With everything taken care of, you can head back to the reports screen whenever you're ready."
      animationRes = R.raw.success_confetti
    }
    ReportCompletionType.CANCELED -> {
      title = "Canceled. If the situation evolves, feel free to reach out again."
      message = "In the meantime, feel free to return to the reports screen whenever you want."
      animationRes = R.raw.loader_cat
    }
  }

  // Lottie animation state
  val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationRes))
  val progress by
      animateLottieCompositionAsState(
          composition = composition,
          iterations = 1,
      )

  AlertDialog(
      onDismissRequest = onDismiss,
      text = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          // --- Animation ---
          LottieAnimation(
              composition = composition,
              progress = { progress },
              modifier = Modifier.size(160.dp).testTag(ReportCompletionDialogTestTags.ANIMATION),
          )

          Spacer(Modifier.height(12.dp))

          // --- Title ---
          Text(
              text = title,
              fontWeight = FontWeight.Bold,
              modifier =
                  Modifier.padding(horizontal = 8.dp).testTag(ReportCompletionDialogTestTags.TITLE),
          )

          Spacer(Modifier.height(6.dp))

          // --- Message ---
          Text(
              text = message,
              modifier =
                  Modifier.padding(horizontal = 8.dp)
                      .testTag(ReportCompletionDialogTestTags.MESSAGE),
          )
        }
      },
      confirmButton = {
        TextButton(
            onClick = onConfirm,
            modifier = Modifier.testTag(ReportCompletionDialogTestTags.CONFIRM),
        ) {
          Text("Back to reports")
        }
      },
  )
}
