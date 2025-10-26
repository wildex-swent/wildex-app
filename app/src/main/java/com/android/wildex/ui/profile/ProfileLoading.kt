package com.android.wildex.ui.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.android.wildex.ui.LoadingFail
import com.android.wildex.ui.LoadingScreen

@Composable
fun ProfileLoading(pd: PaddingValues) {
  LoadingScreen(pd)
}

@Composable
fun ProfileNotFound(pd: PaddingValues) {
  LoadingFail(pd)
}
