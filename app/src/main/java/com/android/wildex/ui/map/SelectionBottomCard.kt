package com.android.wildex.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
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
      IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) {
        Icon(Icons.Filled.Close, contentDescription = "Close", tint = ui.fg)
      }
    }
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
  Row(
      Modifier.padding(start = 12.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    AsyncImage(
        model =
            ImageRequest.Builder(LocalContext.current)
                .data(details.post.pictureURL)
                .crossfade(true)
                .build(),
        contentDescription = "Post image",
        contentScale = ContentScale.Crop,
        modifier =
            Modifier.size(135.dp).clip(RoundedCornerShape(18.dp)).semantics {
              contentDescription = "${MapScreenTestTags.SELECTION_CARD}/Image"
            },
    )

    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        AsyncImage(
            model =
                ImageRequest.Builder(LocalContext.current)
                    .data(details.author?.profilePictureURL)
                    .build(),
            contentDescription = "Author",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(28.dp)),
        )

        Text(
            text =
                if (activeTab == MapTab.MyPosts) "You saw ${articleWithWord(details.animalName)}"
                else
                    "${details.author?.username ?: "Someone"} saw ${articleWithWord(details.animalName)}",
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
              imageVector = Icons.Filled.LocationOn,
              contentDescription = "Location",
              tint = ui.fg,
              modifier = Modifier.size(18.dp),
          )
          val loc = details.post.location
          Text(
              text = loc!!.name.ifEmpty { "Unknown" },
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
          )
        }
      }
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
              modifier = Modifier.size(34.dp),
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
              modifier = Modifier.size(28.dp),
          )

          Text(
              text = details.post.commentsCount.toString(),
              style = MaterialTheme.typography.bodySmall,
              color = ui.fg,
          )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          IconButton(
              onClick = { onPost(details.post.postId) },
              modifier = Modifier.size(34.dp),
              colors = IconButtonDefaults.iconButtonColors(contentColor = ui.fg),
          ) {
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open post",
                tint = ui.fg,
                modifier = Modifier.size(28.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StatIconWithCount(icon: @Composable () -> Unit, count: Int, ui: MapUiColors) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    icon()
    Text(text = count.toString(), style = MaterialTheme.typography.bodySmall, color = ui.fg)
  }
}

@Composable
private fun ReportSelectionCard(
    details: PinDetails.ReportDetails,
    ui: MapUiColors,
    onReport: (Id) -> Unit,
) {
  Row(
      modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    AsyncImage(
        model =
            ImageRequest.Builder(LocalContext.current)
                .data(details.report.imageURL)
                .crossfade(true)
                .build(),
        contentDescription = "Report image",
        contentScale = ContentScale.Crop,
        modifier =
            Modifier.size(135.dp).clip(RoundedCornerShape(18.dp)).semantics {
              contentDescription = "${MapScreenTestTags.SELECTION_CARD}/Image"
            },
    )

    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      AsyncImage(
          model =
              ImageRequest.Builder(LocalContext.current)
                  .data(details.author?.profilePictureURL)
                  .build(),
          contentDescription = "Reporter",
          contentScale = ContentScale.Crop,
          modifier = Modifier.size(48.dp).clip(RoundedCornerShape(28.dp)),
      )
      Text(
          text = "${details.author?.username ?: "Someone"} reported: ${details.report.description}",
          style = MaterialTheme.typography.titleMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
      )

      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = "Location",
            tint = ui.fg,
            modifier = Modifier.size(18.dp),
        )
        val loc = details.report.location
        Text(
            text = loc.name.ifEmpty { "Unknown" },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      }
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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

        Spacer(Modifier.weight(1f))

        IconButton(
            onClick = { onReport(details.report.reportId) },
            modifier = Modifier.size(34.dp),
            colors = IconButtonDefaults.iconButtonColors(contentColor = ui.fg),
        ) {
          Icon(
              Icons.AutoMirrored.Filled.OpenInNew,
              contentDescription = "Open report",
              tint = ui.fg,
              modifier = Modifier.size(26.dp),
          )
        }
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
