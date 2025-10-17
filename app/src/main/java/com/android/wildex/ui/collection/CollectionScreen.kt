package com.android.wildex.ui.collection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.android.wildex.R

@Composable
fun CollectionScreen(bottomBar: @Composable () -> Unit) {
    Scaffold(bottomBar = { bottomBar() }) { innerPadding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            Text(stringResource(R.string.not_implemented), textAlign = TextAlign.Center)
        }
    }
}
