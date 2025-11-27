package com.android.wildex.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
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
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.utils.ClickableProfilePicture

/**
 * Composable that displays a bottom card with details about the selected pin.
 *
 * @param modifier Modifier to be applied to the bottom card.
 * @param selection Details of the selected pin.
 * @param activeTab The currently active map tab.
 * @param onPost Callback invoked when the post is to be opened.
 * @param onReport Callback invoked when the report is to be opened.
 * @param onDismiss Callback invoked when the card is dismissed.
 * @param onToggleLike Callback invoked when the like button is toggled.
 * @param isCurrentUser Boolean indicating if the current user is the author of the post.
 */
@Composable
fun SelectionBottomCard(
    modifier: Modifier,
    selection: PinDetails?,
    activeTab: MapTab,
    onPost: (Id) -> Unit,
    onReport: (Id) -> Unit,
    onDismiss: () -> Unit,
    onToggleLike: (Id) -> Unit,
    onProfile: (Id) -> Unit = {},
    isCurrentUser: Boolean,
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
              isCurrentUser = isCurrentUser,
              onProfile = onProfile,
          )
        }
        is PinDetails.ReportDetails -> {
          ReportSelectionCard(
              details = selection,
              ui = ui,
              onReport = onReport,
              onProfile = onProfile,
          )
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

/**
 * Composable that displays a preview image with rounded corners.
 *
 * @param data The image data to be loaded.
 * @param tag The test tag for the image.
 * @param desc The content description for the image.
 */
@Composable
private fun PreviewImage(data: Any?, tag: String, desc: String) {
  AsyncImage(
      model = ImageRequest.Builder(LocalContext.current).data(data).crossfade(true).build(),
      contentDescription = desc,
      contentScale = ContentScale.Crop,
      modifier = Modifier.size(135.dp).clip(RoundedCornerShape(18.dp)).testTag(tag),
  )
}

/**
 * Composable that displays a location row with an icon and name.
 *
 * @param name The name of the location.
 * @param ui The UI colors for the map tab.
 */
@Composable
private fun LocationRow(name: String, ui: MapUiColors) {
  Row(
      modifier = Modifier.testTag(MapContentTestTags.SELECTION_LOCATION),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Icon(
        Icons.Filled.LocationOn,
        contentDescription = "Location",
        tint = ui.fg,
        modifier = Modifier.size(18.dp),
    )
    Text(
        text = name.ifEmpty { "Unknown" },
        style = typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
  }
}

/**
 * Composable that displays an open button with an icon.
 *
 * @param onClick Callback invoked when the button is clicked.
 * @param ui The UI colors for the map tab.
 * @param size The size of the icon.
 */
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

/**
 * Composable that displays a selection row with a left component and content.
 *
 * @param left The left composable component.
 * @param content The content composable component.
 */
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

/**
 * Composable that displays a post selection card with details and actions.
 *
 * @param details The post details to be displayed.
 * @param ui The UI colors for the map tab.
 * @param onPost Callback invoked when the post is to be opened.
 * @param onToggleLike Callback invoked when the like button is toggled.
 * @param activeTab The currently active map tab.
 * @param isCurrentUser Boolean indicating if the current user is the author of the post.
 */
@Composable
private fun PostSelectionCard(
    details: PinDetails.PostDetails,
    ui: MapUiColors,
    onPost: (Id) -> Unit,
    onToggleLike: (Id) -> Unit,
    activeTab: MapTab = MapTab.Posts,
    isCurrentUser: Boolean,
    onProfile: (Id) -> Unit = {},
) {
  SelectionRow(
      left = {
        PreviewImage(details.post.pictureURL, MapContentTestTags.SELECTION_POST_IMAGE, "Post image")
      },
  ) {
    ClickableProfilePicture(
        modifier = Modifier.size(48.dp).testTag(MapContentTestTags.SELECTION_AUTHOR_IMAGE),
        profileId = details.author?.userId ?: "",
        profilePictureURL = details.author?.profilePictureURL ?: "",
        profileUserType = details.author?.userType ?: UserType.REGULAR,
        onProfile = onProfile,
    )

    Text(
        text =
            if (activeTab == MapTab.MyPosts && isCurrentUser)
                "You saw ${articleWithWord(details.animalName)}"
            else
                "${details.author?.username ?: "Someone"} saw ${articleWithWord(details.animalName)}",
        style = typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )

    LocationRow(details.post.location?.name ?: "", ui)
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
          if (details.likedByMe) Icon(Icons.Filled.Favorite, contentDescription = "Unlike")
          else Icon(Icons.Filled.FavoriteBorder, contentDescription = "Like")
        }

        Text(
            text = details.post.likesCount.toString(),
            style = typography.bodySmall,
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
            style = typography.bodySmall,
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

/**
 * Composable that displays a report selection card with details and actions.
 *
 * @param details The report details to be displayed.
 * @param ui The UI colors for the map tab.
 * @param onReport Callback invoked when the report is to be opened.
 */
@Composable
private fun ReportSelectionCard(
    details: PinDetails.ReportDetails,
    ui: MapUiColors,
    onReport: (Id) -> Unit,
    onProfile: (Id) -> Unit = {},
) {
  SelectionRow(
      left = {
        PreviewImage(
            details.report.imageURL,
            MapContentTestTags.SELECTION_REPORT_IMAGE,
            "Report image",
        )
      },
  ) {
    ClickableProfilePicture(
        modifier = Modifier.size(48.dp).testTag(MapContentTestTags.SELECTION_AUTHOR_IMAGE),
        profileId = details.author?.userId ?: "",
        profilePictureURL = details.author?.profilePictureURL ?: "",
        profileUserType = details.author?.userType ?: UserType.REGULAR,
        onProfile = onProfile,
    )

    // Centered description text
    Text(
        text =
            buildAnnotatedString {
              withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("${details.author?.username ?: "Someone"} reported:")
              }
              append(" ${details.report.description}")
            },
        style = typography.titleMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().testTag(MapContentTestTags.SELECTION_REPORT_DESCRIPTION),
    )

    LocationRow(details.report.location.name, ui)

    Row(
        modifier = Modifier.fillMaxWidth().testTag(MapContentTestTags.REPORT_ASSIGNED_ROW),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      val assigned = !details.report.assigneeId.isNullOrBlank() && details.assignee != null
      val bg = if (assigned) ui.fg.copy(alpha = 0.08f) else ui.fg.copy(alpha = 0.12f)
      val fg = ui.fg

      Surface(
          shape = RoundedCornerShape(percent = 40),
          color = bg,
          contentColor = fg,
          modifier = Modifier.weight(1f).wrapContentWidth(),
      ) {
        if (!assigned) {
          Text(
              text = "Not assigned :(",
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              style = typography.bodySmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
          )
        } else {
          Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(text = "Assigned to", style = typography.bodySmall, maxLines = 1)
            // tiny inline avatar
            AsyncImage(
                model =
                    ImageRequest.Builder(LocalContext.current)
                        .data(details.assignee?.profilePictureURL)
                        .build(),
                contentDescription = "Assignee",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(16.dp).clip(CircleShape),
            )
            // username, single line with ellipsis
            Text(
                text = details.assignee?.username ?: "Unknown",
                style = typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
          }
        }
      }

      OpenButton(
          onClick = { onReport(details.report.reportId) },
          ui = ui,
          size = 26,
      )
    }
  }
}

/**
 * Returns the appropriate indefinite article ("a" or "an") for the given word.
 *
 * @param word The word to evaluate.
 * @return A string containing the article and the word in lowercase.
 */
fun articleWithWord(word: String): String {
  if (word.isBlank()) return "a"
  val first = word.trim().first().lowercaseChar()
  val article = if (first in listOf('a', 'e', 'i', 'o', 'u')) "an" else "a"
  return "$article ${word.lowercase()}"
}
