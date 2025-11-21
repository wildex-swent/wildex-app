package com.android.wildex.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onConfirm: () -> Unit,
) {

  val title: String
  val message: String
  val animationRes: Int
  val context = LocalContext.current

  when (type) {
    ReportCompletionType.RESOLVED -> {
      title = context.getString(R.string.report_details_resolved_title)
      message = context.getString(R.string.report_details_resolved_message)
      animationRes = R.raw.success_confetti
    }
    ReportCompletionType.CANCELED -> {
      title = context.getString(R.string.report_details_canceled_title)
      message = context.getString(R.string.report_details_canceled_message)
      animationRes = R.raw.loader_cat
    }
  }

  val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationRes))
  val progress by
      animateLottieCompositionAsState(
          composition = composition,
          iterations = 1,
      )

  AlertDialog(
      onDismissRequest = onConfirm,
      containerColor = colorScheme.background,
      textContentColor = colorScheme.onBackground,
      titleContentColor = colorScheme.primary,
      title = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          LottieAnimation(
              composition = composition,
              progress = { progress },
              modifier = Modifier.size(160.dp).testTag(ReportCompletionDialogTestTags.ANIMATION),
          )
          Text(
              text = title,
              fontWeight = FontWeight.Bold,
              style = typography.titleMedium,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth().testTag(ReportCompletionDialogTestTags.TITLE),
          )
        }
      },
      text = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
              text = message,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth().testTag(ReportCompletionDialogTestTags.MESSAGE),
          )
        }
      },
      confirmButton = {
        TextButton(
            onClick = onConfirm,
            modifier = Modifier.testTag(ReportCompletionDialogTestTags.CONFIRM),
        ) {
          Text(context.getString(R.string.report_details_back_reports))
        }
      },
  )
}
