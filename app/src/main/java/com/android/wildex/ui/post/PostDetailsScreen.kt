package com.android.wildex.ui.post

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.wildex.R
import com.android.wildex.model.LocalConnectivityObserver
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.URL
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen
import com.android.wildex.ui.navigation.NavigationTestTags
import com.android.wildex.ui.post.PostDetailsScreenTestTags.testTagForProfilePicture
import com.android.wildex.ui.utils.ClickableProfilePicture
import com.android.wildex.ui.utils.offline.OfflineScreen

object PostDetailsScreenTestTags {
  fun testTagForProfilePicture(profileId: String, role: String = ""): String {
    return if (role.isEmpty()) "ProfilePicture_$profileId" else "ProfilePicture_${role}_$profileId"
  }

  const val BACK_BUTTON = "backButton"
  const val DELETE_POST_DIALOG = "delete_post_dialog"
  const val DELETE_POST_CONFIRM_BUTTON = "delete_post_confirm_button"
  const val DELETE_POST_DISMISS_BUTTON = "delete_post_dismiss_button"
  const val PULL_TO_REFRESH = "pullToRefresh"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsScreen(
    postId: String,
    postDetailsScreenViewModel: PostDetailsScreenViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onProfile: (Id) -> Unit = {},
) {
  val context = LocalContext.current
  val uiState by postDetailsScreenViewModel.uiState.collectAsState()
  val connectivityObserver = LocalConnectivityObserver.current
  val isOnline by connectivityObserver.isOnline.collectAsState()
  var showActionSheet by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) { postDetailsScreenViewModel.loadPostDetails(postId) }

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      postDetailsScreenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.testTag(NavigationTestTags.POST_DETAILS_SCREEN),
      topBar = {
        PostDetailsTopBar(
            onGoBack = onGoBack,
            onOpenActions = { showActionSheet = true },
        )
      },
      bottomBar = {
        // Pinned comment input – content scrolls behind it
        CommentInput(
            userId = uiState.currentUserId,
            userProfilePictureURL = uiState.currentUserProfilePictureURL,
            userUserType = uiState.currentUserUserType,
            onProfile = onProfile,
            postDetailsScreenViewModel = postDetailsScreenViewModel,
            isOnline = isOnline,
        )
      },
  ) { innerPadding ->
    if (isOnline) {
      PostDetailsScreenContent(
          innerPadding = innerPadding,
          uiState = uiState,
          postDetailsScreenViewModel = postDetailsScreenViewModel,
          postId = postId,
          onProfile = onProfile,
          onGoBack = onGoBack,
          showActionSheet = showActionSheet,
          onDismissActionSheet = { showActionSheet = false },
      )
    } else {
      OfflineScreen(innerPadding = innerPadding)
    }
  }
}

@Composable
fun PostDetailsScreenContent(
    innerPadding: PaddingValues,
    uiState: PostDetailsUIState,
    postDetailsScreenViewModel: PostDetailsScreenViewModel,
    postId: Id,
    onProfile: (Id) -> Unit,
    onGoBack: () -> Unit,
    showActionSheet: Boolean = false,
    onDismissActionSheet: () -> Unit,
) {
  val context = LocalContext.current
  val pullState = rememberPullToRefreshState()
  var showDeletionValidation by remember { mutableStateOf(false) }

  if (showActionSheet) {
    PostDetailsActions(
        postLocation = uiState.location,
        onDeletePressed = { showDeletionValidation = true },
        onDismissRequest = onDismissActionSheet,
        isAuthor = (uiState.currentUserId == uiState.authorId),
    )
  }

  PullToRefreshBox(
      state = pullState,
      isRefreshing = uiState.isRefreshing,
      modifier = Modifier.padding(innerPadding).testTag(PostDetailsScreenTestTags.PULL_TO_REFRESH),
      onRefresh = { postDetailsScreenViewModel.refreshPostDetails(postId) },
  ) {
    when {
      uiState.isError -> LoadingFail()
      uiState.isLoading -> LoadingScreen()
      else -> {
        PostDetailsContent(
            uiState = uiState,
            postDetailsScreenViewModel = postDetailsScreenViewModel,
            onProfile = onProfile,
        )
        if (showDeletionValidation) {
          AlertDialog(
              onDismissRequest = { showDeletionValidation = false },
              containerColor = colorScheme.background,
              title = {
                Text(
                    text = context.getString(R.string.post_details_delete_post),
                    style = typography.titleLarge,
                )
              },
              text = {
                Text(
                    text = context.getString(R.string.post_details_delete_post_confirmation),
                    style = typography.bodyMedium,
                )
              },
              tonalElevation = 4.dp,
              modifier = Modifier.testTag(PostDetailsScreenTestTags.DELETE_POST_DIALOG),
              confirmButton = {
                TextButton(
                    modifier =
                        Modifier.testTag(PostDetailsScreenTestTags.DELETE_POST_CONFIRM_BUTTON),
                    onClick = {
                      showDeletionValidation = false
                      postDetailsScreenViewModel.removePost(postId)
                      onGoBack()
                    },
                ) {
                  Text(
                      text = context.getString(R.string.post_details_final_delete_post),
                      color = colorScheme.error,
                      style = typography.bodyMedium,
                  )
                }
              },
              dismissButton = {
                TextButton(
                    modifier =
                        Modifier.testTag(PostDetailsScreenTestTags.DELETE_POST_DISMISS_BUTTON),
                    onClick = { showDeletionValidation = false },
                ) {
                  Text(
                      text = context.getString(R.string.post_details_cancel_delete_post),
                      style = typography.bodyMedium,
                  )
                }
              },
          )
        }
      }
    }
  }
}

// ---------- Bottom input (pinned) ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentInput(
    userId: Id = "",
    userProfilePictureURL: URL = "",
    userUserType: UserType = UserType.REGULAR,
    onProfile: (Id) -> Unit = {},
    postDetailsScreenViewModel: PostDetailsScreenViewModel,
    isOnline: Boolean = true
) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .background(colorScheme.background)
              .border(
                  width = 1.dp,
                  color = colorScheme.onBackground.copy(alpha = 0.06f),
                  shape = RoundedCornerShape(0.dp),
              )
              .padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          ClickableProfilePicture(
              modifier =
                  Modifier.size(44.dp)
                      .testTag(
                          testTagForProfilePicture(profileId = userId, role = "comment_input")),
              profileId = userId,
              profilePictureURL = userProfilePictureURL,
              profileUserType = userUserType,
              onProfile = onProfile,
          )

          Spacer(modifier = Modifier.width(8.dp))

          var text by remember { mutableStateOf("") }

          OutlinedTextField(
              value = text,
              enabled = isOnline,
              onValueChange = { text = it },
              placeholder = { Text(text = "Add a comment …", style = typography.bodyMedium) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(32.dp),
              singleLine = true,
              trailingIcon = {
                IconButton(
                    onClick = {
                      if (text.isNotBlank()) {
                        postDetailsScreenViewModel.addComment(text)
                        text = ""
                      }
                    }) {
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
