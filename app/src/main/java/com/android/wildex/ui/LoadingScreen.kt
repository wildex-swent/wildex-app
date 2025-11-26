package com.android.wildex.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.wildex.R

object LoadingScreenTestTags {
  const val LOADING_SCREEN = "loading_screen"
  const val LOADING_FAIL = "loading_fail"
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier, pd: PaddingValues = PaddingValues(0.dp)) {
  Box(
      modifier = modifier.fillMaxSize().padding(pd).testTag(LoadingScreenTestTags.LOADING_SCREEN),
      contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator()
  }
}

@Composable
fun LoadingFail(modifier: Modifier = Modifier, pd: PaddingValues = PaddingValues(0.dp)) {
  Box(
      modifier = modifier.fillMaxSize().padding(pd).testTag(LoadingScreenTestTags.LOADING_FAIL),
      contentAlignment = Alignment.Center,
  ) {
    Text(
        text = LocalContext.current.getString(R.string.fail_loading),
        color = colorScheme.onBackground,
        style = typography.titleLarge,
    )
  }
}
