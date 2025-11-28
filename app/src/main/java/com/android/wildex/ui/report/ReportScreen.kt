package com.android.wildex.ui.report

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.R
import com.android.wildex.model.DefaultConnectivityObserver
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.utils.ClickableProfilePicture
import com.android.wildex.ui.utils.offline.OfflineScreen

/** Test tag constants used for UI testing of CollectionScreen components. */
object ReportScreenTestTags {
  const val NOTIFICATION_BUTTON = "report_screen_notification_button"
  const val NO_REPORT_TEXT = "report_screen_no_report_text"
  const val SCREEN_TITLE = "report_screen_title"
  const val REPORT_LIST = "report_screen_report_list"
  const val SUBMIT_REPORT = "report_screen_submit_report"

  fun testTagForReport(reportId: Id, element: String): String =
      "ReportScreen_report_${reportId}_$element"

  fun testTagForProfilePicture(profileId: String, role: String = ""): String {
    return if (role.isEmpty()) "ProfilePicture_$profileId" else "ProfilePicture_${role}_$profileId"
  }
}

/**
 * A composable that displays the report screen.
 *
 * @param reportScreenViewModel The view model for the report screen.
 * @param onProfileClick The function to be called when a profile picture is clicked.
 * @param onNotificationClick The function to be called when the notification button is clicked.
 * @param onReportClick The function to be called when a report is clicked.
 * @param onSubmitReportClick The function to be called when the submit report button is clicked.
 * @param bottomBar The bottom bar to be displayed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    reportScreenViewModel: ReportScreenViewModel = viewModel(),
    onProfileClick: (Id) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onReportClick: (Id) -> Unit = {},
    onSubmitReportClick: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
) {
  val context = LocalContext.current
  val connectivityObserver = remember { DefaultConnectivityObserver(context) }
  val uiState by reportScreenViewModel.uiState.collectAsState()
  val isOnlineObs by connectivityObserver.isOnline.collectAsState()
  val isOnline = isOnlineObs && LocalConnectivityObserver.current

  LaunchedEffect(Unit) { reportScreenViewModel.loadUIState() }
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      reportScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(NavigationTestTags.REPORT_SCREEN),
      bottomBar = { bottomBar() },
      topBar = {
        ReportScreenTopBar(
            userId = uiState.currentUser.userId,
            userType = uiState.currentUser.userType,
            userProfilePictureURL = uiState.currentUser.profilePictureURL,
            onProfileClick = onProfileClick,
            onNotificationClick = onNotificationClick,
        )
      },
  ) { innerPadding ->
    if (isOnline) {
      ReportScreenContent(
          innerPadding = innerPadding,
          uiState = uiState,
          reportScreenViewModel = reportScreenViewModel,
          context = context,
          onProfileClick = onProfileClick,
          onReportClick = onReportClick,
          onSubmitReportClick = onSubmitReportClick,
      )
    } else {
      OfflineScreen(innerPadding = innerPadding)
    }
  }
}

/**
 * Displays the content of the report screen for when the user is online.
 *
 * @param innerPadding The padding values for the inner content.
 * @param uiState The UI state of the report screen.
 * @param viewModel The view model for the report screen.
 * @param context The context of the application.
 * @param onProfileClick The function to be called when a profile picture is clicked.
 * @param onReportClick The function to be called when a report is clicked.
 * @param onSubmitReportClick The function to be called when the submit report button is clicked.
 */
@Composable
fun ReportScreenContent(
    innerPadding: PaddingValues,
    uiState: ReportScreenUIState,
    reportScreenViewModel: ReportScreenViewModel,
    context: Context,
    onProfileClick: (Id) -> Unit,
    onReportClick: (Id) -> Unit,
    onSubmitReportClick: () -> Unit,
) {
  val pullState = rememberPullToRefreshState()

  Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
    PullToRefreshBox(
        state = pullState,
        isRefreshing = uiState.isRefreshing,
        onRefresh = { reportScreenViewModel.refreshUIState() },
    ) {
      when {
        uiState.isError -> LoadingFail()
        uiState.isLoading -> LoadingScreen()
        uiState.reports.isEmpty() -> NoReportsView()
        else -> {
          ReportsView(
              reports = uiState.reports,
              userId = uiState.currentUser.userId,
              username = uiState.currentUser.username,
              userType = uiState.currentUser.userType,
              onProfileClick = onProfileClick,
              onReportClick = onReportClick,
              cancelReport = reportScreenViewModel::cancelReport,
              selfAssignReport = reportScreenViewModel::selfAssignReport,
              resolveReport = reportScreenViewModel::resolveReport,
              unSelfAssignReport = reportScreenViewModel::unselfAssignReport,
          )
        }
      }
    }
    // Submit Report button
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.onBackground),
        border = BorderStroke(width = 8.dp, color = colorScheme.onBackground.copy(alpha = 0.28f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .clickable { onSubmitReportClick() }
                .testTag(ReportScreenTestTags.SUBMIT_REPORT),
    ) {
      Text(
          text = context.getString(R.string.submit_report),
          color = colorScheme.background,
          style = typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      )
    }
  }
}

/**
 * A composable that displays a list of reports.
 *
 * @param reports The list of reports to be displayed.
 * @param userId The ID of the user.
 * @param username The username of the user.
 * @param userType The type of the user.
 * @param onProfileClick The function to be called when a profile picture is clicked.
 * @param onReportClick The function to be called when a report is clicked.
 * @param cancelReport The function to be called when a report is cancelled.
 * @param selfAssignReport The function to be called when a report is self-assigned.
 * @param resolveReport The function to be called when a report is resolved.
 * @param unSelfAssignReport The function to be called when a report is unassigned.
 */
@Composable
fun ReportsView(
    reports: List<ReportUIState> = emptyList(),
    userId: Id = "",
    username: String = "",
    userType: UserType = UserType.REGULAR,
    onProfileClick: (Id) -> Unit = {},
    onReportClick: (Id) -> Unit = {},
    cancelReport: (Id) -> Unit = {},
    selfAssignReport: (Id) -> Unit = {},
    resolveReport: (Id) -> Unit = {},
    unSelfAssignReport: (Id) -> Unit = {},
) {
  LazyColumn(
      modifier = Modifier.fillMaxSize().testTag(ReportScreenTestTags.REPORT_LIST),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
  ) {
    items(reports.size) { index ->
      ReportItem(
          reportState = reports[index],
          userId = userId,
          username = username,
          userType = userType,
          onProfileClick = onProfileClick,
          onReportClick = onReportClick,
          cancelReport = cancelReport,
          selfAssignReport = selfAssignReport,
          resolveReport = resolveReport,
          unSelfAssignReport = unSelfAssignReport,
      )
    }
  }
}

/**
 * A composable that displays a single report.
 *
 * @param reportState The state of the report to be displayed.
 * @param userId The ID of the user.
 * @param username The username of the user.
 * @param userType The type of the user.
 * @param onReportClick The function to be called when the report is clicked.
 * @param onProfileClick The function to be called when the profile picture of the author is
 *   clicked.
 * @param cancelReport The function to be called when the cancel report button is clicked.
 * @param selfAssignReport The function to be called when the self assign report button is clicked.
 * @param resolveReport The function to be called when the resolve report button is clicked.
 * @param unSelfAssignReport The function to be called when the unself assign report button is
 *   clicked.
 */
@Composable
fun ReportItem(
    reportState: ReportUIState,
    userId: Id = "",
    username: String = "",
    userType: UserType = UserType.REGULAR,
    onProfileClick: (Id) -> Unit = {},
    onReportClick: (Id) -> Unit = {},
    cancelReport: (Id) -> Unit = {},
    selfAssignReport: (Id) -> Unit = {},
    resolveReport: (Id) -> Unit = {},
    unSelfAssignReport: (Id) -> Unit = {},
) {
  val author = reportState.author
  Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colorScheme.background),
      border = BorderStroke(width = 1.dp, color = colorScheme.onBackground.copy(alpha = 0.28f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp)
              .testTag(ReportScreenTestTags.testTagForReport(reportState.reportId, "full")),
  ) {
    // Header: Profile picture + report author + date + location
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      ClickableProfilePicture(
          modifier =
              Modifier.size(40.dp)
                  .testTag(
                      ReportScreenTestTags.testTagForProfilePicture(
                          profileId = author.userId, role = "author")),
          profileId = author.userId,
          profilePictureURL = author.profilePictureURL,
          profileUserType = author.userType,
          onProfile = onProfileClick,
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
            text =
                LocalContext.current.getString(R.string.report_author) +
                    " " +
                    when (userType) {
                      UserType.REGULAR ->
                          LocalContext.current.getString(R.string.report_author_current)
                      UserType.PROFESSIONAL -> {
                        if (author.userId == userId) {
                          LocalContext.current.getString(R.string.report_author_current)
                        } else {
                          author.username
                        }
                      }
                    },
            style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
              text = reportState.date,
              style = typography.labelSmall,
              color = colorScheme.onBackground,
          )
          Row(
              modifier = Modifier.fillMaxWidth(.4f),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                modifier = Modifier.size(13.dp).offset(y = (-1).dp),
                tint = colorScheme.onBackground,
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = reportState.location,
                style = typography.labelMedium,
                color = colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
    // Image
    Box(
        modifier =
            Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onReportClick(reportState.reportId) }) {
          AsyncImage(
              model = reportState.imageURL,
              contentDescription = "Report Image",
              modifier = Modifier.fillMaxWidth(),
              contentScale = ContentScale.FillWidth,
          )
        }
    // Description
    Text(
        text = reportState.description,
        color = colorScheme.onBackground,
        style = typography.bodyMedium,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        softWrap = true,
    )
    // Buttons
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
      when (userType) {
        UserType.REGULAR -> {
          if (reportState.assigneeUsername.isNotEmpty())
              ReportAssigneeCard(
                  assigneeUsername = reportState.assigneeUsername,
                  modifier = Modifier.weight(1f),
              )
          CancelReportButton(
              reportId = reportState.reportId,
              cancelReport = cancelReport,
              modifier = Modifier.weight(1f),
          )
        }
        UserType.PROFESSIONAL -> {
          if (reportState.assigneeUsername.isEmpty()) {
            SelfAssignButton(
                reportId = reportState.reportId,
                selfAssignReport = selfAssignReport,
                modifier = Modifier.weight(1f),
            )
            if (author.userId == userId)
                CancelReportButton(
                    reportId = reportState.reportId,
                    cancelReport = cancelReport,
                    modifier = Modifier.weight(1f),
                )
          } else if (reportState.assigneeUsername == username) {
            ResolveReportButton(
                reportId = reportState.reportId,
                resolveReport = resolveReport,
                modifier = Modifier.weight(1f),
            )
            UnSelfAssignReportButton(
                reportId = reportState.reportId,
                unSelfAssignReport = unSelfAssignReport,
                modifier = Modifier.weight(1f),
            )
          } else {
            ReportAssigneeCard(
                assigneeUsername = reportState.assigneeUsername,
                modifier = Modifier.weight(1f),
            )
            if (author.userId == userId)
                CancelReportButton(
                    reportId = reportState.reportId,
                    cancelReport = cancelReport,
                    modifier = Modifier.weight(1f),
                )
          }
        }
      }
    }
  }
}

/**
 * A composable that displays a cancel report button.
 *
 * @param reportId The ID of the report.
 * @param cancelReport The function to be called when the cancel report button is clicked.
 */
@Composable
fun CancelReportButton(
    modifier: Modifier = Modifier,
    reportId: Id = "",
    cancelReport: (Id) -> Unit = {},
) {
  Card(
      shape = RoundedCornerShape(50.dp),
      colors = CardDefaults.cardColors(containerColor = colorScheme.onBackground),
      border = BorderStroke(width = 1.dp, color = colorScheme.onBackground.copy(alpha = 0.28f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      modifier = modifier.padding(horizontal = 16.dp).clickable { cancelReport(reportId) },
  ) {
    Text(
        text = LocalContext.current.getString(R.string.cancel_report),
        color = colorScheme.background,
        style = typography.bodyMedium,
        modifier =
            Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                .align(Alignment.CenterHorizontally),
    )
  }
}

/**
 * A composable that displays a self assign report button.
 *
 * @param reportId The ID of the report.
 * @param selfAssignReport The function to be called when the self-assign report button is clicked.
 */
@Composable
fun SelfAssignButton(
    modifier: Modifier = Modifier,
    reportId: Id = "",
    selfAssignReport: (Id) -> Unit = {},
) {
  Card(
      shape = RoundedCornerShape(50.dp),
      colors = CardDefaults.cardColors(containerColor = colorScheme.onBackground),
      border = BorderStroke(width = 1.dp, color = colorScheme.onBackground.copy(alpha = 0.28f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      modifier = modifier.padding(horizontal = 16.dp).clickable { selfAssignReport(reportId) },
  ) {
    Text(
        text = LocalContext.current.getString(R.string.self_assign_report),
        color = colorScheme.background,
        style = typography.bodyMedium,
        modifier =
            Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                .align(Alignment.CenterHorizontally),
    )
  }
}

/**
 * A composable that displays a resolve report button.
 *
 * @param reportId The ID of the report.
 * @param resolveReport The function to be called when the resolve report button is clicked.
 */
@Composable
fun ResolveReportButton(
    modifier: Modifier = Modifier,
    reportId: Id = "",
    resolveReport: (Id) -> Unit = {},
) {
  Card(
      shape = RoundedCornerShape(50.dp),
      colors = CardDefaults.cardColors(containerColor = colorScheme.onBackground),
      border = BorderStroke(width = 1.dp, color = colorScheme.onBackground.copy(alpha = 0.28f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      modifier = modifier.padding(horizontal = 16.dp).clickable { resolveReport(reportId) },
  ) {
    Text(
        text = LocalContext.current.getString(R.string.resolve_report),
        color = colorScheme.background,
        style = typography.bodyMedium,
        modifier =
            Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                .align(Alignment.CenterHorizontally),
    )
  }
}

/**
 * A composable that displays an unself-assign report button.
 *
 * @param reportId The ID of the report.
 * @param unSelfAssignReport The function to be called when the unself assign report button is
 */
@Composable
fun UnSelfAssignReportButton(
    modifier: Modifier = Modifier,
    reportId: Id = "",
    unSelfAssignReport: (Id) -> Unit = {},
) {
  Card(
      shape = RoundedCornerShape(50.dp),
      colors = CardDefaults.cardColors(containerColor = colorScheme.onBackground),
      border = BorderStroke(width = 1.dp, color = colorScheme.onBackground.copy(alpha = 0.28f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      modifier = modifier.padding(horizontal = 16.dp).clickable { unSelfAssignReport(reportId) },
  ) {
    Text(
        text = LocalContext.current.getString(R.string.cancel_self_assigned_report),
        color = colorScheme.background,
        style = typography.bodyMedium,
        modifier =
            Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                .align(Alignment.CenterHorizontally),
    )
  }
}

/**
 * A composable that displays a report assignee card.
 *
 * @param assigneeUsername The username of the assignee.
 */
@Composable
fun ReportAssigneeCard(modifier: Modifier = Modifier, assigneeUsername: String = "") {
  Card(
      shape = RoundedCornerShape(8.dp),
      colors =
          CardDefaults.cardColors(containerColor = colorScheme.onBackground.copy(alpha = 0.6f)),
      border = BorderStroke(width = 1.dp, color = colorScheme.onBackground.copy(alpha = 0.28f)),
      modifier = modifier.padding(horizontal = 16.dp),
  ) {
    Text(
        text = LocalContext.current.getString(R.string.report_assignee) + " " + assigneeUsername,
        color = colorScheme.background,
        style = typography.bodyMedium,
        modifier =
            Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                .align(Alignment.CenterHorizontally),
    )
  }
}

/** A composable that displays a message when there are no reports. */
@Composable
fun NoReportsView() {
  Column(
      modifier = Modifier.fillMaxSize().padding(24.dp).testTag(ReportScreenTestTags.NO_REPORT_TEXT),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Icon(
        painter = painterResource(R.drawable.nothing_found),
        contentDescription = "Nothing Found",
        tint = colorScheme.onBackground,
        modifier = Modifier.size(96.dp),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = LocalContext.current.getString(R.string.no_reports),
        color = colorScheme.onBackground,
        style = typography.titleLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
  }
}
