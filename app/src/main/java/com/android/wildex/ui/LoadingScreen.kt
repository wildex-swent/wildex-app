package com.android.wildex.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.android.wildex.R

@Composable
fun LoadingScreen(pd: PaddingValues) {
  Box(modifier = Modifier.fillMaxSize().padding(pd), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
  }
}

@Composable
fun LoadingFail(pd: PaddingValues) {
  Box(modifier = Modifier.fillMaxSize().padding(pd), contentAlignment = Alignment.Center) {
    Text(
        text = LocalContext.current.getString(R.string.fail_loading),
        color = colorScheme.onBackground)
  }
}
