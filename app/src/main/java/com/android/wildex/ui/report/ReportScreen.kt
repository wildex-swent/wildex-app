package com.android.wildex.ui.report

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.wildex.AppTheme
import com.android.wildex.R
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.navigation.TopLevelTopBar
import com.android.wildex.ui.profile.OfflineAwareMiniMap
import com.android.wildex.ui.utils.ClickableProfilePicture
import com.android.wildex.ui.utils.expand.ExpandableTextCore
import com.mapbox.geojson.Point

/** Test tag constants used for UI testing of CollectionScreen components. */
object ReportScreenTestTags {
  const val NO_REPORT_TEXT = "report_screen_no_report_text"
  const val REPORT_LIST = "report_screen_report_list"
  const val MORE_ACTIONS_BUTTON = "report_screen_more_actions_button"
  const val SUBMIT_REPORT_BUTTON = "report_screen_submit_report"
  const val PULL_TO_REFRESH = "report_screen_pull_to_refresh"

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
 * @param onCurrentProfileClick The function to be called when the current user's profile picture is
 *   clicked
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
    onCurrentProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onReportClick: (Id) -> Unit = {},
    onSubmitReportClick: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
) {
  val context = LocalContext.current
  val uiState by reportScreenViewModel.uiState.collectAsState()
  val connectivityObserver = LocalConnectivityObserver.current
  val isOnline by connectivityObserver.isOnline.collectAsState()

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
        val user = uiState.currentUser
        TopLevelTopBar(
            currentUser = user,
            title =
                when (user.userType) {
                  UserType.REGULAR -> stringResource(R.string.report_title_regular)
                  UserType.PROFESSIONAL -> stringResource(R.string.report_title_professional)
                },
            onNotificationClick = onNotificationClick,
            onProfilePictureClick = onCurrentProfileClick,
        )
      },
  ) { innerPadding ->
    val pullState = rememberPullToRefreshState()

    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      PullToRefreshBox(
          state = pullState,
          isRefreshing = uiState.isRefreshing,
          onRefresh = {
            if (isOnline) reportScreenViewModel.refreshUIState()
            else reportScreenViewModel.refreshOffline()
          },
          modifier = Modifier.testTag(ReportScreenTestTags.PULL_TO_REFRESH),
      ) {
        when {
          uiState.isError -> LoadingFail()
          uiState.isLoading -> LoadingScreen()
          uiState.reports.isEmpty() -> NoReportsView()
          else -> {
            ReportsView(
                reports = uiState.reports,
                userId = uiState.currentUser.userId,
                onProfileClick = onProfileClick,
                onReportClick = onReportClick,
            )
          }
        }
      }
      // Hidden buttons
      Box(
          modifier =
              Modifier.align(Alignment.BottomEnd).padding(horizontal = 8.dp, vertical = 8.dp)) {
            ReportScreenButtons(
                onSubmitReportClick = onSubmitReportClick,
                context = context,
            )
          }
    }
  }
}

/**
 * A composable that displays a list of reports.
 *
 * @param reports The list of reports to be displayed.
 * @param userId The ID of the user.
 * @param onProfileClick The function to be called when a profile picture is clicked.
 * @param onReportClick The function to be called when a report is clicked.
 */
@Composable
private fun ReportsView(
    reports: List<ReportUIState> = emptyList(),
    userId: Id = "",
    onProfileClick: (Id) -> Unit = {},
    onReportClick: (Id) -> Unit = {},
) {
  LazyColumn(
      modifier = Modifier.fillMaxSize().testTag(ReportScreenTestTags.REPORT_LIST),
      verticalArrangement = Arrangement.spacedBy(2.dp),
      contentPadding = PaddingValues(top = 2.dp, bottom = 80.dp),
  ) {
    items(reports.size) { index ->
      ReportItem(
          reportState = reports[index],
          userId = userId,
          onProfileClick = onProfileClick,
          onReportClick = onReportClick,
      )
    }
  }
}

/**
 * A composable that displays a single report.
 *
 * @param reportState The state of the report to be displayed.
 * @param userId The ID of the user.
 * @param onProfileClick The function to be called when the profile picture of the author is
 *   clicked.
 * @param onReportClick The function to be called when the report is clicked.
 */
@Composable
private fun ReportItem(
    reportState: ReportUIState,
    userId: Id = "",
    onProfileClick: (Id) -> Unit = {},
    onReportClick: (Id) -> Unit = {},
) {
  val author = reportState.author
  val statusColor = if (reportState.assigned) colorScheme.primary else colorScheme.error
  val statusTextColor = if (reportState.assigned) colorScheme.onPrimary else colorScheme.onError
  val pagerState = rememberPagerState(pageCount = { 2 })
  Card(
      shape = RoundedCornerShape(20.dp),
      colors =
          CardDefaults.cardColors(
              containerColor = colorScheme.surface,
          ),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 6.dp)
              .testTag(ReportScreenTestTags.testTagForReport(reportState.reportId, "full")),
  ) {
    // Header: Profile picture + report author + date + status pill
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      // Profile picture
      ClickableProfilePicture(
          modifier =
              Modifier.size(40.dp)
                  .testTag(
                      ReportScreenTestTags.testTagForProfilePicture(
                          profileId = author.userId,
                          role = "author",
                      )),
          profileId = author.userId,
          profilePictureURL = author.profilePictureURL,
          profileUserType = author.userType,
          onProfile = onProfileClick,
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        // Author
        Text(
            text =
                LocalContext.current.getString(R.string.report_author) +
                    " " +
                    if (author.userId == userId) {
                      LocalContext.current.getString(R.string.report_author_current)
                    } else {
                      author.username
                    },
            style = typography.titleMedium,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // Date
        Text(
            text = reportState.date,
            style = typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      // Status pill (Assigned / Open)
      Card(
          shape = RoundedCornerShape(50),
          colors = CardDefaults.cardColors(containerColor = statusColor),
      ) {
        Text(
            text =
                if (reportState.assigned) {
                  LocalContext.current.getString(R.string.report_assigned)
                } else {
                  LocalContext.current.getString(R.string.report_open)
                },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = statusTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      }
    }
    // Image and map
    ReportSlider(
        reportState = reportState,
        onReportClick = { onReportClick(reportState.reportId) },
        pagerState = pagerState,
    )
    // Slider indicators
    Row(
        modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      SlideState(0, pagerState.currentPage)
      Spacer(modifier = Modifier.width(5.dp))
      SlideState(1, pagerState.currentPage)
    }
    // Description + Location chip
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Description
      ExpandableTextCore(
          text = reportState.description,
          collapsedLines = 4,
      )
      // Location chip
      Row(
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
            modifier =
                Modifier.clip(RoundedCornerShape(50))
                    .background(colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = "Location",
              tint = colorScheme.primary,
              modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(
              text = reportState.location.generalName,
              style = typography.labelMedium,
              color = colorScheme.onSurface,
              maxLines = 1,
              softWrap = true,
              overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
  Spacer(modifier = Modifier.height(6.dp))
}

/**
 * Displays a reports's image and the location on the map
 *
 * @param reportState The report whose image is to be displayed.
 * @param onReportClick The action when the user clicks on the report, to see its details.
 */
@Composable
private fun ReportSlider(
    reportState: ReportUIState,
    onReportClick: () -> Unit,
    pagerState: PagerState,
) {
  HorizontalPager(
      state = pagerState,
      modifier =
          Modifier.fillMaxWidth()
              .height(300.dp)
              .testTag(ReportScreenTestTags.testTagForReport(reportState.reportId, "slider")),
  ) { page ->
    when (page) {
      0 -> {
        AsyncImage(
            model = reportState.imageURL,
            contentDescription = "Report picture",
            modifier =
                Modifier.fillMaxSize()
                    .testTag(ReportScreenTestTags.testTagForReport(reportState.reportId, "image"))
                    .clickable { onReportClick() },
            contentScale = ContentScale.Crop,
        )
      }
      1 -> {
        val context = LocalContext.current
        val isDark =
            when (AppTheme.appearanceMode) {
              AppearanceMode.DARK -> true
              AppearanceMode.LIGHT -> false
              AppearanceMode.AUTOMATIC -> isSystemInDarkTheme()
            }
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .testTag(ReportScreenTestTags.testTagForReport(reportState.reportId, "map"))) {
              OfflineAwareMiniMap(
                  modifier = Modifier.matchParentSize(),
                  pins =
                      listOf(
                          Point.fromLngLat(
                              reportState.location.longitude,
                              reportState.location.latitude,
                          )),
                  styleUri = context.getString(R.string.map_style),
                  styleImportId = context.getString(R.string.map_standard_import),
                  isDark = isDark,
                  fallbackZoom = 4.0,
              )
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier =
                      Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                          .clip(RoundedCornerShape(16.dp))
                          .background(colorScheme.surfaceVariant)
                          .padding(horizontal = 10.dp, vertical = 8.dp),
              ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = "Country Icon",
                    tint = colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = reportState.location.specificName,
                    style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
              }
              Box(
                  modifier =
                      Modifier.matchParentSize()
                          .clickable { onReportClick() }
                          .background(Color.Transparent))
            }
      }
    }
  }
}

/**
 * Small pager indicator dot representing one page in a pager.
 *
 * @param slideIndex Index of this dot.
 * @param currentPage Currently visible pager page.
 */
@Composable
private fun SlideState(slideIndex: Int, currentPage: Int) {
  Box(
      modifier =
          Modifier.size(if (currentPage == slideIndex) 7.dp else 5.dp)
              .clip(RoundedCornerShape(50.dp))
              .background(
                  color =
                      colorScheme.onBackground.copy(
                          alpha = if (currentPage == slideIndex) 0.9f else 0.6f)))
}

/** A composable that displays a message when there are no reports. */
@Composable
private fun NoReportsView() {
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

/**
 * A composable that displays the buttons at the bottom right of the report screen.
 *
 * @param onSubmitReportClick The function to be called when the submit report button is clicked.
 * @param context The context of the application.
 */
@Composable
private fun ReportScreenButtons(
    onSubmitReportClick: () -> Unit = {},
    context: Context,
) {
  var isExpanded by remember { mutableStateOf(false) }
  val rotation by animateFloatAsState(targetValue = if (isExpanded) 45f else 0f)

  Column(
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }) + expandVertically(),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }) + shrinkVertically(),
    ) {
      Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Submit Report button
        FloatingActionButton(
            onClick = onSubmitReportClick,
            containerColor = colorScheme.surfaceVariant,
        ) {
          Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            Icon(
                modifier = Modifier.testTag(ReportScreenTestTags.SUBMIT_REPORT_BUTTON),
                painter = painterResource(R.drawable.report_filled),
                contentDescription = "Submit Report",
                tint = colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = context.getString(R.string.submit_report),
                color = colorScheme.primary,
                style = typography.titleSmall,
            )
          }
        }
      }
    }
    // More actions button
    FloatingActionButton(
        modifier = Modifier.testTag(ReportScreenTestTags.MORE_ACTIONS_BUTTON),
        onClick = { isExpanded = !isExpanded },
        containerColor = colorScheme.surfaceVariant,
    ) {
      Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Expand Actions",
          modifier = Modifier.rotate(rotation),
          tint = colorScheme.primary,
      )
    }
  }
}
