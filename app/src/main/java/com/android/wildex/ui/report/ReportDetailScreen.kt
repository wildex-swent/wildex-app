package com.android.wildex.ui.report

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.utils.ClickableProfilePicture
import com.android.wildex.ui.utils.expand.ExpandableTextCore

object ReportDetailsScreenTestTags {
  const val SCREEN = "report_details_screen"
  const val PULL_TO_REFRESH = "report_details_pull_to_refresh"
  const val CONTENT_LIST = "report_details_content_list"
  const val BACK_BUTTON = "report_details_back_button"
  const val HERO_IMAGE = "report_details_hero_image"
  const val INFO_BAR = "report_details_info_bar"
  const val INFO_AUTHOR_NAME = "report_details_author_name"
  const val INFO_AUTHOR_PICTURE = "report_details_author_picture"
  const val INFO_DATE = "report_details_date"
  const val INFO_LOCATION_PILL = "report_details_location_pill"
  const val DESCRIPTION_CARD = "report_details_description_card"
  const val DESCRIPTION_TEXT = "report_details_description_text"
  const val DESCRIPTION_TOGGLE = "report_details_description_toggle"
  const val ASSIGNEE_CARD = "report_details_assignee_card"
  const val ASSIGNEE_TEXT = "report_details_assignee_text"
  const val COMMENTS_HEADER = "report_details_comments_header"
  const val COMMENTS_COUNT = "report_details_comments_count"
  const val COMMENT_CARD = "report_details_comment_card"
  const val COMMENT_AUTHOR = "report_details_comment_author"
  const val COMMENT_DATE = "report_details_comment_date"
  const val COMMENT_BODY = "report_details_comment_body"
  const val COMMENT_TOGGLE = "report_details_comment_toggle"
  const val COMMENT_INPUT_BAR = "report_details_comment_input_bar"
  const val COMMENT_INPUT_FIELD = "report_details_comment_input_field"
  const val COMMENT_INPUT_SEND = "report_details_comment_input_send"
}

/**
 * Full-screen report details view.
 *
 * @param reportId The ID of the report to display.
 * @param reportDetailsViewModel The ViewModel managing the report details state.
 * @param onGoBack Callback when the user wants to go back.
 * @param onProfile Callback when the user wants to view a profile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailsScreen(
    reportId: Id,
    reportDetailsViewModel: ReportDetailsScreenViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onProfile: (Id) -> Unit = {},
) {
  val uiState by reportDetailsViewModel.uiState.collectAsState()
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current

  var showCompletionDialog by remember { mutableStateOf(false) }
  var completionType by remember { mutableStateOf<ReportCompletionType?>(null) }
  var showNavigationSheet by remember { mutableStateOf(false) }
  var pendingAction by remember { mutableStateOf<ReportActionToConfirm?>(null) }

  // Initial load
  LaunchedEffect(Unit) { reportDetailsViewModel.loadReportDetails(reportId) }

  // Error toast
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      reportDetailsViewModel.clearErrorMsg()
    }
  }

  // events -> popup
  LaunchedEffect(Unit) {
    reportDetailsViewModel.events.collect { event ->
      handleReportDetailsEvent(
          event = event,
          setCompletionType = { completionType = it },
          setShowCompletionDialog = { showCompletionDialog = it },
      )
    }
  }

  // Completion dialog
  if (showCompletionDialog && completionType != null) {
    ReportCompletionDialog(
        type = completionType!!,
        onConfirm = {
          showCompletionDialog = false
          onGoBack()
        },
    )
  }

  if (pendingAction != null) {
    ReportActionConfirmDialog(
        action = pendingAction!!,
        onDismiss = { pendingAction = null },
        onConfirm = {
          val actionToRun = pendingAction
          pendingAction = null
          runPendingReportAction(actionToRun, reportDetailsViewModel)
        },
    )
  }

  if (showNavigationSheet) {
    NavigationOptionsBottomSheet(
        latitude = uiState.location.latitude,
        longitude = uiState.location.longitude,
        displayLabel = uiState.location.name,
        onDismissRequest = { showNavigationSheet = false },
    )
  }

  Scaffold(
      modifier = Modifier.testTag(ReportDetailsScreenTestTags.SCREEN),
      topBar = { ReportDetailsTopBar(onGoBack = onGoBack) },
      bottomBar = {
        ReportCommentInput(
            user = uiState.currentUser,
            onProfile = onProfile,
            onSend = { text -> reportDetailsViewModel.addComment(text) },
        )
      },
  ) { innerPadding ->
    val pullState = rememberPullToRefreshState()

    PullToRefreshBox(
        state = pullState,
        isRefreshing = uiState.isRefreshing,
        modifier =
            Modifier.fillMaxSize()
                .padding(innerPadding)
                .testTag(ReportDetailsScreenTestTags.PULL_TO_REFRESH)
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
        onRefresh = {
          if (!showCompletionDialog) {
            reportDetailsViewModel.refreshReportDetails(reportId)
          }
        },
    ) {
      when {
        uiState.isError -> LoadingFail()
        uiState.isLoading -> LoadingScreen()
        else ->
            ReportDetailsContent(
                uiState = uiState,
                onProfile = onProfile,
                onCancel = { pendingAction = ReportActionToConfirm.CANCEL },
                onSelfAssign = { pendingAction = ReportActionToConfirm.SELF_ASSIGN },
                onResolve = { pendingAction = ReportActionToConfirm.RESOLVE },
                onUnSelfAssign = { pendingAction = ReportActionToConfirm.UNSELFASSIGN },
                onLocationClick = { showNavigationSheet = true },
            )
      }
    }
  }
}

/**
 * Handle events from the ViewModel that require showing dialogs.
 *
 * @param event The event to handle.
 * @param setCompletionType Callback to set the type of completion dialog to show.
 * @param setShowCompletionDialog Callback to show or hide the completion dialog.
 */
private fun handleReportDetailsEvent(
    event: ReportDetailsEvent,
    setCompletionType: (ReportCompletionType) -> Unit,
    setShowCompletionDialog: (Boolean) -> Unit,
) {
  when (event) {
    is ReportDetailsEvent.ShowCompletion -> {
      setCompletionType(event.type)
      setShowCompletionDialog(true)
    }
  }
}

/**
 * Run the action that was pending confirmation.
 *
 * @param action The action to run.
 * @param viewModel The ViewModel managing the report details state.
 */
private fun runPendingReportAction(
    action: ReportActionToConfirm?,
    viewModel: ReportDetailsScreenViewModel,
) {
  when (action) {
    ReportActionToConfirm.CANCEL -> viewModel.cancelReport()
    ReportActionToConfirm.SELF_ASSIGN -> viewModel.selfAssignReport()
    ReportActionToConfirm.RESOLVE -> viewModel.resolveReport()
    ReportActionToConfirm.UNSELFASSIGN -> viewModel.unselfAssignReport()
    null -> Unit
  }
}

/**
 * Main content when the report is loaded. Hero image + sheet + description + assignee + comments.
 *
 * @param uiState The UI state of the report details screen.
 * @param onProfile Callback when the user wants to view a profile.
 * @param onCancel Callback when the user wants to cancel the report.
 * @param onSelfAssign Callback when the user wants to self-assign the report.
 * @param onResolve Callback when the user wants to resolve the report.
 * @param onUnSelfAssign Callback when the user wants to un-self-assign the report
 * @param onLocationClick Callback when the user clicks on the location pill.
 */
@Composable
private fun ReportDetailsContent(
    uiState: ReportDetailsUIState,
    onProfile: (Id) -> Unit = {},
    onCancel: () -> Unit = {},
    onSelfAssign: () -> Unit = {},
    onResolve: () -> Unit = {},
    onUnSelfAssign: () -> Unit = {},
    onLocationClick: () -> Unit = {},
) {
  Box(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(ReportDetailsScreenTestTags.CONTENT_LIST),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
      item { ReportPicture(uiState.imageURL) }
      item {
        Surface(
            color = colorScheme.background,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
              modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          ) {
            Spacer(Modifier.height(8.dp))

            ReportInfoBar(
                author = uiState.author,
                date = uiState.date,
                location = uiState.location.name,
                onProfile = onProfile,
                onLocationClick = onLocationClick,
            )

            Spacer(Modifier.height(8.dp))

            if (uiState.description.isNotBlank()) {
              Card(
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(horizontal = 16.dp)
                          .testTag(ReportDetailsScreenTestTags.DESCRIPTION_CARD),
                  shape = RoundedCornerShape(24.dp),
                  colors =
                      CardDefaults.cardColors(
                          containerColor = colorScheme.surfaceVariant,
                      ),
                  elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  ExpandableTextCore(
                      text = uiState.description,
                      collapsedLines = 4,
                      style = typography.bodyMedium,
                      color = colorScheme.onSurfaceVariant,
                      bodyTag = ReportDetailsScreenTestTags.DESCRIPTION_TEXT,
                      toggleTag = ReportDetailsScreenTestTags.DESCRIPTION_TOGGLE,
                  )
                }
              }

              Spacer(Modifier.height(10.dp))
            }

            uiState.assignee?.let { assignee ->
              ReportAssigneeDetailsCard(
                  assignee = assignee,
                  onProfile = onProfile,
              )
              Spacer(Modifier.height(6.dp))
            }
            ReportDetailsActionRow(
                hasAssignee = uiState.assignee != null,
                currentUser = uiState.currentUser,
                isCreatedByCurrentUser = uiState.isCreatedByCurrentUser,
                isAssignedToCurrentUser = uiState.isAssignedToCurrentUser,
                isActionInProgress = uiState.isActionInProgress,
                onCancel = onCancel,
                onSelfAssign = onSelfAssign,
                onResolve = onResolve,
                onUnSelfAssign = onUnSelfAssign,
            )

            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag(ReportDetailsScreenTestTags.COMMENTS_HEADER),
            ) {
              val count = uiState.commentsUI.size
              val label =
                  pluralStringResource(
                      id = R.plurals.report_details_comments_count,
                      count = count,
                      count,
                  )
              Text(
                  text = label,
                  style = typography.titleSmall,
                  color = colorScheme.onBackground,
                  modifier = Modifier.testTag(ReportDetailsScreenTestTags.COMMENTS_COUNT),
              )
            }
          }
        }
      }

      items(uiState.commentsUI) { commentUI ->
        ReportCommentRow(commentUI = commentUI, onProfile = onProfile)
      }

      item { Spacer(Modifier.height(96.dp)) }
    }
  }
}

/**
 * Big header picture at the top, with subtle gradient similar to PostDetails.
 *
 * @param imageURL The URL of the image to display.
 */
@Composable
private fun ReportPicture(imageURL: URL) {
  Box(
      Modifier.fillMaxWidth(),
  ) {
    AsyncImage(
        model = imageURL,
        contentDescription = "Report image",
        contentScale = ContentScale.FillWidth,
        modifier = Modifier.fillMaxWidth().testTag(ReportDetailsScreenTestTags.HERO_IMAGE),
    )
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(72.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.7f),
                        1f to Color.Transparent,
                    ),
                ))
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(72.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to colorScheme.background,
                    ),
                ))
  }
}

/**
 * Header row: avatar + author + date + location tag.
 *
 * @param author The author of the report.
 * @param date The date the report was created.
 * @param location The location string of the report.
 * @param onProfile Callback when the user clicks on the author's profile.
 * @param onLocationClick Callback when the user clicks on the location pill.
 */
@Composable
private fun ReportInfoBar(
    author: SimpleUser,
    date: String,
    location: String,
    onProfile: (Id) -> Unit = {},
    onLocationClick: () -> Unit = {},
) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 10.dp)
              .testTag(ReportDetailsScreenTestTags.INFO_BAR),
      horizontalArrangement = Arrangement.Start,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
    ) {
      ClickableProfilePicture(
          modifier = Modifier.size(48.dp).testTag(ReportDetailsScreenTestTags.INFO_AUTHOR_PICTURE),
          profileId = author.userId,
          profilePictureURL = author.profilePictureURL,
          profileUserType = author.userType,
          onProfile = onProfile,
      )

      Column(modifier = Modifier.weight(1f)) {
        Text(
            text =
                author.username.ifBlank {
                  LocalContext.current.getString(R.string.report_details_bar_title)
                },
            style = typography.titleMedium,
            color = colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(ReportDetailsScreenTestTags.INFO_AUTHOR_NAME),
        )
        if (date.isNotBlank()) {
          Spacer(Modifier.height(2.dp))
          Text(
              text = date,
              color = colorScheme.onBackground,
              style = typography.labelMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag(ReportDetailsScreenTestTags.INFO_DATE),
          )
        }
      }
    }

    if (location.isNotBlank()) {
      Spacer(Modifier.width(8.dp))
      LocationTagButton(
          location = location,
          onClick = onLocationClick,
      )
    }
  }
}

/**
 * Clickable location “pill” in the header row.
 *
 * @param location The location string to display.
 * @param onClick Callback when the pill is clicked.
 */
@Composable
private fun LocationTagButton(
    location: String,
    onClick: () -> Unit = {},
) {
  Surface(
      shape = RoundedCornerShape(32.dp),
      color = colorScheme.background,
      border = BorderStroke(1.dp, colorScheme.primary),
      tonalElevation = 0.dp,
      modifier =
          Modifier.testTag(ReportDetailsScreenTestTags.INFO_LOCATION_PILL).widthIn(max = 140.dp),
  ) {
    Row(
        modifier =
            Modifier.clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
          imageVector = Icons.Filled.LocationOn,
          contentDescription = "Location",
          tint = colorScheme.primary,
          modifier = Modifier.size(18.dp),
      )
      Text(
          text = location,
          style = typography.labelMedium,
          color = colorScheme.primary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

/**
 * Assignee pill displayed under the description.
 *
 * @param assignee The user assigned to the report.
 * @param onProfile Callback when the user clicks on the assignee's profile.
 */
@Composable
private fun ReportAssigneeDetailsCard(
    assignee: SimpleUser,
    onProfile: (Id) -> Unit = {},
) {
  Card(
      shape = RoundedCornerShape(32.dp),
      colors = CardDefaults.cardColors(containerColor = colorScheme.background),
      border = BorderStroke(width = 1.dp, color = colorScheme.primary),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 6.dp)
              .testTag(ReportDetailsScreenTestTags.ASSIGNEE_CARD),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      ClickableProfilePicture(
          modifier = Modifier.size(32.dp),
          profileId = assignee.userId,
          profilePictureURL = assignee.profilePictureURL,
          profileUserType = assignee.userType,
          onProfile = onProfile,
      )
      val label =
          stringResource(
              id = R.string.report_details_assigned_to,
              assignee.username,
          )
      Text(
          text = label,
          style = typography.titleMedium,
          color = colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f).testTag(ReportDetailsScreenTestTags.ASSIGNEE_TEXT),
      )
    }
  }
}

/**
 * One comment row for report details.
 *
 * @param commentUI The comment UI data to display.
 * @param onProfile Callback when the user clicks on the comment author's profile.
 */
@Composable
private fun ReportCommentRow(
    commentUI: ReportCommentWithAuthorUI,
    onProfile: (Id) -> Unit = {},
) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 6.dp)
              .testTag(ReportDetailsScreenTestTags.COMMENT_CARD),
      shape = RoundedCornerShape(32.dp),
      colors = CardDefaults.cardColors(containerColor = colorScheme.background),
      border = BorderStroke(1.dp, colorScheme.primary),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
      ClickableProfilePicture(
          modifier = Modifier.size(40.dp),
          profileId = commentUI.author.userId,
          profilePictureURL = commentUI.author.profilePictureURL,
          profileUserType = commentUI.author.userType,
          onProfile = onProfile,
      )

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = commentUI.author.username,
              style = typography.labelLarge,
              color = colorScheme.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f).testTag(ReportDetailsScreenTestTags.COMMENT_AUTHOR),
          )
          Text(
              text = commentUI.date,
              style = typography.labelSmall,
              color = colorScheme.onBackground.copy(alpha = 0.7f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag(ReportDetailsScreenTestTags.COMMENT_DATE),
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        ExpandableTextCore(
            text = commentUI.text,
            collapsedLines = 3,
            bodyTag = ReportDetailsScreenTestTags.COMMENT_BODY,
            toggleTag = ReportDetailsScreenTestTags.COMMENT_TOGGLE,
        )
      }
    }
  }
}

/**
 * Bottom pinned comment input.
 *
 * @param user The current user adding the comment.
 * @param onProfile Callback when the user clicks on their profile picture.
 * @param onSend Callback when the user sends a comment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportCommentInput(
    user: SimpleUser,
    onProfile: (Id) -> Unit = {},
    onSend: (String) -> Unit = {},
) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .background(colorScheme.background)
              .padding(horizontal = 12.dp, vertical = 8.dp)
              .testTag(ReportDetailsScreenTestTags.COMMENT_INPUT_BAR),
  ) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      ClickableProfilePicture(
          modifier = Modifier.size(44.dp),
          profileId = user.userId,
          profilePictureURL = user.profilePictureURL,
          profileUserType = user.userType,
          onProfile = onProfile,
      )

      Spacer(modifier = Modifier.width(8.dp))

      var text by remember { mutableStateOf("") }

      OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          placeholder = { Text(context.getString(R.string.report_details_add_comment)) },
          modifier = Modifier.weight(1f).testTag(ReportDetailsScreenTestTags.COMMENT_INPUT_FIELD),
          shape = RoundedCornerShape(32.dp),
          singleLine = true,
          trailingIcon = {
            IconButton(
                onClick = {
                  if (text.isNotBlank()) {
                    onSend(text)
                    text = ""
                  }
                },
                modifier = Modifier.testTag(ReportDetailsScreenTestTags.COMMENT_INPUT_SEND),
            ) {
              Icon(
                  imageVector = Icons.AutoMirrored.Filled.Send,
                  contentDescription = "Send comment",
                  tint = colorScheme.primary,
              )
            }
          },
      )
    }
  }
}
