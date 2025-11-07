package com.android.wildex.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.wildex.model.map.PinDetails
import com.android.wildex.model.utils.Id

@Composable
fun SelectionBottomCard(
    modifier: Modifier,
    selection: PinDetails?,
    activeTab: MapTab,
    onPost: (Id) -> Unit,
    onReport: (Id) -> Unit,
    onDismiss: () -> Unit,
    onToggleLike: (Id) -> Unit,
) {
  if (selection == null) return
  val cs = MaterialTheme.colorScheme
  val ui = colorsForMapTab(activeTab, cs)

  Surface(
      modifier = modifier.widthIn(min = 320.dp).wrapContentHeight(),
      shape = RoundedCornerShape(22.dp),
      tonalElevation = 10.dp,
      color = ui.bg,
      contentColor = ui.fg,
  ) {
    Box {
      when (selection) {
        is PinDetails.PostDetails -> {
          PostSelectionCard(
              details = selection,
              ui = ui,
              onPost = onPost,
              onToggleLike = onToggleLike,
              activeTab = activeTab,
          )
        }
        is PinDetails.ReportDetails -> {
          ReportSelectionCard(details = selection, ui = ui, onReport = onReport)
        }
      }
      IconButton(
          onClick = onDismiss,
          modifier =
              Modifier.align(Alignment.TopEnd)
                  .padding(6.dp)
                  .testTag(MapContentTestTags.SELECTION_CLOSE),
      ) {
        Icon(Icons.Filled.Close, contentDescription = "Close", tint = ui.fg)
      }
    }
  }
}

@Composable
private fun PreviewImage(data: Any?, tag: String, desc: String) {
  AsyncImage(
      model = ImageRequest.Builder(LocalContext.current).data(data).crossfade(true).build(),
      contentDescription = desc,
      contentScale = ContentScale.Crop,
      modifier = Modifier.size(135.dp).clip(RoundedCornerShape(18.dp)).testTag(tag),
  )
}

@Composable
private fun AuthorAvatar(data: Any?) {
  AsyncImage(
      model = ImageRequest.Builder(LocalContext.current).data(data).build(),
      contentDescription = "Author",
      contentScale = ContentScale.Crop,
      modifier =
          Modifier.size(48.dp)
              .clip(RoundedCornerShape(28.dp))
              .testTag(MapContentTestTags.SELECTION_AUTHOR_IMAGE),
  )
}

@Composable
private fun LocationRow(name: String, ui: MapUiColors) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Icon(
        Icons.Filled.LocationOn,
        contentDescription = "Location",
        tint = ui.fg,
        modifier = Modifier.size(18.dp))
    Text(
        text = name.ifEmpty { "Unknown" },
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun OpenButton(onClick: () -> Unit, ui: MapUiColors, size: Int) {
  IconButton(
      onClick = onClick,
      modifier = Modifier.size(34.dp).testTag(MapContentTestTags.SELECTION_OPEN_BUTTON),
      colors = IconButtonDefaults.iconButtonColors(contentColor = ui.fg),
  ) {
    Icon(
        Icons.AutoMirrored.Filled.OpenInNew,
        contentDescription = "Open",
        modifier = Modifier.size(size.dp),
    )
  }
}

@Composable
private fun SelectionRow(
    left: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
  Row(
      modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    left()
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
  }
}

@Composable
private fun PostSelectionCard(
    details: PinDetails.PostDetails,
    ui: MapUiColors,
    onPost: (Id) -> Unit,
    onToggleLike: (Id) -> Unit,
    activeTab: MapTab = MapTab.Posts,
) {
  SelectionRow(
      left = {
        PreviewImage(details.post.pictureURL, MapContentTestTags.SELECTION_POST_IMAGE, "Post image")
      },
  ) {
    AuthorAvatar(details.author?.profilePictureURL)

    Text(
        text =
            if (activeTab == MapTab.MyPosts) "You saw ${articleWithWord(details.animalName)}"
            else
                "${details.author?.username ?: "Someone"} saw ${articleWithWord(details.animalName)}",
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )

    LocationRow(details.post.location?.name ?: "Unknown", ui)
    Spacer(Modifier.height(2.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        IconButton(
            onClick = { onToggleLike(details.post.postId) },
            modifier = Modifier.size(34.dp).testTag(MapContentTestTags.SELECTION_LIKE_BUTTON),
            colors = IconButtonDefaults.iconButtonColors(contentColor = ui.fg),
        ) {
          if (details.likedByMe)
              Icon(Icons.Filled.Favorite, contentDescription = "Unlike", tint = ui.fg)
          else Icon(Icons.Filled.FavoriteBorder, contentDescription = "Like", tint = ui.fg)
        }

        Text(
            text = details.post.likesCount.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = ui.fg,
        )
      }

      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
            Icons.Filled.ChatBubbleOutline,
            contentDescription = "Comments",
            tint = ui.fg,
            modifier = Modifier.size(28.dp).testTag(MapContentTestTags.SELECTION_COMMENT_ICON),
        )

        Text(
            text = details.post.commentsCount.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = ui.fg,
        )
      }

      OpenButton(
          onClick = { onPost(details.post.postId) },
          ui = ui,
          size = 28,
      )
    }
  }
}

@Composable
private fun ReportSelectionCard(
    details: PinDetails.ReportDetails,
    ui: MapUiColors,
    onReport: (Id) -> Unit,
) {
  SelectionRow(
      left = {
        PreviewImage(
            details.report.imageURL, MapContentTestTags.SELECTION_REPORT_IMAGE, "Report image")
      },
  ) {
    AuthorAvatar(details.author?.profilePictureURL)

    // Centered description text
    Text(
        text =
            buildAnnotatedString {
              withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("${details.author?.username ?: "Someone"} reported:")
              }
              append(" ${details.report.description}")
            },
        style = MaterialTheme.typography.titleMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth())

    LocationRow(details.report.location.name, ui)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
      val assignee = details.report.assigneeId
      val (bg, fg, label) =
          if (assignee.isNullOrBlank()) {
            Triple(ui.fg.copy(alpha = 0.12f), ui.fg, "Not assigned")
          } else {
            Triple(ui.fg.copy(alpha = 0.08f), ui.fg, "Assigned")
          }

      Surface(shape = RoundedCornerShape(999.dp), color = bg, contentColor = fg) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      }

      Box(modifier = Modifier.align(Alignment.CenterEnd)) {
        OpenButton(
            onClick = { onReport(details.report.reportId) },
            ui = ui,
            size = 26,
        )
      }
    }
  }
}

fun articleWithWord(word: String): String {
  if (word.isBlank()) return "a"
  val first = word.trim().first().lowercaseChar()
  val article = if (first in listOf('a', 'e', 'i', 'o', 'u')) "an" else "a"
  return "$article $word"
}
