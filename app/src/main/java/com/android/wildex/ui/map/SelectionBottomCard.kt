package com.android.wildex.ui.map

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.android.wildex.R
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
    groupSize: Int = 1,
    groupIndex: Int = 0,
    onNext: () -> Unit = {},
    onPrev: () -> Unit = {},
) {
  if (selection == null) return
  val cs = colorScheme
  Surface(
      modifier =
          modifier.widthIn(min = 320.dp).wrapContentHeight().pointerInput(groupSize) {
            if (groupSize <= 1) {
              return@pointerInput
            }

            var totalDragX = 0f

            detectHorizontalDragGestures(
                onDragStart = { totalDragX = 0f },
                onHorizontalDrag = { _, dragAmount -> totalDragX += dragAmount },
                onDragEnd = {
                  val threshold = 80f

                  when {
                    totalDragX > threshold -> onPrev()
                    totalDragX < -threshold -> onNext()
                  }
                },
                onDragCancel = { totalDragX = 0f },
            )
          },
      shape = RoundedCornerShape(22.dp),
      color = cs.background,
      contentColor = cs.onBackground,
  ) {
    Box {
      Column(
          modifier = Modifier.fillMaxWidth(),
      ) {
        when (selection) {
          is PinDetails.PostDetails -> {
            PostSelectionCard(
                details = selection,
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
                onReport = onReport,
                onProfile = onProfile,
            )
          }
        }
        if (groupSize > 1) {
          Spacer(Modifier.height(4.dp))
          HorizontalDivider(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
              thickness = DividerDefaults.Thickness,
              color = cs.outlineVariant.copy(alpha = 0.3f))
          ClusterFooterPager(
              groupSize = groupSize,
              groupIndex = groupIndex,
              onNext = onNext,
              onPrev = onPrev,
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
        Icon(Icons.Filled.Close, contentDescription = "Close", tint = cs.onBackground)
      }
    }
  }
}

/**
 * Small footer pager for navigating items inside a cluster. Sits at the very bottom of the card,
 * visually separated from content.
 */
@Composable
private fun ClusterFooterPager(
    groupSize: Int,
    groupIndex: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit,
) {
  val cs = colorScheme

  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 6.dp)
              .testTag(MapContentTestTags.SELECTION_PAGER),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    IconButton(
        onClick = onPrev,
        modifier = Modifier.size(32.dp).testTag(MapContentTestTags.SELECTION_PAGER_PREV),
    ) {
      Icon(
          imageVector = Icons.Filled.ChevronLeft,
          contentDescription = "Previous",
          tint = cs.onBackground,
      )
    }

    // Centered, very lightweight counter (no chip background)
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center,
    ) {
      Text(
          text = "${groupIndex + 1} / $groupSize",
          style = typography.labelMedium,
          color = cs.onSurfaceVariant,
          modifier = Modifier.testTag(MapContentTestTags.SELECTION_PAGER_LABEL),
      )
    }

    IconButton(
        onClick = onNext,
        modifier = Modifier.size(32.dp).testTag(MapContentTestTags.SELECTION_PAGER_NEXT),
    ) {
      Icon(
          imageVector = Icons.Filled.ChevronRight,
          contentDescription = "Next",
          tint = cs.onBackground,
      )
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
 */
@Composable
private fun LocationRow(name: String) {
  Row(
      modifier = Modifier.testTag(MapContentTestTags.SELECTION_LOCATION),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Icon(
        Icons.Filled.LocationOn,
        contentDescription = "Location",
        tint = colorScheme.onBackground,
        modifier = Modifier.size(16.dp),
    )
    Text(
        text = name.ifEmpty { LocalContext.current.getString(R.string.map_unknown) },
        style = typography.bodySmall,
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
private fun OpenButton(onClick: () -> Unit, ui: Color, size: Int) {
  IconButton(
      onClick = onClick,
      modifier = Modifier.size(34.dp).testTag(MapContentTestTags.SELECTION_OPEN_BUTTON),
      colors = IconButtonDefaults.iconButtonColors(contentColor = ui),
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
 * @param onPost Callback invoked when the post is to be opened.
 * @param onToggleLike Callback invoked when the like button is toggled.
 * @param activeTab The currently active map tab.
 * @param isCurrentUser Boolean indicating if the current user is the author of the post.
 */
@Composable
private fun PostSelectionCard(
    details: PinDetails.PostDetails,
    onPost: (Id) -> Unit,
    onToggleLike: (Id) -> Unit,
    activeTab: MapTab = MapTab.Posts,
    isCurrentUser: Boolean,
    onProfile: (Id) -> Unit = {},
) {
  val cs = colorScheme
  val context = LocalContext.current
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
    val text = buildAnnotatedString {
      val isOwn = activeTab == MapTab.MyPosts && isCurrentUser
      val animalName = details.animalName
      val article = articleWithWord(animalName)
      val animal = animalName.trim().replaceFirstChar { it.uppercase() }
      val prefix =
          if (isOwn) {
            "${context.getString(R.string.map_you_saw)} "
          } else {
            "${details.author?.username ?: "${context.getString(R.string.map_someone)} "} ${context.getString(R.string.map_saw)} "
          }
      append(prefix)
      append("$article ")
      withStyle(SpanStyle(color = cs.primary, fontWeight = FontWeight.SemiBold)) { append(animal) }
    }
    Text(
        text = text,
        style = typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )

    LocationRow(details.post.location?.name ?: "")
    Spacer(Modifier.height(2.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
      val activeColor = if (details.likedByMe) cs.primary else cs.onBackground

      Row(
          modifier =
              Modifier.height(34.dp)
                  .border(width = 1.dp, color = activeColor, shape = RoundedCornerShape(50))
                  .padding(horizontal = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
            imageVector =
                if (details.likedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = "Like",
            tint = activeColor,
            modifier =
                Modifier.size(20.dp).testTag(MapContentTestTags.SELECTION_LIKE_BUTTON).clickable {
                  onToggleLike(details.post.postId)
                },
        )

        Text(
            text = details.likeCount.toString(),
            style = typography.bodySmall,
            color = activeColor,
        )
      }

      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
            Icons.Filled.ChatBubbleOutline,
            contentDescription = "Comments",
            tint = cs.onBackground,
            modifier = Modifier.size(28.dp).testTag(MapContentTestTags.SELECTION_COMMENT_ICON),
        )

        Text(
            text = details.commentCount.toString(),
            style = typography.bodySmall,
            color = cs.onBackground,
        )
      }

      OpenButton(
          onClick = { onPost(details.post.postId) },
          ui = cs.onBackground,
          size = 28,
      )
    }
  }
}

/**
 * Composable that displays a report selection card with details and actions.
 *
 * @param details The report details to be displayed.
 * @param onReport Callback invoked when the report is to be opened.
 */
@Composable
private fun ReportSelectionCard(
    details: PinDetails.ReportDetails,
    onReport: (Id) -> Unit,
    onProfile: (Id) -> Unit = {},
) {
  val cs = colorScheme
  val context = LocalContext
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
              withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = cs.primary)) {
                append(
                    "${details.author?.username ?: context.current.getString(R.string.map_someone)} ${context.current.getString(R.string.map_reported)}")
              }
              append(" ${details.report.description}")
            },
        style = typography.titleMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().testTag(MapContentTestTags.SELECTION_REPORT_DESCRIPTION),
    )

    LocationRow(details.report.location.name)

    Row(
        modifier = Modifier.fillMaxWidth().testTag(MapContentTestTags.REPORT_ASSIGNED_ROW),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      val assigned = !details.report.assigneeId.isNullOrBlank() && details.assignee != null
      val bg =
          if (assigned) cs.onBackground.copy(alpha = 0.08f) else cs.onBackground.copy(alpha = 0.12f)
      val fg = cs.background

      Surface(
          shape = RoundedCornerShape(percent = 40),
          color = bg,
          contentColor = fg,
          modifier = Modifier.weight(1f).wrapContentWidth(),
      ) {
        if (!assigned) {
          Text(
              text = context.current.getString(R.string.map_not_assigned),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              style = typography.bodySmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              color = cs.onBackground,
          )
        } else {
          Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
                text = context.current.getString(R.string.map_assigned_to),
                style = typography.bodySmall,
                maxLines = 1,
                color = cs.onBackground,
            )
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

            Text(
                text =
                    details.assignee?.username ?: context.current.getString(R.string.map_unknown),
                style = typography.bodySmall,
                color = cs.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
          }
        }
      }

      OpenButton(
          onClick = { onReport(details.report.reportId) },
          ui = cs.onBackground,
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
  return article
}
