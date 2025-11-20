package com.android.wildex.ui.utils.expand

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.wildex.R

@Composable
fun ExpandableTextCore(
    text: String,
    collapsedLines: Int,
    bodyTag: String,
    toggleTag: String,
    style: TextStyle = typography.bodyMedium,
    color: Color = colorScheme.onBackground,
) {
  var expanded by remember { mutableStateOf(false) }
  var hasOverflow by remember { mutableStateOf(false) }
  val context = LocalContext.current

  Column {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { result -> hasOverflow = result.hasVisualOverflow },
        modifier = Modifier.testTag(bodyTag),
    )

    if (hasOverflow || expanded) {
      Spacer(Modifier.height(2.dp))
      Text(
          text =
              if (expanded) context.getString(R.string.report_details_show_less)
              else context.getString(R.string.report_details_read_more),
          style = typography.labelSmall,
          color = colorScheme.tertiary,
          modifier =
              Modifier.testTag(toggleTag).align(Alignment.End).clickable { expanded = !expanded },
      )
    }
  }
}
